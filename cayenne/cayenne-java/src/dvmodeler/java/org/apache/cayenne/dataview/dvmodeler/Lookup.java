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
