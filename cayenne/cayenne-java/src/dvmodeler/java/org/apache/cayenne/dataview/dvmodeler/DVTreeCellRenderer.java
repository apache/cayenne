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

import java.util.*;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Andriy Shapochka
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class DVTreeCellRenderer extends DefaultTreeCellRenderer {
  public static final Icon dataMapNodeIcon = new ImageIcon(
      DVTreeCellRenderer.class.getResource("images/datamap-node.gif"));
  public static final Icon objEntityNodeIcon = new ImageIcon(
      DVTreeCellRenderer.class.getResource("images/objentity-node.gif"));
  public static final Icon dataViewNodeIcon = new ImageIcon(
      DVTreeCellRenderer.class.getResource("images/dataview-node.gif"));
  public static final Icon objEntityViewNodeIcon = new ImageIcon(
      DVTreeCellRenderer.class.getResource("images/objentityview-node.gif"));
  public static final Icon objEntityViewFieldNodeIcon = new ImageIcon(
      DVTreeCellRenderer.class.getResource("images/objentityviewfield-node.gif"));

  private boolean sel;

  public Component getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) {

    this.sel = sel;
    super.getTreeCellRendererComponent(
        tree, value, sel,
        expanded, leaf, row,
        hasFocus);
 /*   if (sel){
      this.setBackgroundSelectionColor(Color.WHITE);
    } else{
      this.setBackgroundNonSelectionColor(Color.WHITE);
    }*/

    if (value instanceof DVDataMap) {
      setForegroundColor(Color.BLACK);
      setIcon(dataMapNodeIcon);
    } else if (value instanceof DVObjEntity) {
      setForegroundColor(Color.BLACK);
      setIcon(objEntityNodeIcon);
    } else if (value instanceof DataView) {
      setForegroundColor(Color.BLACK);
      setIcon(dataViewNodeIcon);
    } else if (value instanceof ObjEntityView) {
      setForegroundColor(Color.BLACK);
      ObjEntityView view = (ObjEntityView)value;
      java.util.List objEntityViews = view.getObjEntityViewFields();
      for (Iterator j = objEntityViews.iterator(); j.hasNext();){
        ObjEntityViewField field = (ObjEntityViewField)j.next();
        if (field.getCalcType().equals("nocalc")){
          if (field.getObjAttribute() == null){
            setForegroundColor(Color.RED);
            break;
          }
        } else if (field.getCalcType().equals("lookup")){
          if ((field.getObjRelationship() == null)
             || (field.getLookup().getLookupObjEntityView() == null)
             || (field.getLookup().getLookupField() == null)){
            setForegroundColor(Color.RED);
            break;

          }
        }
      }
      setIcon(objEntityViewNodeIcon);

    } else if (value instanceof ObjEntityViewField) {
      ObjEntityViewField field = (ObjEntityViewField)value;
      if (field.getCalcType().equals("nocalc")){
        if (field.getObjAttribute() == null){
            setForegroundColor(Color.RED);
        } else {
            setForegroundColor(Color.BLACK);
        }
      } else if (field.getCalcType().equals("lookup")){
        if ((field.getObjRelationship() == null)
           || (field.getLookup().getLookupObjEntityView() == null)
           || (field.getLookup().getLookupField() == null)){
        setForegroundColor(Color.RED);
        } else {
          setForegroundColor(Color.BLACK);
        }
      }

      setIcon(objEntityViewFieldNodeIcon);
    } else {
      setIcon(null);
      setForegroundColor(Color.BLACK);
    }
    return this;
  }

  private void setForegroundColor(Color color){
    if(sel){
        if (color == Color.BLACK){
          this.setTextSelectionColor(Color.WHITE);
          setForeground(getTextSelectionColor());
        } else {
          this.setTextSelectionColor(Color.PINK);
          setForeground(getTextSelectionColor());
        }
      } else {
        this.setTextNonSelectionColor(color);
        setForeground(getTextNonSelectionColor());
      }
  }
}
