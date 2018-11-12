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

package org.apache.cayenne.modeler.editor.cgen.domain;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

/**
 * @since 4.1
 */
public class CgenTab extends JPanel {

    protected ProjectController projectController;
    private CgenTabController cgenTabController;

    private JCheckBox selectAll;
    private JButton generateAll;

    public CgenTab(ProjectController projectController, CgenTabController cgenTabController) {
        this.projectController = projectController;
        this.cgenTabController = cgenTabController;
        this.selectAll = new JCheckBox();
        generateAll = new JButton("Generate");
        generateAll.setEnabled(false);
        generateAll.setIcon(ModelerUtil.buildIcon("icon-gen_java.png"));
        generateAll.setPreferredSize(new Dimension(120, 30));
        generateAll.addActionListener(action -> cgenTabController.runGenerators(cgenTabController.getSelectedDataMaps()));
        setLayout(new BorderLayout());
    }

    public void initView() {
        removeAll();
        cgenTabController.createPanels();
        FormLayout layout = new FormLayout(
                "left:pref, 4dlu, 50dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        ConcurrentMap<DataMap, CgenPanel> panels = cgenTabController.getGeneratorsPanels();

        if(panels.isEmpty()) {
            this.add(new JLabel("There are no cgen configs."), BorderLayout.NORTH);
            return;
        }

        JPanel selectAllPanel = new JPanel(new FlowLayout());
        selectAllPanel.add(new JLabel("Select All"), FlowLayout.LEFT);
        selectAllPanel.add(selectAll, FlowLayout.CENTER);
        builder.append(selectAllPanel);
        builder.nextLine();

        SortedSet<DataMap> keys = new TreeSet<>(panels.keySet());
        for(DataMap dataMap : keys) {
            builder.append(panels.get(dataMap));
            builder.nextLine();
        }
        builder.append(generateAll);
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    void showSuccessMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Class generation finished");
    }

    void showErrorMessage(String msg) {
        JOptionPane.showMessageDialog(
                this,
                "Error generating classes - " + msg);
    }

    void showEmptyMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Nothing to generate - ");
    }

    public JCheckBox getSelectAll() {
        return selectAll;
    }

    public JButton getGenerateAll() {
        return generateAll;
    }
}
