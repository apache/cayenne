/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A default implementation of ClassLoadingService used in CayenneModeler.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class FileClassLoadingService implements ClassLoadingService {

    private static Logger logObj = Logger.getLogger(FileClassLoadingService.class);

    private FileClassLoader classLoader;
    protected List pathFiles;

    public FileClassLoadingService() {
        this.pathFiles = new ArrayList(15);
    }

    /**
     * Returns class for a given name, loading it if needed from configured locations.
     */
    public synchronized Class loadClass(String className) throws ClassNotFoundException {
        return nonNullClassLoader().loadClass(className);
    }

    /**
     * Returns a ClassLoader based on the current configured CLASSPATH settings.
     */
    public ClassLoader getClassLoader() {
        return nonNullClassLoader();
    }

    /**
     * Returns an unmodifiable list of configured CLASSPATH locations.
     */
    public synchronized List getPathFiles() {
        return Collections.unmodifiableList(pathFiles);
    }

    public synchronized void setPathFiles(Collection files) {

        pathFiles.clear();
        classLoader = null;

        Iterator it = files.iterator();
        while (it.hasNext()) {
            addFile((File) it.next());
        }
    }

    /**
     * Adds a new location to the list of configured locations.
     */
    private void addFile(File file) {
        file = file.getAbsoluteFile();

        if (pathFiles.contains(file)) {
            return;
        }

        if (classLoader != null) {
            try {
                classLoader.addURL(file.toURL());
            }
            catch (MalformedURLException ex) {
                logObj.warn("Invalid classpath entry, ignoring: " + file);
                return;
            }
        }

        pathFiles.add(file);
        logObj.debug("Added CLASSPATH entry...: " + file.getAbsolutePath());
    }

    private synchronized FileClassLoader nonNullClassLoader() {
        // init class loader on demand
        if (classLoader == null) {
            classLoader = new FileClassLoader(getClass().getClassLoader(), pathFiles);
        }

        return classLoader;
    }

    // URLClassLoader with addURL method exposed.
    static class FileClassLoader extends URLClassLoader {

        FileClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        FileClassLoader(ClassLoader parent, List files) {
            this(parent);

            Iterator it = files.iterator();
            while (it.hasNext()) {
                File file = (File) it.next();

                // I guess here we have to quetly ignore invalid URLs...
                try {
                    addURL(file.toURL());
                }
                catch (MalformedURLException ex) {
                }
            }
        }

        public void addURL(URL url) {
            super.addURL(url);
        }
    }
}