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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.*;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectTreeModel;
import org.apache.cayenne.modeler.ProjectTreeView;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.query.*;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An instance of this class is responsible for displaying search results and navigating
 * to the selected entity's representation.
 */
public class FindDialog extends CayenneController {

    private FindDialogView view;
    private List paths;
    private static Font font;
    private static Font fontSelected;

    public static Font getFont() {
        return font;
    }

    public static Font getFontSelected() {
        return fontSelected;
    }

    public FindDialog(CayenneController parent, List paths) {
        super(parent);

        this.paths = paths;

        Map<Integer, String> objEntityNames = new HashMap<Integer, String>(), dbEntityNames = new HashMap<Integer, String>(), attrNames = new HashMap<Integer, String>(), relatNames = new HashMap<Integer, String>(), queryNames = new HashMap<Integer, String>(), embeddableNames = new HashMap<Integer, String>(), embeddableAttributeNames = new HashMap<Integer, String>();
        Iterator it = paths.iterator();
        int index = 0;
        while (it.hasNext()) {
            Object path = it.next();

            if (path instanceof ObjEntity) {
                objEntityNames.put(new Integer(index++), ((ObjEntity) path).getName());
            }

            else if (path instanceof DbEntity) {
                dbEntityNames.put(new Integer(index++), ((DbEntity) path).getName());
            }

            else if (path instanceof Query) {
                queryNames.put(new Integer(index++), ((Query) path).getName());
            }

            else if (path instanceof Embeddable) {
                String name = ((Embeddable) path).getClassName();
                embeddableNames.put(new Integer(index++), name);
            }

            else if (path instanceof EmbeddableAttribute) {
                Embeddable parentObject = ((EmbeddableAttribute) path).getEmbeddable();
                embeddableAttributeNames.put(new Integer(index++), parentObject
                        .getClassName()
                        + "."
                        + ((EmbeddableAttribute) path).getName());
            }
            else if (path instanceof Attribute) {
                Object parentObject = ((Attribute) path).getParent();
                attrNames.put(new Integer(index++), getParentName(parentObject)
                        + "."
                        + ((Attribute) path).getName());
            }

            else if (path instanceof Relationship) {
                Object parentObject = ((Relationship) path).getParent();

                /*
                 * relationships are different from attributes in that they do not
                 * correctly return the owning entity when inheritance is involved.
                 * Hopefully this will be reconciled in the future relases
                 */
                String parentName = getParentName(parentObject);
                // if (!parentObject.equals(path)) {
                // parentName = ((ObjEntity) path[path.length - 2]).getName();
                // }

                relatNames.put(new Integer(index++), parentName
                        + "."
                        + ((Relationship) path).getName());
            }
        }

        view = new FindDialogView(
                objEntityNames,
                dbEntityNames,
                attrNames,
                relatNames,
                queryNames,
                embeddableNames,
                embeddableAttributeNames);
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

        font = view.getOkButton().getFont();
        fontSelected = new Font(font.getFamily(), font.BOLD, font.getSize() + 2);

        JTable table = view.getTable();
        table.setRowHeight(fontSelected.getSize() + 6);
        table.setRowMargin(0);
        table.addKeyListener(new JumpToResultsKeyListener());
        table.addMouseListener(new JumpToResultActionListener());
        table.getSelectionModel().setSelectionInterval(0, 0);
    }

