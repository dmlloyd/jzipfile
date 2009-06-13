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

/**
 * Possible compression methods.
 */
public enum ZipCompressionMethod {
    STORE,
    SHRINK,
    REDUCE_1,
    REDUCE_2,
    REDUCE_3,
    REDUCE_4,
    IMPLODE,
    DEFLATE,
    DEFLATE64,
    BZIP2,
    LZMA,
    TERSE,
    LZ77,
    WAVPAK,
    PPMD,

    UNKNOWN;

    /**
     * Get the compression method for the given code.
     *
     * @param code the compression method code
     * @return the compression method, or {@link #UNKNOWN} if it is not recognized
     */
    public static ZipCompressionMethod getMethod(int code) {
        switch (code) {
            case 0: return STORE;
            case 1: return SHRINK;
            case 2: return REDUCE_1;
            case 3: return REDUCE_2;
            case 4: return REDUCE_3;
            case 5: return REDUCE_4;
            case 6: return IMPLODE;
            case 8: return DEFLATE;
            case 9: return DEFLATE64;
            case 12: return BZIP2;
            case 14: return LZMA;
            case 18: return TERSE;
            case 19: return LZ77;
            case 97: return WAVPAK;
            case 98: return PPMD;
            default: return UNKNOWN;
        }
    }
}
