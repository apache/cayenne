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


import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.cayenne.util.Util;

/**
 * TableFilter contain at least one IncludeTable always.
 */
public class TableFilter {

    private final SortedSet<IncludeTableFilter> includes;
    private final SortedSet<Pattern> excludes;

    /**
     * Includes can contain only one include table
     */
    public TableFilter(SortedSet<IncludeTableFilter> includes, SortedSet<Pattern> excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    public boolean isIncludeTable(String tableName) {
        PatternFilter columnFilter = getIncludeTableColumnFilter(tableName);
        return columnFilter != null;
    }

    /**
     * Return filter for columns in case we should take this table
     */
    public PatternFilter getIncludeTableColumnFilter(String tableName) {
        IncludeTableFilter include = getIncludeTableFilter(tableName);
        if (include == null) return null;

        return include.columnsFilter;
    }

    /**
     * @since 4.1
     */
    public PatternFilter getIncludeTableRelationshipFilter(String tableName) {
        IncludeTableFilter include = getIncludeTableFilter(tableName);
        if (include == null) return null;

        return include.relationshipFilter;
    }

    private IncludeTableFilter getIncludeTableFilter(String tableName) {
        IncludeTableFilter include = null;
        for (IncludeTableFilter p : includes) {
            if (p.pattern == null || p.pattern.matcher(tableName).matches()) {
                include = p;
                break;
            }
        }

        if (include == null) {
            return null;
        }

        for (Pattern p : excludes) {
            if (p != null) {
                if (p.matcher(tableName).matches()) {
                    return null;
                }
            }
        }
        return include;
    }

    public SortedSet<IncludeTableFilter> getIncludes() {
        return includes;
    }

    public static TableFilter include(String tablePattern) {
        TreeSet<IncludeTableFilter> includes = new TreeSet<>();
        includes.add(new IncludeTableFilter(tablePattern == null ? null : tablePattern.replaceAll("%", ".*")));

        return new TableFilter(includes, new TreeSet<>());
    }

    public static TableFilter everything() {
        TreeSet<IncludeTableFilter> includes = new TreeSet<>();
        includes.add(new IncludeTableFilter(null));

        return new TableFilter(includes, new TreeSet<>());
    }

    protected StringBuilder toString(StringBuilder res, String prefix) {
        res.append(prefix).append("Tables: ").append("\n");

        for (IncludeTableFilter include : includes) {
            include.toString(res, prefix + "  ");
        }

        if (!excludes.isEmpty()) {
            res.append(prefix).append("  ").append(Util.join(excludes, " OR ")).append("\n");
        }

        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TableFilter)) {
            return false;
        }

        TableFilter that = (TableFilter) o;

        return excludes.equals(that.excludes)
                && includes.equals(that.includes);

    }

    @Override
    public int hashCode() {
        int result = includes.hashCode();
        result = 31 * result + excludes.hashCode();
        return result;
    }
}
