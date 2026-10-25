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


package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.DataNodes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OperationContextTest {

    private static OperationContext newContext(boolean iteratedResult) {
        Query query = mock(Query.class);
        when(query.getMetaData(any())).thenReturn(mock(QueryMetadata.class));
        return new OperationContext(null, query, mock(EntityResolver.class), iteratedResult);
    }

    private static StatementContext newStatementContext(String sql) {
        return new StatementContext(new SQLTemplate<>(sql), mock(QueryMetadata.class), mock(EntityResolver.class));
    }

    @Test
    public void queryContextResolvesMetadataOnce() {
        QueryMetadata metadata = mock(QueryMetadata.class);
        Query query = mock(Query.class);
        when(query.getMetaData(any(EntityResolver.class))).thenReturn(metadata);
        ObjectContext objectContext = mock(ObjectContext.class);

        OperationContext context = new OperationContext(objectContext, query, mock(EntityResolver.class), true);

        assertSame(objectContext, context.getContext());
        assertSame(query, context.getQuery());
        assertSame(metadata, context.getMetadata());
        assertTrue(context.isIteratedResult());
        assertFalse(context.isExclusiveConnection());
        assertTrue(context.getStatementContexts().isEmpty());
    }

    @Test
    public void addStatementContext() {
        DataNode node = DataNodes.named("n1");
        OperationContext context = newContext(false);
        StatementContext statementContext = newStatementContext("SELECT 1");

        context.addStatementContext(node, statementContext);

        assertFalse(statementContext.isIteratedResult());
        assertFalse(statementContext.isExclusiveConnection());
        assertEquals(Map.of(node, List.of(statementContext)), context.getStatementContexts());
    }

    @Test
    public void addedStatementsGetOperationFlags() {
        DataNode node = DataNodes.named("n1");
        OperationContext context = newContext(true);

        StatementContext before = newStatementContext("1");
        context.addStatementContext(node, before);
        assertTrue(before.isIteratedResult());
        assertFalse(before.isExclusiveConnection());

        // the flag reaches both the statements added before and after it is set
        context.setExclusiveConnection(true);
        StatementContext after = newStatementContext("2");
        context.addStatementContext(node, after);
        assertTrue(before.isExclusiveConnection());
        assertTrue(after.isExclusiveConnection());
        assertTrue(after.isIteratedResult());
    }

    @Test
    public void statementsGroupedByNodeInOrder() {
        DataNode n1 = DataNodes.named("n1");
        DataNode n2 = DataNodes.named("n2");
        OperationContext context = newContext(false);

        StatementContext s1 = newStatementContext("1");
        StatementContext s2 = newStatementContext("2");
        StatementContext s3 = newStatementContext("3");
        context.addStatementContext(n2, s1);
        context.addStatementContext(n1, s2);
        context.addStatementContext(n2, s3);

        Map<DataNode, List<StatementContext>> byNode = context.getStatementContexts();
        assertEquals(List.of(n2, n1), List.copyOf(byNode.keySet()));
        assertEquals(List.of(s1, s3), byNode.get(n2));
        assertEquals(List.of(s2), byNode.get(n1));
    }

    @Test
    public void clearStatementContexts() {
        OperationContext context = newContext(false);
        context.addStatementContext(DataNodes.named("n1"), newStatementContext("1"));

        context.clearStatementContexts();

        assertTrue(context.getStatementContexts().isEmpty());
    }
}
