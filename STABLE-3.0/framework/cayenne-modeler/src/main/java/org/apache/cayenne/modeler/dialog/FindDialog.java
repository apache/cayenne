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

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.tree.TreePath;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectTreeModel;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.query.Query;

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

    public FindDialog(CayenneController parent, java.util.List paths) {
        super(parent);

        this.paths = paths;

        Map objEntityNames = new HashMap(), dbEntityNames = new HashMap(), attrNames = new HashMap(), relatNames = new HashMap(), queryNames = new HashMap(), embeddableNames = new HashMap(), embeddableAttributeNames = new HashMap();
        Iterator it = paths.iterator();
        int index = 0;
        while (it.hasNext()) {
            Object[] path = (Object[]) it.next();

            if (path[path.length - 1] instanceof ObjEntity) {
                objEntityNames.put(
                        new Integer(index++),
                        ((ObjEntity) path[path.length - 1]).getName());
            }

            if (path[path.length - 1] instanceof DbEntity) {
                dbEntityNames.put(
                        new Integer(index++),
                        ((DbEntity) path[path.length - 1]).getName());
            }

            if (path[path.length - 1] instanceof Query) {
                queryNames.put(new Integer(index++), ((Query) path[path.length - 1])
                        .getName());
            }

            if (path[path.length - 1] instanceof Embeddable) {
                
                String name = ((Embeddable) path[path.length - 1]).getClassName();
                embeddableNames.put(
                        new Integer(index++),
                        name);
            }
            
            if (path[path.length - 1] instanceof EmbeddableAttribute) {
                
                Object parentObject = ((EmbeddableAttribute) path[path.length - 1]).getEmbeddable();
                String parName = getParentName(path, parentObject);
                embeddableAttributeNames.put(new Integer(index++), parName
                        + "."
                        + ((EmbeddableAttribute) path[path.length - 1]).getName());
            }
            if (path[path.length - 1] instanceof Attribute) {
                Object parentObject = ((Attribute) path[path.length - 1]).getParent();
                attrNames.put(new Integer(index++), getParentName(path, parentObject)
                        + "."
                        + ((Attribute) path[path.length - 1]).getName());
            }

            if (path[path.length - 1] instanceof Relationship) {
                Object parentObject = ((Relationship) path[path.length - 1]).getParent();

                /*
                 * relationships are different from attributes in that they do not
                 * correctly return the owning entity when inheritance is involved.
                 * Hopefully this will be reconciled in the future relases
                 */
                String parentName = getParentName(path, parentObject);
                if (!parentObject.equals(path[path.length - 2])) {
                    parentName = ((ObjEntity) path[path.length - 2]).getName();
                }

                relatNames.put(new Integer(index++), parentName
                        + "."
                        + ((Relationship) path[path.length - 1]).getName());
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

    public static void jumpToResult(Object[] path) {
    	EditorView editor = ((CayenneModelerFrame) Application.getInstance()
                .getFrameController()
                .getView()).getView();
    	
        if (path[path.length - 1] instanceof Entity) {

            /** Make selection in a project tree, open correspondent entity tab */
            editor.getProjectTreeView().getSelectionModel().setSelectionPath(
                    buildTreePath(path, editor));
            EntityDisplayEvent event = new EntityDisplayEvent(
                    editor.getProjectTreeView(),
                    (Entity) path[path.length - 1],
                    (DataMap) path[path.length - 2],
                    (DataDomain) path[path.length - 3]);
            event.setMainTabFocus(true);

            if (path[path.length - 1] instanceof ObjEntity)
                editor.getObjDetailView().currentObjEntityChanged(event);
            if (path[path.length - 1] instanceof DbEntity)
                editor.getDbDetailView().currentDbEntityChanged(event);
        }
        
        if (path[path.length - 1] instanceof Query) {

            /** Make selection in a project tree, open correspondent entity tab */
            editor.getProjectTreeView().getSelectionModel().setSelectionPath(
                    buildTreePath(path, editor));
            QueryDisplayEvent event = new QueryDisplayEvent(
                    editor.getProjectTreeView(),
                    (Query) path[path.length - 1],
                    (DataMap) path[path.length - 2],
                    (DataDomain) path[path.length - 3]);

            editor.currentQueryChanged(event);
        }
        
        if (path[path.length - 1] instanceof Embeddable) {

            /** Make selection in a project tree, open correspondent entity tab */
            editor.getProjectTreeView().getSelectionModel().setSelectionPath(
                    buildTreePath(path, editor));
            EmbeddableDisplayEvent event = new EmbeddableDisplayEvent(
                    editor.getProjectTreeView(),
                    (Embeddable) path[path.length - 1],
                    (DataMap) path[path.length - 2],
                    (DataDomain) path[path.length - 3]);
            event.setMainTabFocus(true);

            editor.currentEmbeddableChanged(event);
        }
        
        if (path[path.length - 1] instanceof EmbeddableAttribute) {

            /** Make selection in a project tree, open correspondent embeddable tab */
            Object[] o = new Object[path.length - 1];
            for (int i = 0; i < path.length - 1; i++)
                o[i] = path[i];
            editor.getProjectTreeView().getSelectionModel().setSelectionPath(
                    buildTreePath(o, editor));
            
            EmbeddableAttributeDisplayEvent event = new EmbeddableAttributeDisplayEvent(
                    editor.getProjectTreeView(),
                    (Embeddable) path[path.length - 2],
                    (EmbeddableAttribute) path[path.length - 1],
                    (DataMap) path[path.length - 3],
                    (DataDomain) path[path.length - 4]);
            event.setMainTabFocus(true);

            editor.getEmbeddableView().currentEmbeddableAttributeChanged(event);
        }
        
        if (path[path.length - 1] instanceof Attribute
                || path[path.length - 1] instanceof Relationship) {

            /** Make selection in a project tree, open correspondent attributes tab */
            Object[] o = new Object[path.length - 1];
            for (int i = 0; i < path.length - 1; i++)
                o[i] = path[i];
            editor.getProjectTreeView().getSelectionModel().setSelectionPath(
                    buildTreePath(o, editor));

            if (path[path.length - 1] instanceof DbAttribute) {
                AttributeDisplayEvent event = new AttributeDisplayEvent(
                        editor.getProjectTreeView(),
                        (Attribute) path[path.length - 1],
                        (Entity) path[path.length - 2],
                        (DataMap) path[path.length - 3],
                        (DataDomain) path[path.length - 4]);
                event.setMainTabFocus(true);
                editor.getDbDetailView().currentDbAttributeChanged(event);
            }

            if (path[path.length - 1] instanceof ObjAttribute) {
                AttributeDisplayEvent event = new AttributeDisplayEvent(
                        editor.getProjectTreeView(),
                        (Attribute) path[path.length - 1],
                        (Entity) path[path.length - 2],
                        (DataMap) path[path.length - 3],
                        (DataDomain) path[path.length - 4]);
                event.setMainTabFocus(true);
                editor.getObjDetailView().currentObjAttributeChanged(event);
            }

            if (path[path.length - 1] instanceof DbRelationship) {
                RelationshipDisplayEvent event = new RelationshipDisplayEvent(
                        editor.getProjectTreeView(),
                        (Relationship) path[path.length - 1],
                        (Entity) path[path.length - 2],
                        (DataMap) path[path.length - 3],
                        (DataDomain) path[path.length - 4]);
                event.setMainTabFocus(true);
                editor.getDbDetailView().currentDbRelationshipChanged(event);
            }
        }

        if (path[path.length - 1] instanceof ObjRelationship) {
            RelationshipDisplayEvent event = new RelationshipDisplayEvent(
                    editor.getProjectTreeView(),
                    (Relationship) path[path.length - 1],
                    (Entity) path[path.length - 2],
                    (DataMap) path[path.length - 3],
                    (DataDomain) path[path.length - 4]);
            event.setMainTabFocus(true);
            editor.getObjDetailView().currentObjRelationshipChanged(event);
        }

    }

    private class JumpToResultActionListener implements MouseListener {

        public void mouseClicked(MouseEvent e) {
            JTable table = (JTable) e.getSource();
            Integer selectedLine = table.getSelectionModel().getLeadSelectionIndex();
            JLabel label = (JLabel) table.getModel().getValueAt(selectedLine, 0);
            Integer index = (Integer) FindDialogView.getLabelAndObjectIndex().get(label);

            Object[] path = (Object[]) paths.get(index);
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

    private String getParentName(Object[] path, Object parentObject) {
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
