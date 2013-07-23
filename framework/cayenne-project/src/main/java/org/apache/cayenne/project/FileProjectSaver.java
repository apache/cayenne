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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.util.Util;

/**
 * A ProjectSaver saving project configuration to the file system.
 * 
 * @since 3.1
 */
public class FileProjectSaver implements ProjectSaver {

    @Inject
    protected ConfigurationNameMapper nameMapper;

    protected ConfigurationNodeVisitor<Resource> resourceGetter;
    protected ConfigurationNodeVisitor<Collection<ConfigurationNode>> saveableNodesGetter;
    protected String fileEncoding;

    public FileProjectSaver() {
        resourceGetter = new ConfigurationSourceGetter();
        saveableNodesGetter = new SaveableNodesGetter();

        // this is not configurable yet... probably doesn't have to be
        fileEncoding = "UTF-8";
    }

    public String getSupportedVersion() {
        return "7";
    }

    public void save(Project project) {

        Collection<ConfigurationNode> nodes = project.getRootNode().acceptVisitor(
                saveableNodesGetter);
        Collection<SaveUnit> saveUnits = new ArrayList<SaveUnit>(nodes.size());

        for (ConfigurationNode node : nodes) {
            saveUnits.add(createSaveUnit(node, null));
        }

        save(saveUnits, true);
    }

    public void saveAs(Project project, Resource baseDirectory) {

        if (baseDirectory == null) {
            throw new NullPointerException("Null 'baseDirectory'");
        }

        Collection<ConfigurationNode> nodes = project.getRootNode().acceptVisitor(
                saveableNodesGetter);
        Collection<SaveUnit> saveUnits = new ArrayList<SaveUnit>(nodes.size());

        for (ConfigurationNode node : nodes) {
            saveUnits.add(createSaveUnit(node, baseDirectory));
        }

        save(saveUnits, false);
    }

    void save(Collection<SaveUnit> units, boolean deleteOldResources) {
        checkAccess(units);

        try {
            saveToTempFiles(units);
            saveCommit(units);
        }
        finally {
            clearTempFiles(units);
        }

        try {
            if (deleteOldResources) {
                clearOldFiles(units);
            }
        }
        catch (IOException ex) {
            throw new CayenneRuntimeException(ex);
        }
    }

    SaveUnit createSaveUnit(ConfigurationNode node, Resource baseResource) {

        SaveUnit unit = new SaveUnit();
        unit.node = node;
        unit.sourceConfiguration = node.acceptVisitor(resourceGetter);

        if (baseResource == null) {
            baseResource = unit.sourceConfiguration;
        }

        String targetLocation = nameMapper.configurationLocation(node);
        Resource targetResource = baseResource.getRelativeResource(targetLocation);

        // attempt to convert targetResource to a File... if that fails,
        // FileProjectSaver is not appropriate for handling a given project..

        URL targetUrl = targetResource.getURL();

        try {
            unit.targetFile = Util.toFile(targetUrl);
        }
        catch (IllegalArgumentException e) {
            throw new CayenneRuntimeException(
                    "Can't save configuration to the following location: '%s'. "
                            + "Is this a valid file location?. (%s)",
                    e,
                    targetUrl,
                    e.getMessage());
        }

        return unit;
    }

    void checkAccess(Collection<SaveUnit> units) {
        for (SaveUnit unit : units) {

            File targetFile = unit.targetFile;

            File parent = targetFile.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new CayenneRuntimeException(
                            "Error creating directory tree for '%s'",
                            parent.getAbsolutePath());
                }
            }

            if (targetFile.isDirectory()) {
                throw new CayenneRuntimeException(
                        "Target file '%s' is a directory",
                        targetFile.getAbsolutePath());
            }

