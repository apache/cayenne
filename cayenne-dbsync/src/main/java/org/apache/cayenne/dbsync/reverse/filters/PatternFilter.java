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

import org.apache.cayenne.util.Util;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * @since 4.0
 */
public class PatternFilter {

    public static final PatternFilter INCLUDE_EVERYTHING = new PatternFilter(true) {

        @Override
        public boolean isIncluded(String obj) {
            return true;
        }

        @Override
        protected StringBuilder toString(StringBuilder res) {
            return res.append("ALL");
        }
    };

    public static final PatternFilter INCLUDE_NOTHING = new PatternFilter(true) {
        @Override
        public boolean isIncluded(String obj) {
            return false;
        }

        @Override
        protected StringBuilder toString(StringBuilder res) {
            return res.append("NONE");
        }
    };

    public static final Comparator<Pattern> PATTERN_COMPARATOR = (o1, o2) -> {
        if (o1 != null && o2 != null) {
            return o1.pattern().compareTo(o2.pattern());
        } else {
            return -1;
        }
    };

    private final SortedSet<Pattern> includes;
    private final SortedSet<Pattern> excludes;
    private boolean useCaseSensitiveNaming;

    public PatternFilter(boolean useCaseSensitiveNaming) {
        this.includes = new TreeSet<>(PATTERN_COMPARATOR);
        this.excludes = new TreeSet<>(PATTERN_COMPARATOR);
        this.useCaseSensitiveNaming = useCaseSensitiveNaming;
    }

    public SortedSet<Pattern> getIncludes() {
        return includes;
    }

    public PatternFilter include(Pattern p) {
        includes.add(p);

        return this;
    }

    public PatternFilter exclude(Pattern p) {
        excludes.add(p);

        return this;
    }

    public static Pattern pattern(String pattern, boolean useCaseSensitiveNaming) {
        if (pattern == null) {
            return null;
        }
        return useCaseSensitiveNaming
                ? Pattern.compile(pattern)
                : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    public PatternFilter include(String p) {
        return include(pattern(p, useCaseSensitiveNaming));
    }

    public PatternFilter exclude(String p) {
        return exclude(pattern(p, useCaseSensitiveNaming));
    }

    public boolean isIncluded(String obj) {
        boolean include = includes.isEmpty();
        for (Pattern p : includes) {
            if (p != null) {
                if (p.matcher(obj).matches()) {
                    include = true;
                    break;
                }
            }
        }

        if (!include) {
            return false;
        }

        for (Pattern p : excludes) {
            if (p.matcher(obj).matches()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }


        PatternFilter filter = (PatternFilter) o;
        return includes.equals(filter.includes)
                && excludes.equals(filter.excludes);
    }

    @Override
    public int hashCode() {
        return includes.hashCode();
    }

    protected StringBuilder toString(StringBuilder res) {
        if (includes.isEmpty()) {
            // Do nothing.
        } else if (includes.size() > 1) {
            res.append("(").append(Util.join(includes, " OR ")).append(")");
        } else {
            res.append(includes.first().pattern());
        }

        if (!excludes.isEmpty()) {
            res.append(" AND NOT (").append(Util.join(includes, " OR ")).append(")");
        }

        return res;
    }

    public boolean isEmpty() {
        return includes.isEmpty() && excludes.isEmpty();
    }
}
