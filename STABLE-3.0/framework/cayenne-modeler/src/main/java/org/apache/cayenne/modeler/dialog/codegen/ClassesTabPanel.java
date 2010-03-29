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

package org.apache.cayenne.modeler.dialog.codegen;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;

/**
 */
public class ClassesTabPanel extends JPanel {

    protected JTable table;
    protected JCheckBox checkAll;
    protected JLabel checkAllLabel;

    public ClassesTabPanel() {

        this.table = new JTable();

        // TODO: andrus 04/07/2006 - is there an easy way to stick that checkbox in the
        // table header????
        this.checkAll = new JCheckBox();
        this.checkAllLabel = new JLabel("Check All Classes");

        checkAll.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent event) {
                if (checkAll.isSelected()) {
                    checkAllLabel.setText("Uncheck All Classess");
                }
                else {
                    checkAllLabel.setText("Check All Classes");
                }
            }
        });

        // assemble
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        topPanel.add(checkAll);
        topPanel.add(checkAllLabel);

        JScrollPane tablePanel = new JScrollPane(
                table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // set some minimal preferred size, so that it is smaller than other forms used in
        // the dialog... this way we get the right automated overall size
        tablePanel.setPreferredSize(new Dimension(300, 200));

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
    }

    public JTable getTable() {
        return table;
    }

    public JCheckBox getCheckAll() {
        return checkAll;
    }
}
