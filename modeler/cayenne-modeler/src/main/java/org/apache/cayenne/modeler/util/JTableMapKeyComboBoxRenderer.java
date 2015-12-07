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

import org.apache.cayenne.modeler.editor.ObjRelationshipTableModel;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Font;

public class JTableMapKeyComboBoxRenderer implements TableCellRenderer {

        private static final String DEFAULT_MAP_KEY = "ID (default)";
        private static final String COLLECTION_TYPE_MAP = "java.util.Map";

        private ObjRelationshipTableModel model;

        public JTableMapKeyComboBoxRenderer() {
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.model = (ObjRelationshipTableModel) table.getModel();
            if ((model.getRelationship(row).getCollectionType() == null)
                    || (!model.getRelationship(row).getCollectionType().equals(COLLECTION_TYPE_MAP))) {
                JComboBox jComboBox = new JComboBox();
                jComboBox.setFocusable(false);
                jComboBox.setEnabled(false);
                return jComboBox;
            }
            if (model.getRelationship(row).getMapKey() == null) {
                model.getRelationship(row).setMapKey(DEFAULT_MAP_KEY);
            }
            JLabel jLabel = new JLabel(model.getRelationship(row).getMapKey());
            jLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
            return jLabel;
        }
    }