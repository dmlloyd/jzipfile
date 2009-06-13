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
import java.io.RandomAccessFile;
import java.io.IOException;

class RandomAccessInputStream extends InputStream {

    private long ptr;
    private final RandomAccessFile delegate;

    public RandomAccessInputStream(final RandomAccessFile delegate) {
        this.delegate = delegate;
    }

    public int read() throws IOException {
        return delegate.read();
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        return delegate.read(b, off, len);
    }

    public void mark(final int readlimit) {
        try {
            ptr = delegate.getFilePointer();
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    public void reset() throws IOException {
        delegate.seek(ptr);
    }

    public boolean markSupported() {
        return true;
    }

    public void close() throws IOException {
        delegate.close();
    }
}
