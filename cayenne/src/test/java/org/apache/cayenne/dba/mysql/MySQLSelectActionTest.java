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
package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.query.resolved.ResolvedQueries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MySQLSelectActionTest {

    private Connection connection;
    private PreparedStatement statement;
    private DataNode dataNode;

    @BeforeEach
    public void setUp() throws Exception {
        RowReaderFactory rowReaderFactory = mock(RowReaderFactory.class);
        when(rowReaderFactory.rowReader(any(), any(), any(), any())).thenReturn(mock(RowReader.class));

        dataNode = mock(DataNode.class);
        when(dataNode.getAdapter()).thenReturn(mock(DbAdapter.class));
        when(dataNode.getRowReaderFactory()).thenReturn(rowReaderFactory);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false);

        statement = mock(PreparedStatement.class);
        when(statement.executeQuery()).thenReturn(resultSet);

        connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

    }

    private MySQLSelectAction action(int statementFetchSize) {
        TranslatedSelect select = new TranslatedSelect(
                ResolvedQueries.select(statementFetchSize),
                "SELECT 1",
                new PSParameter[0],
                new RSColumn[0],
                null,
                false,
                false);
        return new MySQLSelectAction(select, dataNode);
    }

    private StatementContext context(boolean iterated, boolean exclusiveConnection) {
        StatementContext context = mock(StatementContext.class);
        when(context.getMapping()).thenReturn(mock(CompiledMapping.class));
        when(context.isIteratedResult()).thenReturn(iterated);
        when(context.isExclusiveConnection()).thenReturn(exclusiveConnection);
        return context;
    }

    @Test
    public void noFetchSizeOnListResultLeavesDriverDefault() throws Exception {

        action(0).execute(connection, context(false, false));

        verify(statement, never()).setFetchSize(anyInt());
    }

    @Test
    public void noFetchSizeOnIteratorWithExclusiveConnectionStreams() throws Exception {

        action(0).execute(connection, context(true, true));

        InOrder inOrder = inOrder(statement);
        inOrder.verify(statement).setFetchSize(Integer.MIN_VALUE);
        inOrder.verify(statement).executeQuery();
    }

    @Test
    public void noFetchSizeOnIteratorInCallerTransactionLeavesDriverDefault() throws Exception {

        action(0).execute(connection, context(true, false));

        verify(statement, never()).setFetchSize(anyInt());
    }

    @Test
    public void streamingFetchSizeOnListResultIsPassedThrough() throws Exception {

        action(Integer.MIN_VALUE).execute(connection, context(false, false));

        verify(statement).setFetchSize(Integer.MIN_VALUE);
    }

    @Test
    public void streamingFetchSizeOnIteratorInCallerTransactionIsPassedThrough() throws Exception {

        action(Integer.MIN_VALUE).execute(connection, context(true, false));

        verify(statement).setFetchSize(Integer.MIN_VALUE);
    }

    @Test
    public void positiveFetchSizeOnIteratorWithExclusiveConnectionIsPassedThrough() throws Exception {

        action(100).execute(connection, context(true, true));

        verify(statement).setFetchSize(100);
        verify(statement, never()).setFetchSize(Integer.MIN_VALUE);
    }
}
