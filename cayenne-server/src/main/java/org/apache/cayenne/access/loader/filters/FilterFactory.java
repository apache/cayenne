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

import java.util.regex.Pattern;

/**
 * @since 3.2.
 */
public class FilterFactory {

    public static Filter<String> TRUE = new Filter<String>() {
        @Override
        public boolean isInclude(String obj) {
            return true;
        }

        @Override
        public Filter<String> join(Filter<String> filter) {
            return filter == null || NULL.equals(filter) ? this : filter;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass();
        }

        @Override
        public String toString() {
            return "true";
        }
    };

    public static Filter<String> NULL = new Filter<String>() {

        @Override
        public boolean isInclude(String obj) {
            return false;
        }

        @Override
        public Filter<String> join(Filter<String> filter) {
            return filter == null ? this : filter;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass();
        }

        @Override
        public String toString() {
            return "null";
        }
    };

    public static Filter<String> include(String tablePattern) {
        return new IncludeFilter(pattern(tablePattern));
    }

    public static Filter<String> exclude(String tablePattern) {
        return new ExcludeFilter(pattern(tablePattern));
    }

    public static Filter<String> list(Filter<String> ... filters) {
        Filter<String> res = NULL;
        for (Filter<String> filter : filters) {
            res = res.join(filter);
        }
        return res;
    }

    public static Pattern pattern(String tablePattern) {
        return Pattern.compile(tablePattern, Pattern.CASE_INSENSITIVE);
    }


}
