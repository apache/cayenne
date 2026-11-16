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
package org.apache.cayenne.projecttools;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.project.ConfigurationNameMapper;
import org.apache.cayenne.project.ProjectNode;
import org.apache.cayenne.project.ProjectNodeVisitor;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.cgen.CgenDescriptor;
import org.apache.cayenne.project.cgen.CgenPaths;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A ProjectSaver saving project configuration to the file system.
 *
 * @since 3.1
 */
public class FileProjectSaver implements ProjectSaver {

    @Inject
    protected ConfigurationNameMapper nameMapper;

    protected ProjectNodeVisitor<Resource> resourceGetter;
    protected ProjectNodeVisitor<Collection<ProjectNode>> saveableNodesGetter;
    protected String fileEncoding;

    public FileProjectSaver() {
        resourceGetter = new ConfigurationSourceGetter();
        saveableNodesGetter = new SaveableNodesGetter();

        // this is not configurable yet... probably doesn't have to be
        fileEncoding = "UTF-8";
    }

    @Override
    public String getSupportedVersion() {
        return UpgradeHandler.CURRENT_VERSION;
    }

    @Override
    public void save(ProjectNode rootNode, Collection<URL> unusedResources) {
        save(rootNode, rootNode.acceptVisitor(resourceGetter), unusedResources);
    }

    @Override
    public void saveAs(ProjectNode rootNode, Resource baseDirectory) {
        if (baseDirectory == null) {
            throw new NullPointerException("Null 'baseDirectory'");
        }
        save(rootNode, baseDirectory, null);
    }

    /**
     * @param unusedResources the old resources to delete, or null if no old resources should be deleted
     */
    void save(ProjectNode rootNode, Resource baseResource, Collection<URL> unusedResources) {
        Collection<ProjectNode> nodes = rootNode.acceptVisitor(saveableNodesGetter);
        Collection<SaveUnit> units = new ArrayList<>(nodes.size());

        for (ProjectNode node : nodes) {
            String targetLocation = nameMapper.configurationLocation(node);

            if (node instanceof DataMap) {
				DataMap dataMapNode = ((DataMap) node);
				dataMapNode.setLocation(targetLocation);
			}

            Resource targetResource = baseResource.getRelativeResource(targetLocation);
            units.add(createSaveUnit(node, targetResource));
        }

        checkAccess(units);

        Map<CgenDescriptor, CgenOutputDir> originalCgenOutputDirs = new IdentityHashMap<>();
        boolean saved = false;
        try {
            rebaseCgenOutputDirs(units, originalCgenOutputDirs);
            saveToTempFiles(units);
            saveCommit(units);
            saved = true;
        } finally {
            if (!saved) {
                restoreCgenOutputDirs(originalCgenOutputDirs);
            }
            clearTempFiles(units);
        }

        try {
            if (unusedResources != null) {
                clearRenamedFiles(units);

                Collection<URL> toDelete = new HashSet<>(unusedResources);
                for (SaveUnit unit : units) {
                    toDelete.remove(unit.sourceConfiguration.getURL());
                }
                deleteUnusedFiles(toDelete);
            }
        } catch (IOException ex) {
            throw new CayenneRuntimeException(ex);
        }
    }

    SaveUnit createSaveUnit(ProjectNode node, Resource targetResource) {

        SaveUnit unit = new SaveUnit();
        unit.node = node;
        unit.sourceConfiguration = node.acceptVisitor(resourceGetter);

        if (unit.sourceConfiguration == null) {
            unit.sourceConfiguration = targetResource;
        }

        // attempt to convert targetResource to a File... if that fails,
        // FileProjectSaver is not appropriate for handling a given project..

        URL targetUrl = targetResource.getURL();

        try {
            unit.targetFile = Util.toFile(targetUrl);
        } catch (IllegalArgumentException e) {
            throw new CayenneRuntimeException("Can't save configuration to the following location: '%s'. "
                    + "Is this a valid file location?. (%s)", e, targetUrl, e.getMessage());
        }

        return unit;
    }

    void checkAccess(Collection<SaveUnit> units) {
        for (SaveUnit unit : units) {

            File targetFile = unit.targetFile;

            File parent = targetFile.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new CayenneRuntimeException("Error creating directory tree for '%s'",
                            parent.getAbsolutePath());
                }
            }

            if (targetFile.isDirectory()) {
                throw new CayenneRuntimeException("Target file '%s' is a directory", targetFile.getAbsolutePath());
            }

