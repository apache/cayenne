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

    if (value instanceof DataMap) {
      setForegroundColor(Color.BLACK);
      setIcon(dataMapNodeIcon);
    } else if (value instanceof ObjEntity) {
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