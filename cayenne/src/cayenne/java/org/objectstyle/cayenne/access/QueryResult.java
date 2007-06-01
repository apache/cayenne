/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.map.LinkedMap;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/**
 * QueryResult encapsulates a result of execution of zero or more queries using QueryEngine.
 * QueryResult supporting queries with multiple ResultSets and updates, such as StoredProcedures.
 * 
 * @author Andrei Adamchik
 */
public class QueryResult
    extends org.objectstyle.cayenne.access.util.DefaultOperationObserver {

    // SequencedHashMap guarantees that iterating over 
    // its keys is done in the original insertion order-
    // something that is needed to return executed queries in 
    // the right order. (Java 1.4 adds LinkedHashMap that has
    // the same functionality, but we have to stay 1.3 comatible)
    protected LinkedMap queries = new LinkedMap();

    /** Clears any previously collected information. */
    public void clear() {
        queries.clear();
    }

    /**
     * Returns an iterator over all executed queries in the order they were executed.
     */
    public Iterator getQueries() {
        return queries.asList().iterator();
    }

    /**
     * Returns a list of all results of a given query. This is potentially a mix
     * of java.lang.Integer values for update operations and java.util.List for select
     * operations. Results are returned in the order they were obtained.
     */
    public List getResults(Query query) {
        List list = (List) queries.get(query);
        return (list != null) ? list : Collections.EMPTY_LIST;
    }

    /**
    * Returns the first update count for the query. This is a shortcut for
    * <code>(Integer)getUpdates(query).get(0)<code>, kind of like Google's "I'm feeling lucky".
    * Returns -1 if no update count is found for the query.
    */
    public int getFirstUpdateCount(Query query) {
        List allResults = getResults(query);
        int size = allResults.size();
        if (size > 0) {
            Iterator it = allResults.iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof Number) {
                    return ((Number) obj).intValue();
                }
            }
        }
        return -1;
    }

    /**
     * Returns the first results for the query. This is a shortcut for
     * <code>(List)getRows(query).get(0)<code>, kind of like Google's "I'm feeling lucky".
     */
    public List getFirstRows(Query query) {
        List allResults = getResults(query);
        int size = allResults.size();
        if (size == 0) {
            return Collections.EMPTY_LIST;
        } else {
            Iterator it = allResults.iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof List) {
                    return (List) obj;
                }
            }
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Returns a List that itself contains Lists of data rows for each ResultSet 
     * returned by the query. ResultSets are returned in the oder they were obtained.
     * Any updates that were performed are not included.
     */
    public List getRows(Query query) {
        List allResults = getResults(query);
        int size = allResults.size();
        if (size == 0) {
            return Collections.EMPTY_LIST;
        }

        List list = new ArrayList(size);
        Iterator it = allResults.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof List) {
                list.add(obj);
            }
        }

        return list;
    }

    /**
     * Returns a List that contains java.lang.Integer objects for each one of the update
     * counts returned by the query. Update counts are returned in the order they were obtained.
     * Data rows are not included.
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
            Object obj = it.next();
            if (obj instanceof Number) {
                list.add(obj);
            }
        }

        return list;
    }

    /** 
     * Overrides superclass implementation to rethrow an exception
     * immediately. 
     */
    public void nextQueryException(Query query, Exception ex) {
        super.nextQueryException(query, ex);
        throw new CayenneRuntimeException(
            "Query exception.",
            Util.unwindException(ex));
    }

    /** 
     * Overrides superclass implementation to rethrow an exception
     * immediately. 
     */
    public void nextGlobalException(Exception ex) {
        super.nextGlobalException(ex);
        throw new CayenneRuntimeException(
            "Global exception.",
            Util.unwindException(ex));
    }

    /**
     * Always returns <code>false</code>, iterated results are not supported.
     */
    public boolean isIteratedResult() {
        return false;
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        for (int i = 0; i < resultCount.length; i++) {
            nextCount(query, resultCount[i]);
        }
    }

    public void nextCount(Query query, int resultCount) {
        super.nextCount(query, resultCount);

        List list = (List) queries.get(query);
        if (list == null) {
            list = new ArrayList(5);
            queries.put(query, list);
        }

        list.add(new Integer(resultCount));
    }

    public void nextDataRows(Query query, List dataRows) {
        super.nextDataRows(query, dataRows);

        List list = (List) queries.get(query);
        if (list == null) {
            list = new ArrayList(5);
            queries.put(query, list);
        }

        list.add(dataRows);
    }

    public void nextDataRows(Query q, ResultIterator it) {
        throw new CayenneRuntimeException(
            "Iterated results are not supported by "
                + this.getClass().getName());
    }
}
