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

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.RandomAccessFile;
import java.io.DataInput;

/**
 * Similar to {@code DataInputStream}, but reads in little-endian order and keeps track
 * of the current offset.
 */
final class ZipDataInputStream extends InputStream implements DataInput {

    private final InputStream delegate;
    private long offset;

    ZipDataInputStream(final InputStream delegate) {
        this.delegate = delegate;
    }

    ZipDataInputStream(final RandomAccessFile delegate) {
        this.delegate = new RandomAccessInputStream(delegate);
    }

    public int read() throws IOException {
        final int b = delegate.read();
        if (b >= 0) {
            offset++;
            return b & 0xff;
        }
        return b;
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        int cnt = delegate.read(b, off, len);
        if (cnt >= 0) {
            offset += (long)cnt;
        }
        return cnt;
    }

    public long skip(final long n) throws IOException {
        final long cnt = delegate.skip(n);
        offset += cnt;
        return cnt;
    }

    public void skipFully(long n) throws IOException {
        while (n > 0L) {
            final long cnt = skip(n);
            if (cnt <= 0L) {
                readByte();
                n--;
            } else {
                n -= cnt;
            }
        }
    }

    public void close() throws IOException {
        delegate.close();
    }

    public long getOffset() {
        return offset;
    }

    public void readFully(final byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(final byte[] b, int offs, int len) throws IOException {
        while (len > 0) {
            int cnt = read(b, offs, len);
            if (cnt == -1) {
                throw new EOFException();
            }
            offs += cnt;
            len -= cnt;
        }
    }

    public long readLong() throws IOException {
        int a = readInt();
        int b = readInt();
        return ((long)a) | ((long)b) << 0x20L;
    }

    public int readInt() throws IOException {
        int a = read();
        int b = read();
        int c = read();
        int d = read();
        if (a == -1 || b == -1 || c == -1 || d == -1) {
            throw new EOFException();
        }
        return a | (b << 0x8) | (c << 0x10) | (d << 0x18);
    }

    public int readUnsignedShort() throws IOException {
        int a = read();
        int b = read();
        if (a == -1 || b == -1) {
            throw new EOFException();
        }
        return a | b << 0x8;
    }

    public short readShort() throws IOException {
        int a = read();
        int b = read();
        if (a == -1 || b == -1) {
            throw new EOFException();
        }
        return (short) (a & 0xff | (b & 0xff) << 0x8);
    }

    public int readUnsignedByte() throws IOException {
        int a = read();
        if (a == -1) {
            throw new EOFException();
        }
        return a;
    }

    public byte readByte() throws IOException {
        int a = read();
        if (a == -1) {
            throw new EOFException();
        }
        return (byte) a;
    }

    public int skipBytes(final int n) throws IOException {
        return (int) skip(n);
    }

    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    public char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    public String readUTF() throws IOException {
        return null;
    }
}
