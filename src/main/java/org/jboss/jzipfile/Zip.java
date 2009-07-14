/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.jzipfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.DataInput;
import java.io.FileOutputStream;
import java.io.Closeable;
import java.util.zip.ZipException;
import java.util.zip.InflaterInputStream;
import java.util.zip.Inflater;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import static java.lang.Math.min;
import static java.lang.Math.max;
import com.jcraft.jzlib.ZInputStream;

/**
 * Zip file manipulation methods.
 */
public final class Zip {

    private Zip() {
    }

    /**
     * Read the zip catalog of the given file.
     *
     * @param file the file to read
     * @return the built catalog
     * @throws IOException if an I/O error occurs
     */
    public static ZipCatalog readCatalog(File file) throws IOException {
        return readCatalog(findCatalog(file));
    }

    /**
     * Find the zip catalog for the given file.  The returned input stream is positioned at the start of the zip
     * directory structure.
     *
     * @param file the file to examine
     * @return an input stream positioned at the start of the catalog
     * @throws IOException if an I/O error occurs
     */
    public static InputStream findCatalog(File file) throws IOException {
        boolean ok = false;
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        try {
            final long len = raf.length();
            if (len < 22L) {
                throw new ZipException("The provided file is too short to hold even one end-of-central-directory record");
            }
            // First, check at len-22 in the (common) case that there is no zip file comment.
            raf.seek(len - 22);
            if (! catScan(raf, 0)) {
                // OK, let's back off incrementally, starting from 64 bytes out and going up by a factor of 4 each time
                int spos = 64;
                int lim = 64 - 22;
                if (len < 64) {
                    raf.seek(0);
                } else {
                    raf.seek(len - 64);
                }
                while (! catScan(raf, lim)) {
                    int newSpos = spos << 2;
                    lim = newSpos - spos;
                    spos = newSpos;
                    if (spos >= 65536) {
                        throw new ZipException("No directory found");
                    }
                    if (spos > len) {
                        // check from the very start of the file
                        spos = 65536;
                        raf.seek(0);
                    } else {
                        raf.seek(len - spos);
                    }
                }
            }
            // OK, the EOD was located.  Now read it to find the start of the directory
            final int diskNo = Short.reverseBytes(raf.readShort()) & 0xffff; // disk #
            final int cddNo = Short.reverseBytes(raf.readShort()) & 0xffff; // central dir disk #
            final int diskEC = Short.reverseBytes(raf.readShort()) & 0xffff; // entry count in central dir # on this disk
            final int totalEC = Short.reverseBytes(raf.readShort()) & 0xffff; // entry count in central dir #
            if (diskNo != cddNo || cddNo != 0) {
                throw new ZipException("Multi-disk zips not supported");
            }
            if (diskEC != totalEC) {
                throw new ZipException("Entry count inconsistency in end-of-directory record");
            }
            raf.readInt(); // size of central dir
            raf.seek(Integer.reverseBytes(raf.readInt())); // offset of central dir
            final RandomAccessInputStream is = new RandomAccessInputStream(raf);
            ok = true;
            return is;
        } finally {
            if (! ok) safeClose(raf);
        }
    }

    private static boolean catScan(DataInput input, int limit) throws IOException {
        // RAF uses big-endian... :-P
        int sig = Integer.reverseBytes(input.readInt());
        do {
            if (sig == 0x06054b50) {
                return true;
            }
            if (limit-- > 0) {
                sig = (sig >>> 8) | (input.readUnsignedByte() << 24);
            }
        } while (limit > 0);
        return false;
    }

    /**
     * Read the zip catalog referred to by the given input stream, which is pointed at the
     * start of the catalog (also known as the "central directory"), normally located near the <b>end</b> of the
     * zip file.  <b>Note:</b> the passed in {@code InputStream} will be used and closed by this method.
     *
     * @param inputStream the input stream from which to build a catalog
     * @return the built catalog
     * @throws IOException if an I/O error occurs
     */
    public static ZipCatalog readCatalog(InputStream inputStream) throws IOException {
        try {
            final ZipCatalogBuilder builder = new ZipCatalogBuilder();
            builder.readDirectory(inputStream);
            return builder.getZipCatalog();
        } finally {
            Zip.safeClose(inputStream);
        }
    }

