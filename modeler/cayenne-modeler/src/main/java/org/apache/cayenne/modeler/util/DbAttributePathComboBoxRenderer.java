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

import org.apache.cayenne.map.DbAttribute;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Font;

public class DbAttributePathComboBoxRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected,  hasFocus,  row, column);

        setText("");
        setFont(new Font("Verdana", Font.PLAIN , 12));

        if (value instanceof DbAttribute) {
           setText(ModelerUtil.getObjectName(value));
        } else if (value != null) {
            setText(value.toString());
        }
        setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        return this;
    }
}