            if (targetFile.exists() && !targetFile.canWrite()) {
                throw new CayenneRuntimeException("Can't write to file '%s'", targetFile.getAbsolutePath());
            }

        }
    }

    /**
     * Rebases the cgen output directories of the saved DataMaps to their target directories. Must run before the
     * DataMaps are written and before their new locations are committed.
     *
     * @param originalOutputDirs collects the original directories for {@link #restoreCgenOutputDirs(Map)}
     */
    void rebaseCgenOutputDirs(Collection<SaveUnit> units, Map<CgenDescriptor, CgenOutputDir> originalOutputDirs) {
        for (SaveUnit unit : units) {
            if (unit.node instanceof DataMap dataMap) {
                Path oldRoot = CgenPaths.dataMapDir(dataMap).orElse(null);
                Path newRoot = unit.targetFile.getParentFile().toPath();
                for (CgenDescriptor descriptor : dataMap.getCgenDescriptors()) {
                    if (!originalOutputDirs.containsKey(descriptor)) {
                        originalOutputDirs.put(descriptor, new CgenOutputDir(
                                descriptor.getDestDir(),
                                dataMap,
                                dataMap.getConfigurationSource()));
                        descriptor.setDestDir(CgenPaths.rebase(descriptor.getDestDir(), oldRoot, newRoot));
                    }
                }
            }
        }
    }

    /**
     * Restores the cgen output directories after a failed save, except for the DataMaps already committed to their
     * new location.
     */
    void restoreCgenOutputDirs(Map<CgenDescriptor, CgenOutputDir> originalOutputDirs) {
        originalOutputDirs.forEach((descriptor, original) -> {
            if (original.dataMap().getConfigurationSource() == original.dataMapSource()) {
                descriptor.setDestDir(original.destDir());
            }
        });
    }

    void saveToTempFiles(Collection<SaveUnit> units) {

        for (SaveUnit unit : units) {

            String name = unit.targetFile.getName();
            if (name.length() < 3) {
                name = "cayenne-project";
            }

            File parent = unit.targetFile.getParentFile();

            try {
                unit.targetTempFile = File.createTempFile(name, null, parent);
            } catch (IOException e) {
                throw new CayenneRuntimeException("Error creating temp file (%s)", e, e.getMessage());
            }

            if (unit.targetTempFile.exists()) {
                unit.targetTempFile.delete();
            }

            try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(unit.targetTempFile), fileEncoding))) {
                saveToTempFile(unit, printWriter);
            } catch (UnsupportedEncodingException e) {
                throw new CayenneRuntimeException("Unsupported encoding '%s' (%s)", e, fileEncoding, e.getMessage());
            } catch (FileNotFoundException e) {
                throw new CayenneRuntimeException("File not found '%s' (%s)", e, unit.targetTempFile.getAbsolutePath(),
                        e.getMessage());
            }
        }
    }

    void saveToTempFile(SaveUnit unit, PrintWriter printWriter) {
        if (!(unit.node instanceof XMLSerializable serializable)) {
            throw new CayenneRuntimeException("Can't save a project node of type '%s'",
                    unit.node.getClass().getName());
        }

        XMLEncoder encoder = new XMLEncoder(printWriter, "\t");
        encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        serializable.encodeAsXML(encoder);
    }

    void saveCommit(Collection<SaveUnit> units) {

        for (SaveUnit unit : units) {

            File targetFile = unit.targetFile;

            // Per CAY-2119, this is an ugly hack to force Windows to unlock the file that was previously locked by
            // our process. Without it, the delete operation downstream would fail
            System.gc();

            if (targetFile.exists()) {
                if (!targetFile.delete()) {
                    throw new CayenneRuntimeException("Unable to remove old master file '%s'",
                            targetFile.getAbsolutePath());
                }
            }

            File tempFile = unit.targetTempFile;
            if (!tempFile.renameTo(targetFile)) {
                throw new CayenneRuntimeException("Unable to move '%s' to '%s'", tempFile.getAbsolutePath(),
                        targetFile.getAbsolutePath());
            }

            unit.targetTempFile = null;
            try {
                URLResource targetUrlResource = new URLResource(targetFile.toURI().toURL());
                unit.node.acceptVisitor(new ConfigurationSourceSetter(targetUrlResource, nameMapper));
            } catch (MalformedURLException e) {
                throw new CayenneRuntimeException("Malformed URL for file '%s'", e, targetFile.getAbsolutePath());
            }
        }
    }

    private void clearTempFiles(Collection<SaveUnit> units) {
        for (SaveUnit unit : units) {

            if (unit.targetTempFile != null && unit.targetTempFile.exists()) {
                unit.targetTempFile.delete();
                unit.targetTempFile = null;
            }
        }
    }

    private void clearRenamedFiles(Collection<SaveUnit> units) throws IOException {
        for (SaveUnit unit : units) {

            if (unit.sourceConfiguration == null) {
                continue;
            }

            URL sourceUrl = unit.sourceConfiguration.getURL();
            File sourceFile;
            try {
                sourceFile = Util.toFile(sourceUrl);
            } catch (IllegalArgumentException e) {
                // ignore non-file configurations...
                continue;
            }

            if (!sourceFile.exists()) {
                continue;
            }

            // compare against ALL unit target files, not just the current
            // unit... if the
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

    private void deleteUnusedFiles(Collection<URL> unusedResources) throws IOException {
        for (URL unusedResource : unusedResources) {

            File unusedFile;
            try {
                unusedFile = Util.toFile(unusedResource);
            } catch (IllegalArgumentException e) {
                // ignore non-file configurations...
                continue;
            }

            if (!unusedFile.exists()) {
                continue;
            }

            if (!unusedFile.delete()) {
                throw new CayenneRuntimeException("Could not delete file '%s'", unusedFile.getCanonicalPath());
            }

        }
    }

    record CgenOutputDir(String destDir, DataMap dataMap, Resource dataMapSource) {
    }

    static class SaveUnit {

        private ProjectNode node;

        // source can be an abstract resource, but target is always a file...
        private Resource sourceConfiguration;
        private File targetFile;
        private File targetTempFile;

    }
}
