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
package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A metadata object that defines how a DataRow can be converted to result objects. This
 * object provides mapping in a JPA-compilant manner, i.e. the DataRow is mapped either to
 * a single Object or an Object[]. Each object (single result object or an array element
 * object) can be a scalar or a Persistent object.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
// TODO: andrus, 6/22/2007 - support entity results mapping.
public class SQLResultSetMapping {

    protected String name;
    protected Collection columnResults;

    public SQLResultSetMapping() {

    }

    public SQLResultSetMapping(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a collection of mapped columns.
     */
    public Collection getColumnResults() {
        return columnResults != null ? columnResults : Collections.EMPTY_LIST;
    }

    /**
     * Adds a result set column name to the mapping.
     */
    public void addColumnResult(String column) {
        if (columnResults == null) {
            columnResults = new ArrayList(3);
        }

        columnResults.add(column);
    }
}
