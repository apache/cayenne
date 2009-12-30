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
package org.apache.cayenne.modeler.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.swing.ImageRendererColumn;


/**
 * Swing component displaying results produced by search feature.
 */
public class FindDialogView extends JDialog {

    private JButton okButton;
    private static JScrollPane scrollPane;
    private JTable table;
    private static Map LabelAndObjectIndex;

    public static Map getLabelAndObjectIndex() {
        return LabelAndObjectIndex;
    }

    public JTable getTable() {
        return table;
    }

    public FindDialogView(Map objEntityNames, Map dbEntityNames, Map attrNames,
            Map relatNames, Map queryNames, Map embeddableNames, Map embeddableAttributeNames) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        if (objEntityNames.isEmpty()
                && dbEntityNames.isEmpty()
                && attrNames.isEmpty()
                && relatNames.isEmpty()
                && queryNames.isEmpty()
                && embeddableNames.isEmpty()
                && embeddableAttributeNames.isEmpty()) {
            panel.add(new JLabel("Nothing found!"));
        }
        else {

            int curentLineInTable = 0;
            int sizeDataVector = objEntityNames.size()
                    + dbEntityNames.size()
                    + attrNames.size()
                    + relatNames.size()
                    + queryNames.size()
                    + embeddableNames.size()
                    + embeddableAttributeNames.size();

            Object[][] dataVector = new Object[sizeDataVector][];

            TableModel tableModel = new TableModel();

            LabelAndObjectIndex = new HashMap<JLabel, Integer>();

            dataVector = createResultTable(objEntityNames, CellRenderers
                    .iconForObject(new ObjEntity()), dataVector, curentLineInTable);
            
            curentLineInTable = objEntityNames.size();            
            dataVector = createResultTable(embeddableNames, CellRenderers
                    .iconForObject(new Embeddable()), dataVector, curentLineInTable);

            curentLineInTable = curentLineInTable + embeddableNames.size();            
            dataVector = createResultTable(dbEntityNames, CellRenderers
                    .iconForObject(new DbEntity()), dataVector, curentLineInTable);
            
            curentLineInTable = curentLineInTable + dbEntityNames.size();            
            dataVector = createResultTable(attrNames, CellRenderers
                    .iconForObject(new ObjAttribute()), dataVector, curentLineInTable);
            
            curentLineInTable = curentLineInTable + attrNames.size();         
            dataVector = createResultTable(embeddableAttributeNames, CellRenderers
                    .iconForObject(new ObjAttribute()), dataVector, curentLineInTable);

            curentLineInTable = curentLineInTable + embeddableAttributeNames.size();
            dataVector = createResultTable(relatNames, CellRenderers
                    .iconForObject(new ObjRelationship()), dataVector, curentLineInTable);
            
            curentLineInTable = curentLineInTable + relatNames.size();
            dataVector = createResultTable(queryNames, CellRenderers
                    .iconForObject(new SelectQuery()), dataVector, curentLineInTable);
            
            tableModel.setDataVector(dataVector, new Object[] {
                ""
            });

            table = new JTable(tableModel);

            table.getColumnModel().getColumn(0).setCellRenderer(new ImageRendererColumn());
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            InputMap im = table
                    .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            InputMap imParent = im.getParent();
            imParent.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            im.setParent(imParent);
            im.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            table.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, im);
        }

        JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okButton = new JButton("OK");
        okPanel.add(okButton);

        JComponent contentPane = (JComponent) getContentPane();

        contentPane.setLayout(new BorderLayout());

        scrollPane = new JScrollPane( // panel
                table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        contentPane.add(scrollPane);
        contentPane.add(okPanel, BorderLayout.SOUTH);

        contentPane.setPreferredSize(new Dimension(400, 325));
        setTitle("Search results");
    }

    private Object[][] createResultTable(
            Map names,
            Icon icon,
            Object[][] dataVector,
            int curentLineInTable) {

        Comparator<String> comparer = new Comparator<String>() {

            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };

        Map sortedByNameMap = sortMapByValue(names, comparer);

        Iterator it = sortedByNameMap.keySet().iterator();
        Object[] objectHelper = new Object[] {};

        while (it.hasNext()) {
            Integer index = (Integer) it.next();
            JLabel labelIcon = new JLabel();
            labelIcon.setIcon(icon);
            labelIcon.setVisible(true);
            labelIcon.setText((String) sortedByNameMap.get(index));
            objectHelper = new Object[] {
                labelIcon
            };
            dataVector[curentLineInTable] = objectHelper;
            LabelAndObjectIndex.put(labelIcon, index);
            curentLineInTable++;
        }

        return dataVector;
    }

    public JButton getOkButton() {
        return okButton;
    }

    public <K, V> Map<K, V> sortMapByValue(Map<K, V> in, Comparator<? super V> compare) {
        Map<V, K> swapped = new TreeMap<V, K>(compare);
        for (Entry<K, V> entry : in.entrySet()) {
            if (entry.getValue() != null) {
                swapped.put(entry.getValue(), entry.getKey());
            }
        }
        LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
        for (Entry<V, K> entry : swapped.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getValue(), entry.getKey());
            }
        }
        return result;
    }

}

class TableModel extends javax.swing.table.DefaultTableModel {

    public boolean isCellEditable(int row, int col) {
        return false;
    }
}
