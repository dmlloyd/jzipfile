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
import static java.lang.StrictMath.min;

class LimitedInputStream extends InputStream {

    private final InputStream delegate;
    private long limit;

    LimitedInputStream(final InputStream delegate, final long limit) {
        this.delegate = delegate;
        this.limit = limit;
    }

    public int read() throws IOException {
        if (limit > 0) {
            final int b = delegate.read();
            if (b == -1) {
                limit = 0;
            }
            return b;
        }
        return -1;
    }

    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (limit == 0) {
            return -1;
        }
        final int cnt = delegate.read(b, off, (int)min(limit, len));
        if (cnt > 0) {
            limit -= (long)cnt;
        } else {
            limit = 0;
        }
        return cnt;
    }

    public long skip(final long n) throws IOException {
        final long cnt = delegate.skip(min(n, limit));
        limit -= cnt;
        return cnt;
    }

    public int available() throws IOException {
        return (int) Math.min(delegate.available(), limit);
    }

    public void close() throws IOException {
        limit = 0;
        delegate.close();
    }
}
