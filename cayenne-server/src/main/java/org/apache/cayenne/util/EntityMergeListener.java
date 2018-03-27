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
package org.apache.cayenne.util;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;

/**
 * EntityMergeListener interface defines API useful for listening to
 * EntityMergeSupport processing.
 *  
 */
public interface EntityMergeListener {
    /**
     * Invoked when a missing attribute in ObjEntity is completed from DbEntity
     */
    void objAttributeAdded(ObjAttribute attr);
    
    /**
     * Invoked when a missing relationship in ObjEntity is completed from DbEntity
     */
    void objRelationshipAdded(ObjRelationship relationship);
}
