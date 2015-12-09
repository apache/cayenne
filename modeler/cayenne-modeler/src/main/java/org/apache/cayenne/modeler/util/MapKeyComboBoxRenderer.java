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

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Font;

public class MapKeyComboBoxRenderer extends DefaultTableCellRenderer {

    private static final String DEFAULT_MAP_KEY = "ID (default)";
    private static final String COLLECTION_TYPE_MAP = "java.util.Map";

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected,  hasFocus,  row, column);

        setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        if ((model.getRelationship(row).getCollectionType() == null)
                || (!model.getRelationship(row).getCollectionType().equals(COLLECTION_TYPE_MAP))) {

            setEnabled(false);
            setText("");
            return this;
        }
        if (model.getRelationship(row).getMapKey() == null) {
            model.getRelationship(row).setMapKey(DEFAULT_MAP_KEY);
        }

        setText(model.getRelationship(row).getMapKey());
        setFont(new Font("Verdana", Font.PLAIN, 12));
        setEnabled(true);
        return this;
    }
}