/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.gen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.apache.velocity.util.ExtProperties;

/**
 * Velocity template resource loader customized for Cayenne use. Supports loading
 * templates from the thread ClassLoader and from relative and absolute paths.
 * 
 * @since 1.2
 */
// must be public top-level class as it is
// instantiated via reflection by Velocity
public class ClassGeneratorResourceLoader extends FileResourceLoader {

    private Path rootPath;

    @Override
    public void init(ExtProperties configuration) {
        String pathStr = configuration.getString("root");
        if(pathStr != null) {
            rootPath = Paths.get(pathStr);
        }
    }

    /**
     * Returns resource as InputStream. First calls super implementation. If resource
     * wasn't found, it attempts to load it from current directory or as an absolute path.
     */
    @Override
    public synchronized Reader getResourceReader(String name, String charset) throws ResourceNotFoundException {
        Reader stream;

        stream = loadFromThreadClassLoader(name);
        if (stream != null) {
            return stream;
        }

        stream = loadFromThisClassLoader(name);
        if (stream != null) {
            return stream;
        }

        stream = loadFromRelativePath(name);
        if (stream != null) {
            return stream;
        }

        stream = loadFromAbsPath(name);
        if (stream != null) {
            return stream;
        }

        throw new ResourceNotFoundException("Couldn't find resource '"
                + name
                + "'. Searched filesystem path and classpath");
    }

    protected Reader loadFromRelativePath(String name) {
        if (rootPath != null) {
            Path absolutePath = rootPath.resolve(name).normalize();
            return loadFromAbsPath(absolutePath.toString());
        }
        return null;
    }

    protected Reader loadFromAbsPath(String name) {
        try {
            File file = new File(name);
            return file.canRead()
                    ? new BufferedReader(new FileReader(file.getAbsolutePath()))
                    : null;
        } catch (FileNotFoundException unused) {
            return null;
        }
    }

    protected Reader loadFromThreadClassLoader(String name) {
    	InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        return stream != null
                ? new InputStreamReader(stream)
                : null;
    }

    protected Reader loadFromThisClassLoader(String name) {
    	InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        return stream != null
                ? new InputStreamReader(stream)
                : null;
    }
}
