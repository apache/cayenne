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
import java.io.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class DVDataMap extends DVObject {
  private CayenneProject cayenneProject;
  private List objEntities = new ArrayList();
  private List objRelationships = new ArrayList();
  private File file;

  public DVDataMap(CayenneProject cayenneProject, String name, File file){
    setName(name);
    this.cayenneProject = cayenneProject;
    this.file = file;
  }

  public DVDataMap(CayenneProject cayenneProject, File file){
    setName(file.getName());
    this.cayenneProject = cayenneProject;
    this.file = file;
  }

  public CayenneProject getCayenneProject(){
    return cayenneProject;
  }

  public void addObjEntityView(ObjEntityView objEntityView){
    String objEntityName = objEntityView.getObjEntity().getName();
    DVObjEntity entity = this.getObjEntity(objEntityName);
    entity.addObjEntityView(objEntityView);
  }

  public void addObjRelationship(DVObjRelationship objRelationship){

    objRelationships.add(objRelationship);
  }

  public List getObjRelationshipsBySource(DVObjEntity objEntity){
    List list = new ArrayList();
    Iterator itr = objRelationships.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      DVObjRelationship objRelationship = (DVObjRelationship)o;
      if (objRelationship.getSourceObjEntity() == objEntity){
        list.add(objRelationship);
      }
    }
    return Collections.unmodifiableList(list);
  }

  public List getObjRelationshipsBySourceToOne(DVObjEntity objEntity){
    List list = new ArrayList();
    Iterator itr = objRelationships.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      DVObjRelationship objRelationship = (DVObjRelationship)o;
      if ((objRelationship.getSourceObjEntity() == objEntity)
         && (!objRelationship.isToMany())){
        list.add(objRelationship);
      }
    }
    return Collections.unmodifiableList(list);
  }

  public List getObjRelationshipsByTarget(DVObjEntity objEntity){
    List list = new ArrayList();
    Iterator itr = objRelationships.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      DVObjRelationship objRelationship = (DVObjRelationship)o;
      if (objRelationship.getTargetObjEntity() == objEntity){
        list.add(objRelationship);
      }
    }
    return Collections.unmodifiableList(list);
  }

  public DVObjRelationship getObjRelationship(String name, DVObjEntity source){
    Iterator itr = objRelationships.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      DVObjRelationship objRelationship = (DVObjRelationship)o;
      if ((objRelationship.getSourceObjEntity() == source) && (objRelationship.getName().equals(name))){
        return objRelationship;
      }
    }
    return null;
  }

  public void setFile(File file){
    this.file = file;
  }

  public File getFile(){
    return file;
  }

  public List getObjEntities() {
    return Collections.unmodifiableList(objEntities);
  }

  public void addObjEntity(DVObjEntity objEntity){
    objEntities.add(objEntity);
  }

  public DVObjEntity getObjEntity(String name){
    Iterator itr = objEntities.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      DVObjEntity objEnt = (DVObjEntity)o;
      if (objEnt.getName().equals(name)){
        return objEnt;
      }
    }
    return null;
  }

  public DVObjEntity getObjEntity(int index){
    return (DVObjEntity)(objEntities.get(index));
  }

  public int getIndexOfObjEntity(DVObjEntity objEntity){
    return objEntities.indexOf(objEntity);
  }


  public int getObjEntityCount(){
    return objEntities.size();
  }

  public void clear(){
    Iterator itr = objEntities.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      DVObjEntity entity =(DVObjEntity)o;
      for(int i = 0; i < entity.getObjEntityViewCount(); i++){
        entity.getObjEntityView(i).clearObjEntity();
      }
    }
    objEntities.clear();
    objRelationships.clear();
  }

  public String toString(){
    return this.getName();
  }
}
