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

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.util.DefaultOperationObserver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.Util;

/**
 * QueryResult encapsulates a result of execution of zero or more queries using
 * QueryEngine. QueryResult supports queries with multiple mixed selects and updates, such
 * as ProcedureQueries.
 */
public class QueryResult extends DefaultOperationObserver {

    // a map with order of iteration == to the order of insertion
    protected Map queries = new LinkedHashMap();

    /**
     * Clears any previously collected information.
     */
    public void clear() {
        queries.clear();
    }

    /**
     * Returns an iterator over all executed queries in the order they were executed.
     */
    public Iterator getQueries() {
        return queries.keySet().iterator();
    }

    /**
     * Returns a list of all results of a given query. This is potentially a mix of
     * java.lang.Integer values for update operations and java.util.List for select
     * operations. Results are returned in the order they were obtained.
     */
    public List getResults(Query query) {
        List list = (List) queries.get(query);
        return (list != null) ? list : Collections.EMPTY_LIST;
    }

    /**
     * Returns the first update count for the query. This is a shortcut for <code>(Integer)getUpdates(query).get(0)<code>, kind of like Google's "I'm feeling lucky".
     * Returns -1 if no update count is found for the query.
     */
    public int getFirstUpdateCount(Query query) {
        List allResults = getResults(query);
        int size = allResults.size();
        if (size > 0) {
            Iterator it = allResults.iterator();
            while (it.hasNext()) {
                Object object = it.next();

                // if int
                if (object instanceof Number) {
                    return ((Number) object).intValue();
                }
                // if batch...
                else if (object instanceof int[]) {
                    int[] counts = (int[]) object;
                    return counts.length > 0 ? counts[0] : -1;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the first update count. Returns int[0] if there was no update results for
     * the query.
     * 
     * @since 1.2
     */
    public int[] getFirstUpdateCounts(Query query) {
        List allResults = getResults(query);
        int size = allResults.size();

        if (size > 0) {
            Iterator it = allResults.iterator();
            while (it.hasNext()) {
                Object object = it.next();

                // if int
                if (object instanceof Number) {
                    return new int[] {
                        ((Number) object).intValue()
                    };
                }
                // if batch...
                else if (object instanceof int[]) {
                    return (int[]) object;
                }
            }
        }

        return new int[0];
    }

    /**
     * Returns the first results for the query. This is a shortcut for <code>(List)getRows(query).get(0)<code>, kind of like Google's "I'm feeling lucky".
     */
    public List getFirstRows(Query query) {
        List allResults = getResults(query);
        int size = allResults.size();
        if (size == 0) {
            return Collections.EMPTY_LIST;
        }
        else {
            for (Object obj : allResults) {
                if (obj instanceof List) {
                    return (List) obj;
                }
            }
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Returns a List that itself contains Lists of data rows for each ResultSet returned
     * by the query. ResultSets are returned in the oder they were obtained. Any updates
     * that were performed are not included.
     */
    public List<?> getRows(Query query) {
        List allResults = getResults(query);
        int size = allResults.size();
        if (size == 0) {
            return Collections.EMPTY_LIST;
        }

        List<Object> list = new ArrayList<Object>(size);
        for (Object obj : allResults) {
            if (obj instanceof List) {
                list.add(obj);
            }
        }

        return list;
    }

    /**
     * Returns a List that contains java.lang.Integer objects for each one of the update
     * counts returned by the query. Update counts are returned in the order they were
     * obtained. Batched and regular updates are combined together.
     */
    public List getUpdates(Query query) {
        List allResults = getResults(query);
        int size = allResults.size();
        if (size == 0) {
            return Collections.EMPTY_LIST;
        }

        List list = new ArrayList(size);
        Iterator it = allResults.iterator();
        while (it.hasNext()) {
            Object object = it.next();
            if (object instanceof Number) {
                list.add(object);
            }
            else if (object instanceof int[]) {
                int[] ints = (int[]) object;
                for (int anInt : ints) {
                    list.add(Integer.valueOf(anInt));
                }
            }
        }

        return list;
    }

    /**
     * Overrides superclass implementation to rethrow an exception immediately.
     */
    @Override
    public void nextQueryException(Query query, Exception ex) {
        super.nextQueryException(query, ex);
        throw new CayenneRuntimeException("Query exception.", Util.unwindException(ex));
    }

    /**
     * Overrides superclass implementation to rethrow an exception immediately.
     */
    @Override
    public void nextGlobalException(Exception ex) {
        super.nextGlobalException(ex);
        throw new CayenneRuntimeException("Global exception.", Util.unwindException(ex));
    }

    /**
     * Always returns <code>false</code>, iterated results are not supported.
     */
    @Override
    public boolean isIteratedResult() {
        return false;
    }

    @Override
    public void nextBatchCount(Query query, int[] resultCount) {
        List list = (List) queries.get(query);
        if (list == null) {
            list = new ArrayList(5);
            queries.put(query, list);
        }

        list.add(resultCount);
    }

    @Override
    public void nextCount(Query query, int resultCount) {
        super.nextCount(query, resultCount);

        List list = (List) queries.get(query);
        if (list == null) {
            list = new ArrayList(5);
            queries.put(query, list);
        }

        list.add(Integer.valueOf(resultCount));
    }

    @Override
    public void nextRows(Query query, List<?> dataRows) {
        super.nextRows(query, dataRows);

        List list = (List) queries.get(query);
        if (list == null) {
            list = new ArrayList(5);
            queries.put(query, list);
        }

        list.add(dataRows);
    }

    @Override
    public void nextRows(Query q, ResultIterator it) {
        throw new CayenneRuntimeException("Iterated results are not supported by "
                + this.getClass().getName());
    }

}
