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
package org.apache.cayenne.map.compiled;

import java.util.List;

/**
 * A compiled stored procedure.
 *
 * @param map            the map the procedure belongs to
 * @param name           the procedure name
 * @param catalog        the catalog, or null
 * @param schema         the schema, or null
 * @param returningValue true if the procedure returns a value, described by its first parameter
 * @param parameters     the call parameters in order
 * @since 6.0
 */
public record CompiledProcedure(
        CompiledDataMap map,
        String name,
        String catalog,
        String schema,
        boolean returningValue,
        List<CompiledProcedureParameter> parameters) {

    /**
     * Returns the procedure name including the catalog and the schema, if any of those are present.
     */
    public String fullyQualifiedName() {
        return (catalog != null && !catalog.isEmpty() ? catalog + '.' : "")
                + (schema != null && !schema.isEmpty() ? schema + '.' : "")
                + name;
    }

    @Override
    public String toString() {
        return "CompiledProcedure[" + name + "]";
    }
}
