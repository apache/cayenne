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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A metadata object that defines how a row in a result set can be converted to result
 * objects. SQLResult can be mapped to a single scalar, a single entity or a mix of
 * scalars and entities that is represented as an Object[].
 * 
 * @since 3.0
 */
public class SQLResult {

    protected String name;
    protected List<Object> resultDescriptors;

    /**
     * Creates an unnamed SQLResultSet.
     */
    public SQLResult() {

    }

    /**
     * Creates a named SQLResultSet.
     */
    public SQLResult(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a list of result descriptors. Column descriptors are returned as Strings,
     * entity descriptors - as {@link EntityResult}.
     */
    public List<Object> getComponents() {
        return resultDescriptors != null ? resultDescriptors : Collections.EMPTY_LIST;
    }

    public void addEntityResult(EntityResult entityResult) {
        if (resultDescriptors == null) {
            resultDescriptors = new ArrayList<Object>(3);
        }

        resultDescriptors.add(entityResult);
    }

    /**
     * Adds a result set column name to the mapping.
     */
    public void addColumnResult(String column) {
        if (resultDescriptors == null) {
            resultDescriptors = new ArrayList<Object>(3);
        }

        resultDescriptors.add(column);
    }
}
