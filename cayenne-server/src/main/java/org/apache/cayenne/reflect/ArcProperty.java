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

package org.apache.cayenne.reflect;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;

/**
 * A Property that represents an "arc" connecting source node to the target node
 * in the graph.
 * 
 * @since 1.2
 */
public interface ArcProperty extends PropertyDescriptor {

    /**
     * Returns a relationship associated with this arc.
     * 
     * @since 3.0
     */
    ObjRelationship getRelationship();

    /**
     * Returns a path over reverse DbRelationships for this arc's
     * ObjRelationship.
     * 
     * @since 4.0
     */
    String getComplimentaryReverseDbRelationshipPath();

    /**
     * Returns a complimentary reverse ArcProperty or null if no reverse arc
     * exists.
     */
    ArcProperty getComplimentaryReverseArc();

    /**
     * Returns a ClassDescriptor for the type of graph nodes pointed to by this
     * arc property. Note that considering that a target object may be a
     * subclass of the class handled by the descriptor, users of this method may
     * need to call {@link ClassDescriptor#getSubclassDescriptor(Class)} before
     * using the descriptor to access objects.
     */
    ClassDescriptor getTargetDescriptor();

    /**
     * Returns whether a target node connected to a given object is an
     * unresolved fault.
     * 
     * @param source
     *            an object that is a source object of the relationship.
     */
    boolean isFault(Object source);

    /**
     * Turns a property of an object into a fault.
     * 
     * @since 3.0
     */
    void invalidate(Object object);
}
