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
package org.apache.cayenne.modeler.dialog.template;

import org.apache.cayenne.map.template.ClassGenerationDescriptor;
import org.apache.cayenne.map.template.ClassTemplate;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.template.TemplateType;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.configuration.event.DataMapListener;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.configuration.event.TemplateEvent;
import org.apache.cayenne.configuration.event.TemplateListener;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayListener;
import org.apache.cayenne.modeler.event.TemplateDisplayEvent;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * @since 4.0
 */
public class TemplateView extends JPanel {
    protected ProjectController controller;
    protected TemplateController templateController;
    protected JPanel templatePanel;

    protected JButton addTemplateButton;
    protected JButton deleteTemplateButton;

    protected JComboBox templateType;
    protected DefaultComboBoxModel<TemplateType> comboBoxModel;

    protected JSeparator separator;
    protected JSplitPane splitPane;
    protected JLabel templateListLabel;
    protected JLabel templateLabel;
    protected Icon icon;

    protected TemplateEditor templateEditor;
    protected DocumentListener documentListener;

    protected DefaultTableModel tableModel;
    protected ListSelectionListener templateTextRefresh;
    protected TableModelListener templateNameRefresh;

    protected int lastSelectedRow;

    protected Map<String, DefaultTableModel> templateViewMap;

    public TemplateListEditor getTemplateListEditor() {
        return templateListEditor;
    }

    protected TemplateListEditor templateListEditor;

    public TemplateView(ProjectController controller) {
        this.controller = controller;

        this.templateViewMap = new HashMap<>(3);
        initView();
        initController();
    }

    private DefaultTableModel getTemplateNames(DataMap dataMap) {
        tableModel = new DefaultTableModel();
        tableModel.addColumn("Templates");
        if (dataMap != null) {
            ClassGenerationDescriptor classGenerationDescriptor = dataMap.getClassGenerationDescriptor();
            if (classGenerationDescriptor != null) {
                for (ClassTemplate template : classGenerationDescriptor.getTemplates().values()) {
                    Vector<String> vector = new Vector<>();
                    vector.add(template.getName());
                    tableModel.addRow(vector);
                }
            }
        }
        return tableModel;
    }

    private DefaultComboBoxModel getTemplateTypes() {
        comboBoxModel = new DefaultComboBoxModel<>();
        comboBoxModel.addElement(null);
        comboBoxModel.addElement(TemplateType.ENTITY_SUPERCLASS);
        comboBoxModel.addElement(TemplateType.ENTITY_SINGLE_CLASS);
        comboBoxModel.addElement(TemplateType.ENTITY_SUBCLASS);
        comboBoxModel.addElement(TemplateType.EMBEDDABLE_SUPERCLASS);
        comboBoxModel.addElement(TemplateType.EMBEDDABLE_SINGLE_CLASS);
        comboBoxModel.addElement(TemplateType.EMBEDDABLE_SUBCLASS);
        comboBoxModel.addElement(TemplateType.DATAMAP_SINGLE_CLASS);
        comboBoxModel.addElement(TemplateType.DATAMAP_SUBCLASS);
        comboBoxModel.addElement(TemplateType.DATAMAP_SUPERCLASS);
        return comboBoxModel;
    }

    private void initView() {
        this.templatePanel = new JPanel();
        this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);

        this.addTemplateButton = new JButton();
        this.addTemplateButton.setContentAreaFilled(false);
        this.icon = ModelerUtil.buildIcon("icon-plus.gif");
        this.addTemplateButton.setIcon(icon);
        this.addTemplateButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        this.deleteTemplateButton = new JButton();
        this.deleteTemplateButton.setContentAreaFilled(false);
        this.icon = ModelerUtil.buildIcon("icon-trash.gif");
        this.deleteTemplateButton.setIcon(icon);
        this.deleteTemplateButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        this.templateType = new JComboBox();
        this.templateListLabel = new JLabel("Templates");
        this.templateLabel = new JLabel("Editor");
        this.templateLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        this.templateListLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        this.templateEditor = new TemplateEditor(controller);
        this.templateListEditor = new TemplateListEditor(controller);

        JPanel templateHeaderComponent = new JPanel(new FlowLayout(FlowLayout.LEFT));
        templateHeaderComponent.add(templateLabel);
        templateHeaderComponent.add(addTemplateButton);

