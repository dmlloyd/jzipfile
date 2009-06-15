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
import java.util.Iterator;

import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

@Test
public final class SimpleZipFilesTests {

    private static File testFile(String name) {
        return new File("src/test/resources/" + name);
    }

    public void testEmpty() throws IOException {
        final ZipCatalog catalog = Zip.readCatalog(testFile("empty.zip"));
        assertFalse("Unexpected content in empty zip", catalog.allEntries().iterator().hasNext());
        assertFalse("Unexpected content in empty zip map", catalog.indexedByName().entrySet().iterator().hasNext());
    }

    public void testStored() throws IOException {
       final File file = testFile("single-stored.zip");
       testZipFile(file, 1244852648000L);
    }

   public void testDeflated() throws IOException {
        final File file = testFile("single-deflated.zip");
        testZipFile(file, 1244852648000L);
    }

    protected void testZipFile(File file, long expectedModTime) throws IOException {
        final ZipCatalog catalog = Zip.readCatalog(file);
        final Iterator<ZipEntry> i = catalog.allEntries().iterator();
        assertTrue("Missing entry", i.hasNext());
        final ZipEntry entry = i.next();
        assertFalse("Extra entry", i.hasNext());

        // check the mod time
        assertEquals("Dates do not match", expectedModTime, entry.getModificationTime());

        // now open it
        final InputStream inputStream = Zip.openEntry(file, entry);

        // read a few bytes
        inputStream.read(new byte[64]);
        inputStream.close();
    }
}
