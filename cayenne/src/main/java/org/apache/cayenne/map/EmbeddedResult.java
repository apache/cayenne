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

import java.util.HashMap;
import java.util.Map;

/**
 * A metadata object that provides mapping of a set of result columns to an Embeddable object.
 * Used by {@link SQLResult}.
 * Note that fields in the EmbeddedResult are not required to follow the order of columns
 * in the actual query, and can be added in the arbitrary order.
 *
 * @since 4.2
 */
public class EmbeddedResult {

    private final Map<String, String> fields;
    private final Embeddable embeddable;

    public EmbeddedResult(Embeddable embeddable, int size) {
        this.embeddable = embeddable;
        this.fields = new HashMap<>((int) Math.ceil(size / 0.75));
    }

    public void addAttribute(ObjAttribute attr) {
        fields.put(attr.getDbAttributePath().value(), getAttributeName(attr));
    }

    private static String getAttributeName(ObjAttribute attr) {
        String name = attr.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public Embeddable getEmbeddable() {
        return embeddable;
    }
}
