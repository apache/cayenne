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

import org.jdom.*;

/**
 *
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class DVObjRelationship extends DVObject {
  DVObjEntity sourceObjEntity;
  DVObjEntity targetObjEntity;
  boolean toMany;

  public DVObjRelationship(DVDataMap dataMap, Element element) {
    setName(element.getAttributeValue("name"));
    String sourceName =  element.getAttributeValue("source");
    sourceObjEntity = dataMap.getObjEntity(sourceName);
    String targetName = element.getAttributeValue("target");
    targetObjEntity = dataMap.getObjEntity(targetName);
    String toManyString = element.getAttributeValue("toMany");
    toMany = Boolean.valueOf(toManyString).booleanValue();
  }

  public DVObjEntity getSourceObjEntity(){
    return sourceObjEntity;
  }

  public DVObjEntity getTargetObjEntity(){
    return targetObjEntity;
  }

  public boolean isToMany(){
    return toMany;
  }

  public String toString(){
    return getName();
  }
}
