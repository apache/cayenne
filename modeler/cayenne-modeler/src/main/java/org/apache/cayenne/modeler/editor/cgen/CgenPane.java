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

package org.apache.cayenne.modeler.editor.cgen;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * @since 4.1
 */
public class CgenPane extends JPanel {

    private JButton generateButton;
    private JComboBox<String> configurationsComboBox;
    private JButton addConfigBtn;
    private JButton editConfigBtn;
    private JButton removeConfigBtn;
    private JSplitPane splitPane;

    public CgenPane(Component generatorPanel, Component entitySelectorPanel) {
        super();
        this.setLayout(new BorderLayout());

        initSplitPanel(generatorPanel, entitySelectorPanel);
        buildView();
    }

    private void buildView() {
        add(getTopPanel(), BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void initSplitPanel(Component generatorPanel, Component entitySelectorPanel) {
        this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JScrollPane scrollPane = new JScrollPane(
                generatorPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(150, 400));

        // assemble
        splitPane.setRightComponent(scrollPane);
        splitPane.setLeftComponent(entitySelectorPanel);
    }

    private JPanel getTopPanel() {
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BorderLayout());
        configPanel.add(getConfigurationsPanel(), BorderLayout.WEST);
        configPanel.add(getGeneratePanel(), BorderLayout.EAST);
        configPanel.setBorder(CgenConfigPanel.CGEN_PANEL_BORDER);
        return configPanel;
    }

    private JPanel getConfigurationsPanel() {
        FormLayout configCroupLayout = new FormLayout(
                "109dlu,3dlu,pref,3dlu,pref,3dlu,pref",
                "p");
        PanelBuilder configCroupBuilder = new PanelBuilder(configCroupLayout);
        CellConstraints cc = new CellConstraints();
        this.configurationsComboBox = new JComboBox<>();
        this.addConfigBtn = new JButton(ModelerUtil.buildIcon("icon-new.png"));
        addConfigBtn.setToolTipText("New configuration");
        this.editConfigBtn = new JButton(ModelerUtil.buildIcon("icon-edit.png"));
        editConfigBtn.setToolTipText("Rename configuration");
        this.removeConfigBtn = new JButton(ModelerUtil.buildIcon("icon-trash.png"));
        removeConfigBtn.setToolTipText("Remove configuration");
        configCroupBuilder.add(configurationsComboBox, cc.xy(1, 1));
        configCroupBuilder.add(addConfigBtn, cc.xy(3, 1));
        configCroupBuilder.add(editConfigBtn, cc.xy(5, 1));
        configCroupBuilder.add(removeConfigBtn, cc.xy(7, 1));
        return configCroupBuilder.getPanel();
    }

    private JPanel getGeneratePanel() {
        this.generateButton = new JButton("Generate");
        this.generateButton.setIcon(ModelerUtil.buildIcon("icon-gen_java.png"));
        this.generateButton.setEnabled(false);
        FormLayout generateCroupLayout = new FormLayout(
                "60dlu", "p");
        PanelBuilder generateCroupBuilder = new PanelBuilder(generateCroupLayout);
        CellConstraints cc = new CellConstraints();

        generateCroupBuilder.add(generateButton,cc.xy(1,1));
        return generateCroupBuilder.getPanel();
    }

    public JButton getGenerateButton() {
        return generateButton;
    }

    public JComboBox<String> getConfigurationsComboBox() {
        return configurationsComboBox;
    }

    public JButton getAddConfigBtn() {
        return addConfigBtn;
    }

    public JButton getEditConfigBtn() {
        return editConfigBtn;
    }

    public JButton getRemoveConfigBtn() {
        return removeConfigBtn;
    }
}
