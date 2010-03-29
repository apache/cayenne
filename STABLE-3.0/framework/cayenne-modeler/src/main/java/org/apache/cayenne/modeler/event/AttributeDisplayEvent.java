package org.apache.cayenne.modeler.event;
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


import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;

/** 
  */
public class AttributeDisplayEvent extends EntityDisplayEvent {
   
    protected Attribute[] attributes;
    
    public AttributeDisplayEvent(
            Object src,
            Attribute attribute,
            Entity entity,
            DataMap dataMap,
            DataDomain domain) {
        
            super(src, entity, dataMap, domain);
            attributes = new Attribute[] { attribute };
    }

    public AttributeDisplayEvent(
        Object src,
        Attribute[] attributes,
        Entity entity,
        DataMap dataMap,
        DataDomain domain) {

        super(src, entity, dataMap, domain);
        this.attributes = attributes;
    }
    
    public Attribute[] getAttributes() {
        return attributes;
    }
}
