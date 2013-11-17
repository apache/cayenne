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

import java.io.Serializable;

/**
 * Represents a placeholder for an unresolved relationship from a source object. Fault is
 * resolved via {@link #resolveFault(Persistent, String)}. Depending on the type of fault
 * it is resolved differently. Each type of fault is a singleton that can be obtained via
 * corresponding static method.
 * 
 * @since 1.1
 */
public abstract class Fault implements Serializable {

    protected Fault() {
    }

    /**
     * Returns an object for a given source object and relationship.
     */
    public abstract Object resolveFault(Persistent sourceObject, String relationshipName);
}
