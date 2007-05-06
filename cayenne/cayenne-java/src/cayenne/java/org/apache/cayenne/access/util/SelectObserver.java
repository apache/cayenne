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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.Util;

/**
 * OperationObserver that accumulates select query results provided by callback methods.
 * Later the results can be retrieved via different <code>getResults</code> methods.
 * Also supports instantiating DataObjects within a provided DataContext.
 * <p>
 * This class is used as a default OperationObserver by DataContext. Also it can serve as
 * a helper for classes that work with DataNode directly, bypassing DataContext.
 * </p>
 * <p>
 * If exceptions happen during the execution, they are immediately rethrown.
 * </p>
 * <p>
 * <i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide. </a> </i>
 * </p>
 * 
 * @deprecated since 1.2 SelectObserver is no longer used in Cayenne internally. Users
 *             should switch to QueryResult.
 * @author Andrei Adamchik
 * @see org.apache.cayenne.access.QueryResult
 */
public class SelectObserver extends DefaultOperationObserver {

    protected Map results = new HashMap();
    protected int selectCount;

    public SelectObserver() {

    }

    /**
     * @deprecated since 1.2
     */
    public SelectObserver(Level logLevel) {
        super.setLoggingLevel(logLevel);
    }

    /**
     * Returns a count of select queries that returned results since the last time "clear"
     * was called, or since this object was created.
     */
    public int getSelectCount() {
        return selectCount;
    }

    /**
     * Returns a list of result snapshots for the specified query, or null if this query
     * has never produced any results.
     */
    public List getResults(Query q) {
        return (List) results.get(q);
    }

    /**
     * Returns query results accumulated during query execution with this object as an
     * operation observer.
     */
    public Map getResults() {
        return results;
    }

    /** Clears fetched objects stored in an internal list. */
    public void clear() {
        selectCount = 0;
        results.clear();
    }

    /**
     * Stores all objects in <code>dataRows</code> in an internal result list.
     */
    public void nextDataRows(Query query, List dataRows) {

        if (dataRows != null) {
            results.put(query, dataRows);
        }

        selectCount++;
    }

    /**
     * @since 1.1
     * @deprecated since 1.2. Note that this implementation no longer resolves prefetches
     *             properly.
     */
    public List getResultsAsObjects(DataContext dataContext, Query rootQuery) {

        QueryMetadata info = rootQuery.getMetaData(dataContext.getEntityResolver());
        return dataContext.objectsFromDataRows(
                info.getObjEntity(),
                getResults(rootQuery),
                info.isRefreshingObjects(),
                info.isResolvingInherited());
    }

    /**
     * Overrides super implementation to rethrow an exception immediately.
     */
    public void nextQueryException(Query query, Exception ex) {
        super.nextQueryException(query, ex);
        throw new CayenneRuntimeException("Query exception.", Util.unwindException(ex));
    }

    /**
     * Overrides superclass implementation to rethrow an exception immediately.
     */
    public void nextGlobalException(Exception ex) {
        super.nextGlobalException(ex);
        throw new CayenneRuntimeException("Global exception.", Util.unwindException(ex));
    }
}
