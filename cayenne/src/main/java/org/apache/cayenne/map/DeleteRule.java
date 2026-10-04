/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
 * Defines what happens to the target objects of an ObjRelationship when the source object is deleted. A relationship
 * with no delete rule takes no action on its target objects.
 */
public enum DeleteRule {

    /**
     * Remove the reference that the destination has to this source (if the inverse relationship is toOne, nullify,
     * if toMany, remove the source object)
     */
    NULLIFY,

    /**
     * Delete the destination object(s)
     */
    CASCADE,

    /**
     * If the relationship has any objects (toOne or toMany), deny the delete. (Destination objects would therefore
     * have to be deleted manually first)
     */
    DENY;

    /**
     * Default delete rule for one-to-many relationships. It is used when new rels are created via modeler, or when
     * synchrozining Obj- and DbEntities
     */
    public static final DeleteRule DEFAULT_DELETE_RULE_TO_MANY = DENY;

    /**
     * Default delete rule for many-to-one relationships. It is used when new rels are created via modeler, or when
     * synchrozining Obj- and DbEntities
     */
    public static final DeleteRule DEFAULT_DELETE_RULE_TO_ONE = NULLIFY;
}
