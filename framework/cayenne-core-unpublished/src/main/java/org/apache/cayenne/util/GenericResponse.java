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
import java.util.List;

import org.apache.cayenne.QueryResponse;

/**
 * A simple serializable implementation of QueryResponse.
 * 
 * @since 1.2
 */
public class GenericResponse implements QueryResponse, Serializable {

    protected List results;

    protected transient int currentIndex;

    /**
     * Creates an empty BaseResponse.
     */
    public GenericResponse() {
        results = new ArrayList();
    }

    /**
     * Creates a BaseResponse with a single result list.
     */
    public GenericResponse(List list) {
        results = new ArrayList(1);
        addResultList(list);
    }

    /**
     * Creates a response that it a shallow copy of another response.
     */
    public GenericResponse(QueryResponse response) {

        results = new ArrayList(response.size());

        response.reset();
        while (response.next()) {
            if (response.isList()) {
                addResultList(response.currentList());
            }
            else {
                addBatchUpdateCount(response.currentUpdateCount());
            }
        }
    }

    public List firstList() {
        for (reset(); next();) {
            if (isList()) {
                return currentList();
            }
        }

        return null;
    }

    public int[] firstUpdateCount() {
        for (reset(); next();) {
            if (!isList()) {
                return currentUpdateCount();
            }
        }

        return null;
    }

    public List currentList() {
        return (List) results.get(currentIndex - 1);
    }

    public int[] currentUpdateCount() {
        return (int[]) results.get(currentIndex - 1);
    }

    public boolean isList() {
        return results.get(currentIndex - 1) instanceof List;
    }

    public boolean next() {
        return ++currentIndex <= results.size();
    }

    public void reset() {
        // use a zero-based index, not -1, as this will simplify serialization handling
        currentIndex = 0;
    }

    public int size() {
        return results.size();
    }

    /**
     * Clears any previously collected information.
     */
    public void clear() {
        results.clear();
    }

    public void addBatchUpdateCount(int[] resultCount) {

        if (resultCount != null) {
            results.add(resultCount);
        }
    }

    public void addUpdateCount(int resultCount) {
        results.add(new int[] {
            resultCount
        });
    }

    public void addResultList(List list) {
        this.results.add(list);
    }

    /**
     * Replaces previously stored result with a new result.
     */
    public void replaceResult(Object oldResult, Object newResult) {
        int index = results.indexOf(oldResult);
        if (index >= 0) {
            results.set(index, newResult);
        }
    }
}
