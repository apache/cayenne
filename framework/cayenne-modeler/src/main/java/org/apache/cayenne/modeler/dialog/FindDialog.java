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
import java.util.*;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.tree.TreePath;

import org.apache.cayenne.map.*;
import org.apache.cayenne.modeler.*;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.access.DataDomain;

/**
 * An instance of this class is responsible for displaying search results
 * and navigating to the selected entity's representation. 
 */
public class FindDialog extends CayenneController {
    private FindDialogView view;
    private List paths;

    public FindDialog(CayenneController parent, java.util.List paths) {
        super(parent);

        this.paths = paths;

        Map objEntityNames = new HashMap(), dbEntityNames = new HashMap(), attrNames = new HashMap(), relatNames = new HashMap();
        Iterator it = paths.iterator();
        int index = 0;
        while (it.hasNext()) {
            Object[] path = (Object[]) it.next();

            if (path[path.length - 1] instanceof ObjEntity) {
                objEntityNames.put(new Integer(index++), ((ObjEntity) path[path.length - 1]).getName());
            }

            if (path[path.length - 1] instanceof DbEntity) {
                dbEntityNames.put(new Integer(index++), ((DbEntity) path[path.length - 1]).getName());
            }

            if (path[path.length - 1] instanceof Attribute) {
                attrNames.put(new Integer(index++), ((Attribute) path[path.length - 1]).getName());
            }

            if (path[path.length - 1] instanceof Relationship) {
                relatNames.put(new Integer(index++), ((Relationship) path[path.length - 1]).getName());
            }

        }

        view = new FindDialogView(objEntityNames, dbEntityNames, attrNames, relatNames);

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
        private EditorView editor = ((CayenneModelerFrame) application.getFrameController().getView()).getView();

        public void actionPerformed(ActionEvent e) {
            Object[] path = (Object[]) paths.get(((FindDialogView.EntityButtonModel) ((JButton) e.getSource()).getModel()).getIndex().intValue());


//            DbEntityTabbedView dbEntityTabbedView = editor.getDbDetailView();
            if(path[path.length - 1] instanceof ObjEntity || path[path.length - 1] instanceof DbEntity)
                /**
                 * Make selection in a project tree, open correspondent entity tab.
                 */
                editor.getProjectTreeView().getSelectionModel().addSelectionPath(buildTreePath(path));


            if(path[path.length - 1] instanceof Attribute || path[path.length - 1] instanceof Relationship) {
                /**
                 * Make selection in a project tree, open correspondent attributes tab.
                 */
                Object[] o = new Object[path.length - 1];
                for(int i = 0; i < path.length - 1; i++)
                    o[i] = path[i];
                editor.getProjectTreeView().getSelectionModel().addSelectionPath(buildTreePath(o));


                if (path[path.length - 1] instanceof DbAttribute) {
                    AttributeDisplayEvent event = new AttributeDisplayEvent(
                            editor.getDbDetailView(),
                            (Attribute) path[path.length - 1],
                            (Entity) path[path.length - 2],
                            (DataMap) path[path.length - 3],
                            (DataDomain) path[path.length - 4]);

                    ((CayenneModelerController) parent).getProjectController().fireDbAttributeDisplayEvent(event);
                }

                if (path[path.length - 1] instanceof ObjAttribute) {
                    AttributeDisplayEvent event = new AttributeDisplayEvent(
                            editor.getObjDetailView(),
                            (Attribute) path[path.length - 1],
                            (Entity) path[path.length - 2],
                            (DataMap) path[path.length - 3],
                            (DataDomain) path[path.length - 4]);
                    ((CayenneModelerController) parent).getProjectController().fireObjAttributeDisplayEvent(event);
                }

                if (path[path.length - 1] instanceof DbRelationship) {
                    RelationshipDisplayEvent event = new RelationshipDisplayEvent(
                            editor.getDbDetailView(),
                            (Relationship) path[path.length - 1],
                            (Entity) path[path.length - 2],
                            (DataMap) path[path.length - 3],
                            (DataDomain) path[path.length - 4]
                    );
                    ((CayenneModelerController) parent).getProjectController().fireDbRelationshipDisplayEvent(event);
                }
            }

                if (path[path.length - 1] instanceof ObjRelationship) {
                    RelationshipDisplayEvent event = new RelationshipDisplayEvent(
                            editor.getObjDetailView(),
                            (Relationship) path[path.length - 1],
                            (Entity) path[path.length - 2],
                            (DataMap) path[path.length - 3],
                            (DataDomain) path[path.length - 4]
                    );
                    ((CayenneModelerController) parent).getProjectController().fireObjRelationshipDisplayEvent(event);
                }

            view.dispose();
        }

        /**
         * Builds a tree path for a given path. Urgent for later selection.
         * @param path
         * @return tree path
         */
        private TreePath buildTreePath(Object[] path) {
            Object[] mutableTreeNodes = new Object[path.length];
            mutableTreeNodes[0] = ((ProjectTreeModel) editor.getProjectTreeView().getModel()).getRootNode();

            Object[] helper;
            for(int i = 1; i < path.length; i++) {
                helper = new Object[i];
                for(int j = 0; j < i;) {
                    helper[j] = path[++j];
                }
                mutableTreeNodes[i] = ((ProjectTreeModel) editor.getProjectTreeView().getModel()).getNodeForObjectPath(helper);
            }

            return new TreePath(mutableTreeNodes);
        }

    }

}
