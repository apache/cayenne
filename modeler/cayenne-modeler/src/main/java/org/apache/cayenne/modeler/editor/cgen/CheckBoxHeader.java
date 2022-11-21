package org.apache.cayenne.modeler.editor.cgen;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


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


