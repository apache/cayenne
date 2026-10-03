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
package org.apache.cayenne.map;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.XMLEncoder;

import java.util.Map;

/**
 * @since 4.0
 */
public class ProcedureQueryDescriptor extends QueryDescriptor {

    protected String resultEntityName;
    protected int fetchLimit = QueryMetadata.FETCH_LIMIT_DEFAULT;
    protected int fetchOffset = QueryMetadata.FETCH_OFFSET_DEFAULT;
    protected CapsStrategy columnNamesCapitalization;

    /**
     * Returns result entity name.
     */
    public String getResultEntityName() {
        return resultEntityName;
    }

    /**
     * Sets result entity name.
     */
    public void setResultEntityName(String resultEntityName) {
        this.resultEntityName = resultEntityName;
    }

    /**
     * Returns the fetch limit of the query. Zero means no limit.
     *
     * @since 5.0
     */
    public int getFetchLimit() {
        return fetchLimit;
    }

    /**
     * @since 5.0
     */
    public void setFetchLimit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
    }

    /**
     * Returns the fetch offset of the query.
     *
     * @since 5.0
     */
    public int getFetchOffset() {
        return fetchOffset;
    }

    /**
     * @since 5.0
     */
    public void setFetchOffset(int fetchOffset) {
        this.fetchOffset = fetchOffset;
    }

    /**
     * Returns the capitalization strategy applied to the column names of the query result, or null if none is set.
     *
     * @since 5.0
     */
    public CapsStrategy getColumnNamesCapitalization() {
        return columnNamesCapitalization;
    }

    /**
     * @since 5.0
     */
    public void setColumnNamesCapitalization(CapsStrategy columnNamesCapitalization) {
        this.columnNamesCapitalization = columnNamesCapitalization;
    }

    @Override
    public ProcedureQuery<?> buildQuery() {
        ProcedureQuery<?> procedureQuery = new ProcedureQuery<>();

        if (root != null) {
            procedureQuery.setRoot(root);
        }

        procedureQuery.setResultEntityName(this.getResultEntityName());
        procedureQuery.setFetchLimit(fetchLimit);
        procedureQuery.setFetchOffset(fetchOffset);
        procedureQuery.setPageSize(pageSize);
        procedureQuery.setStatementFetchSize(statementFetchSize);
        procedureQuery.setFetchingDataRows(fetchingDataRows);
        procedureQuery.setCacheStrategy(cacheStrategy);
        procedureQuery.setCacheGroup(cacheGroup);
        procedureQuery.setColumnNamesCapitalization(columnNamesCapitalization);

        return procedureQuery;
    }

    /**
     * @since 5.0
     */
    @Override
    public ProcedureQuery<?> buildQuery(Map<String, ?> parameters) {
        ProcedureQuery<?> procedureQuery = buildQuery();
        procedureQuery.setParameters(parameters);
        return procedureQuery;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("procedure-query")
                .attribute("name", getName())
                .attribute("root", QueryDescriptor.PROCEDURE_ROOT);

        String rootString = null;
        if (root instanceof String) {
            rootString = root.toString();
        } else if (root instanceof Procedure) {
            rootString = ((Procedure) root).getName();
        }

        encoder.attribute("root-name", rootString)
                .attribute("result-entity", resultEntityName)
                .attribute("cache-strategy", cacheStrategy != QueryCacheStrategy.NO_CACHE ? cacheStrategy.name() : null)
                .attribute("data-rows", fetchingDataRows)
                .attribute("fetch-limit", fetchLimit)
                .attribute("fetch-offset", fetchOffset)
                .attribute("page-size", pageSize)
                .attribute("statement-fetch-size", statementFetchSize)
                .attribute("column-name-capitalization",
                        columnNamesCapitalization != null && columnNamesCapitalization != CapsStrategy.DEFAULT
                                ? columnNamesCapitalization.name()
                                : null);

        encodeCacheGroup(encoder);

        delegate.visitQuery(this);
        encoder.end();
    }
}
