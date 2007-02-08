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

package org.apache.cayenne;


/**
 * An ObjectId for new objects that hasn't been committed to the external data store. On
 * commit, a TempObjectId is replaced with a permanent ObjectId tied to a primary key of
 * an object in the external data store.
 * <h3>Upgrade Note:</h3>
 * <p>
 * If you were referencing TempObjectId explicitly in your code (e.g. if(id instanceof
 * TempObjectId)...), you will need to modify the code and use "isTemporary()" superclass
 * method.
 * </p>
 * 
 * @author Andrei Adamchik
 * @deprecated since 1.2 superclass can represent both permanent and temporary id.
 */
public class TempObjectId extends ObjectId {

    /**
     * Creates a non-portable temporary ObjectId that should be replaced by a permanent id
     * once a corresponding object is committed.
     */
    public TempObjectId(Class objectClass) {
        super(objectClass);
    }

    /**
     * Always returns <code>true</code>.
     */
    public boolean isTemporary() {
        return true;
    }
}
