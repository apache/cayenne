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

import java.awt.Color;
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
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectTreeModel;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneController;

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
                Object parentObject = ((Attribute) path[path.length - 1]).getParent(); 
                attrNames.put(new Integer(index++), getParentName(path, parentObject) +"."+ ((Attribute) path[path.length - 1]).getName());
             }

            if (path[path.length - 1] instanceof Relationship) {
                Object parentObject = ((Relationship) path[path.length - 1]).getParent(); 
                relatNames.put(new Integer(index++), getParentName(path, parentObject) +"."+ ((Relationship) path[path.length - 1]).getName());                
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

        font = view.getOkButton().getFont();
        fontSelected = new Font(font.getFamily(), font.BOLD, font.getSize()+2 ); 

        Color color = view.getOkButton().getBackground();        
  
        JTable table = view.getTable();
        
        table.addKeyListener(new JumpToResultsKeyListener());
        table.addMouseListener(new JumpToResultActionListener());       
        table.getSelectionModel().setSelectionInterval(0, 0);
    }

    public static void jumpToResult(Object[] path, EditorView editor) {
        
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
       private EditorView editor = ((CayenneModelerFrame) application.getFrameController().getView()).getView();
       
                public void mouseClicked(MouseEvent e) {
                    JTable table = (JTable) e.getSource();  
                    Integer selectedLine = table.getSelectionModel().getLeadSelectionIndex();
                    JLabel label = (JLabel) table.getModel().getValueAt(selectedLine, 0);                   
                    Integer index = (Integer) FindDialogView.getLabelAndObjectIndex().get(label);
                     
                    Object[] path = (Object[]) paths.get(index);
                    jumpToResult(path, editor);
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

        private EditorView editor = ((CayenneModelerFrame) application
                .getFrameController().getView()).getView();
     
        public void keyPressed(KeyEvent e) { 
           
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                
                JTable table = (JTable) e.getSource();  
                Integer selectedLine = table.getSelectionModel().getLeadSelectionIndex();
                JLabel label = (JLabel) table.getModel().getValueAt(selectedLine, 0);                   
                Integer index = (Integer) FindDialogView.getLabelAndObjectIndex().get(label);
                 
                Object[] path = (Object[]) paths.get(index);
                jumpToResult(path, editor);          
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
        mutableTreeNodes[0] = ((ProjectTreeModel) editor
                .getProjectTreeView()
                .getModel()).getRootNode();

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
  
    
    private String getParentName(Object[] path, Object  parentObject) {
        String nameParent = null;
        
        if(parentObject  instanceof ObjEntity){
            ObjEntity objEntity = (ObjEntity)parentObject;
            nameParent = objEntity.getName();
        }
        if(parentObject  instanceof DbEntity){
            DbEntity dbEntity = (DbEntity)parentObject;
            nameParent = dbEntity.getName();                    
        }
        return nameParent;
    }  
   
}
