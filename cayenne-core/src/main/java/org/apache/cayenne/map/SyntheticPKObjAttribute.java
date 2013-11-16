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
package org.apache.cayenne.map;


/**
 * A "synthetic" server-side ObjAttribute used to describe unmapped PK>.
 * 
 * @since 3.0
 */
class SyntheticPKObjAttribute extends ObjAttribute {

    SyntheticPKObjAttribute(String name) {
        super(name);
    }

    @Override
    public ObjAttribute getClientAttribute() {
        ClientObjAttribute attribute = new ClientObjAttribute(getName());
        attribute.setType(getType());

        // unconditionally expose DbAttribute path and configure as mandatory.
        attribute.setDbAttributePath(dbAttributePath);
        attribute.setMandatory(true);

        DbAttribute dbAttribute = getDbAttribute();
        if (dbAttribute != null) {
            attribute.setMaxLength(dbAttribute.getMaxLength());
        }

        // TODO: will likely need "userForLocking" property as well.

        return attribute;
    }
    
    @Override
    public boolean isPrimaryKey() {
        return true;
    }
}
