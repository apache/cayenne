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
 * A client version of ObjAttribute that has some properties from DbAttribute that the
 * client may want.
 * 
 * @since 3.0
 * @author Tore Halset
 */
public class ClientObjAttribute extends ObjAttribute {
    
    protected boolean mandatory;
    protected int maxLength = -1;


    public ClientObjAttribute() {
        super();
    }

    public ClientObjAttribute(String name, String type, ObjEntity entity) {
        super(name, type, entity);
    }

    public ClientObjAttribute(String name) {
        super(name);
    }

    /**
     * 
     * @see DbAttribute#isMandatory()
     */
    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
    
    /**
     * 
     * @see DbAttribute#getMaxLength()
     */
    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

}
