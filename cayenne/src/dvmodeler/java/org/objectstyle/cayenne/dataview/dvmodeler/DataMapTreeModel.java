/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.dataview.dvmodeler;

import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class DataMapTreeModel implements TreeModel{
  private List dataMaps;
  private String root = "root";
    /** Listeners. */
  private Vector treeModelListeners;

  public DataMapTreeModel() {
  }

  public void setDataMaps(List dataMaps){
    this.dataMaps = dataMaps;
    fireTreeStructureChanged(new TreeModelEvent(this, new TreePath(root)));
  }


  /**
   * Returns the child of parent at index index in the parent's child array.
   */
  public Object getChild(Object parent, int index) {
    if (root.equals(parent)) {
      return dataMaps.get(index);
    } else if (parent instanceof DataMap){
      DataMap p = (DataMap)parent;
      return p.getObjEntity(index);
    } else if (parent instanceof ObjEntity){
      ObjEntity p = (ObjEntity)parent;
      return p.getObjEntityView(index);
    } else if (parent instanceof ObjEntityView){
      ObjEntityView p = (ObjEntityView)parent;
      return p.getObjEntityViewField(index);
    } else
      return null;
  }
   /**
   * Returns the number of children of parent.
   */
  public int getChildCount(Object parent) {
    if (root.equals(parent)) {
      return dataMaps.size();
    } else if (parent instanceof DataMap){
      DataMap p = (DataMap)parent;
      return p.getObjEntityCount();
    } else if (parent instanceof ObjEntity){
      ObjEntity p = (ObjEntity)parent;
      return p.getObjEntityViewCount();
    } else if (parent instanceof ObjEntityView){
      ObjEntityView p = (ObjEntityView)parent;
      return p.getObjEntityViewFieldCount();
    } else
      return 0;
  }

  /**
   * Returns the index of child in parent.
   */
  public int getIndexOfChild(Object parent, Object child) {
    if (root.equals(parent)) {
      return dataMaps.indexOf(child);
    } else if (parent instanceof DataMap){
      DataMap p = (DataMap)parent;
      return p.getIndexOfObjEntity((ObjEntity)child);
    } else if (parent instanceof ObjEntity){
      ObjEntity p = (ObjEntity)parent;
      return p.getIndexOfObjEntityView((ObjEntityView)child);
    } else if (parent instanceof ObjEntityView){
      ObjEntityView p = (ObjEntityView)parent;
      return p.getIndexOfObjEntityViewField((ObjEntityViewField)child);
    } else
      return 0;
  }

  /**
   * Returns the root of the tree.
   */
  public Object getRoot() {
    return root;
  }

  /**
   * Returns true if node is a leaf.
   */
  public boolean isLeaf(Object node) {
    if (root.equals(node)) {
      return dataMaps == null || dataMaps.isEmpty();
    } else if (node instanceof DataMap){
      DataMap p = (DataMap)node;
      return p.getObjEntityCount() == 0;
    } else if (node instanceof ObjEntity){
      ObjEntity p = (ObjEntity)node;
      return p.getObjEntityViewCount() == 0;
    } else if (node instanceof ObjEntityView){
      ObjEntityView p = (ObjEntityView)node;
      return p.getObjEntityViewFieldCount() == 0;
    } else if (node instanceof ObjEntityViewField){
      return true;
    } else
      return true;
  }



  /**
   * Messaged when the user has altered the value for the item
   * identified by path to newValue.  Not used by this model.
   */
  public void valueForPathChanged(TreePath path, Object newValue) {
    System.out.println("*** valueForPathChanged : "
                       + path + " --> " + newValue);
  }

  public synchronized void removeTreeModelListener(TreeModelListener l) {
    if (treeModelListeners != null && treeModelListeners.contains(l)) {
      Vector v = (Vector) treeModelListeners.clone();
      v.removeElement(l);
      treeModelListeners = v;
    }
  }
  public synchronized void addTreeModelListener(TreeModelListener l) {
    Vector v = treeModelListeners == null ? new Vector(2) : (Vector) treeModelListeners.clone();
    if (!v.contains(l)) {
      v.addElement(l);
      treeModelListeners = v;
    }
  }
  protected void fireTreeNodesChanged(TreeModelEvent e) {
    if (treeModelListeners != null) {
      Vector listeners = treeModelListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((TreeModelListener) listeners.elementAt(i)).treeNodesChanged(e);
      }
    }
  }
  protected void fireTreeNodesInserted(TreeModelEvent e) {
    if (treeModelListeners != null) {
      Vector listeners = treeModelListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((TreeModelListener) listeners.elementAt(i)).treeNodesInserted(e);
      }
    }
  }
  protected void fireTreeNodesRemoved(TreeModelEvent e) {
    if (treeModelListeners != null) {
      Vector listeners = treeModelListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((TreeModelListener) listeners.elementAt(i)).treeNodesRemoved(e);
      }
    }
  }
  protected void fireTreeStructureChanged(TreeModelEvent e) {
    if (treeModelListeners != null) {
      Vector listeners = treeModelListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((TreeModelListener) listeners.elementAt(i)).treeStructureChanged(e);
      }
    }
  }

  public void replaceObjEntityView(ObjEntity oldObjEntity,
                                   int oldIndex,
                                   ObjEntity newObjEntity,
                                   ObjEntityView objEntityView) {

    DataMap oldDataMap = oldObjEntity.getDataMap();
    fireTreeNodesRemoved(new TreeModelEvent(
        this,
        new Object[] {root, oldDataMap, oldObjEntity},
        new int[] {oldIndex},
        new Object[] {objEntityView}));

    DataMap newDataMap = newObjEntity.getDataMap();

    int newIndex = newObjEntity.getIndexOfObjEntityView(objEntityView);
    fireTreeNodesInserted(new TreeModelEvent(
        this,
        new Object[] {root, newDataMap, newObjEntity},
        new int[] {newIndex},
        new Object[] {objEntityView}));

  }

  public TreePath objEntityViewAdded(ObjEntityView view) {
    ObjEntity entity = view.getObjEntity();
    if (entity == null)
      return null;
    DataMap dataMap = entity.getDataMap();
    int index = entity.getIndexOfObjEntityView(view);
    fireTreeNodesInserted(new TreeModelEvent(
        this,
        new Object[] {root, dataMap, entity},
        new int[] {index},
        new Object[] {view}));
    return new TreePath(new Object[] {root, dataMap, entity, view});
  }

  public void objEntityViewRemoved(ObjEntity entity, ObjEntityView view, int index) {
    DataMap dataMap = entity.getDataMap();
    fireTreeNodesRemoved(new TreeModelEvent(
        this,
        new Object[] {root, dataMap, entity},
        new int[] {index},
        new Object[] {view}));
  }

  public void objEntityViewsRemoved(Map removingViews) {
    HashMap views = (HashMap)removingViews;
    for(Iterator j = views.keySet().iterator(); j.hasNext();){
      ObjEntityView view = (ObjEntityView)j.next();
      ObjEntity entity = view.getObjEntity();
      Integer index = (Integer)views.get(view);
      DataMap dataMap = entity.getDataMap();

      fireTreeNodesRemoved(new TreeModelEvent(
          this,
          new Object[] {root, dataMap, entity},
          new int[] {index.intValue()},
          new Object[] {view}));
    }
  }


  public TreePath objEntityViewChanged(ObjEntityView view) {
    ObjEntity entity = view.getObjEntity();
    if (entity == null)
      return null;
    DataMap dataMap = entity.getDataMap();
    int index = entity.getIndexOfObjEntityView(view);
    fireTreeNodesChanged(new TreeModelEvent(
        this,
        new Object[] {root, dataMap, entity},
        new int[] {index},
        new Object[] {view}));
    return new TreePath(new Object[] {root, dataMap, entity, view});
  }

  public TreePath fieldAdded(ObjEntityViewField field) {
    ObjEntityView owner = field.getObjEntityView();
    ObjEntity entity = owner.getObjEntity();
    if (entity == null)
      return null;
    DataMap dataMap = entity.getDataMap();
    int index = owner.getIndexOfObjEntityViewField(field);
    fireTreeNodesInserted(new TreeModelEvent(
        this,
        new Object[] {root, dataMap, entity, owner},
        new int[] {index},
        new Object[] {field}));
    return new TreePath(new Object[] {root, dataMap, entity, owner, field});
  }

  public void fieldRemoved(ObjEntityViewField field, int index) {
    ObjEntityView owner = field.getObjEntityView();
    ObjEntity entity = owner.getObjEntity();

    DataMap dataMap = entity.getDataMap();
    fireTreeNodesRemoved(new TreeModelEvent(
        this,
        new Object[] {root, dataMap, entity, owner},
        new int[] {index},
        new Object[] {field}));

  }

  public TreePath fieldChanged(ObjEntityViewField field) {
    ObjEntityView owner = field.getObjEntityView();
    ObjEntity entity = owner.getObjEntity();
    if (entity == null)
      return null;
    DataMap dataMap = entity.getDataMap();
    int index = owner.getIndexOfObjEntityViewField(field);
    fireTreeNodesChanged(new TreeModelEvent(
        this,
        new Object[] {root, dataMap, entity, owner},
        new int[] {index},
        new Object[] {field}));
    return new TreePath(new Object[] {root, dataMap, entity, owner, field});
  }
}
