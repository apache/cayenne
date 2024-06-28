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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @since 4.0
 */
public class PatternFilter {

    public static final PatternFilter INCLUDE_EVERYTHING = new PatternFilter() {

        @Override
        public boolean isIncluded(String obj) {
            return true;
        }

        @Override
        protected StringBuilder toString(StringBuilder res) {
            return res.append("ALL");
        }
    };

    public static final PatternFilter INCLUDE_NOTHING = new PatternFilter() {
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

    private final List<Pattern> includes;
    private final List<Pattern> excludes;

    public PatternFilter() {
        this.includes = new ArrayList<>();
        this.excludes = new ArrayList<>();
    }

    public List<Pattern> getIncludes() {
        return includes;
    }

    public List<Pattern> getExcludes() {
        return excludes;
    }

    public PatternFilter include(Pattern p) {
        includes.add(p);

        return this;
    }

    public PatternFilter exclude(Pattern p) {
        excludes.add(p);

        return this;
    }

    public static Pattern pattern(String pattern) {
        if (pattern == null) {
            return null;
        }
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    public PatternFilter include(String p) {
        return include(pattern(p));
    }

    public PatternFilter exclude(String p) {
        return exclude(pattern(p));
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

        if (!(o instanceof PatternFilter)) {
            return false;
        }

        PatternFilter that = (PatternFilter) o;

        if (includes == that.includes) {
            return true;
        }

        if (includes.size() != that.includes.size()) {
            return false;
        }

        // Check if the lists have the same patterns in the same order
        for (int i = 0; i < includes.size(); i++) {
            Pattern pattern = excludes.get(i);
            Pattern thatPattern = that.excludes.get(i);
            if (!pattern.pattern().equals(thatPattern.pattern())) {
                return false;
            }
        }
        return true;
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
            res.append(includes.get(0).pattern());
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