    public static void jumpToResult(Object path) {
        EditorView editor = ((CayenneModelerFrame) Application
                .getInstance()
                .getFrameController()
                .getView()).getView();
        DataChannelDescriptor domain = (DataChannelDescriptor) Application
                .getInstance()
                .getProject()
                .getRootNode();
        if (path instanceof Entity) {

            Object[] o = new Object[3];
            o[0] = domain;
            o[1] = ((Entity) path).getDataMap();
            o[2] = (Entity) path;

            TreePath treePath = buildTreePath(o, editor);
            ProjectTreeView projectTreeView = editor.getProjectTreeView();
            if (!projectTreeView.isExpanded(treePath.getParentPath())) {
                projectTreeView.expandPath(treePath.getParentPath());
            }

            /** Make selection in a project tree, open correspondent entity tab */
            projectTreeView.getSelectionModel().setSelectionPath(treePath);
            EntityDisplayEvent event = new EntityDisplayEvent(
                    projectTreeView,
                    (Entity) path,
                    ((Entity) path).getDataMap(),
                    domain);
            event.setMainTabFocus(true);

            if (path instanceof ObjEntity)
                editor.getObjDetailView().currentObjEntityChanged(event);
            if (path instanceof DbEntity)
                editor.getDbDetailView().currentDbEntityChanged(event);
        }
        else if (path instanceof QueryDescriptor) {

            DataMap dmForQuery = null;

            dmForQuery = ((QueryDescriptor) path).getDataMap();

            Object[] o = new Object[3];
            o[0] = domain;
            o[1] = dmForQuery;
            o[2] = path;

            TreePath treePath = buildTreePath(o, editor);
            ProjectTreeView projectTreeView = editor.getProjectTreeView();
            if (!projectTreeView.isExpanded(treePath.getParentPath())) {
                projectTreeView.expandPath(treePath.getParentPath());
            }

            /** Make selection in a project tree, open correspondent entity tab */
            editor.getProjectTreeView().getSelectionModel().setSelectionPath(treePath);
            QueryDisplayEvent event = new QueryDisplayEvent(
                    projectTreeView,
                    (QueryDescriptor) path,
                    dmForQuery,
                    domain);

            editor.currentQueryChanged(event);
        }

        else if (path instanceof Embeddable) {

            Object[] o = new Object[3];
            o[0] = domain;
            o[1] = ((Embeddable) path).getDataMap();
            o[2] = (Embeddable) path;

            TreePath treePath = buildTreePath(o, editor);
            ProjectTreeView projectTreeView = editor.getProjectTreeView();
            if (!projectTreeView.isExpanded(treePath.getParentPath())) {
                projectTreeView.expandPath(treePath.getParentPath());
            }

            /** Make selection in a project tree, open correspondent entity tab */
            editor.getProjectTreeView().getSelectionModel().setSelectionPath(treePath);
            EmbeddableDisplayEvent event = new EmbeddableDisplayEvent(projectTreeView, (Embeddable) path, ((Embeddable) path)
                    .getDataMap(), domain);
            event.setMainTabFocus(true);

            editor.currentEmbeddableChanged(event);
        }

        else if (path instanceof EmbeddableAttribute) {

            /** Make selection in a project tree, open correspondent embeddable tab */
            Object[] o = new Object[3];
            o[0] = domain;
            o[1] = ((EmbeddableAttribute) path).getEmbeddable().getDataMap();
            o[2] = ((EmbeddableAttribute) path).getEmbeddable();

            TreePath treePath = buildTreePath(o, editor);
            ProjectTreeView projectTreeView = editor.getProjectTreeView();
            if (!projectTreeView.isExpanded(treePath.getParentPath())) {
                projectTreeView.expandPath(treePath.getParentPath());
            }

            editor.getProjectTreeView().getSelectionModel().setSelectionPath(treePath);

            EmbeddableAttributeDisplayEvent event = new EmbeddableAttributeDisplayEvent(
                    projectTreeView,
                    ((EmbeddableAttribute) path).getEmbeddable(),
                    (EmbeddableAttribute) path,
                    ((EmbeddableAttribute) path).getEmbeddable().getDataMap(),
                    domain);
            event.setMainTabFocus(true);

            editor.getEmbeddableView().currentEmbeddableAttributeChanged(event);
        }

        else if (path instanceof Attribute || path instanceof Relationship) {

            /** Make selection in a project tree, open correspondent attributes tab */
            Object[] o = new Object[3];
            o[0] = domain;
            if (path instanceof Attribute) {
                o[1] = ((Attribute) path).getEntity().getDataMap();
                o[2] = ((Attribute) path).getEntity();
            }
            else {
                o[1] = ((Relationship) path).getSourceEntity().getDataMap();
                o[2] = ((Relationship) path).getSourceEntity();
            }

            TreePath treePath = buildTreePath(o, editor);
            ProjectTreeView projectTreeView = editor.getProjectTreeView();
            if (!projectTreeView.isExpanded(treePath.getParentPath())) {
                projectTreeView.expandPath(treePath.getParentPath());
            }

            projectTreeView.getSelectionModel().setSelectionPath(treePath);

            if (path instanceof DbAttribute) {
                AttributeDisplayEvent event = new AttributeDisplayEvent(projectTreeView, (Attribute) path, ((Attribute) path)
                        .getEntity(), ((Attribute) path).getEntity().getDataMap(), domain);
                event.setMainTabFocus(true);
                editor.getDbDetailView().currentDbAttributeChanged(event);
            }

            if (path instanceof ObjAttribute) {
                AttributeDisplayEvent event = new AttributeDisplayEvent(projectTreeView, (Attribute) path, ((Attribute) path)
                        .getEntity(), ((Attribute) path).getEntity().getDataMap(), domain);
                event.setMainTabFocus(true);
                editor.getObjDetailView().currentObjAttributeChanged(event);
            }

            if (path instanceof DbRelationship) {
                RelationshipDisplayEvent event = new RelationshipDisplayEvent(projectTreeView, (Relationship) path, ((Relationship) path)
                        .getSourceEntity(), ((Relationship) path)
                        .getSourceEntity()
                        .getDataMap(), domain);
                event.setMainTabFocus(true);
                editor.getDbDetailView().currentDbRelationshipChanged(event);
            }
            if (path instanceof ObjRelationship) {
                RelationshipDisplayEvent event = new RelationshipDisplayEvent(projectTreeView, (Relationship) path, ((Relationship) path)
                        .getSourceEntity(), ((Relationship) path)
                        .getSourceEntity()
                        .getDataMap(), domain);
                event.setMainTabFocus(true);
                editor.getObjDetailView().currentObjRelationshipChanged(event);
            }
        }
    }

