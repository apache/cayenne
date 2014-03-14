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

package org.apache.cayenne.dba.oracle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;

/**
 * Helper class to extract the information from BatchQueries, essential for LOB
 * columns processing.
 * 
 */
class OracleLOBBatchQueryWrapper {

    protected BatchQuery query;

    protected List<DbAttribute> dbAttributes;

    // attribute list decoders
    protected boolean[] qualifierAttributes;
    protected boolean[] allLOBAttributes;
    protected Object[] updatedLOBAttributes;

    OracleLOBBatchQueryWrapper(BatchQuery query) {
        this.query = query;
        this.dbAttributes = query.getDbAttributes();

        int len = dbAttributes.size();
        this.qualifierAttributes = new boolean[len];
        this.allLOBAttributes = new boolean[len];
        this.updatedLOBAttributes = new Object[len];

        indexQualifierAttributes();
    }

    /**
     * Indexes attributes
     */
    protected void indexQualifierAttributes() {
        int len = this.dbAttributes.size();
        for (int i = 0; i < len; i++) {
            DbAttribute attribute = this.dbAttributes.get(i);
            int type = attribute.getType();
            qualifierAttributes[i] = attribute.isPrimaryKey();
            allLOBAttributes[i] = (type == Types.BLOB || type == Types.CLOB);
        }
    }

    /**
     * Indexes attributes
     */
    void indexLOBAttributes() {
        int len = updatedLOBAttributes.length;
        for (int i = 0; i < len; i++) {
            updatedLOBAttributes[i] = null;

            if (allLOBAttributes[i]) {
                // skip null and empty LOBs
                Object value = query.getValue(i);

                if (value == null) {
                    continue;
                }

                if (dbAttributes.get(i).getType() == Types.BLOB) {
                    updatedLOBAttributes[i] = convertToBlobValue(value);
                } else {
                    updatedLOBAttributes[i] = convertToClobValue(value);
                }
            }
        }
    }

    /**
     * Converts value to byte[] if possible.
     */
    protected byte[] convertToBlobValue(Object value) {
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            return bytes.length == 0 ? null : bytes;
        } else if (value instanceof Serializable) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream() {

                @Override
                public synchronized byte[] toByteArray() {
                    return buf;
                }
            };

            try {
                ObjectOutputStream out = new ObjectOutputStream(bytes);
                out.writeObject(value);
                out.close();
            } catch (IOException e) {
                throw new CayenneRuntimeException("Error serializing object", e);
            }

            return bytes.toByteArray();
        }

        return null;
    }

    /**
     * Converts to char[] or String. Both are acceptable when writing CLOBs.
     */
    protected Object convertToClobValue(Object value) {

        if (value instanceof char[]) {
            char[] chars = (char[]) value;
            return (chars.length == 0) ? null : chars;
        } else {
            String strValue = value.toString();
            return (strValue.length() == 0) ? null : strValue;
        }
    }

    /**
     * Returns a list of DbAttributes used in the qualifier of the query that
     * selects a LOB row for LOB update.
     */
    List<DbAttribute> getDbAttributesForLOBSelectQualifier() {

        int len = qualifierAttributes.length;
        List<DbAttribute> attributes = new ArrayList<DbAttribute>(len);

        for (int i = 0; i < len; i++) {
            if (this.qualifierAttributes[i]) {
                attributes.add(this.dbAttributes.get(i));
            }
        }
        return attributes;
    }

    /**
     * Returns a list of DbAttributes that correspond to the LOB columns updated
     * in the current row in the batch query. The list will not include LOB
     * attributes that are null or empty.
     */
    List<DbAttribute> getDbAttributesForUpdatedLOBColumns() {

        int len = updatedLOBAttributes.length;
        List<DbAttribute> attributes = new ArrayList<DbAttribute>(len);

        for (int i = 0; i < len; i++) {
            if (this.updatedLOBAttributes[i] != null) {
                attributes.add(this.dbAttributes.get(i));
            }
        }
        return attributes;
    }

    List getValuesForLOBSelectQualifier() {

        int len = this.qualifierAttributes.length;
        List values = new ArrayList(len);
        for (int i = 0; i < len; i++) {
            if (this.qualifierAttributes[i]) {
                values.add(query.getValue(i));
            }
        }

        return values;
    }

    List getValuesForUpdatedLOBColumns() {

        int len = this.updatedLOBAttributes.length;
        List values = new ArrayList(len);
        for (int i = 0; i < len; i++) {
            if (this.updatedLOBAttributes[i] != null) {
                values.add(this.updatedLOBAttributes[i]);
            }
        }

        return values;
    }

}
