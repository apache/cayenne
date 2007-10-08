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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.EntitySorter;
import org.objectstyle.cayenne.query.Query;

public class MockDataNode extends DataNode {

    protected DataDomain domain;
    protected DataNode node;

    // mockup the actual results
    protected boolean replaceResults;
    protected Map results = new HashMap();
    protected int runCount;

    public static MockDataNode interceptNode(DataDomain domain, String nodeName) {
        DataNode node = domain.getNode(nodeName);
        if (node == null) {
            throw new IllegalArgumentException("No node for name: " + nodeName);
        }
        return interceptNode(domain, node);
    }

    public static MockDataNode interceptNode(DataDomain domain, DataNode node) {
        MockDataNode mockNode = new MockDataNode(node);
        mockNode.domain = domain;
        domain.removeDataNode(node.getName());
        domain.addNode(mockNode);
        return mockNode;
    }

    public MockDataNode(DataNode node) {
        this.node = node;
    }

    public void stopInterceptNode() {
        if (domain == null) {
            throw new IllegalStateException("No domain set.");
        }

        domain.removeDataNode(getName());
        domain.addNode(node);
    }

    public void reset() {
        runCount = 0;
        results.clear();
    }

    public int getRunCount() {
        return runCount;
    }

    public void addExpectedResult(Query query, List result) {
        replaceResults = true;
        results.put(query, result);
    }

    public void performQueries(Collection queries, OperationObserver resultConsumer) {
        runCount += queries.size();

        if (replaceResults) {
            initWithPresetResults(queries, resultConsumer);
        }
        else {
            node.performQueries(queries, resultConsumer);
        }
    }

    private void initWithPresetResults(
            Collection queries,
            OperationObserver resultConsumer) {

        // stick preset results to the consumer
        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Query query = (Query) it.next();
            resultConsumer.nextDataRows(query, (List) results.get(query));
        }
    }

    public void addDataMap(DataMap map) {
        node.addDataMap(map);
    }

    public boolean equals(Object obj) {
        return node.equals(obj);
    }

    public DbAdapter getAdapter() {
        return node.getAdapter();
    }

    public Collection getDataMaps() {
        return node.getDataMaps();
    }

    public DataSource getDataSource() {
        return node.getDataSource();
    }

    public String getDataSourceFactory() {
        return node.getDataSourceFactory();
    }

    public String getDataSourceLocation() {
        return node.getDataSourceLocation();
    }

    public EntityResolver getEntityResolver() {
        return node.getEntityResolver();
    }

    public EntitySorter getEntitySorter() {
        return node.getEntitySorter();
    }

    public String getName() {
        return node.getName();
    }

    public void removeDataMap(String mapName) {
        node.removeDataMap(mapName);
    }

    public void setAdapter(DbAdapter adapter) {
        node.setAdapter(adapter);
    }

    public void setDataMaps(Collection dataMaps) {
        node.setDataMaps(dataMaps);
    }

    public void setDataSource(DataSource dataSource) {
        node.setDataSource(dataSource);
    }

    public void setDataSourceFactory(String dataSourceFactory) {
        node.setDataSourceFactory(dataSourceFactory);
    }

    public void setDataSourceLocation(String dataSourceLocation) {
        node.setDataSourceLocation(dataSourceLocation);
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        node.setEntityResolver(entityResolver);
    }

    public void setEntitySorter(EntitySorter entitySorter) {
        node.setEntitySorter(entitySorter);
    }

    public void setName(String name) {
        node.setName(name);
    }

    public void shutdown() {
        node.shutdown();
    }

    public String toString() {
        return node.toString();
    }

}
