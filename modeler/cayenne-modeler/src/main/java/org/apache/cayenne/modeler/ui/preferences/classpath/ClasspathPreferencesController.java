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

package org.apache.cayenne.modeler.ui.preferences.classpath;

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.pref.ClasspathPrefs;
import org.apache.cayenne.modeler.toolkit.filechooser.CMFileChooserPrefs;
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialogController;
import org.apache.cayenne.modeler.ui.preferences.classpath.maven.MavenDependencyDialogController;
import org.apache.cayenne.modeler.util.FileFilters;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClasspathPreferencesController extends ChildController<PreferenceDialogController> {

    private final ClasspathPreferencesView view;
    private final List<String> classPathEntries;
    private final ClasspathTableModel tableModel;

    public ClasspathPreferencesController(PreferenceDialogController parent) {
        super(parent);

        this.view = new ClasspathPreferencesView();
        this.classPathEntries = new ArrayList<>(ClasspathPrefs.of(getApplication().getPreferencesRepository()).getEntries());
        this.tableModel = new ClasspathTableModel(classPathEntries);

        initBindings();
    }

    public Component getView() {
        return view;
    }

    public List<String> getEntries() {
        return classPathEntries;
    }

    /**
     * Flushes the working snapshot to {@link ClasspathPrefs} and refreshes the
     * Modeler classloader. Called on dialog Save.
     */
    public void commit() {
        ClasspathPrefs.of(getApplication().getPreferencesRepository()).setEntries(classPathEntries);
        getApplication().refreshClassLoader();
    }

    protected void initBindings() {
        view.getTable().setModel(tableModel);
        view.getAddDirButton().addActionListener(e -> addClassDirectoryAction());
        view.getAddJarButton().addActionListener(e -> addJarAction());
        view.getAddMvnButton().addActionListener(e -> addMvnDependencyAction());
        view.getDeleteEntryButton().addActionListener(e -> removeEntryAction());
    }

    protected void addJarAction() {
        chooseClassEntry(FileFilters.getExtensionFileFilter("jar", "JAR Files"), "Select JAR File.", JFileChooser.FILES_ONLY);
    }

    protected void addClassDirectoryAction() {
        chooseClassEntry(null, "Select Java Class Directory.", JFileChooser.DIRECTORIES_ONLY);
    }

    protected void addMvnDependencyAction() {
        MavenDependencyDialogController dialog = new MavenDependencyDialogController(this);
        dialog.getView().setVisible(true);
    }

    protected synchronized void removeEntryAction() {
        int selected = view.getTable().getSelectedRow();
        if (selected < 0) {
            return;
        }

        classPathEntries.remove(selected);
        tableModel.fireTableRowsDeleted(selected, selected);
    }

    protected void chooseClassEntry(FileFilter filter, String title, int selectionMode) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(selectionMode);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);

        CMFileChooserPrefs.of(getApplication().getPreferencesRepository(), "classpath/lastDir").bind(chooser);
        if (filter != null) {
            chooser.addChoosableFileFilter(filter);
            chooser.setFileFilter(filter);
        }
        chooser.setDialogTitle(title);

        File selected = null;
        int result = chooser.showOpenDialog(view);
        if (result == JFileChooser.APPROVE_OPTION) {
            selected = chooser.getSelectedFile();
        }

        addClasspathEntry(selected);
    }

    public synchronized void addClasspathEntry(File selected) {
        if (selected == null) {
            return;
        }
        String path = selected.getAbsolutePath();
        if (classPathEntries.contains(path)) {
            return;
        }

        int len = classPathEntries.size();
        classPathEntries.add(path);
        tableModel.fireTableRowsInserted(len, len);
    }

    static class ClasspathTableModel extends AbstractTableModel {

        private final List<String> classPathEntries;

        ClasspathTableModel(List<String> classPathEntries) {
            this.classPathEntries = classPathEntries;
        }

        public int getColumnCount() {
            return 1;
        }

        public int getRowCount() {
            return classPathEntries.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return classPathEntries.get(rowIndex);
        }

        public String getColumnName(int column) {
            return "Custom ClassPath";
        }
    }
}
