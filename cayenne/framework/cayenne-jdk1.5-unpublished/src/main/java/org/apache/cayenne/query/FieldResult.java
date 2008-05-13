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

import org.apache.cayenne.util.ToStringBuilder;

/**
 * A metadata object that maps a result set column to an ObjAttribute or DbAttribute. Used
 * by {@link EntityResult}.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class FieldResult {

    protected String entityName;
    protected String attributeName;
    protected String column;
    protected boolean dbAttribute;

    FieldResult(String entityName, String attributeName, String column,
            boolean dbAttribute) {

        this.entityName = entityName;
        this.attributeName = attributeName;
        this.column = column;
        this.dbAttribute = dbAttribute;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getColumn() {
        return column;
    }

    public boolean isDbAttribute() {
        return dbAttribute;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("attributeName", attributeName).append(
                "column",
                column).append("db", dbAttribute).toString();
    }
}
