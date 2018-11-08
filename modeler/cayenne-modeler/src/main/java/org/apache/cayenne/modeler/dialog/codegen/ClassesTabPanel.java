///*****************************************************************
// *   Licensed to the Apache Software Foundation (ASF) under one
// *  or more contributor license agreements.  See the NOTICE file
// *  distributed with this work for additional information
// *  regarding copyright ownership.  The ASF licenses this file
// *  to you under the Apache License, Version 2.0 (the
// *  "License"); you may not use this file except in compliance
// *  with the License.  You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing,
// *  software distributed under the License is distributed on an
// *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// *  KIND, either express or implied.  See the License for the
// *  specific language governing permissions and limitations
// *  under the License.
// ****************************************************************/
//
//package org.apache.cayenne.modeler.dialog.codegen;
//
//import org.apache.cayenne.map.DataMap;
//
//import javax.swing.BoxLayout;
//import javax.swing.JCheckBox;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.ScrollPaneConstants;
//import javax.swing.UIManager;
//import javax.swing.border.EmptyBorder;
//import java.awt.BorderLayout;
//import java.awt.Component;
//import java.awt.Dimension;
//import java.awt.FlowLayout;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// */
//public class ClassesTabPanel extends JPanel {
//
//    protected JCheckBox checkAll;
//    protected JLabel checkAllLabel;
//
//    private Map<DataMap, JTable> dataMapTables;
//
//    private Map<DataMap, JCheckBox> dataMapJCheckBoxMap;
//
//    public ClassesTabPanel(Collection<DataMap> dataMaps) {
//        dataMapTables = new HashMap<>();
//        dataMapJCheckBoxMap = new HashMap<>();
//
//        // TODO: andrus 04/07/2006 - is there an easy way to stick that checkbox in the
//        // table header????
//        this.checkAll = new JCheckBox();
//        this.checkAllLabel = new JLabel("Check All Classes");
//
//        checkAll.addItemListener(event -> {
//            if (checkAll.isSelected()) {
//                checkAllLabel.setText("Uncheck All Classess");
//                dataMapJCheckBoxMap.keySet().forEach(val -> dataMapJCheckBoxMap.get(val).setSelected(true));
//            }
//            else {
//                checkAllLabel.setText("Check All Classes");
//                dataMapJCheckBoxMap.keySet().forEach(val -> dataMapJCheckBoxMap.get(val).setSelected(false));
//            }
//        });
//
//        // assemble
//        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//        topPanel.setBorder(UIManager.getBorder("ToolBar.border"));
//        topPanel.add(checkAll);
//        topPanel.add(checkAllLabel);
//
//        JPanel panel = new JPanel();
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//        for(DataMap dataMap : dataMaps) {
//            JTable table = new JTable();
//            table.setRowHeight(22);
//            dataMapTables.put(dataMap, table);
//            JPanel scrollTable = new JPanel(new BorderLayout());
//            scrollTable.add(dataMapTables.get(dataMap).getTableHeader(), BorderLayout.NORTH);
//            scrollTable.add(dataMapTables.get(dataMap), BorderLayout.CENTER);
//            scrollTable.setPreferredSize(new Dimension(dataMapTables.get(dataMap).getPreferredSize().width,
//                    (dataMap.getEmbeddables().size() + dataMap.getObjEntities().size()) * dataMapTables.get(dataMap).getRowHeight() + 45));
//            JPanel labelPanel = new JPanel(new BorderLayout());
//            labelPanel.setPreferredSize(new Dimension(dataMapTables.get(dataMap).getPreferredSize().width, 20));
//            JLabel dataMapLabel = new JLabel(dataMap.getName());
//            dataMapLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
//            dataMapLabel.setBorder(new EmptyBorder(8, 8, 8, 0));
//            labelPanel.add(dataMapLabel, BorderLayout.CENTER);
//
//            JCheckBox dataMapCheckBox = new JCheckBox();
//            dataMapJCheckBoxMap.put(dataMap, dataMapCheckBox);
//            labelPanel.add(dataMapCheckBox, BorderLayout.WEST);
//
//            JPanel currPanel = new JPanel(new BorderLayout());
//            currPanel.add(labelPanel, BorderLayout.NORTH);
//            currPanel.add(scrollTable, BorderLayout.CENTER);
//
//            panel.add(currPanel);
//        }
//
//        JScrollPane tablePanel = new JScrollPane(
//                panel,
//                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
//                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//
//        // set some minimal preferred size, so that it is smaller than other forms used in
//        // the dialog... this way we get the right automated overall size
//        tablePanel.setPreferredSize(new Dimension(450, 400));
//        setLayout(new BorderLayout());
//        add(topPanel, BorderLayout.NORTH);
//        add(tablePanel, BorderLayout.CENTER);
//    }
//
//    public boolean isAllCheckBoxesFromDataMapSelected(DataMap dataMap) {
//        JTable table = dataMapTables.get(dataMap);
//        for(int i = 0; i < table.getRowCount(); i++) {
//            if(!(Boolean)table.getModel().getValueAt(i, 0)) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    public Map<DataMap, JTable> getDataMapTables() {
//        return dataMapTables;
//    }
//
//    public Map<DataMap, JCheckBox> getDataMapJCheckBoxMap() {
//        return dataMapJCheckBoxMap;
//    }
//
//    public JCheckBox getCheckAll() {
//        return checkAll;
//    }
//}