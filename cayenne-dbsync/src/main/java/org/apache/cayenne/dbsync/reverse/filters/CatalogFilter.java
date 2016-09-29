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
package org.apache.cayenne.dbsync.reverse.filters;

import java.util.Arrays;

/**
* @since 4.0.
*/
public class CatalogFilter {
    public final String name;
    public final SchemaFilter[] schemas;

    public CatalogFilter(String name, SchemaFilter... schemas) {
        if (schemas == null || schemas.length == 0) {
            throw new IllegalArgumentException("schemas(" + Arrays.toString(schemas) + ") can't be null or empty");
        }

        this.name = name;
        this.schemas = schemas;
    }

    public SchemaFilter getSchema(String schema) {
        for (SchemaFilter schemaFilter : schemas) {
            if (schemaFilter.name == null || schemaFilter.name.equals(schema)) {
                return schemaFilter;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder(), "").toString();
    }

    public StringBuilder toString(StringBuilder res, String prefix) {
        res.append(prefix).append("Catalog: ").append(name).append("\n");
        for (SchemaFilter schema : schemas) {
            schema.toString(res, prefix + "  ");
        }

        return res;
    }
}
