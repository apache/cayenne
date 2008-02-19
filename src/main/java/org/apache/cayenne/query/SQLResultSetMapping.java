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
import java.util.Collections;
import java.util.List;

/**
 * A metadata object that defines how a DataRow can be converted to result objects. This
 * object provides mapping in a JPA-compliant manner, i.e. the DataRow is mapped either to
 * a single Object or an Object[]. Each object (single result object or an array element
 * object) can be a scalar or a Persistent object.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
// TODO: andrus, 6/22/2007 - support entity results mapping.
public class SQLResultSetMapping {

    protected String name;
    protected List<Object> resultDescriptors;

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
     * Returns a list of result descriptors. Column descriptors are returned as Strings,
     * entity descriptors - as {@link EntityResult}.
     */
    public List<Object> getResultDescriptors() {
        return resultDescriptors != null ? resultDescriptors : Collections.EMPTY_LIST;
    }

    public int[] getEntityResultPositions() {
        if (resultDescriptors == null) {
            return new int[0];
        }

        int[] positions = new int[resultDescriptors.size()];
        int j = 0;
        for (int i = 0; i < positions.length; i++) {
            if (resultDescriptors.get(i) instanceof EntityResult) {
                positions[j++] = i;
            }
        }

        int[] trimmed = new int[j];
        System.arraycopy(positions, 0, trimmed, 0, j);
        return trimmed;
    }

    public int[] getColumnResultPositions() {
        if (resultDescriptors == null) {
            return new int[0];
        }

        int[] positions = new int[resultDescriptors.size()];
        int j = 0;
        for (int i = 0; i < positions.length; i++) {
            if (resultDescriptors.get(i) instanceof String) {
                positions[j++] = i;
            }
        }

        int[] trimmed = new int[j];
        System.arraycopy(positions, 0, trimmed, 0, j);
        return trimmed;
    }

    public EntityResult getEntityResult(int position) {
        if (resultDescriptors == null) {
            throw new IndexOutOfBoundsException("Invalid EntityResult index: " + position);
        }

        Object result = resultDescriptors.get(position);
        if (result instanceof EntityResult) {
            return (EntityResult) result;
        }

        throw new IllegalArgumentException("Result at position "
                + position
                + " is not an entity result");
    }

    public String getColumnResult(int position) {
        if (resultDescriptors == null) {
            throw new IndexOutOfBoundsException("Invalid column index: " + position);
        }

        Object result = resultDescriptors.get(position);
        if (result instanceof String) {
            return (String) result;
        }

        throw new IllegalArgumentException("Result at position "
                + position
                + " is not a column result");
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
