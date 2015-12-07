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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.ObjRelationshipTableModel;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JTableCollectionTypeComboBoxEditor extends AbstractCellEditor implements TableCellEditor {

        private static final String COLLECTION_TYPE_MAP = "java.util.Map";
        private static final String COLLECTION_TYPE_SET = "java.util.Set";
        private static final String COLLECTION_TYPE_COLLECTION = "java.util.Collection";
        private static final String DEFAULT_COLLECTION_TYPE = "java.util.List";
        private static final int REL_COLLECTION_TYPE_COLUMN = 3;

        private ObjRelationshipTableModel model;
        private int row;

        public JTableCollectionTypeComboBoxEditor() {
        }

        @Override
        public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, final int column) {
            this.model = (ObjRelationshipTableModel) table.getModel();
            this.row = row;

            final JComboBox collectionTypeCombo = Application.getWidgetFactory().createComboBox(
                    new Object[]{
                            COLLECTION_TYPE_MAP,
                            COLLECTION_TYPE_SET,
                            COLLECTION_TYPE_COLLECTION,
                            DEFAULT_COLLECTION_TYPE
                    },
                    false);
            if (model.getRelationship(row).isToMany()) {
                collectionTypeCombo.setEnabled(true);
                collectionTypeCombo.setSelectedItem(model.getRelationship(row).getCollectionType());
            } else {
                JLabel labelIfToOneRelationship = new JLabel();
                labelIfToOneRelationship.setEnabled(false);
                return labelIfToOneRelationship;
            }
            collectionTypeCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object selected = collectionTypeCombo.getSelectedItem();
                    model.setUpdatedValueAt(selected, row, REL_COLLECTION_TYPE_COLUMN);
                    table.repaint();
                }
            });
            return collectionTypeCombo;
        }

        @Override
        public Object getCellEditorValue() {
            return model.getValueAt(row, REL_COLLECTION_TYPE_COLUMN);
        }
    }