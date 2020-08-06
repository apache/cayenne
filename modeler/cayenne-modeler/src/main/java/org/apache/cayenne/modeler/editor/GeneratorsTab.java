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
package org.apache.cayenne.modeler.editor;

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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

/**
 * @since 4.1
 */
public class GeneratorsTab extends JPanel {

    protected ProjectController projectController;
    private GeneratorsTabController<?> additionalTabController;

    private TopGeneratorPanel generationPanel;

    public GeneratorsTab(ProjectController projectController, GeneratorsTabController<?> additionalTabController, String icon, String text) {
        this.projectController = projectController;
        this.additionalTabController = additionalTabController;
        this.generationPanel = new TopGeneratorPanel(icon);
        this.generationPanel.generateAll.addActionListener(action -> additionalTabController.runGenerators(additionalTabController.getSelectedDataMaps()));
        this.generationPanel.generateAll.setToolTipText(text);
        setLayout(new BorderLayout());
    }

    public void initView() {
        removeAll();
        additionalTabController.createPanels();
        FormLayout layout = new FormLayout(
                "left:pref, 4dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        ConcurrentMap<DataMap, GeneratorsPanel> panels = additionalTabController.getGeneratorsPanels();

        if(panels.isEmpty()) {
            this.add(new JLabel("There are no datamaps."), BorderLayout.NORTH);
            return;
        }

        builder.append(generationPanel);
        builder.nextLine();
        SortedSet<DataMap> keys = new TreeSet<>(panels.keySet());
        for(DataMap dataMap : keys) {
            builder.append(panels.get(dataMap));
            builder.nextLine();
        }
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void showEmptyMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Nothing to generate");
    }

    TopGeneratorPanel getGenerationPanel() {
        return generationPanel;
    }

    class TopGeneratorPanel extends JPanel {

        private JCheckBox selectAll;
        JButton generateAll;

        TopGeneratorPanel(String icon) {
            setLayout(new BorderLayout());
            FormLayout layout = new FormLayout(
                    "left:pref, 4dlu, fill:70dlu, 3dlu, fill:120, 3dlu, fill:120", "");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            this.selectAll = new JCheckBox();
            generateAll = new JButton("Run");
            generateAll.setEnabled(false);
            generateAll.setIcon(ModelerUtil.buildIcon(icon));
            builder.append(selectAll, new JLabel("Select All"), generateAll);
            this.add(builder.getPanel(), BorderLayout.CENTER);
        }

        JCheckBox getSelectAll() {
            return selectAll;
        }

        JButton getGenerateAll() {
            return generateAll;
        }
    }
}
