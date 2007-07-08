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

package org.apache.cayenne.enhancer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.SecureClassLoader;

class EnhancingClassLoader extends SecureClassLoader {

    protected ClassFileTransformer transformer;

    public EnhancingClassLoader(ClassFileTransformer transformer) {
        super(Thread.currentThread().getContextClassLoader());
        this.transformer = transformer;
    }

    /**
     * Returns true if the class does not need to be enhanced.
     */
    protected boolean skipClassEnhancement(String className) {
        return transformer == null;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {

        if (skipClassEnhancement(name)) {
            return super.loadClass(name, resolve);
        }

        Class c = findLoadedClass(name);

        if (c == null) {
            c = findClass(name);
        }

        if (resolve) {
            resolveClass(c);
        }

        return c;
    }

    /**
     * If a class name is one of the managed classes, loads it
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (skipClassEnhancement(name)) {
            return Class.forName(name, true, getParent());
        }
        else {
            return findEnhancedClass(name);
        }
    }

    /**
     * Loads class bytes, and passes them through the registered ClassTransformers.
     */
    protected Class<?> findEnhancedClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";

        InputStream in = getResourceAsStream(path);
        if (in == null) {
            return Class.forName(name, true, getParent());
        }

        try {

            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];
            int read;

            while ((read = in.read(buffer, 0, 1024)) > 0) {
                out.write(buffer, 0, read);
            }

            out.close();
            byte[] classBytes = out.toByteArray();

            byte[] bytes;
            try {
                bytes = transformer.transform(getParent(), name, null, null, classBytes);
            }
            catch (IllegalClassFormatException e) {
                throw new ClassNotFoundException("Could not transform class '"
                        + name
                        + "' due to invalid format", e);
            }

            if (bytes != null) {
                classBytes = bytes;
            }
            else {
                // if transformer didn't transform ... this is suboptimal as
                // we've already read the bytes from classfile...
                return Class.forName(name, true, getParent());
            }

            return defineClass(name, classBytes, 0, classBytes.length);
        }
        catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException e) {
                // ignore close exceptions...
            }
        }
    }
}
