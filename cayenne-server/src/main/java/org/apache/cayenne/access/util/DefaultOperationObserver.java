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

package org.apache.cayenne.access.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.Util;

/**
 * Simple implementation of OperationObserver interface. Useful as a superclass
 * of other implementations of OperationObserver. This implementation only
 * tracks transaction events and exceptions.
 * <p>
 * <i>This operation observer is unsafe to use in application, since it doesn't
 * rethrow the exceptions immediately, and may cause the database to hang.</i>
 * </p>
 * 
 */
public class DefaultOperationObserver implements OperationObserver {

    protected List<Throwable> globalExceptions = new ArrayList<Throwable>();
    protected Map<Query, Throwable> queryExceptions = new HashMap<Query, Throwable>();

    /**
     * Prints the information about query and global exceptions.
     */
    public void printExceptions(PrintWriter out) {
        if (globalExceptions.size() > 0) {
            if (globalExceptions.size() == 1) {
                out.println("Global Exception:");
            } else {
                out.println("Global Exceptions:");
            }

            for (final Throwable th : globalExceptions) {
                th.printStackTrace(out);
            }
        }

        if (queryExceptions.size() > 0) {
            if (queryExceptions.size() == 1) {
                out.println("Query Exception:");
            } else {
                out.println("Query Exceptions:");
            }

            for (final Query query : queryExceptions.keySet()) {
                Throwable th = queryExceptions.get(query);
                th.printStackTrace(out);
            }
        }
    }

    /**
     * Returns a list of global exceptions that occured during data operation
     * run.
     */
    public List<Throwable> getGlobalExceptions() {
        return globalExceptions;
    }

    /**
     * Returns a list of exceptions that occured during data operation run by
     * query.
     */
    public Map<Query, Throwable> getQueryExceptions() {
        return queryExceptions;
    }

    /**
     * Returns <code>true</code> if at least one exception was registered during
     * query execution.
     */
    public boolean hasExceptions() {
        return globalExceptions.size() > 0 || queryExceptions.size() > 0;
    }

    public void nextCount(Query query, int resultCount) {
    }

    public void nextBatchCount(Query query, int[] resultCount) {

    }

    public void nextRows(Query query, List<?> dataRows) {
        // noop
    }

    /**
     * Closes ResultIterator without reading its data. If you implement a custom
     * subclass, only call super if closing the iterator is what you need.
     */
    public void nextRows(Query query, ResultIterator it) {
        if (it != null) {
            it.close();
        }
    }

    /**
     * Closes ResultIterator without reading its data. If you implement a custom
     * subclass, only call super if closing the iterator is what you need.
     * 
     * @since 3.0
     */
    public void nextGeneratedRows(Query query, ResultIterator keysIterator) {
        if (keysIterator != null) {
            keysIterator.close();
        }
    }

    public void nextQueryException(Query query, Exception ex) {
        queryExceptions.put(query, Util.unwindException(ex));
    }

    public void nextGlobalException(Exception ex) {
        globalExceptions.add(Util.unwindException(ex));
    }

    /**
     * Returns <code>false</code>.
     */
    public boolean isIteratedResult() {
        return false;
    }
}
