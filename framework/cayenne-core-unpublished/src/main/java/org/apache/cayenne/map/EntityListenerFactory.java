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
 * A factory used to create entity listeners. By default listeners are created
 * using a noop constructor on the listener class, however if a user needs to
 * customize listener creation process (e.g. perform dependency injection,
 * etc.), a custom factory can be installed on the EntityResolver.
 * EntityListenerFactory also supports defining listener interfaces, not just
 * classes).
 * 
 * @since 3.0
 * @deprecated since 3.2, as EntityResolver no longer loads listeners from its
 *             DataMaps.
 */
public interface EntityListenerFactory {

    /**
     * Creates an instance of entity listener of a given class. "entity"
     * parameter denotes ObjEntity for which the listener is installed. It is
     * null if this is a default listener.
     * 
     * @return a listener of the given class. May return null to indicate that a
     *         configured listener should be suppressed.
     */
    Object createListener(EntityListener listenerMapping, ObjEntity entity);
}
