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
 * @since 4.0
 */
public class IncludeFilter implements Filter<String> {

    private final Pattern pattern;

    IncludeFilter(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean isInclude(String obj) {
        return pattern.matcher(obj).matches();
    }

    @Override
    public Filter join(Filter filter) {
        return ListFilter.create(this, filter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof ListFilter) {
            return o.equals(this);
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return pattern.toString().equals(((IncludeFilter) o).pattern.toString());

    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public String toString() {
        return "+(" + pattern + ')';
    }

    protected Pattern getPattern() {
        return pattern;
    }
}