            if (targetFile.exists() && !targetFile.canWrite()) {
                throw new CayenneRuntimeException("Can't write to file '%s'", targetFile
                        .getAbsolutePath());
            }

        }
    }

    void saveToTempFiles(Collection<SaveUnit> units) {

        for (SaveUnit unit : units) {

            String name = unit.targetFile.getName();
            if (name == null || name.length() < 3) {
                name = "cayenne-project";
            }

            File parent = unit.targetFile.getParentFile();

            try {
                unit.targetTempFile = File.createTempFile(name, null, parent);
            }
            catch (IOException e) {
                throw new CayenneRuntimeException("Error creating temp file (%s)", e, e
                        .getMessage());
            }

            if (unit.targetTempFile.exists()) {
                unit.targetTempFile.delete();
            }

            PrintWriter printWriter;
            try {
                printWriter = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream(unit.targetTempFile),
                        fileEncoding));
            }
            catch (UnsupportedEncodingException e) {
                throw new CayenneRuntimeException(
                        "Unsupported encoding '%s' (%s)",
                        e,
                        fileEncoding,
                        e.getMessage());
            }
            catch (FileNotFoundException e) {
                throw new CayenneRuntimeException(
                        "File not found '%s' (%s)",
                        e,
                        unit.targetTempFile.getAbsolutePath(),
                        e.getMessage());
            }

            try {
                saveToTempFile(unit, printWriter);
            }
            finally {
                printWriter.close();
            }
        }
    }

    void saveToTempFile(SaveUnit unit, PrintWriter printWriter) {
        unit.node
                .acceptVisitor(new ConfigurationSaver(printWriter, getSupportedVersion()));
    }

    void saveCommit(Collection<SaveUnit> units) {

        for (SaveUnit unit : units) {

            File targetFile = unit.targetFile;

            if (targetFile.exists()) {
                if (!targetFile.delete()) {
                    throw new CayenneRuntimeException(
                            "Unable to remove old master file '%s'",
                            targetFile.getAbsolutePath());
                }
            }

            File tempFile = unit.targetTempFile;
            if (!tempFile.renameTo(targetFile)) {
                throw new CayenneRuntimeException("Unable to move '%s' to '%s'", tempFile
                        .getAbsolutePath(), targetFile.getAbsolutePath());
            }

            unit.targetTempFile = null;
            try {
                unit.node.acceptVisitor(new ConfigurationSourceSetter(new URLResource(
                        targetFile.toURL())));
            }
            catch (MalformedURLException e) {
                throw new CayenneRuntimeException(
                        "Malformed URL for file '%s'",
                        e,
                        targetFile.getAbsolutePath());
            }
        }
    }

    void clearTempFiles(Collection<SaveUnit> units) {
        for (SaveUnit unit : units) {

            if (unit.targetTempFile != null && unit.targetTempFile.exists()) {
                unit.targetTempFile.delete();
                unit.targetTempFile = null;
            }
        }
    }

    private void clearOldFiles(Collection<SaveUnit> units) throws IOException {
        for (SaveUnit unit : units) {

            if (unit.sourceConfiguration == null) {
                continue;
            }

            URL sourceUrl = unit.sourceConfiguration.getURL();
            File sourceFile;
            try {
                sourceFile = Util.toFile(sourceUrl);
            }
            catch (IllegalArgumentException e) {
                // ignore non-file configurations...
                continue;
            }

            if (!sourceFile.exists()) {
                continue;
            }

            // compare against ALL unit target files, not just the current unit... if the
            // target matches, skip this file
            boolean isTarget = false;
            for (SaveUnit xunit : units) {
                if (isFilesEquals(sourceFile, xunit.targetFile)) {
                    isTarget = true;
                    break;
                }
            }

            if (!isTarget) {
                if (!sourceFile.delete()) {
                    throw new CayenneRuntimeException("Could not delete file '%s'", sourceFile.getCanonicalPath());
                }
            }
        }
    }

    private boolean isFilesEquals(File firstFile, File secondFile) throws IOException {
        boolean isFirstFileExists = firstFile.exists();
        boolean isSecondFileExists = secondFile.exists();

        String firstFilePath = firstFile.getCanonicalPath();
        String secondFilePath = secondFile.getCanonicalPath();

        return isFirstFileExists && isSecondFileExists && firstFilePath.equals(secondFilePath);
    }

    class SaveUnit {

        private ConfigurationNode node;

        // source can be an abstract resource, but target is always a file...
        private Resource sourceConfiguration;
        private File targetFile;
        private File targetTempFile;

    }
}
