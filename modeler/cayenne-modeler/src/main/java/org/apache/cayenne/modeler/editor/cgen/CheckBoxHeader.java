/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/


package org.apache.cayenne.modeler.editor.cgen;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


/**
 *
 * @since 4.2
 */
    class CheckBoxHeader extends JCheckBox implements TableCellRenderer, MouseListener  {
        protected int column;
        protected boolean mousePressed = false;
        private final CheckBoxHeader rendererComponent;

        public CheckBoxHeader() {
            this.rendererComponent = this;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.column = column;
            if (table != null) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    header.addMouseListener(rendererComponent);
                }
            }
            rendererComponent.setBackground(UIManager.getColor("Table.selectionBackground"));
            setBorder(UIManager.getBorder("CheckBoxHeader.border"));
            return rendererComponent;
        }

        public void mouseClicked(MouseEvent e) {
            if (mousePressed) {
                mousePressed=false;
                JTableHeader header = (JTableHeader) (e.getSource());
                int columnAtPoint = header.columnAtPoint(e.getPoint());
                if (columnAtPoint == column) {
                    doClick();
                }
            }
        }

        public void mousePressed(MouseEvent e) {
            mousePressed = true;
        }
        public void mouseReleased(MouseEvent e) {
        }
        public void mouseEntered(MouseEvent e) {
        }
        public void mouseExited(MouseEvent e) {
        }

    }


