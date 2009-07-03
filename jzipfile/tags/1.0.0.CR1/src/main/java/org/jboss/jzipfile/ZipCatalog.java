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

import java.util.Map;
import java.util.Collection;

/**
 * A zip file catalog.
 */
public interface ZipCatalog {

    /**
     * Get all the named entries, indexed by name.  The returned hash map will return the names
     * in the order that they were found in the file.  If a name occurs more than once, the first occurrence is used.
     *
     * @return the named entries
     */
    Map<String, ZipEntry> indexedByName();

    /**
     * Get all the zip entries, including unnamed and duplicate entries, in the order they appear in the file.
     *
     * @return the entries
     */
    Collection<ZipEntry> allEntries();
}
