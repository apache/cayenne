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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.pref.CayennePreferenceEditor;
import org.apache.cayenne.pref.PreferenceEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClasspathPreferences extends CayenneController {

    private static final Logger logger = LoggerFactory.getLogger(ClasspathPreferences.class);

    private final ClasspathPreferencesView view;
    private final List<String> classPathEntries;
    private final List<String> classPathKeys;
    private final ClasspathTableModel tableModel;
    private final CayennePreferenceEditor editor;
    private final Preferences preferences;

    private int counter;

    public ClasspathPreferences(PreferenceDialog parentController) {
        super(parentController);

        this.view = new ClasspathPreferencesView();

        PreferenceEditor editor = parentController.getEditor();
        this.editor = editor instanceof CayennePreferenceEditor
                ? (CayennePreferenceEditor) editor
                : null;

        // this prefs node is shared with other dialog panels... be aware of
        // that when accessing the keys
        this.preferences = getApplication().getPreferencesNode(this.getClass(), "");

        this.classPathEntries = new ArrayList<>();
        this.classPathKeys = new ArrayList<>();
        this.counter = loadPreferences();
        this.tableModel = new ClasspathTableModel(classPathEntries);

        initBindings();
    }

    private synchronized int loadPreferences() {

        String[] cpKeys;
        try {
            cpKeys = preferences.keys();
        } catch (BackingStoreException e) {
            logger.info("Error loading preferences", e);
            return 0;
        }

        int max = 0;
        for (String cpKey : cpKeys) {
            try {
                int c = Integer.parseInt(cpKey);
                if (c > max) {
                    max = c;
                }
            } catch (NumberFormatException e) {
                // we are sharing the 'preferences' node with other dialogs, and
                // this is a rather poor way of telling our preference keys from
                // other dialog keys ... ours are numeric, the rest are
                // string..

                // TODO: better key namespacing...
                continue;
            }

            String tempValue = preferences.get(cpKey, "");
            if (!"".equals(tempValue)) {
                classPathEntries.add(tempValue);
                classPathKeys.add(cpKey);
            }
        }

        return max;
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        view.getTable().setModel(tableModel);
        view.getAddDirButton().addActionListener(e -> addClassDirectoryAction());
        view.getAddJarButton().addActionListener(e -> addJarOrZipAction());
        view.getAddMvnButton().addActionListener(e -> addMvnDependencyAction());
        view.getRemoveEntryButton().addActionListener(e -> removeEntryAction());
    }

    protected void addJarOrZipAction() {
        chooseClassEntry(FileFilters.getClassArchiveFilter(), "Select JAR or ZIP File.", JFileChooser.FILES_ONLY);
    }

    protected void addClassDirectoryAction() {
        chooseClassEntry(null, "Select Java Class Directory.", JFileChooser.DIRECTORIES_ONLY);
    }

    protected void addMvnDependencyAction() {
        MavenDependencyDialog dialog = new MavenDependencyDialog(this);
        dialog.getView().setVisible(true);
    }

    protected synchronized void removeEntryAction() {
        int selected = view.getTable().getSelectedRow();
        if (selected < 0) {
            return;
        }

        updatePreferences(classPathKeys.get(selected), "");
        classPathEntries.remove(selected);
        classPathKeys.remove(selected);

        tableModel.fireTableRowsDeleted(selected, selected);
    }

    protected void chooseClassEntry(FileFilter filter, String title, int selectionMode) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(selectionMode);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);
        getLastDirectory().updateChooser(chooser);
        if (filter != null) {
            chooser.addChoosableFileFilter(filter);
        }
        chooser.setDialogTitle(title);

        File selected = null;
        int result = chooser.showOpenDialog(view);
        if (result == JFileChooser.APPROVE_OPTION) {
            selected = chooser.getSelectedFile();
        }

        // store last dir in preferences
        getLastDirectory().updateFromChooser(chooser);
        // add to classpath list
        addClasspathEntry(selected);
    }

    public synchronized void addClasspathEntry(File selected) {
        if (selected == null || classPathEntries.contains(selected.getAbsolutePath())) {
            return;
        }

        int len = classPathEntries.size();
        int key = ++counter;

        String value = selected.getAbsolutePath();
        updatePreferences(Integer.toString(key), value);
        classPathEntries.add(value);
        classPathKeys.add(Integer.toString(key));

        tableModel.fireTableRowsInserted(len, len);
    }

    public void updatePreferences(String key, String value) {
        Map<String, String> map = editor.getChangedPreferences().get(preferences);
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, value);
        editor.getChangedPreferences().put(preferences, map);
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
