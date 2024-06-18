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

package org.apache.cayenne.access.flush;

import java.io.Serializable;

import org.apache.cayenne.access.types.InternalUnsupportedTypeFactory;

/**
 * Special value that denotes generated id attribute
 *
 * @since 4.2
 */
class IdGenerationMarker implements Serializable, InternalUnsupportedTypeFactory.Marker {
    private static final long serialVersionUID = -5339942931435878094L;

    static IdGenerationMarker marker() {
        return new IdGenerationMarker();
    }

    private IdGenerationMarker() {
    }

    @Override
    public String toString() {
        return "{IdGenerationMarker}";
    }

    @Override
    public String errorMessage() {
        return "PK is not generated. Check your PK generation strategy or presence of the mutually dependent entities.";
    }
}
