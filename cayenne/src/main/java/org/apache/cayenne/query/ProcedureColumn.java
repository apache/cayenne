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

package org.apache.cayenne.query;

/**
 * An adapter-independent description of a single column in a {@link ProcedureQuery} result set. Used to explicitly
 * describe procedure results when ResultSet metadata is insufficient. Translated into a runtime {@code ColumnDescriptor}
 * at query execution time.
 *
 * @since 5.0
 */
public record ProcedureColumn(String name, String dataRowKey, int jdbcType, String javaClass) {

    public ProcedureColumn {
        if (dataRowKey == null) {
            dataRowKey = name;
        }
    }
}