    private class JumpToResultActionListener implements MouseListener {

        public void mouseClicked(MouseEvent e) {
            JTable table = (JTable) e.getSource();
            Integer selectedLine = table.getSelectionModel().getLeadSelectionIndex();
            JLabel label = (JLabel) table.getModel().getValueAt(selectedLine, 0);
            Integer index = (Integer) FindDialogView.getLabelAndObjectIndex().get(label);

            Object path = paths.get(index);
            jumpToResult(path);
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

    }

    private class JumpToResultsKeyListener implements KeyListener {

        public void keyPressed(KeyEvent e) {

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                JTable table = (JTable) e.getSource();
                Integer selectedLine = table.getSelectionModel().getLeadSelectionIndex();
                JLabel label = (JLabel) table.getModel().getValueAt(selectedLine, 0);
                Integer index = (Integer) FindDialogView.getLabelAndObjectIndex().get(
                        label);

                Object[] path = (Object[]) paths.get(index);
                jumpToResult(path);
            }
        }

        public void keyReleased(KeyEvent e) {
        }

        public void keyTyped(KeyEvent e) {
        }
    }

    /**
     * Builds a tree path for a given path. Urgent for later selection.
     * 
     * @param path
     * @return tree path
     */
    private static TreePath buildTreePath(Object[] path, EditorView editor) {
        Object[] mutableTreeNodes = new Object[path.length];
        mutableTreeNodes[0] = ((ProjectTreeModel) editor.getProjectTreeView().getModel())
                .getRootNode();

        Object[] helper;
        for (int i = 1; i < path.length; i++) {
            helper = new Object[i];
            for (int j = 0; j < i;) {
                helper[j] = path[++j];
            }
            mutableTreeNodes[i] = ((ProjectTreeModel) editor
                    .getProjectTreeView()
                    .getModel()).getNodeForObjectPath(helper);
        }
        return new TreePath(mutableTreeNodes);
    }

    private String getParentName(Object parentObject) {
        String nameParent = null;

        if (parentObject instanceof ObjEntity) {
            ObjEntity objEntity = (ObjEntity) parentObject;
            nameParent = objEntity.getName();
        }
        if (parentObject instanceof DbEntity) {
            DbEntity dbEntity = (DbEntity) parentObject;
            nameParent = dbEntity.getName();
        }
        if (parentObject instanceof Embeddable) {
            Embeddable embeddable = (Embeddable) parentObject;
            nameParent = embeddable.getClassName();
        }
        return nameParent;
    }

}
