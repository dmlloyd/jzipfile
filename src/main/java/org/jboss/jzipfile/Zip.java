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
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.io.DataInput;
import java.io.FileOutputStream;
import java.io.Closeable;
import java.util.zip.ZipException;
import java.util.zip.InflaterInputStream;
import java.util.zip.Inflater;

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
                raf.seek(len - 64);
                while (! catScan(raf, lim)) {
                    int newSpos = spos << 2;
                    lim = newSpos - spos;
                    spos = newSpos;
                    if (spos >= 65536) {
                        throw new ZipException("No directory found");
                    }
                    raf.seek(len - spos);
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
     * start of the catalog (also known as the "central directory").  <b>Note:</b> the passed in
     * {@code InputStream} will be used and closed by this method.
     *
     * @param inputStream the input stream from which to build a catalog
     * @return the built catalog
     * @throws IOException if an I/O error occurs
     */
    public static ZipCatalog readCatalog(InputStream inputStream) throws IOException {
        final ZipCatalogBuilder builder = new ZipCatalogBuilder();
        builder.readDirectory(inputStream);
        return builder.getZipCatalog();
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
        final FileInputStream fis = new FileInputStream(zipFile);
        new ZipDataInputStream(fis).skipFully(zipEntry.getOffset());
        return openEntry(fis, zipEntry);
    }

    /**
     * Open a zip entry.  The given input stream must be located at the start of the zip entry's data.  When the
     * returned input stream is closed, the provided input stream will be closed as well.
     *
     * @param inputStream the input stream
     * @param zipEntry the zip entry
     * @return an uncompressing input stream
     * @throws IOException if an I/O error occurs
     */
    public static InputStream openEntry(InputStream inputStream, ZipEntry zipEntry) throws IOException {
        // read the local file header...
        final ZipDataInputStream zdis = inputStream instanceof ZipDataInputStream ? (ZipDataInputStream) inputStream : new ZipDataInputStream(inputStream);
        readLocalFile(zdis, zipEntry);
        switch (zipEntry.getEntryType()) {
            case FILE: break;
            default: {
                throw new ZipException("Attempt to open a zip entry with an unsupported type");
            }
        }
        switch (zipEntry.getCompressionMethod()) {
            case STORE: {
                return new LimitedInputStream(inputStream, zipEntry.getCompressedSize());
            }
            case DEFLATE: {
                final Inflater inflater = new Inflater(true);
                return new LimitedInputStream(new InflaterInputStream(new LimitedInputStream(inputStream, zipEntry.getCompressedSize()), inflater), zipEntry.getSize());
            }
        }
        throw new ZipException("Unsupported compression algorithm " + zipEntry.getCompressionMethod());
    }

    private static void readLocalFile(final ZipDataInputStream is, final ZipEntry entry) throws IOException {
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
        final byte[] buf = new byte[16384];
        final ZipCatalog catalog = readCatalog(zipFile);
        for (ZipEntry zipEntry : catalog.allEntries()) {
            final String name = zipEntry.getName();
            final ZipEntryType entryType = zipEntry.getEntryType();
            if (entryType == ZipEntryType.DIRECTORY) {
                new File(destDir, name).mkdirs();
            } else if (entryType == ZipEntryType.FILE) {
                final File file = new File(destDir, name).getCanonicalFile();
                final File parentFile = file.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
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

    static void safeClose(final Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (IOException e) {
            // eat
        }
    }
}
