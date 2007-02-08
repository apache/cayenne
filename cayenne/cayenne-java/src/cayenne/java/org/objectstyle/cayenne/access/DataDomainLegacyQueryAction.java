/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.QueryRouter;

/**
 * DataDomain query action that relies on expernally provided OperationObserver to process
 * the results.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class DataDomainLegacyQueryAction implements QueryRouter, OperationObserver {

    static final boolean DONE = true;

    DataDomain domain;
    OperationObserver callback;
    Query query;
    QueryMetadata metadata;

    Map queriesByNode;
    Map queriesByExecutedQueries;

    DataDomainLegacyQueryAction(DataDomain domain, Query query, OperationObserver callback) {
        this.domain = domain;
        this.query = query;
        this.metadata = query.getMetaData(domain.getEntityResolver());
        this.callback = callback;
    }

    /*
     * Gets response from the underlying DataNodes.
     */
    final void execute() {

        // reset
        queriesByNode = null;
        queriesByExecutedQueries = null;

        // categorize queries by node and by "executable" query...
        query.route(this, domain.getEntityResolver(), null);

        // run categorized queries
        if (queriesByNode != null) {
            Iterator nodeIt = queriesByNode.entrySet().iterator();
            while (nodeIt.hasNext()) {
                Map.Entry entry = (Map.Entry) nodeIt.next();
                QueryEngine nextNode = (QueryEngine) entry.getKey();
                Collection nodeQueries = (Collection) entry.getValue();
                nextNode.performQueries(nodeQueries, this);
            }
        }
    }

    public void route(QueryEngine engine, Query query, Query substitutedQuery) {

        List queries = null;
        if (queriesByNode == null) {
            queriesByNode = new HashMap();
        }
        else {
            queries = (List) queriesByNode.get(engine);
        }

        if (queries == null) {
            queries = new ArrayList(5);
            queriesByNode.put(engine, queries);
        }

        queries.add(query);

        // handle case when routing resuled in an "exectable" query different from the
        // original query.
        if (substitutedQuery != null && substitutedQuery != query) {

            if (queriesByExecutedQueries == null) {
                queriesByExecutedQueries = new HashMap();
            }

            queriesByExecutedQueries.put(query, substitutedQuery);
        }
    }

    public QueryEngine engineForDataMap(DataMap map) {
        if (map == null) {
            throw new NullPointerException("Null DataMap, can't determine DataNode.");
        }

        QueryEngine node = domain.lookupDataNode(map);

        if (node == null) {
            throw new CayenneRuntimeException("No DataNode exists for DataMap " + map);
        }

        return node;
    }

    public void nextCount(Query query, int resultCount) {
        callback.nextCount(queryForExecutedQuery(query), resultCount);
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        callback.nextBatchCount(queryForExecutedQuery(query), resultCount);
    }

    public void nextDataRows(Query query, List dataRows) {
        callback.nextDataRows(queryForExecutedQuery(query), dataRows);
    }

    public void nextDataRows(Query q, ResultIterator it) {
        callback.nextDataRows(queryForExecutedQuery(q), it);
    }

    public void nextGeneratedDataRows(Query query, ResultIterator keysIterator) {
        callback.nextGeneratedDataRows(queryForExecutedQuery(query), keysIterator);
    }

    public void nextQueryException(Query query, Exception ex) {
        callback.nextQueryException(queryForExecutedQuery(query), ex);
    }

    public void nextGlobalException(Exception e) {
        callback.nextGlobalException(e);
    }

    /**
     * @deprecated since 1.2, as corresponding interface method is deprecated too.
     */
    public Level getLoggingLevel() {
        return callback.getLoggingLevel();
    }

    public boolean isIteratedResult() {
        return callback.isIteratedResult();
    }

    Query queryForExecutedQuery(Query executedQuery) {
        Query q = null;

        if (queriesByExecutedQueries != null) {
            q = (Query) queriesByExecutedQueries.get(executedQuery);
        }

        return q != null ? q : executedQuery;
    }
}
