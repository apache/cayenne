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
package org.apache.cayenne.modeler.ui.project;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.action.OpenProjectAction;
import org.apache.cayenne.modeler.ui.action.SaveAction;
import org.apache.cayenne.modeler.ui.filedeleted.FileDeletedDialog;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks changes in cayenne.xml and other Cayenne project files
 */
class ProjectFileChangeTracker extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectFileChangeTracker.class);

    // The default delay between every file modification check
    private static final long DEFAULT_DELAY = 4000;

    private final Map<URI, TrackedFile> files;
    private final ProjectController controller;

    private boolean paused;
    private boolean isShownChangeDialog;
    private boolean isShownRemoveDialog;

    public ProjectFileChangeTracker(ProjectController controller) {
        this.files = new ConcurrentHashMap<>();
        this.controller = controller;
        setName("cayenne-modeler-file-change-tracker");
    }

    /**
     * Reloads files to watch from the project. Useful when project's structure has changed
     */
    public void reset() {
        pauseTracking();

        files.clear();

        Project project = controller.getProject();

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

        resumeTracking();
    }

    private void doOnChange() {

        SwingUtilities.invokeLater(() -> {
            isShownChangeDialog = true;
            if (showConfirmation("One or more project files were changed by external program. "
                    + "Do you want to load the changes?")) {

                // Currently we are reloading all project
                if (controller.getProject() != null) {
                    File fileDirectory;
                    try {
                        fileDirectory = new File(controller.getProject().getConfigurationResource().getURL().toURI());
                    } catch (URISyntaxException e) {
                        throw new CayenneRuntimeException("Unable to open project %s",
                                e, controller.getProject().getConfigurationResource().getURL());
                    }
                    Application.getInstance().getActionManager()
                            .getAction(OpenProjectAction.class)
                            .openProject(fileDirectory);
                }
            } else {
                controller.setDirty(true);
            }
            isShownChangeDialog = false;
        });
    }

    private void doOnRemove() {
        if (controller.getProject() != null) {

            SwingUtilities.invokeLater(() -> {
                isShownRemoveDialog = true;
                FileDeletedDialog dialog = new FileDeletedDialog(controller.getApplication().getFrameController().getView());
                dialog.show();

                if (dialog.shouldSave()) {
                    Application.getInstance().getActionManager().getAction(SaveAction.class).performAction(null);
                } else if (dialog.shouldClose()) {
                    Application.getInstance().getFrameController().onProjectClosed();
                } else {
                    controller.setDirty(true);
                }
                isShownRemoveDialog = false;
            });
        }
    }

    private boolean showConfirmation(String message) {
        int outcome = JOptionPane.showConfirmDialog(controller.getApplication().getFrameController().getView(), message, "File changed", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return outcome == JOptionPane.YES_OPTION;
    }

    /**
     * Adds a new file to watch
     *
     * @param location path of file
     */
    public void addFile(URI location) {
        try {
            files.put(location, new TrackedFile(location));
        } catch (SecurityException e) {
            LOGGER.error("SecurityException adding file " + location, e);
        }
    }

    private void check() {
        if (paused) {
            return;
        }

        boolean hasChanges = false;
        boolean hasDeletions = false;

        for (Iterator<TrackedFile> it = files.values().iterator(); it.hasNext(); ) {
            TrackedFile fi = it.next();

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

    @Override
    public void run() {
        while (true) {
            try {
                check();
                Thread.sleep(DEFAULT_DELAY);
            } catch (InterruptedException e) {
                // someone asked to stop
                return;
            }
        }
    }

    /**
     * Tells watcher to pause watching for some time. Useful before changing files
     */
    public void pauseTracking() {
        paused = true;
    }

    /**
     * Resumes watching for files
     */
    public void resumeTracking() {
        paused = false;
    }

    /**
     * Class to store information about files (last modification time & File
     * pointer)
     */
    private static class TrackedFile {

        private final File file;
        private long lastModified;

        protected TrackedFile(URI location) {
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
