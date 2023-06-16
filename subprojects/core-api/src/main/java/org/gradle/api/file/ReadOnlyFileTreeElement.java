/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.file;

import org.gradle.api.Incubating;

/**
 * Information about a file in a directory/file tree.
 *
 * @since 8.3
 */
@Incubating
public interface ReadOnlyFileTreeElement {
    /**
     * Returns true if this element is a directory, or false if this element is a regular file.
     * Returns true if this element is a symbolic link to a directory.
     *
     * @return true if this element is a directory.
     */
    boolean isDirectory();

    /**
     * Returns true if this element is a symbolic link.
     *
     * @return true if this element is a symbolic link.
     */
    boolean isSymbolicLink(); //TODO: is it nessesary?

    //FIXME: use relative path?
    String getSymbolicLinkTarget();

    /**
     * Returns the last modified time of this file at the time of file traversal.
     *
     * @return The last modified time.
     */
    long getLastModified();

    /**
     * Returns the size of this file at the time of file traversal.
     *
     * @return The size, in bytes.
     */
    long getSize();

    /**
     * Returns the base name of this file.
     *
     * @return The name. Never returns null.
     */
    String getName();

    /**
     * Returns the path of this file, relative to the root of the containing file tree. Always uses '/' as the hierarchy
     * separator, regardless of platform file separator. Same as calling <code>getRelativePath().getPathString()</code>.
     *
     * @return The path. Never returns null.
     */
    String getPath();

    /**
     * Returns the path of this file, relative to the root of the containing file tree.
     *
     * @return The path. Never returns null.
     */
    RelativePath getRelativePath();

    int getMode();

    /**
     * Provides a read-only view of access permissions of this file.
     * For details see {@link FilePermissions}.
     *
     * @since 8.3
     */
    @Incubating
    FilePermissions getImmutablePermissions();
}

