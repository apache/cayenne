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


package org.apache.cayenne.dataview.dvmodeler;

import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class DataViewTreeModel implements TreeModel{
  private List dataViews;
  private String root = "root";
    /** Listeners. */
  private Vector treeModelListeners;

  public DataViewTreeModel() {
  }

  public void setDataViews(List dataViews){
    this.dataViews = dataViews;
    fireTreeStructureChanged(new TreeModelEvent(this, new TreePath(root)));
  }


  /**
   * Returns the child of parent at index index in the parent's child array.
   */
  public Object getChild(Object parent, int index) {
    if (root.equals(parent)) {
      return dataViews.get(index);
    } else if (parent instanceof DataView){
      DataView p = (DataView)parent;
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
      return dataViews.size();
    } else if (parent instanceof DataView){
      DataView p = (DataView)parent;
      return p.getObjEntityViewCount();
    } else if (parent instanceof ObjEntityView){
      ObjEntityView p = (ObjEntityView)parent;
      return p.getObjEntityViewFieldCount();
    } else if (parent instanceof ObjEntityViewField){
      return 0;
    } else
      return -1;
  }

  /**
   * Returns the index of child in parent.
   */
  public int getIndexOfChild(Object parent, Object child) {
    if (root.equals(parent)) {
      return dataViews.indexOf(child);
    } else if (parent instanceof DataView){
      DataView p = (DataView)parent;
      return p.getIndexOfObjEntityView((ObjEntityView)child);
    } else if (parent instanceof ObjEntityView){
      ObjEntityView p = (ObjEntityView)parent;
      return p.getIndexOfObjEntityViewField((ObjEntityViewField)child);
    } else
      return -1;
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
      return dataViews == null || dataViews.isEmpty();
    } else if (node instanceof DataView){
      DataView p = (DataView)node;
      return p.getObjEntityViewCount() == 0;
    } else if (node instanceof ObjEntityView){
      ObjEntityView p = (ObjEntityView)node;
      return p.getObjEntityViewFieldCount() == 0;
    } else if (node instanceof ObjEntityViewField){
      return true;
    } else
      return true;
  }

  public TreePath dataViewAdded(DataView dataView) {
    int index = dataViews.indexOf(dataView);
    if (index < 0) return null;
    fireTreeNodesInserted(new TreeModelEvent(
        this,
        new TreePath(root),
        new int[] {index},
        new Object[] {dataView}));
    return new TreePath(new Object[] {root, dataView});
  }

  public void dataViewRemoved(DataView dataView, int index) {

    fireTreeNodesRemoved(new TreeModelEvent(
        this,
        new TreePath(root),
        new int[] {index},
        new Object[] {dataView}));
  }

  public TreePath dataViewChanged(DataView dataView) {
    int index = dataViews.indexOf(dataView);
    if (index < 0) return null;
    fireTreeNodesChanged(new TreeModelEvent(
        this,
        new TreePath(root),
        new int[] {index},
        new Object[] {dataView}));
    return new TreePath(new Object[] {root, dataView});
  }

  public TreePath objEntityViewAdded(ObjEntityView view) {
    DataView owner = view.getDataView();
    int index = owner.getIndexOfObjEntityView(view);
    fireTreeNodesInserted(new TreeModelEvent(
        this,
        new Object[] {root, owner},
        new int[] {index},
        new Object[] {view}));
    return new TreePath(new Object[] {root, owner, view});
  }

  public void objEntityViewRemoved(ObjEntityView view, int index) {
    DataView owner = view.getDataView();
    fireTreeNodesRemoved(new TreeModelEvent(
        this,
        new Object[] {root, owner},
        new int[] {index},
        new Object[] {view}));
  }

  public TreePath objEntityViewChanged(ObjEntityView view) {
    DataView owner = view.getDataView();
    int index = owner.getIndexOfObjEntityView(view);
    fireTreeNodesChanged(new TreeModelEvent(
        this,
        new Object[] {root, owner},
        new int[] {index},
        new Object[] {view}));
    return new TreePath(new Object[] {root, owner, view});
  }

  public TreePath fieldAdded(ObjEntityViewField field) {
    ObjEntityView owner = field.getObjEntityView();
    DataView dataView = owner.getDataView();
    int index = owner.getIndexOfObjEntityViewField(field);
    fireTreeNodesInserted(new TreeModelEvent(
        this,
        new Object[] {root, dataView, owner},
        new int[] {index},
        new Object[] {field}));
    return new TreePath(new Object[] {root, dataView, owner, field});
  }

  public void fieldRemoved(ObjEntityViewField field, int index) {
    ObjEntityView owner = field.getObjEntityView();
    DataView dataView = owner.getDataView();
    fireTreeNodesRemoved(new TreeModelEvent(
        this,
        new Object[] {root, dataView, owner},
        new int[] {index},
        new Object[] {field}));
  }

  public TreePath fieldChanged(ObjEntityViewField field) {
    ObjEntityView owner = field.getObjEntityView();
    DataView dataView = owner.getDataView();
    int index = owner.getIndexOfObjEntityViewField(field);
    fireTreeNodesChanged(new TreeModelEvent(
        this,
        new Object[] {root, dataView, owner},
        new int[] {index},
        new Object[] {field}));
    return new TreePath(new Object[] {root, dataView, owner, field});
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
}
