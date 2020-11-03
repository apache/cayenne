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
package org.apache.cayenne.dbsync.model;

import org.apache.cayenne.map.DbEntity;

/**
 * A {@link DbEntity} subclass used to hold extra JDBC metadata.
 * @since 4.2 moved from org.apache.cayenne.map package
 */
public class DetectedDbEntity extends DbEntity {

    protected String primaryKeyName;
    protected String type;

    public DetectedDbEntity(String name) {
        super(name);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * Sets the optional primary key name of this DbEntity. This is not the same as the
     * name of the DbAttribute, but the name of the unique constraint.
     */
    public void setPrimaryKeyName(String primaryKeyName) {
        this.primaryKeyName = primaryKeyName;
    }

    /**
     * Returns the optional primary key name of this DbEntity. This is not the same as the
     * name of the DbAttribute, but the name of the unique constraint.
     */
    public String getPrimaryKeyName() {
        return primaryKeyName;
    }

}
