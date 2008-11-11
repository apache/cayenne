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

package org.apache.cayenne.swing.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.apache.cayenne.util.Util;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A control that renders as a text field and a button to choose a file. Fires a property
 * change event when a current directory is changed, either explictly or during a file
 * selection by the user.
 * 
 */
public class FileChooser extends JPanel {

    public static final String CURRENT_DIRECTORY_PROPERTY = "currentDirectory";

    protected boolean existingOnly;
    protected boolean allowFiles;
    protected boolean allowDirectories;
    protected FileFilter fileFilter;
    protected String title;
    protected File currentDirectory;

    protected JTextField fileName;
    protected JButton chooseButton;

    public FileChooser() {
        this.allowFiles = false;
        this.allowFiles = true;

        this.fileName = new JTextField();
        this.chooseButton = new JButton("...");

        FormLayout layout = new FormLayout("pref:grow, 3dlu, pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.append(fileName, chooseButton);

        chooseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                chooseFileAction();
            }
        });
    }

    public FileChooser(String title, boolean allowFiles, boolean allowDirectories) {
        this();

        this.allowFiles = allowFiles;
        this.allowDirectories = allowDirectories;
        this.title = title;
    }

    public File getFile() {
        String value = fileName.getText();
        if (Util.isEmptyString(value)) {
            return null;
        }

        File file = new File(value);
        if (existingOnly && !file.exists()) {
            return null;
        }

        return file;
    }

    public void setFile(File file) {
        fileName.setText(file != null ? file.getAbsolutePath() : "");
    }

    protected void chooseFileAction() {
        int mode = getSelectionMode();

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(mode);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);

        if (fileFilter != null) {
            chooser.setFileFilter(fileFilter);
        }

        if (currentDirectory != null) {
            chooser.setCurrentDirectory(currentDirectory);
        }

        chooser.setDialogTitle(makeTitle(mode));

        int result = chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this));
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            fileName.setText(selected != null ? selected.getAbsolutePath() : "");
        }

        setCurrentDirectory(chooser.getCurrentDirectory());
    }

    protected String makeTitle(int selectionMode) {
        if (title != null) {
            return title;
        }

        switch (selectionMode) {
            case JFileChooser.FILES_AND_DIRECTORIES:
                return "Choose a file or a directory";
            case JFileChooser.DIRECTORIES_ONLY:
                return "Choose a directory";
            default:
                return "Choose a file";
        }
    }

    protected int getSelectionMode() {
        if (allowFiles && allowDirectories) {
            return JFileChooser.FILES_AND_DIRECTORIES;
        }
        else if (allowFiles && !allowDirectories) {
            return JFileChooser.FILES_ONLY;
        }
        else if (!allowFiles && allowDirectories) {
            return JFileChooser.DIRECTORIES_ONLY;
        }
        else {
            // invalid combination... return files...
            return JFileChooser.FILES_ONLY;
        }
    }

    public boolean isAllowDirectories() {
        return allowDirectories;
    }

    public void setAllowDirectories(boolean allowDirectories) {
        this.allowDirectories = allowDirectories;
    }

    public boolean isAllowFiles() {
        return allowFiles;
    }

    public void setAllowFiles(boolean allowFiles) {
        this.allowFiles = allowFiles;
    }

    public FileFilter getFileFilter() {
        return fileFilter;
    }

    public void setFileFilter(FileFilter filteFiler) {
        this.fileFilter = filteFiler;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isExistingOnly() {
        return existingOnly;
    }

    public void setExistingOnly(boolean existingOnly) {
        this.existingOnly = existingOnly;
    }

    public void setColumns(int col) {
        fileName.setColumns(col);
    }

    public int getColumns() {
        return fileName.getColumns();
    }

    /**
     * Returns the last directory visited when picking a file.
     */
    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(File currentDirectory) {
        if (!Util.nullSafeEquals(this.currentDirectory, currentDirectory)) {
            File oldValue = this.currentDirectory;
            this.currentDirectory = currentDirectory;
            firePropertyChange(CURRENT_DIRECTORY_PROPERTY, oldValue, currentDirectory);
        }
    }
}
