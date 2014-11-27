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

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import static org.apache.cayenne.access.loader.filters.FilterFactory.NULL;
import static org.apache.cayenne.access.loader.filters.FilterFactory.TRUE;

/**
 * @since 4.0
 */
public class ListFilter<T> implements Filter<T> {

    private final Collection<Filter<T>> filters;

    public ListFilter(Collection<Filter<T>> filters) {
        this.filters = filters;
    }

    @Override
    public boolean isInclude(T obj) {
        for (Filter<T> filter : filters) {
            if (!filter.isInclude(obj)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Filter<T> join(Filter<T> filter) {
        LinkedList<Filter<T>> list = new LinkedList<Filter<T>>(filters);
        if (TRUE.equals(filter) || NULL.equals(filter)) {
            // Do nothing.
        } else if (filter instanceof ListFilter) {
            list.addAll(((ListFilter<T>) filter).filters);
        } else {
            list.add(filter);
        }

        return new ListFilter<T>(list);
    }

    public static <T> Filter<T> create(Filter<T> filter1, Filter<T> filter2) {
        if (filter1 == null || TRUE.equals(filter1) || NULL.equals(filter1)) {
            return filter2;
        }

        if (filter2 == null || TRUE.equals(filter2) || NULL.equals(filter2)) {
            return filter1;
        }

        if (filter1 instanceof ListFilter) {
            return filter1.join(filter2);
        }

        if (filter2 instanceof ListFilter) {
            return filter2.join(filter1);
        }

        if (filter1.equals(filter2)) {
            return filter1;
        }

        return new ListFilter<T>(Arrays.asList(filter1, filter2));
    }

    @Override
    public String toString() {
        return StringUtils.join(filters, " & ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (getClass() != o.getClass()) {
            if (o instanceof Filter && filters.size() == 1) {
                return o.equals(filters.iterator().next());
            } else {
                return false;
            }
        }

        ListFilter that = (ListFilter) o;
        return filters != null ? filters.equals(that.filters) : that.filters == null;

    }

    @Override
    public int hashCode() {
        return filters != null ? filters.hashCode() : 0;
    }
}
