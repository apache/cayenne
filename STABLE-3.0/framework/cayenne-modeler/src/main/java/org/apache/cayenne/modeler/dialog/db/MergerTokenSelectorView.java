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

package org.apache.cayenne.modeler.dialog.db;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class MergerTokenSelectorView extends JPanel {

    protected JTable tokens;
    protected JCheckBox checkAll;
    protected JLabel checkAllLabel;
    protected JButton reverseAll;

    public MergerTokenSelectorView() {

        this.checkAll = new JCheckBox();
        this.checkAllLabel = new JLabel("Check All Operations");
        this.reverseAll = new JButton("Reverse All Operations");

        checkAll.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent event) {
                if (checkAll.isSelected()) {
                    checkAllLabel.setText("Uncheck All Operations");
                }
                else {
                    checkAllLabel.setText("Check All Operations");
                }
            }
        });

        // assemble
        JPanel checkAllPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        checkAllPanel.add(checkAll);
        checkAllPanel.add(checkAllLabel);
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(checkAllPanel, BorderLayout.WEST);
        topPanel.add(ButtonBarFactory.buildRightAlignedBar(reverseAll), BorderLayout.EAST);

        tokens = new JTable();
        tokens.setRowHeight(25);
        tokens.setRowMargin(3);

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "p, 3dlu, fill:40dlu:grow"));
        builder.setDefaultDialogBorder();
        builder.addSeparator("Select Operations", cc.xy(1, 1));
        builder.add(new JScrollPane(
                tokens,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JTable getTokens() {
        return tokens;
    }

    public JCheckBox getCheckAll() {
        return checkAll;
    }
    
    public JButton getReverseAll() {
        return reverseAll;
    }
}
