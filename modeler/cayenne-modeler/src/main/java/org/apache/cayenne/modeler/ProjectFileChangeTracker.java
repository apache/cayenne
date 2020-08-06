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
package org.apache.cayenne.modeler;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.action.SaveAction;
import org.apache.cayenne.modeler.dialog.FileDeletedDialog;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProjectWatchdog class is responsible for tracking changes in cayenne.xml and
 * other Cayenne project files
 * 
 */
public class ProjectFileChangeTracker extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectFileChangeTracker.class);

    /**
     * The default delay between every file modification check
     */
    private static final long DEFAULT_DELAY = 4000;

    /**
     * The names of the files to observe for changes.
     */
    protected final Map<URI, FileInfo> files;
    protected final ProjectController mediator;

    protected boolean paused;
    protected boolean isShownChangeDialog;
    protected boolean isShownRemoveDialog;

    public ProjectFileChangeTracker(ProjectController mediator) {
        this.files = new ConcurrentHashMap<>();
        this.mediator = mediator;
        setName("cayenne-modeler-file-change-tracker");
    }

    /**
     * Reloads files to watch from the project. Useful when project's structure
     * has changed
     */
    public void reconfigure() {
        pauseWatching();

        removeAllFiles();

        Project project = mediator.getProject();

        // check if project exists and has been saved at least once.
        if (project != null && project.getConfigurationResource() != null) {
            try {
                addFile(project.getConfigurationResource().getURL().toURI());

                for (DataMap dm : ((DataChannelDescriptor) project.getRootNode()).getDataMaps()) {
                    if (dm.getConfigurationSource() != null) {
                        // if DataMap is in separate file, monitor it
                        addFile(dm.getConfigurationSource().getURL().toURI());
                    }
                }
            } catch (URISyntaxException ex) {
                throw new CayenneRuntimeException("Unable to start change tracker", ex);
            }
        }

        resumeWatching();
    }

    protected void doOnChange() {

        SwingUtilities.invokeLater(() -> {
            isShownChangeDialog = true;
            if (showConfirmation("One or more project files were changed by external program. "
                    + "Do you want to load the changes?")) {

                // Currently we are reloading all project
                if (mediator.getProject() != null) {
                    File fileDirectory;
                    try {
                        fileDirectory = new File(mediator.getProject().getConfigurationResource().getURL().toURI());
                    } catch (URISyntaxException e) {
                        throw new CayenneRuntimeException("Unable to open project %s",
                                e, mediator.getProject().getConfigurationResource().getURL());
                    }
                    Application.getInstance().getActionManager()
                            .getAction(OpenProjectAction.class)
                            .openProject(fileDirectory);
                }
            } else {
                mediator.setDirty(true);
            }
            isShownChangeDialog = false;
        });
    }

    protected void doOnRemove() {
        if (mediator.getProject() != null) {

            SwingUtilities.invokeLater(() -> {
                isShownRemoveDialog = true;
                FileDeletedDialog dialog = new FileDeletedDialog(Application.getFrame());
                dialog.show();

                if (dialog.shouldSave()) {
                    Application.getInstance().getActionManager().getAction(SaveAction.class).performAction(null);
                } else if (dialog.shouldClose()) {
                    Application.getInstance().getFrameController().projectClosedAction();
                } else {
                    mediator.setDirty(true);
                }
                isShownRemoveDialog = false;
            });
        }
    }

    /**
     * Shows confirmation dialog
     */
    private boolean showConfirmation(String message) {
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(Application.getFrame(), message, "File changed",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Adds a new file to watch
     * 
     * @param location
     *            path of file
     */
    public void addFile(URI location) {
        try {
            files.put(location, new FileInfo(location));
        } catch (SecurityException e) {
            LOGGER.error("SecurityException adding file " + location, e);
        }
    }

    /**
     * Turns off watching for a specified file
     * 
     * @param location
     *            path of file
     */
    public void removeFile(String location) {
        files.remove(URI.create(location));
    }

    /**
     * Turns off watching for all files
     */
    public void removeAllFiles() {
        files.clear();
    }

    protected void check() {
        if (paused) {
            return;
        }

        boolean hasChanges = false;
        boolean hasDeletions = false;

        for (Iterator<FileInfo> it = files.values().iterator(); it.hasNext();) {
            FileInfo fi = it.next();

            boolean fileExists;
            try {
                fileExists = fi.getFile().exists();
            } catch (SecurityException e) {
                LOGGER.error("SecurityException checking file " + fi.getFile().getPath(), e);

                // we still process with other files
                continue;
            }

            if (fileExists) {
                // this can also throw a SecurityException
                long l = fi.getFile().lastModified();
                if (l > fi.getLastModified()) {
                    // however, if we reached this point this is very unlikely.
                    fi.setLastModified(l);
                    hasChanges = true;
                }
            } else if (fi.getLastModified() != -1) {
                // the file has been removed
                hasDeletions = true;
                it.remove(); // no point to watch the file now
            }
        }

        if (hasDeletions && !isShownRemoveDialog) {
            doOnRemove();
        } else if (hasChanges && !isShownChangeDialog) {
            doOnChange();
        }
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(DEFAULT_DELAY);
                check();
            } catch (InterruptedException e) {
                // someone asked to stop
                return;
            }
        }
    }

    /**
     * Tells watcher to pause watching for some time. Useful before changing
     * files
     */
    public void pauseWatching() {
        paused = true;
    }

    /**
     * Resumes watching for files
     */
    public void resumeWatching() {
        paused = false;
    }

    /**
     * Class to store information about files (last modification time & File
     * pointer)
     */
    protected static class FileInfo {

        /**
         * Exact java.io.File object, may not be null
         */
        private final File file;

        /**
         * Time the file was modified
         */
        private long lastModified;

        /**
         * Creates new object
         * 
         * @param location
         *            the file path
         */
        protected FileInfo(URI location) {
            file = new File(location);
            lastModified = file.exists() ? file.lastModified() : -1;
        }

        protected File getFile() {
            return file;
        }

        protected long getLastModified() {
            return lastModified;
        }

        protected void setLastModified(long l) {
            lastModified = l;
        }
    }

}
