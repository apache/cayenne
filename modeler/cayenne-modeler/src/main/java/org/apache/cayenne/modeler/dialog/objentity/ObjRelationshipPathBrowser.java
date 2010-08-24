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
package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreePath;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.util.MultiColumnBrowser;

/**
 * Multi-column browser for obj relationships
 */
public class ObjRelationshipPathBrowser extends MultiColumnBrowser {

    /**
     * Listener, which performs adding of new column
     */
    protected MouseListener panelOpener;

    /**
     * Listener, which performs removing of columns to the right of selected row
     */
    protected ListSelectionListener panelRemover;

    public ObjRelationshipPathBrowser() {
        this(DEFAULT_MIN_COLUMNS_COUNT);
    }

    public ObjRelationshipPathBrowser(int minColumns) {
        super(minColumns);
    }

    @Override
    protected void installColumn(BrowserPanel panel) {
        if (panelOpener == null) {
            panelOpener = new PanelOpener();
        }

        if (panelRemover == null) {
            panelRemover = new PanelRemover();
        }

        panel.addMouseListener(panelOpener);
        panel.addListSelectionListener(panelRemover);

        panel.setCellRenderer(renderer);
    }

    /**
     * Selects one path component. Need to override this method, because list selection
     * does not cause loading in this browser.
     */
    @Override
    protected void selectRow(Object row, int index, TreePath path) {
        if (index > 0 && columns.get(index - 1).getSelectedValue() != row) {
            columns.get(index - 1).setSelectedValue(row, true);
        }

        if (index != path.getPathCount() - 1) {
            updateFromModel(row, index - 1);
        }
    }

    @Override
    protected void uninstallColumn(BrowserPanel panel) {
        panel.removeMouseListener(panelOpener);
        panel.removeListSelectionListener(panelRemover);
    }

    /**
     * Listener, which performs adding of new column at double-click
     */
    protected class PanelOpener extends MouseAdapter {

        /**
         * Invoked when the mouse has been clicked on a component.
         */
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                process(e);
            }
        }

        private void process(MouseEvent e) {
            BrowserPanel panel = (BrowserPanel) e.getSource();
            Object selectedNode = panel.getSelectedValue();

            // ignore unselected
            if (selectedNode != null && selectedNode instanceof DbRelationship) {
                updateFromModel(selectedNode, columns.indexOf(panel));
            }
        }
    }

    /**
     * Listener, which performs removing columns to the right of selected row
     */
    protected class PanelRemover implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            // ignore "adjusting"
            if (!e.getValueIsAdjusting()) {
                BrowserPanel panel = (BrowserPanel) e.getSource();

                Object selectedNode = panel.getSelectedValue();

                if (selectedNode != null) {
                    updateFromModel(selectedNode, columns.indexOf(panel), false);
                }
            }
        }

    }
}
