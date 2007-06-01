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
package org.objectstyle.cayenne.modeler.action;

import java.awt.Frame;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.dialog.OverwriteDialog;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.project.DataMapProject;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.ProjectFile;

/**
 * File chooser panel used to select a directory to store project files.
 * 
 * @author Andrei Adamchik
 */
class ProjectOpener extends JFileChooser {

    private static final Logger logObj = Logger.getLogger(ProjectOpener.class);

    /**
     * Selects a directory to store the project.
     */
    File newProjectDir(Frame f, Project p) {
        if (p instanceof ApplicationProject) {
            // configure for application project
            return newProjectDir(f, Configuration.DEFAULT_DOMAIN_FILE, FileFilters
                    .getApplicationFilter());
        }
        else if (p instanceof DataMapProject) {
            // configure for DataMap project
            ProjectFile projFileWrapper = p.projectFileForObject(p);
            return newProjectDir(f, projFileWrapper.getLocation(), FileFilters
                    .getDataMapFilter());
        }
        else {
            String message = (p == null)
                    ? "Null project."
                    : "Unrecognized project class: " + p.getClass().getName();
            throw new CayenneRuntimeException(message);
        }
    }

    File newProjectDir(Frame f, String location, FileFilter filter) {
        // configure dialog
        setDialogTitle("Select Project Directory");
        setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        setCurrentDirectory(getDefaultStartDir());

        // preselect current directory
        if (getCurrentDirectory() != null) {
            setSelectedFile(getCurrentDirectory());
        }

        // configure filters
        resetChoosableFileFilters();
        // allow users to see directories with cayenne.xml files
        addChoosableFileFilter(filter);

        File selectedDir;

        while (true) {
            int status = showDialog(f, "Select");
            selectedDir = getSelectedFile();
            if (status != JFileChooser.APPROVE_OPTION || selectedDir == null) {
                logObj.info("Save canceled.");
                return null;
            }

            // normalize selection
            logObj.info("Selected: " + selectedDir);
            if (!selectedDir.isDirectory()) {
                selectedDir = getSelectedFile().getParentFile();
            }

            // check for overwrite
            File projectFile = new File(selectedDir, location);
            if (projectFile.exists()) {
                OverwriteDialog dialog = new OverwriteDialog(projectFile, f);
                dialog.show();

                if (dialog.shouldOverwrite()) {
                    break;
                }
                else if (dialog.shouldSelectAnother()) {
                    continue;
                }
                else {
                    // canceled
                    return null;
                }
            }
            else {
                break;
            }
        }

        return selectedDir;
    }

    /**
     * Runs a dialog to open Cayenne project.
     */
    File openProjectFile(Frame f) {

        // configure dialog
        setDialogTitle("Select Project File");
        setFileSelectionMode(JFileChooser.FILES_ONLY);
        setCurrentDirectory(getDefaultStartDir());

        // configure filters
        resetChoosableFileFilters();
        addChoosableFileFilter(FileFilters.getApplicationFilter());
        addChoosableFileFilter(FileFilters.getDataMapFilter());

        // default to App projects
        setFileFilter(FileFilters.getApplicationFilter());

        int status = showOpenDialog(f);
        if (status != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        return getSelectedFile();
    }

    /**
     * Returns directory where file search should start. This is either coming from saved
     * preferences, or a current directory is used.
     */
    File getDefaultStartDir() {
        // find start directory in preferences
        return Application
                .getInstance()
                .getFrameController()
                .getLastDirectory()
                .getExistingDirectory(true);
    }
}