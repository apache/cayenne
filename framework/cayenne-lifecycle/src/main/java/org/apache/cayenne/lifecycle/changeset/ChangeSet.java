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
package org.apache.cayenne.lifecycle.changeset;

import java.util.Map;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.GraphDiff;

/**
 * Represents a set of changes to persistent objects corresponding to a certain lifecycle
 * stage. The changes are presented in a more usable form compared to the internal Cayenne
 * representation as {@link GraphDiff}. One or more changes to the same property of the
 * same object are all combined in a single {@link PropertyChange} instance.
 * 
 * @since 3.1
 */
public interface ChangeSet {

    public static final String OBJECT_ID_PROPERTY_NAME = "cayenne:objectId";

    /**
     * Returns a map of changes for a given object in its context, keyed by property name.
     * If the object is unchanged, an empty map is returned.
     */
    Map<String, PropertyChange> getChanges(Persistent object);
}
