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
import java.io.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class DataMap extends DVObject {
  private CayenneProject cayenneProject;
  private List objEntities = new ArrayList();
  private List objRelationships = new ArrayList();
  private File file;

  public DataMap(CayenneProject cayenneProject, String name, File file){
    setName(name);
    this.cayenneProject = cayenneProject;
    this.file = file;
  }

  public DataMap(CayenneProject cayenneProject, File file){
    setName(file.getName());
    this.cayenneProject = cayenneProject;
    this.file = file;
  }

  public CayenneProject getCayenneProject(){
    return cayenneProject;
  }

  public void addObjEntityView(ObjEntityView objEntityView){
    String objEntityName = objEntityView.getObjEntity().getName();
    ObjEntity entity = this.getObjEntity(objEntityName);
    entity.addObjEntityView(objEntityView);
  }

  public void addObjRelationship(ObjRelationship objRelationship){

    objRelationships.add(objRelationship);
  }

  public List getObjRelationshipsBySource(ObjEntity objEntity){
    List list = new ArrayList();
    Iterator itr = objRelationships.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      ObjRelationship objRelationship = (ObjRelationship)o;
      if (objRelationship.getSourceObjEntity() == objEntity){
        list.add(objRelationship);
      }
    }
    return Collections.unmodifiableList(list);
  }

  public List getObjRelationshipsBySourceToOne(ObjEntity objEntity){
    List list = new ArrayList();
    Iterator itr = objRelationships.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      ObjRelationship objRelationship = (ObjRelationship)o;
      if ((objRelationship.getSourceObjEntity() == objEntity)
         && (!objRelationship.isToMany())){
        list.add(objRelationship);
      }
    }
    return Collections.unmodifiableList(list);
  }

  public List getObjRelationshipsByTarget(ObjEntity objEntity){
    List list = new ArrayList();
    Iterator itr = objRelationships.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      ObjRelationship objRelationship = (ObjRelationship)o;
      if (objRelationship.getTargetObjEntity() == objEntity){
        list.add(objRelationship);
      }
    }
    return Collections.unmodifiableList(list);
  }

  public ObjRelationship getObjRelationship(String name, ObjEntity source){
    Iterator itr = objRelationships.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      ObjRelationship objRelationship = (ObjRelationship)o;
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

  public void addObjEntity(ObjEntity objEntity){
    objEntities.add(objEntity);
  }

  public ObjEntity getObjEntity(String name){
    Iterator itr = objEntities.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      ObjEntity objEnt = (ObjEntity)o;
      if (objEnt.getName().equals(name)){
        return objEnt;
      }
    }
    return null;
  }

  public ObjEntity getObjEntity(int index){
    return (ObjEntity)(objEntities.get(index));
  }

  public int getIndexOfObjEntity(ObjEntity objEntity){
    return objEntities.indexOf(objEntity);
  }


  public int getObjEntityCount(){
    return objEntities.size();
  }

  public void clear(){
    Iterator itr = objEntities.iterator();
    while (itr.hasNext()){
      Object o = itr.next();
      ObjEntity entity =(ObjEntity)o;
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
