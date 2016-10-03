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
package org.apache.cayenne.dbsync.merge;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * A interface used to tell about modifications performed on the model by
 * {@link MergerToken} with {@link MergeDirection#TO_MODEL}
 */
public interface ModelMergeDelegate {

    void dbEntityAdded(DbEntity ent);

    void dbEntityRemoved(DbEntity ent);

    void objEntityAdded(ObjEntity ent);

    void objEntityRemoved(ObjEntity ent);

    void dbAttributeAdded(DbAttribute att);

    void dbAttributeRemoved(DbAttribute att);

    void dbAttributeModified(DbAttribute att);

    void objAttributeAdded(ObjAttribute att);

    void objAttributeRemoved(ObjAttribute att);

    void objAttributeModified(ObjAttribute att);

    void dbRelationshipAdded(DbRelationship rel);

    void dbRelationshipRemoved(DbRelationship rel);

    void objRelationshipAdded(ObjRelationship rel);

    void objRelationshipRemoved(ObjRelationship rel);

}
