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

package org.apache.cayenne.tools.ant;

import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Checks that all classes coming from a jar file can be loaded in the surrounding
 * ClassLoader.
 * 
 * @author Andrus Adamchik
 */
public class JarChecker {

    protected File jarFile;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: " + JarChecker.class.getName() + " jar_file");
            System.exit(1);
        }

        try {
            new JarChecker(new File(args[0])).check();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    JarChecker(File jarFile) {
        this.jarFile = jarFile;
    }

    void check() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        JarFile jar = new JarFile(jarFile);

        try {
            Enumeration en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = (JarEntry) en.nextElement();

                if (e.isDirectory()) {
                    continue;
                }

                if (!e.getName().endsWith(".class")) {
                    continue;
                }

                String className = e.getName().replaceAll("/", ".");
                className = className.substring(0, className.length() - ".class".length());
                System.out.println("Loading... " + className);

                // this is what we are testing ... that current ClassLoader has all
                // dependencies to load a class from the jar file (including the class
                // itself).
                Class.forName(className, true, loader);
            }
        }
        finally {
            jar.close();
        }
    }
}
