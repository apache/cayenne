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

package org.apache.cayenne.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.QueryResponse;

/**
 * A QueryResponse optimized to hold a single object or data row list.
 * 
 * @since 1.2
 */
public class ListResponse implements QueryResponse, Serializable {

    protected List objectList;

    protected transient int currentIndex;

    /**
     * Creates an empty response.
     */
    public ListResponse() {
        this.objectList = new ArrayList(1);
    }

    public ListResponse(Object object) {
        this.objectList = Collections.singletonList(object);
    }

    public ListResponse(List objectList) {
        this.objectList = objectList;
    }

    public int size() {
        return 1;
    }

    public boolean isList() {
        if (currentIndex != 1) {
            throw new IndexOutOfBoundsException("Past iteration end: " + currentIndex);
        }

        return true;
    }

    public List currentList() {
        if (currentIndex != 1) {
            throw new IndexOutOfBoundsException("Past iteration end: " + currentIndex);
        }

        return objectList;
    }

    public int[] currentUpdateCount() {
        throw new IllegalStateException("Current object is not an update count");
    }

    public boolean next() {
        return ++currentIndex <= 1;
    }

    public void reset() {
        // use a zero-based index, not -1, as this will simplify serialization handling
        currentIndex = 0;
    }

    public List firstList() {
        return objectList;
    }

    public int[] firstUpdateCount() {
        return new int[0];
    }
}
