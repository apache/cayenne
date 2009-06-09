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
package org.apache.cayenne.merge;

import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * A interface used to tell about modifications performed on the model by
 * {@link MergerToken} with {@link MergeDirection#TO_MODEL}
 * 
 * @see DbLoaderDelegate
 */
public interface ModelMergeDelegate {

    public void dbEntityAdded(DbEntity ent);

    public void dbEntityRemoved(DbEntity ent);

    public void objEntityAdded(ObjEntity ent);

    public void objEntityRemoved(ObjEntity ent);

    public void dbAttributeAdded(DbAttribute att);

    public void dbAttributeRemoved(DbAttribute att);

    public void dbAttributeModified(DbAttribute att);

    public void objAttributeAdded(ObjAttribute att);

    public void objAttributeRemoved(ObjAttribute att);

    public void objAttributeModified(ObjAttribute att);

    public void dbRelationshipAdded(DbRelationship rel);

    public void dbRelationshipRemoved(DbRelationship rel);

    public void objRelationshipAdded(ObjRelationship rel);

    public void objRelationshipRemoved(ObjRelationship rel);

}