    /**
     * Read the zip catalog of the given file.
     *
     * @param fileName the name of the file to read
     * @return the built catalog
     * @throws IOException if an I/O error occurs
     */
    public static ZipCatalog readCatalog(String fileName) throws IOException {
        return readCatalog(new File(fileName));
    }

    /**
     * Open a zip entry, returning an input stream which may be used to read the contents of the
     * entry.  Depending on how the entry is stored, the returned stream may or may not support
     * {@code mark/reset}.
     *
     * @param zipFile the zip file to access
     * @param zipEntry the zip entry from that file
     * @return an {@code InputStream} which may be used to read the zip file entry data
     * @throws IOException if an I/O error occurs
     */
    public static InputStream openEntry(File zipFile, ZipEntry zipEntry) throws IOException {
        final RandomAccessFile raf = new RandomAccessFile(zipFile, "r");
        boolean ok = false;
        try {
            raf.seek(zipEntry.getOffset());
            final InputStream is = openEntry(new RandomAccessInputStream(raf), zipEntry);
            ok = true;
            return is;
        } finally {
            if (! ok) Zip.safeClose(raf);
        }
    }

    /**
     * Open a zip entry.  The given input stream must be located at the start of the zip entry's local header.  When the
     * returned input stream is closed, the provided input stream will be closed as well.
     *
     * @param inputStream the input stream
     * @param zipEntry the zip entry
     * @return an uncompressing input stream
     * @throws IOException if an I/O error occurs
     */
    public static InputStream openEntry(InputStream inputStream, ZipEntry zipEntry) throws IOException {
        boolean ok = false;
        try {
            // read the local file header...
            final ZipDataInputStream zdis = inputStream instanceof ZipDataInputStream ? (ZipDataInputStream) inputStream : new ZipDataInputStream(inputStream);
            readLocalFileForEntry(zdis, zipEntry);
            ok = true;
            return openEntryData(inputStream, zipEntry);
        } finally {
            if (! ok) safeClose(inputStream);
        }
    }

    /**
     * Open a zip entry's raw data.  The given input stream must be located at the start of the zip entry's actual
     * compressed data (that is, after the local file header).  When the returned input stream is closed, the provided
     * input stream will be closed as well.
     *
     * @param inputStream the input stream
     * @param zipEntry the zip entry
     * @return an uncompressing input stream
     * @throws IOException if an I/O error occurs
     */
    public static InputStream openEntryData(InputStream inputStream, ZipEntry zipEntry) throws IOException {
        boolean ok = false;
        try {
            final ZipEntryType entryType = zipEntry.getEntryType();
            switch (entryType) {
                case FILE: break;
                default: {
                    throw new ZipException("Attempt to open a zip entry '" + zipEntry.getName() + "' with an unsupported type '" + entryType + "'");
                }
            }
            final ZipCompressionMethod compressionMethod = zipEntry.getCompressionMethod();
            switch (compressionMethod) {
                case STORE: {
                    final LimitedInputStream is = new LimitedInputStream(inputStream, zipEntry.getCompressedSize());
                    ok = true;
                    return is;
                }
                case DEFLATE: {
                    final LimitedInputStream is = new LimitedInputStream(new JZFInflaterStream(new LimitedInputStream(inputStream, zipEntry.getCompressedSize())), zipEntry.getSize());
                    ok = true;
                    return is;
                }
            }
            throw new ZipException("Unsupported compression algorithm " + compressionMethod);
        } finally {
            if (! ok) safeClose(inputStream);
        }
    }

    private static void readLocalFileForEntry(final ZipDataInputStream is, final ZipEntry entry) throws IOException {
        // main header
        final int sig = is.readInt();
        if (sig != 0x04034b50) {
            throw new ZipException("Corrupted zip entry (local file header signature is incorrect)");
        }
        final int extVers = is.readUnsignedShort();
        if (extVers > 20) {
            throw new ZipException("Entry requires a later version to extract");
        }
        is.readUnsignedShort(); // GP bits - may be needed for some methods?
        final ZipCompressionMethod method = ZipCompressionMethod.getMethod(is.readUnsignedShort());
        is.readInt(); // local mod time
        is.readInt(); // local crc32 (usually 0)
        is.readInt(); // compressed size (header) (usually 0)
        is.readInt(); // uncomp size (header) (usually 0)
        final int fnameLen = is.readUnsignedShort();
        final int extraLen = is.readUnsignedShort();
        final byte[] fileNameBytes = new byte[fnameLen];
        is.readFully(fileNameBytes);
        final ZipCompressionMethod expectedMethod = entry.getCompressionMethod();
        if (! expectedMethod.equals(method)) {
            throw new ZipException(String.format("Compression methods do not match (expected \"%s\", got \"%s\")", expectedMethod, method));
        }
        final String actualFileName = new String(fileNameBytes, "US-ASCII");
        final String expectFileName = entry.getName();
        if (! expectFileName.equals(actualFileName)) {
            throw new ZipException(String.format("File names do not match (expected \"%s\", got \"%s\")", expectFileName, actualFileName));
        }
        is.skipFully(extraLen);
    }

