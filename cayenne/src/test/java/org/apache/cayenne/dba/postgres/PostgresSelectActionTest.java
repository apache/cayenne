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
package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks the local transaction that PostgresSelectAction opens around a select. pgjdbc uses a server-side cursor
 * for "statementFetchSize" only inside a transaction, so an autocommit connection must be switched for the duration
 * of the query (CAY-3011), the same way it already is for LOB reads.
 */
public class PostgresSelectActionTest {

    private static final TranslatedSelect NO_LOBS = new TranslatedSelect(
            "SELECT 1",
            new PSParameter[0],
            new RSColumn[0],
            false,
            false);

    private QueryMetadata metadata;
    private Connection connection;
    private PreparedStatement statement;
    private PostgresSelectAction action;

    @BeforeEach
    public void setUp() throws Exception {
        metadata = mock(QueryMetadata.class);
        when(metadata.getQueryTimeout()).thenReturn(QueryMetadata.QUERY_TIMEOUT_DEFAULT);

        Select<?> query = mock(Select.class);
        when(query.getMetaData(any(EntityResolver.class))).thenReturn(metadata);

        RowReaderFactory rowReaderFactory = mock(RowReaderFactory.class);
        when(rowReaderFactory.rowReader(any(), any(), any())).thenReturn(mock(RowReader.class));

        DataNode dataNode = mock(DataNode.class);
        when(dataNode.getEntityResolver()).thenReturn(mock(EntityResolver.class));
        when(dataNode.getAdapter()).thenReturn(mock(DbAdapter.class));
        when(dataNode.getRowReaderFactory()).thenReturn(rowReaderFactory);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false);

        statement = mock(PreparedStatement.class);
        when(statement.executeQuery()).thenReturn(resultSet);

        connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        action = new PostgresSelectAction(query, dataNode);
    }

    @Test
    public void fetchSizeOnAutoCommitConnectionRunsInTransaction() throws Exception {
        when(connection.getAutoCommit()).thenReturn(true);
        when(metadata.getStatementFetchSize()).thenReturn(10);

        action.performAction(connection, mock(OperationObserver.class), NO_LOBS);

        InOrder inOrder = inOrder(connection, statement);
        inOrder.verify(connection).setAutoCommit(false);
        inOrder.verify(statement).setFetchSize(10);
        inOrder.verify(statement).executeQuery();
        inOrder.verify(connection).commit();
        inOrder.verify(connection).setAutoCommit(true);
        verify(connection, never()).rollback();
    }

    @Test
    public void noFetchSizeOnAutoCommitConnectionLeavesAutoCommitAlone() throws Exception {
        when(connection.getAutoCommit()).thenReturn(true);
        when(metadata.getStatementFetchSize()).thenReturn(0);

        action.performAction(connection, mock(OperationObserver.class), NO_LOBS);

        verify(statement, never()).setFetchSize(anyInt());
        verify(connection, never()).setAutoCommit(anyBoolean());
        verify(connection, never()).commit();
    }

    @Test
    public void fetchSizeInsideExistingTransactionIsNotCommittedLocally() throws Exception {
        when(connection.getAutoCommit()).thenReturn(false);
        when(metadata.getStatementFetchSize()).thenReturn(10);

        action.performAction(connection, mock(OperationObserver.class), NO_LOBS);

        // the transaction belongs to the caller: the fetch size is passed through, but nothing is committed here
        verify(statement).setFetchSize(10);
        verify(connection, never()).setAutoCommit(anyBoolean());
        verify(connection, never()).commit();
    }

    @Test
    public void failureInTransactionRollsBackAndRestoresAutoCommit() throws Exception {
        when(connection.getAutoCommit()).thenReturn(true);
        when(metadata.getStatementFetchSize()).thenReturn(10);
        when(statement.executeQuery()).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class,
                () -> action.performAction(connection, mock(OperationObserver.class), NO_LOBS));

        InOrder inOrder = inOrder(connection);
        inOrder.verify(connection).setAutoCommit(false);
        inOrder.verify(connection).rollback();
        inOrder.verify(connection).setAutoCommit(true);
        verify(connection, never()).commit();
    }
}
