/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.dialog.db;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.db.model.DBModel;
import org.apache.cayenne.modeler.dialog.pref.TreeEditor;
import org.apache.cayenne.modeler.dialog.pref.XMLFileEditor;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayListener;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class ReverseEngineeringView extends JPanel {
    protected ProjectController controller;
    public ReverseEngineeringController reverseEngineeringController;
    protected JPanel reverseEngineering;

    protected JComboBox dataSources;
    protected JButton configButton;
    protected JButton syncButton;
    protected JButton executeButton;

    protected PanelBuilder builder;
    protected JSeparator separator;
    protected JSplitPane splitPane;
    protected JLabel xmlLabel;
    protected JLabel treeLabel;
    protected Icon icon;

    protected XMLFileEditor xmlFileEditor;
    protected TreeEditor treeEditor;

    protected DataMap tempDataMap;

    protected Map<String, DataMapViewModel> reverseEngineeringViewMap;


    private String template =
            "<reverseEngineering>\n" +
                    "    <skipRelationshipsLoading>false</skipRelationshipsLoading>\n" +
                    "    <skipPrimaryKeyLoading>false</skipPrimaryKeyLoading>\n" +
                    "\n" +
                    "    <catalog>\n" +
                    "        <schema>\n" +
                    "            <includeTable>\n" +
                    "            </includeTable>\n" +
                    "        </schema>\n" +
                    "    </catalog>\n" +
                    "    <includeProcedure pattern=\".*\"/>\n" +
                    "</reverseEngineering>";

    public ReverseEngineeringView(ProjectController controller) {
        this.controller = controller;

        this.reverseEngineeringViewMap = new HashMap<>();
        initView();
        initController();
    }

    private void initView() {
        this.reverseEngineering = new JPanel();
        this.dataSources = new JComboBox();
        this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        this.configButton = new JButton("...");
        this.configButton.setToolTipText("configure local DataSource");
        this.syncButton = new JButton();
        this.icon = ModelerUtil.buildIcon("icon-refresh.png");
        this.syncButton.setIcon(icon);
        this.syncButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        this.executeButton = new JButton("Execute");
        this.treeLabel = new JLabel("Preview");
        this.xmlLabel = new JLabel("Reverse Engineering XML Editor");
        this.treeLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        this.xmlLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

        this.treeEditor = new TreeEditor(controller);
        this.xmlFileEditor = new XMLFileEditor(controller);

        CellConstraints cc = new CellConstraints();
        this.builder = new PanelBuilder(new FormLayout(
                "210dlu:grow, pref, 0dlu, fill:max(172dlu;pref), 3dlu, fill:20dlu",
                "p"));
        builder.setDefaultDialogBorder();
        builder.add(dataSources, cc.xy(4, 1));
        builder.add(configButton, cc.xy(6, 1));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(executeButton);

        JPanel treeHeaderComponent = new JPanel(new FlowLayout(FlowLayout.LEFT));
        treeHeaderComponent.add(treeLabel);
        treeHeaderComponent.add(syncButton);

        JPanel leftComponent = new JPanel();
        leftComponent.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        leftComponent.setLayout(new BorderLayout());
        leftComponent.add(xmlLabel, BorderLayout.NORTH);
        leftComponent.add(xmlFileEditor.getView().getScrollPane(), BorderLayout.CENTER);

        JPanel rightComponent = new JPanel();
        rightComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        rightComponent.setLayout(new BorderLayout());
        rightComponent.add(treeHeaderComponent, BorderLayout.NORTH);
        rightComponent.add(treeEditor.getView().getScrollPane(), BorderLayout.CENTER);

        splitPane.setLeftComponent(leftComponent);
        splitPane.setRightComponent(rightComponent);
        splitPane.setResizeWeight(0.5);

        JPanel splitWithErrorsPanel = new JPanel();
        splitWithErrorsPanel.setLayout(new BorderLayout());
        splitWithErrorsPanel.add(splitPane, BorderLayout.CENTER);
        xmlFileEditor.getView().getLabel().setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        splitWithErrorsPanel.add(xmlFileEditor.getView().getLabel(), BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.NORTH);
        add(splitWithErrorsPanel, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    public void initController() {
        controller.addDataMapDisplayListener(new DataMapDisplayListener() {
            public void currentDataMapChanged(DataMapDisplayEvent e) {
                DataMap map = e.getDataMap();

                if (tempDataMap != null) {
                    String mapName = tempDataMap.getName();
                    DataMapViewModel dataMapViewModel = new DataMapViewModel(mapName);
                    String xmlText = xmlFileEditor.getView().getEditorPane().getText();
                    dataMapViewModel.setReverseEngineeringText(xmlText);
                    if (reverseEngineeringViewMap.containsKey(mapName)) {
                        DataMapViewModel dataMapViewPrevious = reverseEngineeringViewMap.get(mapName);
                        if (dataMapViewPrevious.getReverseEngineeringTree() != null) {
                            dataMapViewModel.setReverseEngineeringTree(dataMapViewPrevious.getReverseEngineeringTree());
                        } else {
                            dataMapViewModel.setReverseEngineeringTree(new DBModel(""));
                        }
                    } else {
                        dataMapViewModel.setReverseEngineeringTree(new DBModel(""));
                    }
                    reverseEngineeringViewMap.put(mapName, dataMapViewModel);
                }
                tempDataMap = map;

                if (map != null) {
                    loadPreviousData();
                    xmlFileEditor.removeAlertMessage();
                }
            }
        });

        this.reverseEngineeringController = new ReverseEngineeringController(controller, this);
    }

    public void loadPreviousData() {
        DataMap dataMap = controller.getCurrentDataMap();
        try {
            if (dataMap != null) {
                String reverseEngineeringText = null;
                if (reverseEngineeringViewMap.containsKey(dataMap.getName())) {
                    reverseEngineeringText = reverseEngineeringViewMap.get(dataMap.getName()).getReverseEngineeringText();
                }
                if (reverseEngineeringText != null) {
                    xmlFileEditor.getView().getEditorPane().setText(reverseEngineeringText);
                } else {
                    if (dataMap.getReverseEngineering() == null) {
                        getXmlFileEditor().getView().getEditorPane().setText(template);
                    } else {
                        ReverseEngineering reverseEngineering = dataMap.getReverseEngineering();
                        if (reverseEngineering.getConfigurationSource() != null) {
                            xmlFileEditor.getView().getEditorPane().setPage(reverseEngineering.getConfigurationSource().getURL());
                        }
                    }
                }

                if (reverseEngineeringViewMap.containsKey(dataMap.getName())) {
                    if (reverseEngineeringViewMap.get(dataMap.getName()).getReverseEngineeringTree() != null) {
                        DBModel loadedPreviousTree = reverseEngineeringViewMap.get(dataMap.getName()).getReverseEngineeringTree();
                        treeEditor.convertTreeViewIntoTreeNode(loadedPreviousTree);
                    } else {
                        treeEditor.setRoot("");
                    }
                } else {
                    treeEditor.setRoot("");
                }
            }

            if (((CayenneModelerController) controller.getParent())
                    .getEditorView()
                    .getDataMapView()
                    .getSelectedIndex() == 1) {
                ((CayenneModelerController) controller.getParent())
                        .getEditorView()
                        .getDataMapView()
                        .setSelectedIndex(1);
            }
        } catch (IOException e) {
            throw new CayenneRuntimeException("Invalid URL");
        }
    }

    public JComboBox getDataSources() {
        return dataSources;
    }

    public JButton getConfigButton() {
        return configButton;
    }

    public JButton getSyncButton() {
        return syncButton;
    }

    public TreeEditor getTreeEditor() {
        return treeEditor;
    }

    public XMLFileEditor getXmlFileEditor() {
        return xmlFileEditor;
    }

    public JButton getExecuteButton() {
        return executeButton;
    }

    public Map<String, DataMapViewModel> getReverseEngineeringViewMap() {
        return reverseEngineeringViewMap;
    }

    public void setTempDataMap(DataMap dataMap) {
        tempDataMap = dataMap;
    }
}
