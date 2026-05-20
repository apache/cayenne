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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.adapters.ClasspathPrefs;
import org.apache.cayenne.modeler.pref.adapters.FileChooserPrefs;
import org.apache.cayenne.modeler.toolkit.table.CMTable;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.modeler.ui.preferences.classpath.maven.MavenDependencyDialog;
import org.apache.cayenne.modeler.toolkit.filechooser.FileFilters;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClasspathPrefsPanel extends AppPanel {

    private final List<String> entries;
    private final JTable table;
    private final ClasspathTableModel tableModel;

    public ClasspathPrefsPanel(Application app) {
        super(app);

        this.entries = new ArrayList<>(new ClasspathPrefs(app.getPrefsLocator().appNode(ClasspathPrefs.NODE)).getEntries());
        this.tableModel = new ClasspathTableModel();
        this.table = new CMTable();
        this.table.setRowMargin(3);
        this.table.setRowHeight(25);
        this.table.setTableHeader(null);
        this.table.setModel(tableModel);

        initLayout();
    }

    public List<String> getEntries() {
        return entries;
    }

    public void commit() {
        new ClasspathPrefs(app.getPrefsLocator().appNode(ClasspathPrefs.NODE)).setEntries(entries);
        app.refreshClassLoader();
    }

    /**
     * Adds the given file to the classpath if not already present and refreshes the table.
     * Called from the Maven dependency download dialog after a successful download.
     */
    public void entryAdded(File selected) {
        if (selected == null) {
            return;
        }

        String path = selected.getAbsolutePath();
        if (entries.contains(path)) {
            return;
        }

        entries.add(path);
        int len = tableModel.getRowCount();
        tableModel.fireTableRowsInserted(len, len);
    }

    private void initLayout() {
        JButton addJarButton = new JButton("Add Jar");
        JButton addDirButton = new JButton("Add Class Folder");
        JButton addMvnButton = new JButton("Get From Maven Central");
        JButton deleteEntryButton = new JButton("Delete");

        addJarButton.addActionListener(e ->
                chooseClassEntry(FileFilters.getExtensionFileFilter("jar", "JAR Files"), "Select JAR File.", JFileChooser.FILES_ONLY));
        addDirButton.addActionListener(e ->
                chooseClassEntry(null, "Select Java Class Directory.", JFileChooser.DIRECTORIES_ONLY));
        addMvnButton.addActionListener(e ->
                new MavenDependencyDialog(app, SwingUtilities.getWindowAncestor(this), this).open());
        deleteEntryButton.addActionListener(e -> removeEntryClicked());

        DefaultFormBuilder sidebar = new DefaultFormBuilder(
                new FormLayout("fill:min(150dlu;pref)", ""));
        sidebar.append(addJarButton);
        sidebar.append(addDirButton);
        sidebar.append(addMvnButton);
        sidebar.append(deleteEntryButton);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel content = new JPanel(new BorderLayout());
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(sidebar.getPanel(), BorderLayout.EAST);

        CellConstraints cc = new CellConstraints();
        PanelBuilder outer = new PanelBuilder(new FormLayout(
                "fill:default:grow",
                "p, 3dlu, fill:default:grow"));
        outer.setDefaultDialogBorder();
        outer.addSeparator("Extra Classpath", cc.xy(1, 1));
        outer.add(content, cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(outer.getPanel(), BorderLayout.CENTER);
    }

    private void chooseClassEntry(FileFilter filter, String title, int selectionMode) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(selectionMode);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);

        new FileChooserPrefs(app.getPrefsManager().uiNode("classpath/lastDir")).bind(chooser);
        if (filter != null) {
            chooser.addChoosableFileFilter(filter);
            chooser.setFileFilter(filter);
        }
        chooser.setDialogTitle(title);

        File selected = null;
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selected = chooser.getSelectedFile();
        }

        entryAdded(selected);
    }

    private void removeEntryClicked() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        entries.remove(selectedRow);
        tableModel.fireTableRowsDeleted(selectedRow, selectedRow);
    }

    private class ClasspathTableModel extends AbstractTableModel {

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return entries.get(rowIndex);
        }

        @Override
        public String getColumnName(int column) {
            return "Custom ClassPath";
        }
    }
}
