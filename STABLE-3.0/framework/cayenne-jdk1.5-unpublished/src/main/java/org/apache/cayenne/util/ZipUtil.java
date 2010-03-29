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

package org.apache.cayenne.util;

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
            Enumeration<? extends ZipEntry> en = zip.entries();
            int bufSize = 8 * 1024;

            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
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
            for (File source : sources) {
                if (!source.exists()) {
                    throw new IllegalArgumentException(
                            "File or directory does not exist: " + source);
                }

                if (source.isDirectory()) {
                    zipDirectory(out, stripPath, source, pathSeparator);
                }
                else {
                    zipFile(out, stripPath, source, pathSeparator);
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
        for (String entry : entries) {
            File file = new File(dir, entry);
            if (file.isDirectory()) {
                zipDirectory(out, stripPath, file, pathSeparator);
            }
            else {
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
