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
import org.jdom.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class Lookup{
  private ObjEntityViewField objEntityViewField;
  private ObjEntityView lookupObjEntityView;//required
  private ObjEntityViewField lookupField;//required

  private List saveErrors = new ArrayList();

  public Lookup(ObjEntityViewField objEntityViewField){
    this.objEntityViewField = objEntityViewField;
    lookupObjEntityView = null;
    lookupField = null;
  }

  public List getSaveErrors(){
    return Collections.unmodifiableList(saveErrors);
  }

  public boolean isEmpty(){
    return (lookupField == null)&&(lookupObjEntityView == null);
  }

  public void setLookupObjEntityView(ObjEntityView view){
    lookupObjEntityView = view;
  }

  public ObjEntityView getLookupObjEntityView(){
    return lookupObjEntityView;
  }


  public void setLookupField(ObjEntityViewField field){
    lookupField = field;
  }

  public ObjEntityViewField getLookupField(){
    return lookupField;
  }
  public String toString(){
    String resultString = "";
    if (lookupObjEntityView != null){
      resultString += lookupObjEntityView.getName();
    }

    if (lookupField != null ){
      resultString += "." + lookupField.getName();
    }else{
      resultString += "";
    }
    return resultString;
  }

  public Element getLookupElement(){
    Element e = new Element("lookup");
    if (saveErrors.size() != 0){
      saveErrors.clear();
    }
    ObjEntityView view = objEntityViewField.getObjEntityView();
    DataView dataView = view.getDataView();
    String fieldPath = "<b>" + dataView.getName() + "." + view.getName()
                       + "." + objEntityViewField.getName() + "</b><br>";
    if (lookupObjEntityView != null){
      e.setAttribute(new Attribute("obj-entity-view-name", lookupObjEntityView.getName()));
    }else {
      e.setAttribute(new Attribute("obj-entity-view-name", ""));
      saveErrors.add(fieldPath + "lookup hasn't attribute \"obj-entity-view-name\"<br><br>");
    }
    if (lookupField != null){
      e.setAttribute(new Attribute("field-name", lookupField.getName()));
    }else {
      e.setAttribute(new Attribute("field-name", ""));
      saveErrors.add(fieldPath + "lookup hasn't attribute \"field-name\"<br><br>");
    }
    e.addContent("");

    return e;
  }
}
