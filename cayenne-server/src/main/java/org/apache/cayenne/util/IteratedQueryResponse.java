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

package org.apache.cayenne.util;

import org.apache.cayenne.ResultIterator;

/**
 * Implementation of QueryResponse for iterated query.
 *
 * @since 5.0
 */
public class IteratedQueryResponse extends GenericResponse {
    private ResultIterator<?> iterator;

    public IteratedQueryResponse(ResultIterator<?> iterator) {
        this.iterator = iterator;
    }

    public void setIterator(ResultIterator<?> iterator) {
        this.iterator = iterator;
    }

    @Override
    public int size() {
        return -1;
    }

    @Override
    public boolean isIterator() {
        return true;
    }

    @Override
    public ResultIterator<?> currentIterator() {
        return iterator;
    }

    @Override
    public boolean next() {
        return false;
    }

    @Override
    public ResultIterator<?> firstIterator() {
        return iterator;
    }

}
