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

package org.apache.cayenne.modeler.util;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.ObjRelationshipTableModel;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class MapKeyComboBoxEditor extends AbstractCellEditor implements TableCellEditor {

    private static final String DEFAULT_MAP_KEY = "ID (default)";
    private static final String COLLECTION_TYPE_MAP = "java.util.Map";
    private static final int REL_MAP_KEY_COLUMN = 4;

    private List<String> mapKeys = new ArrayList<>();
    private ObjRelationshipTableModel model;
    private int row;

    private void initMapKeys() {
        mapKeys.clear();
        mapKeys.add(DEFAULT_MAP_KEY);
        /**
         * Object target can be null when selected target DbEntity has no
         * ObjEntities
         */
        ObjEntity objectTarget = model.getRelationship(row).getTargetEntity();
        if (objectTarget == null) {
            return;
        }
        for (ObjAttribute attribute : objectTarget.getAttributes()) {
            mapKeys.add(attribute.getName());
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, final int row, int column) {
        this.model = (ObjRelationshipTableModel) table.getModel();
        this.row = row;
        initMapKeys();
        final JComboBox mapKeysComboBox = Application.getWidgetFactory().createComboBox(
                mapKeys,
                false);
        if ((model.getRelationship(row).getCollectionType() == null)
                || (!model.getRelationship(row).getCollectionType().equals(COLLECTION_TYPE_MAP))) {
            JLabel labelIfNotMapCollection = new JLabel();
            labelIfNotMapCollection.setEnabled(false);
            return labelIfNotMapCollection;
        }
        mapKeysComboBox.setFocusable(true);
        mapKeysComboBox.setEnabled(true);

        mapKeysComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selected = mapKeysComboBox.getSelectedItem();
                model.setUpdatedValueAt(selected, row, REL_MAP_KEY_COLUMN);
            }
        });
        mapKeysComboBox.setSelectedItem(model.getRelationship(row).getMapKey());

        mapKeysComboBox.setFocusable(false);
        mapKeysComboBox.setEditable(true);
        ((JComponent) mapKeysComboBox.getEditor().getEditorComponent()).setBorder(null);
        mapKeysComboBox.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));

        return mapKeysComboBox;
    }

    @Override
    public Object getCellEditorValue() {
        return model.getValueAt(row, REL_MAP_KEY_COLUMN);
    }
}