    /**
     * Extract a zip file (in entirety) to a destination directory.
     *
     * @param zipFile the zip file
     * @param destDir the destination directory
     * @throws IOException if an I/O error occurs
     */
    public static void extract(File zipFile, File destDir) throws IOException {
        if (! destDir.isDirectory()) {
            throw new IOException("Destination is not a directory");
        }
        final byte[] buf = new byte[65536];
        final ZipCatalog catalog = readCatalog(zipFile);
        final Set<String> createdPaths = new HashSet<String>(256);
        for (ZipEntry zipEntry : catalog.allEntries()) {
            final String name = zipEntry.getName();
            final ZipEntryType entryType = zipEntry.getEntryType();
            if (entryType == ZipEntryType.DIRECTORY) {
                for (String path : parentPaths(name)) {
                    if (createdPaths.add(path)) {
                        new File(destDir, path).mkdir();
                    }
                }
            } else if (entryType == ZipEntryType.FILE) {
                final File file = new File(destDir, name).getCanonicalFile();
                final Iterator<String> it = parentPaths(name).iterator();
                while (it.hasNext()) {
                    String path = it.next();
                    if (it.hasNext()) {
                        if (createdPaths.add(path)) {
                            new File(destDir, path).mkdir();
                        }
                    }
                }
                file.delete();
                final FileOutputStream fos = new FileOutputStream(file);
                try {
                    final InputStream inputStream = Zip.openEntry(zipFile, zipEntry);
                    try {
                        for (;;) {
                            final int cnt = inputStream.read(buf);
                            if (cnt == -1) {
                                inputStream.close();
                                fos.close();
                                break;
                            }
                            fos.write(buf, 0, cnt);
                        }
                    } finally {
                        safeClose(inputStream);
                    }
                } finally {
                    safeClose(fos);
                }
            } else {
                // skip unknown entry
            }
        }
    }

    private static Iterable<String> parentPaths(final String wholePath) {
        final int len = wholePath.length();
        int n = 0;
        while (n < len && wholePath.charAt(n) == '/') {
            n ++;
        }
        if (n == len) {
            return Collections.emptySet();
        }
        final int start = n;
        return new Iterable<String>() {
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    private int i = start;

                    public boolean hasNext() {
                        return i < len;
                    }

                    public String next() {
                        final int next = wholePath.indexOf('/', i);
                        if (next == -1) {
                            i = len;
                            return wholePath.substring(start);
                        } else {
                            i = next + 1;
                            return wholePath.substring(start, next);
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    static void safeClose(final Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (IOException e) {
            // eat
        }
    }

    static long getTimestamp(final int rawTime, final int rawDate) {
        final int hour = min(rawTime >> 11, 23);
        final int minute = min(rawTime >> 5 & 0x3f, 59);
        // two-second resolution
        final int second = min((rawTime & 0x1f) << 1, 59);
        final int year = 1980 + (rawDate >> 9);
        // Months are from 1-12
        final int month = max(1, min(12, rawDate >> 5 & 0x0f));
        // Days might roll over; if so, let the calendar deal with it
        final int day = rawDate & 0x1f;
        // convert to millis
        return new GregorianCalendar(year, month - 1, day, hour, minute, second).getTimeInMillis();
    }

    private static final class JZFInflaterStream extends InflaterInputStream {
        private final Inflater inf;

        JZFInflaterStream(InputStream in) {
            this(in, new Inflater(true));
        }

        public JZFInflaterStream(InputStream in, Inflater inf) {
            super(in, inf, 4096);
            this.inf = inf;
        }

        public void close() throws IOException {
            try {
                super.close();
            } finally {
                inf.end();
            }
        }
    }
}
