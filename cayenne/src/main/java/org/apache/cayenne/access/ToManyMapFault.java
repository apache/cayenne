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
package org.apache.cayenne.access;

import org.apache.cayenne.Fault;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.Accessor;

/**
 * The fault of a to-many relationship mapped as a Map, resolved to a map of the related objects by their key.
 *
 * @param relationshipName the name of the relationship this fault stands for
 * @param mapKeyAccessor   the accessor of the map key on the related objects
 * @since 3.0
 */
public record ToManyMapFault(String relationshipName, Accessor mapKeyAccessor) implements Fault {

    @Override
    public Object resolveFault(Persistent sourceObject) {
        return new ToManyMap<>(sourceObject, relationshipName, mapKeyAccessor);
    }
}
