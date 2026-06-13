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
package org.apache.cayenne.dba;

/**
 * Describes a single database-native (external) SQL type that a JDBC type (see {@link java.sql.Types}) maps to.
 * A JDBC type may map to more than one external type, e.g. a primary name plus the auto-increment variant used
 * for generated columns (PostgreSQL "serial").
 *
 * @since 5.0
 */
public record NativeColumnType(int jdbcType, String nativeType, boolean autoIncrement) {

    /**
     * Creates a plain external type.
     */
    public static NativeColumnType of(int jdbcType, String dbType) {
        return new NativeColumnType(jdbcType, dbType, false);
    }

    /**
     * Returns a copy of this type flagged as the auto-increment variant, e.g. PostgreSQL "serial" for
     * {@link java.sql.Types#INTEGER}. Used to render generated columns.
     */
    public NativeColumnType asAutoIncrement() {
        return new NativeColumnType(jdbcType, nativeType, true);
    }
}
