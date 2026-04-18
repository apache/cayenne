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
package org.apache.cayenne.modeler.dialog.objentity;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ObjAttributePathBrowser extends ObjRelationshipPathBrowser {

    private final JButton selectPathButton;
    private final JButton doneButton;

    public ObjAttributePathBrowser(JButton selectPathButton, JButton doneButton) {
        this.selectPathButton = selectPathButton;
        this.doneButton = doneButton;
    }

    @Override
    protected void installColumn(BrowserPanel panel) {
        if (panelOpener == null) {
            panelOpener = new PanelAttributeOpener();
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

    /**
     * Listener, which performs adding of new column at double-click
     */
    protected class PanelAttributeOpener extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                process(e);
            }
        }

        private void process(MouseEvent e) {
            BrowserPanel panel = (BrowserPanel) e.getSource();
            Object selectedNode = panel.getSelectedValue();

            // ignore unselected
            if (selectedNode instanceof DbRelationship) {
                updateFromModel(selectedNode, columns.indexOf(panel));
                selectPathButton.setEnabled(false);
                doneButton.setEnabled(false);
            }
            else if (selectedNode instanceof DbAttribute) {
                doneButton.setEnabled(true);
                selectPathButton.setEnabled(true);
            }
        }
    }
}
