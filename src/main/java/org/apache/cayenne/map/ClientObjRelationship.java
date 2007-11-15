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
 * A read-only ObjRelationship relationship that caches some information that is
 * dynamically calculated in a superclass.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ClientObjRelationship extends ObjRelationship {

    String reverseRelationshipName;

    // note that field names are different from the ones defined by super for the same
    // property... This is needed so that Hessian sreialization mechanism could work.
    boolean clientReadOnly;
    boolean clientToMany;

    ClientObjRelationship(String name, String reverseRelationshipName, boolean toMany,
            boolean readOnly) {

        super(name);
        this.clientToMany = toMany;
        this.clientReadOnly = readOnly;
        this.reverseRelationshipName = reverseRelationshipName;
    }

    public boolean isToMany() {
        return clientToMany;
    }

    public boolean isReadOnly() {
        return clientReadOnly;
    }

    public String getReverseRelationshipName() {
        return reverseRelationshipName;
    }

    public ObjRelationship getReverseRelationship() {
        if (reverseRelationshipName == null) {
            return null;
        }

        Entity target = getTargetEntity();
        if (target == null) {
            return null;
        }

        return (ObjRelationship) target.getRelationship(reverseRelationshipName);
    }
}
