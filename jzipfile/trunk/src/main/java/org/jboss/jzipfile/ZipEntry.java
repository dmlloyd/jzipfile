/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

/**
 * A zip entry.
 */
public interface ZipEntry {

    /**
     * Get the name of this entry.
     *
     * @return the name
     */
    String getName();

    /**
     * Get the comment associated with this zip entry.
     *
     * @return the comment
     */
    String getComment();

    /**
     * Get the offset of the data for this zip entry within the file.
     *
     * @return the byte offset from the start of the zip file
     */
    long getOffset();

    /**
     * Get the size (uncompressed) of this entry.
     *
     * @return the size in bytes
     */
    long getSize();

    /**
     * Get the compressed size of this entry.
     *
     * @return the size in bytes
     */
    long getCompressedSize();

    /**
     * Get the CRC-32 checksum of this entry, if there is one.
     *
     * @return the checksum
     */
    int getCrc32();

    /**
     * Get the entry type.
     *
     * @return the entry type (will not be {@code null})
     */
    ZipEntryType getEntryType();

    /**
     * Get the entry modification time.
     *
     * @return the modification time
     */
    long getModificationTime();

    /**
     * Get the compression method.
     *
     * @return the compression method (will not be {@code null})
     */
    ZipCompressionMethod getCompressionMethod();

    /**
     * Get the raw extra data bytes.
     *
     * @return the extra data bytes
     */
    byte[] getRawExtraData();
}
