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

package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLAction;

import java.sql.ResultSet;

/**
 * A convenience superclass for SQLAction implementations.
 * 
 * @since 1.2
 */
public abstract class BaseSQLAction implements SQLAction {

    protected DataNode dataNode;

    /**
     * @since 4.0
     */
    public BaseSQLAction(DataNode dataNode) {
        this.dataNode = dataNode;
    }

    /**
     * Helper method to process a ResultSet, reporting it to the statement context as a list of rows, or as a
     * ResultIterator if the context asks for an iterated result.
     */
    protected void readResultSet(ResultSet resultSet, RSColumn[] columns, StatementContext context) throws Exception {

        ResultIterator<?> it = resultIterator(resultSet, columns, context);

        if (!context.isIteratedResult()) {
            context.addResult(new SelectResult<>(it.allRows()));
        } else {
            try {
                context.addResult(it);
            } catch (Exception ex) {
                it.close();
                throw ex;
            }
        }
    }

    /**
     * Returns an iterator over the rows of a ResultSet, reading them per the statement's result mapping and applying
     * its in-memory offset and limit. The ResultSet is left open to be closed by the caller.
     *
     * @since 6.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected ResultIterator<?> resultIterator(ResultSet resultSet, RSColumn[] columns, StatementContext context) {
        QueryMetadata metadata = context.getMetadata();
        RowReader<?> rowReader = dataNode.getRowReaderFactory()
                .rowReader(columns, metadata.getResultSegments(), metadata, dataNode.getAdapter());

        RSIterator resultReader = new RSIterator(null, resultSet, rowReader);
        return new LimitResultIterator(resultReader, getInMemoryOffset(metadata.getFetchOffset()),
                metadata.getFetchLimit());
    }

    /**
     * Returns a value of the offset that will be used to rewind the ResultSet
     * within the SQL action before reading the result rows. The default
     * implementation returns 'queryOffset' argument. If the adapter supports
     * setting offset at the SQL level, this method must be overridden to return
     * zero to suppress manual offset.
     * 
     * @since 3.0
     */
    protected int getInMemoryOffset(int queryOffset) {
        return queryOffset;
    }
}
