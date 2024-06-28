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
package org.apache.cayenne.dbsync.reverse.filters;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @since 4.0.
 */
public class IncludeTableFilter implements Comparable<IncludeTableFilter> {
    public final Pattern pattern;

    public final PatternFilter columnsFilter;

    /**
     * @since 4.1
     */
    public final PatternFilter relationshipFilter;

    public IncludeTableFilter(String pattern) {
        this(pattern, PatternFilter.INCLUDE_EVERYTHING, PatternFilter.INCLUDE_EVERYTHING);
    }

    public IncludeTableFilter(String pattern, PatternFilter columnsFilter) {
        this(pattern, columnsFilter, PatternFilter.INCLUDE_EVERYTHING);
    }

    /**
     * @since 4.1
     */
    public IncludeTableFilter(String pattern, PatternFilter columnsFilter, PatternFilter relationshipFilter) {
        this.pattern = PatternFilter.pattern(pattern);
        this.columnsFilter = columnsFilter;
        this.relationshipFilter = relationshipFilter;
    }

    public boolean isIncludeColumn(String name) {
        return columnsFilter.isIncluded(name);
    }

    /**
     * @since 4.1
     */
    public boolean isIncludeRelationship(String name) {
        return relationshipFilter.isIncluded(name);
    }

    @Override
    public int compareTo(IncludeTableFilter o) {
        if (pattern == null && o.pattern == null) {
            return 0;
        } else if (pattern == null) {
            return 1;
        } else if (o.pattern == null) {
            return -1;
        } else {
            return pattern.pattern().compareTo(o.pattern.pattern());
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IncludeTableFilter that = (IncludeTableFilter) o;

        if (!Objects.equals(pattern, that.pattern)) return false;
        if (!Objects.equals(columnsFilter, that.columnsFilter)) return false;
        return Objects.equals(relationshipFilter, that.relationshipFilter);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder(), "").toString();
    }

    protected StringBuilder toString(StringBuilder res, String prefix) {
        res.append(prefix).append("Include: ").append(String.valueOf(pattern)).append(" Columns: ");
        columnsFilter.toString(res);
        res.append("\n");

        return res;
    }
}
