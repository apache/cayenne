/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.gen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

/**
 * Velocity template resource loader customized for Cayenne use. Supports loading
 * templates from the thread ClassLoader and from relative and absolute paths.
 * 
 * @since 1.2
 */
// must be public top-level class as it is
// instantiated via reflection by Velocity
public class ClassGeneratorResourceLoader extends FileResourceLoader {

    /**
     * Returns resource as InputStream. First calls super implementation. If resource
     * wasn't found, it attempts to load it from current directory or as an absolute path.
     */
    @Override
    public synchronized InputStream getResourceStream(String name)
            throws ResourceNotFoundException {

        InputStream stream = loadFromRelativePath(name);
        if (stream != null) {
            return stream;
        }

        stream = loadFromAbsPath(name);
        if (stream != null) {
            return stream;
        }

        stream = loadFromThreadClassLoader(name);
        if (stream != null) {
            return stream;
        }

        stream = loadFromThisClassLoader(name);
        if (stream != null) {
            return stream;
        }

        throw new ResourceNotFoundException("Couldn't find resource '"
                + name
                + "'. Searched filesystem path and classpath");
    }

    protected InputStream loadFromRelativePath(String name) {
        try {
            return super.getResourceStream(name);
        }
        catch (ResourceNotFoundException rnfex) {
            return null;
        }
    }

    protected InputStream loadFromAbsPath(String name) {
        try {
            File file = new File(name);
            return (file.canRead()) ? new BufferedInputStream(new FileInputStream(file
                    .getAbsolutePath())) : null;

        }
        catch (FileNotFoundException fnfe) {
            return null;
        }
    }

    protected InputStream loadFromThreadClassLoader(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    protected InputStream loadFromThisClassLoader(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }
}
