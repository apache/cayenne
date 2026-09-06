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
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.SelectTranslator;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MySQLSelectActionTest {

    private static final TranslatedSelect SELECT = new TranslatedSelect(
            "SELECT 1",
            new PSParameter[0],
            new RSColumn[0],
            false,
            false);

    private QueryMetadata metadata;
    private Connection connection;
    private PreparedStatement statement;
    private MySQLSelectAction action;

    @BeforeEach
    public void setUp() throws Exception {
        metadata = mock(QueryMetadata.class);
        when(metadata.getQueryTimeout()).thenReturn(QueryMetadata.QUERY_TIMEOUT_DEFAULT);

        Select<?> query = mock(Select.class);
        when(query.getMetaData(any(EntityResolver.class))).thenReturn(metadata);

        RowReaderFactory rowReaderFactory = mock(RowReaderFactory.class);
        when(rowReaderFactory.rowReader(any(), any(), any())).thenReturn(mock(RowReader.class));

        SelectTranslator translator = mock(SelectTranslator.class);
        when(translator.translate(any(), any(), any())).thenReturn(SELECT);

        DataNode dataNode = mock(DataNode.class);
        when(dataNode.getEntityResolver()).thenReturn(mock(EntityResolver.class));
        when(dataNode.getAdapter()).thenReturn(mock(DbAdapter.class));
        when(dataNode.getRowReaderFactory()).thenReturn(rowReaderFactory);
        when(dataNode.getSelectTranslator()).thenReturn(translator);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false);

        statement = mock(PreparedStatement.class);
        when(statement.executeQuery()).thenReturn(resultSet);

        connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        action = new MySQLSelectAction(query, dataNode);
    }

    private static OperationObserver observer(boolean iterated, boolean exclusiveConnection) {
        OperationObserver observer = mock(OperationObserver.class);
        when(observer.isIteratedResult()).thenReturn(iterated);
        when(observer.isIteratorExclusiveConnection()).thenReturn(exclusiveConnection);
        return observer;
    }

    @Test
    public void noFetchSizeOnListResultLeavesDriverDefault() throws Exception {
        when(metadata.getStatementFetchSize()).thenReturn(0);

        action.performAction(connection, observer(false, false));

        verify(statement, never()).setFetchSize(anyInt());
    }

    @Test
    public void noFetchSizeOnIteratorWithExclusiveConnectionStreams() throws Exception {
        when(metadata.getStatementFetchSize()).thenReturn(0);

        action.performAction(connection, observer(true, true));

        InOrder inOrder = inOrder(statement);
        inOrder.verify(statement).setFetchSize(Integer.MIN_VALUE);
        inOrder.verify(statement).executeQuery();
    }

    @Test
    public void noFetchSizeOnIteratorInCallerTransactionLeavesDriverDefault() throws Exception {
        when(metadata.getStatementFetchSize()).thenReturn(0);

        action.performAction(connection, observer(true, false));

        verify(statement, never()).setFetchSize(anyInt());
    }

    @Test
    public void streamingFetchSizeOnListResultIsPassedThrough() throws Exception {
        when(metadata.getStatementFetchSize()).thenReturn(Integer.MIN_VALUE);

        action.performAction(connection, observer(false, false));

        verify(statement).setFetchSize(Integer.MIN_VALUE);
    }

    @Test
    public void streamingFetchSizeOnIteratorInCallerTransactionIsPassedThrough() throws Exception {
        when(metadata.getStatementFetchSize()).thenReturn(Integer.MIN_VALUE);

        action.performAction(connection, observer(true, false));

        verify(statement).setFetchSize(Integer.MIN_VALUE);
    }

    @Test
    public void positiveFetchSizeOnIteratorWithExclusiveConnectionIsPassedThrough() throws Exception {
        when(metadata.getStatementFetchSize()).thenReturn(100);

        action.performAction(connection, observer(true, true));

        verify(statement).setFetchSize(100);
        verify(statement, never()).setFetchSize(Integer.MIN_VALUE);
    }
}
