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

public class DVObjEntity extends DVObject {
  private DVDataMap dataMap;
  private String className;
  private List objAttributes = new ArrayList();
  private List objEntityViews = new ArrayList();
  private List loadErrors = new ArrayList();

  private Set objAttributesNames = new HashSet();

  public DVObjEntity(DVDataMap dataMap, Element element) throws DVModelerException{
    this.dataMap = dataMap;
    String entityPath = "<b>" + dataMap.getName() + ".";
    String attributeValue = element.getAttributeValue("name");
    if ((attributeValue == null) || (attributeValue.length() == 0)){
      entityPath += "</b><br>";
      loadErrors.add(entityPath + " ObjEntity has no attribute \"name\" and cannot be loaded.<br><br>");
      throw new DVModelerException(entityPath + " Entity has no attribute \"name\".");
    } else {
      setName(attributeValue);
      entityPath += attributeValue + "</b><br>";
    }
    attributeValue = element.getAttributeValue("className");
    if ((attributeValue == null) || (attributeValue.length() == 0)){
      className = "";
      loadErrors.add(entityPath + " ObjEntity has no attribute \"class-name\"<br><br>");
    } else {
      className = attributeValue;
    }
    List children = element.getChildren();
    java.util.List attributeErrors = new ArrayList();
    Iterator itr = children.iterator();
    while(itr.hasNext()){
      Object o = itr.next();
      Element e = (Element)o;
      if (e.getName().equals("obj-attribute")){
        DVObjAttribute objAttribute = new DVObjAttribute(e);
        objAttributes.add(objAttribute);
        if (objAttributesNames.add(objAttribute.getName()) == false){
          String path = "<b>" + dataMap + "." + getName() + "</b><br>";
          loadErrors.add(path + "ObjAttribute \"" + objAttribute.getName()
              + "\" already exists in the ObjEntity\"" + getName() + "\"<br><br>");
        }

        attributeErrors.addAll(objAttribute.getLoadErrors());
      }
    }
    objAttributesNames.clear();
    for (Iterator j = attributeErrors.iterator(); j.hasNext();){
      String error = entityPath + ((String)j.next());
      loadErrors.add(error);
    }
    dataMap.addObjEntity(this);
  }

  public DVObjEntity(DVDataMap dataMap){
    this.dataMap = dataMap;
    setName("ObjEntity");
    className = "";
    dataMap.addObjEntity(this);
  }

  public List getLoadErrors(){
    return Collections.unmodifiableList(loadErrors);
  }

  public void setDataMap(DVDataMap dataMap){
    this.dataMap = dataMap;
  }

  public DVDataMap getDataMap(){
    return dataMap;
  }

  public void addObjEntityView(ObjEntityView objEntityView){
    objEntityViews.add(objEntityView);
  }

  public void removeObjEntityView(ObjEntityView objEntityView){
    objEntityViews.remove(objEntityView);
  }

  public List getObjAttributes(){
    return Collections.unmodifiableList(objAttributes);
  }

  public DVObjAttribute getObjAttribute(String name){
  	Iterator itr = objAttributes.iterator();
  	while (itr.hasNext()){
  		Object o = itr.next();
  		DVObjAttribute attribute = (DVObjAttribute)o;
  		if(attribute.getName().equals(name)){
  		  return attribute;
  		}
  	}
  	return null;
  }

  public DVObjAttribute getObjAttribute(int index){
    return (DVObjAttribute)objAttributes.get(index);
  }

  public List getObjEntityViews(){
    return Collections.unmodifiableList(objEntityViews);
  }

  public void setClassName(String className){
    this.className = className;
  }

  public String getClassName(){
    return className;
  }

  public String toString(){
    return this.getName();
  }

  public ObjEntityView getObjEntityView(int index){
    return (ObjEntityView)(objEntityViews.get(index));
  }

  public int getObjEntityViewCount(){
    return objEntityViews.size();
  }

  public int getObjAttributeCount(){
    return objAttributes.size();
  }

  public int getIndexOfObjEntityView(ObjEntityView objEntityView){
    return objEntityViews.indexOf(objEntityView);
  }

}
