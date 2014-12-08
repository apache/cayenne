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
package org.apache.cayenne.access.loader.filters;

import org.apache.cayenne.map.DbEntity;

import java.util.regex.Pattern;

/**
* @since 4.0.
*/
public class DbPath implements Comparable<DbPath> {

    public static final DbPath EMPTY = new DbPath();

    public static final String SEPARATOR = "/";
    public final String catalog;
    public final String schema;
    public final String tablePattern;

    private final String path;

    public DbPath() {
        this(null, null, null);
    }

    public DbPath(String catalog) {
        this(catalog, null, null);
    }

    public DbPath(String catalog, String schema) {
        this(catalog, schema, null);
    }

    public DbPath(String catalog, String schema, String tablePattern) {
        this.catalog = prepareValue(catalog);
        this.schema = prepareValue(schema);
        this.tablePattern = prepareValue(tablePattern);

        this.path = join(this.catalog, this.schema, this.tablePattern);
    }

    private static String join(String first, String second) {
        if (second == null || second.equals("%")) {
            return first;
        } else {
            return escapeNull(first) + SEPARATOR + second;
        }
    }

    private static String join(String catalog, String schema, String table) {
        String join = join(catalog, join(schema, table));
        return escapeNull(join);
    }

    private static String escapeNull(String join) {
        return join == null ? "%" : join;
    }

    private String prepareValue(String value) {
        return value == null ? null : value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DbPath dbPath = (DbPath) o;

        return path.equals(dbPath.path);
    }


    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public int compareTo(DbPath o) {
        return path.compareTo(o.path);
    }

    public boolean isCover(String catalog, String schema) {
        return isCover(catalog, schema, null);
    }

    public boolean isCover(String catalog, String schema, String table) {
        if (this.catalog == null && catalog == null) {
            return schemaCover(schema, table);
        } else if (this.catalog == null) {
            return schemaCover(schema, table);
        } else {
            return this.catalog.equalsIgnoreCase(catalog) && schemaCover(schema, table);
        }
    }

    private boolean schemaCover(String schema, String table) {
        if (this.schema == null && schema == null) {
            return tableCover(table);
        } else if (this.schema == null) {
            return tableCover(table);
        } else {
            return this.schema.equalsIgnoreCase(schema) && tableCover(table);
        }
    }

    private boolean tableCover(String table) {
        return this.tablePattern == null
                || table != null && Pattern.compile(this.tablePattern, Pattern.CASE_INSENSITIVE).matcher(table).matches();
    }

    public boolean isCover(DbPath dbPath) {
        if (dbPath == null) {
            throw new IllegalArgumentException("dbPath can't be null");
        }
        return isCover(dbPath.catalog, dbPath.schema, dbPath.tablePattern);
    }

    public DbPath merge(DbPath path) {
        if (this.isCover(path)) {
            return this;
        } else if (path.isCover(this)) {
            return path;
        } else {
            return null;
        }
    }

    public static DbPath build(DbEntity entity) {
        return new DbPath(entity.getCatalog(), entity.getSchema(), entity.getName());
    }
}
