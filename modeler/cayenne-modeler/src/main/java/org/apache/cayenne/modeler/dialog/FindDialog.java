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
package org.apache.cayenne.modeler.dialog;

import org.apache.cayenne.modeler.action.FindAction;
import org.apache.cayenne.modeler.util.CayenneController;

import javax.swing.JDialog;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * An instance of this class is responsible for displaying search results and navigating
 * to the selected entity's representation.
 */
public class FindDialog extends CayenneController {

    private FindDialogView view;
    private List<FindAction.SearchResultEntry> searchResults;

    public FindDialog(CayenneController parent, List<FindAction.SearchResultEntry> searchResults) {
        super(parent);

        this.searchResults = searchResults;
        view = new FindDialogView(searchResults);
        initBindings();
    }

    public void startupAction() {
        view.pack();

        centerView();
        makeCloseableOnEscape();

        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setVisible(true);
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        view.getOkButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                view.dispose();
            }
        });

        JTable table = view.getTable();
        table.setRowHeight(24);
        table.setRowMargin(3);
        JumpToResultActionListener listener = new JumpToResultActionListener();
        table.addKeyListener(listener);
        table.addMouseListener(listener);
        table.getSelectionModel().setSelectionInterval(0, 0);
    }

    private class JumpToResultActionListener implements MouseListener, KeyListener {

        @Override
        public void mouseReleased(MouseEvent e) {
            openResult(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                return;
            }
            openResult(e);
        }

        private void openResult(InputEvent e) {
            JTable table = (JTable) e.getSource();
            Integer selectedLine = table.getSelectionModel().getLeadSelectionIndex();
            FindAction.jumpToResult(searchResults.get(selectedLine));
        }

        @Override public void keyReleased(KeyEvent e) {}
        @Override public void keyTyped(KeyEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mousePressed(MouseEvent e) {}
        @Override public void mouseClicked(MouseEvent e) {}
    }
}
