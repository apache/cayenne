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
package org.apache.cayenne.swing;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.cayenne.modeler.dialog.FindDialog;

public class ImageRendererColumn extends DefaultTableCellRenderer {

    JLabel lbl = new JLabel();
    ImageIcon icon = null;

    public ImageRendererColumn() {
        super();
    }

    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
        lbl.setOpaque(true);
        lbl.setText(((JLabel) value).getText().toString());
        lbl.setIcon(((JLabel) value).getIcon());
        lbl.setBorder(BorderFactory.createLineBorder(Color.WHITE, 0));
        lbl.setBackground(Color.WHITE);

        lbl.setHorizontalAlignment(JLabel.LEFT);
        lbl.setFont(isSelected ? FindDialog.getFontSelected() : FindDialog.getFont());

        return lbl;
    }
}
