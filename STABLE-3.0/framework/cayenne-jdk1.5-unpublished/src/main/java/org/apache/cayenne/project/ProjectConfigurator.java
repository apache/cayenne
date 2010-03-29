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

package org.apache.cayenne.project;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.ZipUtil;

/**
 * Performs on the fly reconfiguration of Cayenne projects.
 * 
 * @deprecated since 3.0. {@link ProjectConfigurator} approach turned out to be not
 *             usable, and is in fact rarely used (if ever). It will be removed in
 *             subsequent releases.
 */
public class ProjectConfigurator {

    protected ProjectConfigInfo info;

    public ProjectConfigurator(ProjectConfigInfo info) {
        this.info = info;
    }

    /**
     * Performs reconfiguration of the project.
     * 
     * @throws ProjectException
     */
    public void execute() throws ProjectException {
        File tmpDir = null;
        File tmpDest = null;
        try {
            // initialize default settings
            if (info.getDestJar() == null) {
                info.setDestJar(info.getSourceJar());
            }

            // sanity check
            validate();

            // do the processing
            tmpDir = makeTempDirectory();
            ZipUtil.unzip(info.getSourceJar(), tmpDir);

            reconfigureProject(tmpDir);

            tmpDest = makeTempDestJar();
            ZipUtil.zip(tmpDest, tmpDir, tmpDir.listFiles(), '/');

            // finally, since everything goes well so far, rename temp file to final name
            if (info.getDestJar().exists() && !info.getDestJar().delete()) {
                throw new IOException("Can't delete old jar file: " + info.getDestJar());
            }

            if (!tmpDest.renameTo(info.getDestJar())) {
                throw new IOException("Error renaming: "
                        + tmpDest
                        + " to "
                        + info.getDestJar());
            }
        }
        catch (IOException ex) {
            throw new ProjectException("Error performing reconfiguration.", ex);
        }
        finally {
            if (tmpDir != null) {
                cleanup(tmpDir);
            }

            if (tmpDest != null) {
                tmpDest.delete();
            }
        }
    }

    /**
     * Performs reconfiguration of the unjarred project.
     * 
     * @param projectDir a directory where a working copy of the project is located.
     */
    protected void reconfigureProject(File projectDir) throws ProjectException {
        File projectFile = new File(projectDir, Configuration.DEFAULT_DOMAIN_FILE);

        // process alternative project file
        if (info.getAltProjectFile() != null) {
            if (!Util.copy(info.getAltProjectFile(), projectFile)) {
                throw new ProjectException("Can't copy project file: "
                        + info.getAltProjectFile());
            }
        }

        // copy driver files, delete unused
        Iterator<DataNodeConfigInfo> it = info.getNodes().iterator();
        boolean needFix = it.hasNext();
        while (it.hasNext()) {
            DataNodeConfigInfo nodeInfo = it.next();
            String name = nodeInfo.getName();

            File targetDriverFile = new File(projectDir, name
                    + DataNodeFile.LOCATION_SUFFIX);

            // these are the two cases when the driver file must be deleted
            if (nodeInfo.getDataSource() != null || nodeInfo.getDriverFile() != null) {
                if (targetDriverFile.exists()) {
                    targetDriverFile.delete();
                }
            }

            if (nodeInfo.getDriverFile() != null
                    && !nodeInfo.getDriverFile().equals(targetDriverFile)) {
                // need to copy file from another location
                if (!Util.copy(nodeInfo.getDriverFile(), targetDriverFile)) {
                    throw new ProjectException("Can't copy driver file from "
                            + nodeInfo.getDriverFile());
                }
            }
        }

        // load project
        if (needFix) {
            // read the project and fix data nodes
            PartialProject project = new PartialProject(projectFile);
            project.updateNodes(info.getNodes());
            project.save();
        }
    }

    /**
     * Returns a temporary file for the destination jar.
     */
    protected File makeTempDestJar() throws IOException {
        File destFolder = info.getDestJar().getParentFile();
        if (destFolder != null && !destFolder.isDirectory()) {
            if (!destFolder.mkdirs()) {
                throw new IOException("Can't create directory: "
                        + destFolder.getCanonicalPath());
            }
        }

        String baseName = "tmp_" + info.getDestJar().getName();

        // seeting upper limit on a number of tries, though normally we would expect
        // to succeed from the first attempt...
        for (int i = 0; i < 50; i++) {
            File tmpFile = (destFolder != null)
                    ? new File(destFolder, baseName + i)
                    : new File(baseName + i);
            if (!tmpFile.exists()) {
                return tmpFile;
            }
        }

        throw new IOException("Problems creating temporary file.");
    }

    /**
     * Deletes a temporary directories and files created.
     */
    protected void cleanup(File dir) {
        Util.delete(dir.getPath(), true);
    }

    /**
     * Creates a temporary directory to unjar the jar file.
     * 
     * @return File
     * @throws IOException
     */
    protected File makeTempDirectory() throws IOException {
        File destFolder = info.getDestJar().getParentFile();
        if (destFolder != null && !destFolder.isDirectory()) {
            if (!destFolder.mkdirs()) {
                throw new IOException("Can't create directory: "
                        + destFolder.getCanonicalPath());
            }
        }

        String baseName = info.getDestJar().getName();
        if (baseName.endsWith(".jar")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        // seeting upper limit on a number of tries, though normally we would expect
        // to succeed from the first attempt...
        for (int i = 0; i < 50; i++) {
            File tmpDir = (destFolder != null)
                    ? new File(destFolder, baseName + i)
                    : new File(baseName + i);
            if (!tmpDir.exists()) {
                if (!tmpDir.mkdir()) {
                    throw new IOException("Can't create directory: "
                            + tmpDir.getCanonicalPath());
                }

                return tmpDir;
            }
        }

        throw new IOException("Problems creating temporary directory.");
    }

    /**
     * Validates consistency of the reconfiguration information.
     */
    protected void validate() throws IOException, ProjectException {
        if (info == null) {
            throw new ProjectException("ProjectConfig info is not set.");
        }

        if (info.getSourceJar() == null) {
            throw new ProjectException("Source jar file is not set.");
        }

        if (!info.getSourceJar().isFile()) {
            throw new IOException(info.getSourceJar() + " is not a file.");
        }

        if (!info.getSourceJar().canRead()) {
            throw new IOException("Can't read file: " + info.getSourceJar());
        }
    }
}
