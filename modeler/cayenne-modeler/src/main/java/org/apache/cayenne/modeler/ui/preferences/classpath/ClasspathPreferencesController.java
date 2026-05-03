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
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClasspathPreferencesController extends ChildController<PreferenceDialogController> {

    private final ClasspathPreferencesView view;
    private final List<String> entries;

    public ClasspathPreferencesController(PreferenceDialogController parent) {
        super(parent);

        this.entries = new ArrayList<>(ClasspathPrefs.of(getApplication().getPreferencesRepository()).getEntries());
        this.view = new ClasspathPreferencesView(this);
    }

    @Override
    public Component getView() {
        return view;
    }

    public List<String> getEntries() {
        return entries;
    }

    public void commit() {
        ClasspathPrefs.of(getApplication().getPreferencesRepository()).setEntries(entries);
        getApplication().refreshClassLoader();
    }

    void addJarClicked() {
        chooseClassEntry(FileFilters.getExtensionFileFilter("jar", "JAR Files"), "Select JAR File.", JFileChooser.FILES_ONLY);
    }

    void addClassDirectoryClicked() {
        chooseClassEntry(null, "Select Java Class Directory.", JFileChooser.DIRECTORIES_ONLY);
    }

    void addMvnDependencyClicked() {
        MavenDependencyDialogController dialog = new MavenDependencyDialogController(this);
        dialog.getView().setVisible(true);
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

        entryAdded(selected);
    }

    public void entryRemoved(int selectedRow) {
        if (selectedRow >= 0) {
            entries.remove(selectedRow);
        }
    }

    public void entryAdded(File selected) {
        if (selected == null) {
            return;
        }

        String path = selected.getAbsolutePath();
        if (entries.contains(path)) {
            return;
        }

        entries.add(path);
        view.entryAdded();
    }
}
