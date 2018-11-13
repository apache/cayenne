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
package org.apache.cayenne.modeler.dialog.cgen;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.util.CayenneDialog;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 4.1
 */
public class TemplateDialogView extends CayenneDialog {

    private JTable pathTable;
    private JButton useDefault;
    private JButton addTemplate;
    private List<String> missingPathes;

    public TemplateDialogView(Dialog parent, String templatePath, String superTemplatePath) {
        super(parent);
        initPathes(templatePath, superTemplatePath);
        init();
    }

    public TemplateDialogView(Frame parent, String templatePath, String superTemplatePath) {
        super(parent);
        initPathes(templatePath, superTemplatePath);
        init();
    }

    private void initPathes(String templatePath, String superTemplatePath) {
        this.missingPathes = new ArrayList<>();
        if(templatePath != null) {
            missingPathes.add(templatePath);
        }
        if(superTemplatePath != null) {
            missingPathes.add(superTemplatePath);
        }
    }

    private void init() {
        useDefault = new JButton("Use default");
        addTemplate = new JButton("Add template");

        pathTable = new JTable();
        pathTable.setRowHeight(25);
        pathTable.setRowMargin(3);
        pathTable.setCellSelectionEnabled(false);


        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout("fill:200dlu:grow", "pref, 3dlu, fill:40dlu:grow"));

        builder.setDefaultDialogBorder();

        builder.addLabel("This templates are missing: ", cc.xy(1, 1));
        builder.add(new JScrollPane(pathTable), cc.xy(1, 3));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(useDefault);
        buttons.add(addTemplate);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pathTable.setModel(new MissingPathTableModel());

        setPreferredSize(new Dimension(450, 350));
    }

    public JButton getUseDefault() {
        return useDefault;
    }

    public JButton getAddTemplate() {
        return addTemplate;
    }

    class MissingPathTableModel extends AbstractTableModel {

        public int getRowCount() {
            return missingPathes.size();
        }

        public int getColumnCount() {
            return 1;
        }

        public Object getValueAt(int row, int col) {
            return missingPathes.get(row);
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public String getColumnName(int column) {
            return " ";
        }

        public Class getColumnClass(int columnIndex) {
            return String.class;
        }
    }
}
