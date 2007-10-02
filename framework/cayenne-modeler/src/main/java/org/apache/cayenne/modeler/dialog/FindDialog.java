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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectTreeModel;
import org.apache.cayenne.modeler.ProjectTreeView;
import org.apache.cayenne.modeler.util.CayenneController;

/**
 * An instance of this class is responsible for displaying search results
 * and navigating to the selected entity's representation. 
 */
public class FindDialog extends CayenneController {
    private FindDialogView view;
    private java.util.List paths;
    private java.util.List names;

    public FindDialog(CayenneController parent, java.util.List paths) {
        super(parent);

        this.paths = paths;

        names = new ArrayList();
        Iterator it = paths.iterator();
        while (it.hasNext()) {
            Object[] path = (Object[]) it.next();
            ObjEntity entity = (ObjEntity) path[path.length - 1];
            names.add(entity.getName());
        }

        view = new FindDialogView(names);

        initBindings();
    }

    public void startupAction() {
        view.pack();

        centerView();
        makeCloseableOnEscape();

        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
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

        Iterator it = view.getEntityButtons().iterator();
        while (it.hasNext()) {
            JButton b = (JButton) it.next();
            b.addActionListener(new JumpToResultActionListener());
        }
    }

    private class JumpToResultActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object[] path = (Object[]) paths.get(names.indexOf(((JButton)e.getSource()).getActionCommand()));

            ProjectTreeView treeView = ((CayenneModelerFrame) application.getFrameController().getView())
                    .getView().getProjectTreeView();
            ProjectTreeModel treeModel = (ProjectTreeModel) treeView.getModel();


            Object[] mutableTreeNodes = new Object[path.length];
            mutableTreeNodes[0] = treeModel.getRootNode();

            Object[] helper;
            for(int i = 1; i < path.length; i++) {
                helper = new Object[i];
                for(int j = 0; j < i;) {
                    helper[j] = path[++j];
                }
                mutableTreeNodes[i] = treeModel.getNodeForObjectPath(helper);
            }

            TreePath tp = new TreePath(mutableTreeNodes);

            // TODO: selection in a project tree view itself: there is no selection currently, so event doesn't track the previous selection
            TreeSelectionListener listener = treeView.getTreeSelectionListener();
            listener.valueChanged(new TreeSelectionEvent(treeView,
                    new TreePath[] {tp},
                    new boolean[] {true},
                    null, tp)
            );

            view.dispose();
        }
    }
}
