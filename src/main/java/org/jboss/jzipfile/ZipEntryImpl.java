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

final class ZipEntryImpl implements ZipEntry {
    private final String name;
    private final String comment;
    private final long offset;
    private final long size;
    private final long compressedSize;
    private final int crc32;
    private final ZipEntryType entryType;
    private final long modificationTime;
    private final ZipCompressionMethod compressionMethod;
    private final byte[] rawExtraData;

    ZipEntryImpl(final String name, final String comment, final long offset, final long size, final long compressedSize, final int crc32, final ZipEntryType entryType, final long modificationTime, final ZipCompressionMethod compressionMethod, final byte[] rawExtraData) {
        this.name = name;
        this.comment = comment;
        this.offset = offset;
        this.size = size;
        this.compressedSize = compressedSize;
        this.crc32 = crc32;
        this.entryType = entryType;
        this.modificationTime = modificationTime;
        this.compressionMethod = compressionMethod;
        this.rawExtraData = rawExtraData;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public int getCrc32() {
        return crc32;
    }

    public ZipEntryType getEntryType() {
        return entryType;
    }

    public long getModificationTime() {
        return modificationTime;
    }

    public ZipCompressionMethod getCompressionMethod() {
        return compressionMethod;
    }

    public byte[] getRawExtraData() {
        return rawExtraData;
    }

    public String toString() {
        return String.format("Zip Entry: name=\"%s\", compressed size=%d, uncompressed size=%d, offset=%d, type=%s, method=%s, crc32=0x%08x, comment=\"%s\"", name, Long.valueOf(compressedSize), Long.valueOf(size), Long.valueOf(offset), entryType, compressionMethod, Integer.valueOf(crc32), comment);
    }
}
