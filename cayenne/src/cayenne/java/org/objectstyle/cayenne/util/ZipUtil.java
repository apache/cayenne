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
package org.objectstyle.cayenne.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Utility class to perform zip/unzip operations on files and directories.
 *  
 * @author Andrei Adamchik
 */
public class ZipUtil {

    /**
     * Constructor for ZipUtil.
     */
    public ZipUtil() {
        super();
    }

    /**
      * Unpacks a zip file to the target directory.
      *
      * @param zipFile
      * @param destDir
      * @throws IOException
      */
    public static void unzip(File zipFile, File destDir) throws IOException {
        ZipFile zip = new ZipFile(zipFile);

        try {
            Enumeration en = zip.entries();
            int bufSize = 8 * 1024;

            while (en.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) en.nextElement();
                File file =
                    (destDir != null)
                        ? new File(destDir, entry.getName())
                        : new File(entry.getName());

                if (entry.isDirectory()) {
                    if (!file.mkdirs()) {
                        throw new IOException(
                            "Error creating directory: " + file);
                    }
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        if (!parent.mkdirs()) {
                            throw new IOException(
                                "Error creating directory: " + parent);
                        }
                    }

                    InputStream in = zip.getInputStream(entry);
                    try {
                        OutputStream out =
                            new BufferedOutputStream(
                                new FileOutputStream(file),
                                bufSize);

                        try {
                            Util.copyPipe(in, out, bufSize);
                        } finally {
                            out.close();
                        }

                    } finally {
                        in.close();
                    }
                }
            }
        } finally {
            zip.close();
        }
    }

    /**
      * Recursively zips a set of root entries into a zipfile, compressing the
      * contents.
      *
      * @param zipFile target zip file.
      * @param parentDir a directory containing source files to zip.
      * @param sources an array of files and/or directories to zip.
      * @param pathSeparator path separator for zip entries.
      * 
      * @throws IOException
      */
    public static void zip(
        File zipFile,
        File parentDir,
        File[] sources,
        char pathSeparator)
        throws IOException {
            
        String stripPath = (parentDir != null) ? parentDir.getPath() : "";
        if (stripPath.length() > 0 && !stripPath.endsWith(File.separator)) {
            stripPath += File.separator;
        }

        ZipOutputStream out =
            new ZipOutputStream(new FileOutputStream(zipFile));
        out.setMethod(ZipOutputStream.DEFLATED);

        try {
            // something like an Ant directory scanner wouldn't hurt here
            for (int i = 0; i < sources.length; i++) {
                if (!sources[i].exists()) {
                    throw new IllegalArgumentException(
                        "File or directory does not exist: " + sources[i]);
                }

                if (sources[i].isDirectory()) {
                    zipDirectory(out, stripPath, sources[i], pathSeparator);
                } else {
                    zipFile(out, stripPath, sources[i], pathSeparator);
                }
            }
        } finally {
            out.close();
        }
    }

    /**
     * Uses code fragments from Jakarta-Ant, Copyright: Apache Software
     * Foundation.
     */
    private static void zipDirectory(
        ZipOutputStream out,
        String stripPath,
        File dir,
        char pathSeparator)
        throws IOException {

        String[] entries = dir.list();

        if (entries == null || entries.length == 0) {
            return;
        }

        // recurse via entries
        for (int i = 0; i < entries.length; i++) {
            File file = new File(dir, entries[i]);
            if (file.isDirectory()) {
                zipDirectory(out, stripPath, file, pathSeparator);
            } else {
                zipFile(out, stripPath, file, pathSeparator);
            }
        }
    }

    /**
     * Uses code fragments from Jakarta-Ant, Copyright: Apache Software
     * Foundation.
     */
    private static void zipFile(
        ZipOutputStream out,
        String stripPath,
        File file,
        char pathSeparator)
        throws IOException {
        ZipEntry ze =
            new ZipEntry(processPath(file.getPath(), stripPath, pathSeparator));
        ze.setTime(file.lastModified());
        out.putNextEntry(ze);

        byte[] buffer = new byte[8 * 1024];
        BufferedInputStream in =
            new BufferedInputStream(new FileInputStream(file), buffer.length);

        try {
            int count = 0;
            while ((count = in.read(buffer, 0, buffer.length)) >= 0) {
                if (count != 0) {
                    out.write(buffer, 0, count);
                }
            }
        } finally {
            in.close();
        }
    }

    private static String processPath(
        String path,
        String stripPath,
        char pathSeparator) {
        if (!path.startsWith(stripPath)) {
            throw new IllegalArgumentException(
                "Invalid entry: "
                    + path
                    + "; expected to start with "
                    + stripPath);
        }

        return path.substring(stripPath.length()).replace(
            File.separatorChar,
            pathSeparator);
    }
}