        JPanel leftComponent = new JPanel();
        leftComponent.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        leftComponent.setLayout(new BorderLayout());
        leftComponent.add(templateHeaderComponent, BorderLayout.NORTH);
        leftComponent.add(templateListEditor.getView().getScrollPane(), BorderLayout.CENTER);

        templateType.setModel(getTemplateTypes());
        JPanel typeSelector = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typeSelector.add(templateType);

        JPanel rightComponent = new JPanel();
        rightComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        rightComponent.setLayout(new BorderLayout());
        rightComponent.add(typeSelector, BorderLayout.NORTH);
        rightComponent.add(templateEditor.getView().getScrollPane(), BorderLayout.CENTER);

        splitPane.setLeftComponent(leftComponent);
        splitPane.setRightComponent(rightComponent);
        splitPane.setResizeWeight(0.2);

        JPanel splitWithErrorsPanel = new JPanel();
        splitWithErrorsPanel.setLayout(new BorderLayout());
        splitWithErrorsPanel.add(splitPane, BorderLayout.CENTER);
        templateEditor.getView().getLabel().setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        splitWithErrorsPanel.add(templateEditor.getView().getLabel(), BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(splitWithErrorsPanel, BorderLayout.CENTER);
    }

    public void initController() {
        templateType.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    updateType();
                }
            }
        });

        templateTextRefresh = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    if (!templateEditor.getView().getEditorPane().isVisible()) {
                        templateEditor.getView().getEditorPane().setVisible(true);
                    }
                    if (!templateType.isVisible()) {
                        templateType.setVisible(true);
                    }
                    JTable table = templateListEditor.getView().getTemplateList();

                    if (table.getSelectedRow() >= 0) {
                        String templateName = (String) table.getValueAt(table.getSelectedRow(), 0);
                        ClassTemplate template = controller.getCurrentDataMap().getClassGenerationDescriptor().getTemplates().get(templateName);

                        TemplateDisplayEvent event = new TemplateDisplayEvent(
                                this,
                                template,
                                controller.getCurrentDataMap(),
                                (DataChannelDescriptor) controller.getProject().getRootNode());
                        controller.fireTemplateDisplayEvent(event);
                    }
                    updateTemplate();
                }
            }
        };

        ListSelectionModel cellSelectionModel = templateListEditor.getView().getTemplateList().getSelectionModel();
        cellSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cellSelectionModel.addListSelectionListener(templateTextRefresh);

        controller.addTemplateListener(new TemplateListener() {
            @Override
            public void templateChanged(TemplateEvent e) {}

            @Override
            public void templateAdded(TemplateEvent e) {
                ClassTemplate template = e.getTemplate();

                JTable templateList = templateListEditor.getView().getTemplateList();
                DataMap dataMap = template.getDataMap();

                DefaultTableModel templateModel = getTemplateNames(dataMap);

                if (dataMap == controller.getCurrentDataMap()) {
                    templateList.setModel(templateModel);
                }
                int row = getRowFromModel(template.getName());

                TableRowSorter<TableModel> sorter = createRowSorter(templateList.getModel());
                sorter.sort();
                sorter.setSortsOnUpdates(true);
                templateList.setRowSorter(sorter);
                templateViewMap.put(dataMap.getName(), templateModel);

                if (dataMap == controller.getCurrentDataMap()) {
                    row = sorter.convertRowIndexToView(row);
                    templateList.setRowSelectionInterval(row, row);
                }
                templateList.getModel().addTableModelListener(templateNameRefresh);
            }

            @Override
            public void templateRemoved(TemplateEvent e) {
                JTable templateList = templateListEditor.getView().getTemplateList();

                ClassTemplate template = e.getTemplate();
                DataMap dataMap = template.getDataMap();

                DefaultTableModel templateModel = getTemplateNames(dataMap);
                templateViewMap.put(dataMap.getName(), templateModel);

                int viewRow = 0;
                if (dataMap == controller.getCurrentDataMap()) {
                    int row = getRowFromModel(template.getName());

                    TableRowSorter sorter = (TableRowSorter) templateList.getRowSorter();
                    viewRow = sorter.convertRowIndexToView(row);

                    templateList.setModel(templateModel);

                    TableRowSorter<TableModel> newSorter = createRowSorter(templateList.getModel());
                    newSorter.sort();
                    newSorter.setSortsOnUpdates(true);
                    templateList.setRowSorter(newSorter);

                    if (dataMap == controller.getCurrentDataMap()) {
                        if (templateList.getRowCount() > viewRow) {
                            templateList.setRowSelectionInterval(viewRow, viewRow);
                        } else {
                            if (templateList.getRowCount() != 0) {
                                templateList.setRowSelectionInterval(viewRow - 1, viewRow - 1);
                            } else {
                                templateEditor.getView().getEditorPane().setVisible(false);
                                templateType.setVisible(false);

                                DataMapDisplayEvent event = new DataMapDisplayEvent(
                                        this,
                                        controller.getCurrentDataMap(),
                                        controller.getCurrentDataChanel());
                                controller.fireDataMapDisplayEvent(event);
                            }
                        }
                    }
                }
                templateList.getModel().addTableModelListener(templateNameRefresh);
            }
        });

        templateNameRefresh = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                JTable templateList = templateListEditor.getView().getTemplateList();

                String updatedName = (String) templateList.getValueAt(templateList.getSelectedRow(), 0);
                DataMap dataMap = controller.getCurrentDataMap();
                DataChannelDescriptor dataChannelDescriptor = controller.getCurrentDataChanel();
                boolean isNameInUse = NameCheckers.template.isNameInUse(dataChannelDescriptor, updatedName);

                int row;
                if (!isNameInUse) {
                    String templateName = controller.getCurrentTemplate().getName();
                    Map<String, ClassTemplate> templates = dataMap.getClassGenerationDescriptor().getTemplates();
                    ClassTemplate removedTemplate = templates.remove(templateName);
                    removedTemplate.setName(updatedName);
                    templates.put(updatedName, removedTemplate);

                    DefaultTableModel templateModel = getTemplateNames(dataMap);
                    templateList.setModel(templateModel);
                    templateViewMap.put(dataMap.getName(), templateModel);
                    row = getRowFromModel(updatedName);
                } else {
                    DefaultTableModel templateModel = getTemplateNames(dataMap);
                    templateList.setModel(templateModel);
                    templateViewMap.put(dataMap.getName(), templateModel);
                    row = getRowFromModel(controller.getCurrentTemplate().getName());
                }

                TableRowSorter<TableModel> newSorter = createRowSorter(templateList.getModel());
                newSorter.sort();
                newSorter.setSortsOnUpdates(true);
                templateList.setRowSorter(newSorter);

                row = newSorter.convertRowIndexToView(row);
                templateList.setRowSelectionInterval(row, row);

                templateList.getModel().addTableModelListener(templateNameRefresh);
            }
        };

        documentListener = new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                JTable templateList = templateListEditor.getView().getTemplateList();

                if (e.getType() == DocumentEvent.EventType.INSERT) {
                    if (templateList.getSelectedRow() >= 0) {
                        String name = (String) templateList.getValueAt(templateList.getSelectedRow(), 0);

                        DataMap dataMap = controller.getCurrentDataMap();
                        ClassTemplate classTemplate = dataMap.getClassGenerationDescriptor().getTemplates().get(name);

                        classTemplate.setText(templateEditor.getView().getEditorPane().getText());

                        controller.fireTemplateEvent(new TemplateEvent(this, classTemplate, 1));
                    }
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                JTable templateList = templateListEditor.getView().getTemplateList();

                if (e.getType() == DocumentEvent.EventType.REMOVE) {
                    if (templateList.getSelectedRow() >= 0) {
                        String name = (String) templateList.getValueAt(templateList.getSelectedRow(), 0);

                        DataMap dataMap = controller.getCurrentDataMap();
                        ClassTemplate classTemplate = dataMap.getClassGenerationDescriptor().getTemplates().get(name);

                        classTemplate.setText(templateEditor.getView().getEditorPane().getText());

                        controller.fireTemplateEvent(new TemplateEvent(this, classTemplate, 1));
                    }
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };

        templateEditor.getView().getEditorPane().getDocument().addDocumentListener(documentListener);

        controller.addDataMapDisplayListener(new DataMapDisplayListener() {
            public void currentDataMapChanged(DataMapDisplayEvent e) {
                if (!templateViewMap.containsKey(e.getDataMap().getName())) {
                    TemplateViewModel model = templateListEditor.getView();
                    JTable templateList = model.getTemplateList();
                    model.getTemplateList().setModel(getTemplateNames(controller.getCurrentDataMap()));

                    templateList.getModel().addTableModelListener(templateNameRefresh);
                    templateList.setAutoCreateRowSorter(true);

                    TableRowSorter<TableModel> sorter = createRowSorter(templateList.getModel());
                    sorter.sort();
                    sorter.setSortsOnUpdates(true);
                    templateList.setRowSorter(sorter);

                    templateViewMap.put(e.getDataMap().getName(), (DefaultTableModel) templateList.getModel());
                } else {
                    DefaultTableModel model = templateViewMap.get(e.getDataMap().getName());
                    templateListEditor.getView().setTableModel(model);
                    JTable templateList = templateListEditor.getView().getTemplateList();

                    TableRowSorter<TableModel> newSorter = createRowSorter(templateList.getModel());
                    newSorter.sort();
                    newSorter.setSortsOnUpdates(true);
                    templateList.setRowSorter(newSorter);
                }
                TemplateViewModel model = templateListEditor.getView();
                if (model != null) {
                    templateEditor.getView().getEditorPane().setVisible(false);
                    templateType.setVisible(false);

                    lastSelectedRow = -1;
                    templateEditor.getView().getEditorPane().setText("");
                }
            }
        });

        controller.addDataMapListener(new DataMapListener() {
            @Override
            public void dataMapChanged(DataMapEvent e) {
            }

            @Override
            public void dataMapAdded(DataMapEvent e) {
            }

            @Override
            public void dataMapRemoved(DataMapEvent e) {
                templateViewMap.remove(e.getDataMap().getName());
            }
        });

        this.templateController = new TemplateController(controller, this);
    }

    private int getRowFromModel(String templateName) {
        JTable templateList = templateListEditor.getView().getTemplateList();
        int row = 0;
        int i = 0;
        DefaultTableModel model = (DefaultTableModel) templateList.getModel();
        for (Object templates : model.getDataVector()) {
            Vector<String> vector = (Vector<String>) templates;
            if (vector.get(0).equals(templateName)) {
                row = i;
                break;
            }
            ++i;
        }
        return row;
    }

    private TableRowSorter<TableModel> createRowSorter(TableModel model) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>(1);
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        return sorter;
    }

    public JButton getAddTemplateButton() {
        return addTemplateButton;
    }

    private void updateTemplate() {
        DataMap dataMap = controller.getCurrentDataMap();
        JTable templateList = templateListEditor.getView().getTemplateList();

        lastSelectedRow = templateList.getSelectedRow();

        if (lastSelectedRow >= 0) {
            ClassGenerationDescriptor classGenerationDescriptor = dataMap.getClassGenerationDescriptor();
            if (classGenerationDescriptor.getTemplates().size() > 0) {
                String templateName = (String) templateList.getValueAt(lastSelectedRow, 0);
                ClassTemplate template = classGenerationDescriptor.getTemplates().get(templateName);
                if (template != null) {
                    templateEditor.setView(template.getText());
                    templateType.setSelectedItem(template.getType());
                } else {
                    templateEditor.getView().getEditorPane().setText("");
                    templateType.setSelectedIndex(-1);
                }
            }
        } else {
            templateEditor.getView().getEditorPane().setText("");
            templateType.setSelectedIndex(-1);
        }
    }

    private void updateType() {
        DataMap dataMap = controller.getCurrentDataMap();
        JTable templateList = templateListEditor.getView().getTemplateList();

        ClassGenerationDescriptor classGenerationDescriptor = dataMap.getClassGenerationDescriptor();
        if (templateList.getRowCount() >= 0) {
            String templateName = (String) templateList.getValueAt(lastSelectedRow, 0);
            if (templateName != null) {
                TemplateType type = (TemplateType) templateType.getSelectedItem();
                classGenerationDescriptor.getTemplates().get(templateName).setType(type);
            }
        }
    }

    public JButton getDeleteTemplateButton() {
        return deleteTemplateButton;
    }
}
