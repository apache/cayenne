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
import org.apache.cayenne.modeler.toolkit.table.CMTable;

import javax.swing.*;
import java.awt.*;


public class ClasspathPreferencesView extends JPanel {

    private final JButton addJarButton;
    private final JButton addDirButton;
    private final JButton addMvnButton;
    private final JButton deleteEntryButton;
    private final JTable table;

    public ClasspathPreferencesView() {

        // create widgets
        addJarButton = new JButton("Add Jar");
        addDirButton = new JButton("Add Class Folder");
        addMvnButton = new JButton("Get From Maven Central");
        deleteEntryButton = new JButton("Delete");

        table = new CMTable();
        table.setRowMargin(3);
        table.setRowHeight(25);
        table.setTableHeader(null);

        // assemble

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

    public JButton getAddDirButton() {
        return addDirButton;
    }

    public JButton getAddJarButton() {
        return addJarButton;
    }

    public JButton getAddMvnButton() {
        return addMvnButton;
    }

    public JButton getDeleteEntryButton() {
        return deleteEntryButton;
    }

    public JTable getTable() {
        return table;
    }
}
