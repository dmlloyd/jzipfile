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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.zip.ZipException;
import java.io.InputStream;
import java.io.IOException;
import static java.lang.Math.max;
import static java.lang.Math.min;

final class ZipCatalogBuilder {
    private final Map<String, ZipEntry> entryMap = new LinkedHashMap<String, ZipEntry>();
    private final List<ZipEntry> allEntries = new ArrayList<ZipEntry>();

    ZipCatalogBuilder() {
    }

    void readDirectory(InputStream is) throws IOException {
        readDirectory(is instanceof ZipDataInputStream ? (ZipDataInputStream) is : new ZipDataInputStream(is));
    }

    void readDirectory(ZipDataInputStream is) throws IOException {
        final List<ZipEntry> allEntries = this.allEntries;
        final Map<String, ZipEntry> entryMap = this.entryMap;
        try {
            // Format:
            // central directory
            // [zip64 end of central directory rec]
            // [zip64 end of central directory locator]
            // end of central directory record
            int sig = is.readInt();
            while (sig == 0x02014b50) {
                // central directory file header (0..n)
                is.readUnsignedShort(); // madeBy
                final int needed = is.readUnsignedShort();
                if (needed > 20) {
                    throw new ZipException("Need a later version to extract");
                }
                is.readUnsignedShort(); // gpbits
                final ZipCompressionMethod method = ZipCompressionMethod.getMethod(is.readUnsignedShort());
                final int modTimeRaw = is.readUnsignedShort();
                final int modDateRaw = is.readUnsignedShort();

                final int hour = min(modTimeRaw >> 11, 23);
                final int minute = min(modTimeRaw >> 5 & 0x3f, 59);
                final int second = min(modTimeRaw & 0x1f, 59);

                final int year = 1980 + (modDateRaw >> 9);
                // Months are from 1-12
                final int month = max(1, min(12, modDateRaw >> 5 & 0x0f));
                // Days might roll over; if so, let the calendar deal with it
                final int day = modDateRaw & 0x1f;

                // convert to millis
                final long modTime = new GregorianCalendar(year, month - 1, day, hour, minute, second).getTimeInMillis();

                int crc32 = is.readInt();
                int compSize = is.readInt();
                int uncompSize = is.readInt();
                int fnameLen = is.readUnsignedShort();
                int extraLen = is.readUnsignedShort();
                int commentLen = is.readUnsignedShort();
                int diskNumStart = is.readUnsignedShort();
                if (diskNumStart != 0) {
                    throw new ZipException("Multi-disk archives not supported");
                }
                is.readUnsignedShort(); // internal attr
                is.readInt(); // external attr
                int localHeaderOffs = is.readInt();
                final byte[] fileNameBytes = new byte[fnameLen];
                is.readFully(fileNameBytes);
                final byte[] extraBytes = new byte[extraLen];
                is.readFully(extraBytes);
                final byte[] commentBytes = new byte[commentLen];
                is.readFully(commentBytes);
                final String name = new String(fileNameBytes, "US-ASCII");
                // interpret type
                final ZipEntryType type;
                if (name.indexOf('/') == 0) {
                    throw new ZipException("Leading slash not allowed in file name \"" + name + "\"");
                }
                if (uncompSize == 0 && name.lastIndexOf('/') == name.length() - 1) {
                    type = ZipEntryType.DIRECTORY;
                } else {
                    type = ZipEntryType.FILE;
                }
                final String comment = new String(commentBytes, "US-ASCII");
                final ZipEntryImpl entry = new ZipEntryImpl(name, comment, localHeaderOffs, uncompSize & 0xffffffffL, compSize & 0xffffffffL, crc32, type, modTime, method, extraBytes);
                allEntries.add(entry);
                if (! entryMap.containsKey(name) && name.length() > 0) {
                    entryMap.put(name, entry);
                }
                // next sig
                sig = is.readInt();
            }
            if (sig == 0x05054b50) {
                // central directory signature (0..1)
                final int size = is.readUnsignedShort();
                is.skipFully(size & 0xffffffffL);

                // next sig
                sig = is.readInt();
            }
            if (sig == 0x06064b50) {
                if (true) throw new ZipException("64-bit zip records unsupported");
                // zip64 EOD record (0..1)
                is.readLong();
                is.readUnsignedShort();
                is.readUnsignedShort();
                is.readInt();
                is.readInt();
                is.readLong();
                is.readLong();
                is.readLong();
                is.readLong();
                // next sig
                sig = is.readInt();
            }
            if (sig == 0x07064b50) {
                if (true) throw new ZipException("64-bit zip records unsupported");
                // zip64 EOD locator (0..1)

                // next sig
                sig = is.readInt();
            }
            if (sig == 0x06054b50) {
                // EOD (exactly 1)
                is.close();
                return;
            }
            throw new ZipException(String.format("Unexpected signature byte 0x%08x", Integer.valueOf(sig)));
        } finally {
            Zip.safeClose(is);
        }
    }

    public ZipCatalog getZipCatalog() {
        final Map<String, ZipEntry> byNameMap = Collections.unmodifiableMap(entryMap);
        final Collection<ZipEntry> allEntries = Collections.unmodifiableCollection(ZipCatalogBuilder.this.allEntries);
        return new ZipCatalog() {
            public Map<String, ZipEntry> indexedByName() {
                return byNameMap;
            }

            public Collection<ZipEntry> allEntries() {
                return allEntries;
            }
        };
    }
}
