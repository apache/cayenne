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

public class DVObjAttribute extends DVObject {
  String type = "";
  List loadErrors = new ArrayList();

  public DVObjAttribute(Element element) {
    String attributeValue = element.getAttributeValue("name");
    if ((attributeValue == null) || (attributeValue.length() == 0)){
      setName("");
      loadErrors.add("ObjAttribute has no name<br><br>");
    } else {
      setName(attributeValue);
    }
    attributeValue = element.getAttributeValue("type");
    if ((attributeValue == null) || (attributeValue.length() == 0)){
      type = "";
      loadErrors.add( "ObjAttribute \"" + getName() + "\" has no attribute \"type\"<br><br>");
    } else {
      type = attributeValue;
    }
  }

  public List getLoadErrors(){
    return Collections.unmodifiableList(loadErrors);
  }

  public String getType(){
    return type;
  }

  public String toString(){
    return getName();
  }
}
