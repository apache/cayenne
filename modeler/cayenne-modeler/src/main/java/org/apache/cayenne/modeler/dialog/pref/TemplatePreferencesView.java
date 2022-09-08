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

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.util.CayenneTable;

public class TemplatePreferencesView extends JPanel {

    protected JButton addButton;
    protected JButton createFromDefaultButton;
    protected JButton removeButton;
    protected JTable table;

    public TemplatePreferencesView() {

        // create widgets
        addButton = new JButton("Add Template");
        createFromDefaultButton = new JButton("Create from default");
        removeButton = new JButton("Remove Template");

        table = new CayenneTable();
        table.setRowMargin(3);
        table.setRowHeight(25);

        // assemble

        FormLayout layout = new FormLayout("fill:min(150dlu;pref)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append(addButton);
        builder.append(createFromDefaultButton);
        builder.append(removeButton);

        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
        add(builder.getPanel(), BorderLayout.EAST);
    }

    public JTable getTable() {
        return table;
    }

    public JButton getAddButton() {
        return addButton;
    }

    public JButton getCreateFromDefaultButton() {
        return createFromDefaultButton;
    }

    public JButton getRemoveButton() {
        return removeButton;
    }
}
