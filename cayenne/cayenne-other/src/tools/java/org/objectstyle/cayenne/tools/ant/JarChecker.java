/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.tools.ant;

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
