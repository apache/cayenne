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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jdom.Element;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class DataView extends DVObject {
  private CayenneProject cayenneProject;
  private List objEntityViews = new ArrayList();
  private File file;

  private List saveErrors = new ArrayList();

  public DataView(CayenneProject cayenneProject, String name, File file){
    setName(name);
    this.cayenneProject = cayenneProject;
    this.file = file;
  }

  public DataView(CayenneProject cayenneProject, File file){
    setName(file.getName());
    this.cayenneProject = cayenneProject;
    this.file = file;
  }

  public DataView(CayenneProject cayenneProject){
    this.cayenneProject = cayenneProject;
    setName("DataView");
    file = new File(cayenneProject.getProjectDirectory(), "DataView.view.xml");
  }

  public List getSaveErrors(){
    return Collections.unmodifiableList(saveErrors);
  }

  public CayenneProject getCayenneProject(){
    return cayenneProject;
  }

  public void setFile(File file){
    this.file = file;
  }

  public File getFile(){
    return file;
  }

  public List getObjEntityViews() {
    return Collections.unmodifiableList(objEntityViews);
  }

  public ObjEntityView createObjEntityView(List allObjEntityViews){
    String nameRoot = "ObjEntityView";
    String name = nameRoot;
    for (int i = 0; i < Integer.MAX_VALUE; i++) {
      name = nameRoot + i;
      boolean nameExists = false;
      for (int j = 0; j < allObjEntityViews.size(); j++) {
        ObjEntityView objEntityView = (ObjEntityView)allObjEntityViews.get(j);
        nameExists = name.equalsIgnoreCase(objEntityView.getName());
        if (nameExists)
          break;
      }
      if (!nameExists)
        break;
    }
    ObjEntityView objEntityView = new ObjEntityView(this);
    objEntityView.setName(name);
    return objEntityView;
  }

  public void addObjEntityView(ObjEntityView objEntityView){
    objEntityViews.add(objEntityView);
  }

  public void removeObjEntityView(ObjEntityView objEntityView){
    DVObjEntity entity = objEntityView.getObjEntity();
    if (entity != null){
      entity.removeObjEntityView(objEntityView);
    }
    objEntityViews.remove(objEntityView);
  }

  public ObjEntityView getObjEntityView(String name){
    Iterator itr = objEntityViews.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      ObjEntityView objEnt = (ObjEntityView)o;
      if (objEnt.getName().equals(name)){
        return objEnt;
      }
    }
    return null;
  }

  public ObjEntityView getObjEntityView(int index){
    return (ObjEntityView)(objEntityViews.get(index));
  }

  public int getIndexOfObjEntityView(ObjEntityView objEntityView){
    return objEntityViews.indexOf(objEntityView);
  }


  public int getObjEntityViewCount(){
    return objEntityViews.size();
  }

  public void clear(){
    if (objEntityViews.size() != 0){
      for (Iterator j = this.getObjEntityViews().iterator(); j.hasNext();){
        ObjEntityView view = (ObjEntityView)j.next();
        DVObjEntity entity = view.getObjEntity();
        entity.removeObjEntityView(view);
      }
      objEntityViews.clear();
    }
  }

  public String toString(){
    return this.getName();
  }
  public Element getDataViewElement(){
    Element dataViewElement = new Element("data-view");

    if (saveErrors.size() != 0){
      saveErrors.clear();
    }

    List objEntityViewElements = new ArrayList();
    Set objEntityViewsSort = new TreeSet(new ObjEntityViewsComparator());
    objEntityViewsSort.addAll(objEntityViews);

    Iterator itr = objEntityViewsSort.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      ObjEntityView view = (ObjEntityView)o;
      Element objEntityViewElement =
                     view.getObjEntityViewElement();
      saveErrors.addAll(view.getSaveErrors());
      objEntityViewElements.add(objEntityViewElement);
    }
    dataViewElement.setContent(objEntityViewElements);

    return dataViewElement;
  }
}
