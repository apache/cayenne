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
package org.apache.cayenne.unit.telemetry;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DataNodeInitializer;
import org.apache.cayenne.access.DefaultDataNodeFactory;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.BatchTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.SQLTemplateTranslator;
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.junit.jupiter.api.Assertions;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TelemetricDataNodeFactory extends DefaultDataNodeFactory {

    @Override
    protected DataNode newDataNode(
            String name,
            DataSource dataSource,
            DataNodeInitializer initializer,
            PkGenerator pkGenerator) {
        return new TelemetricDataNode(name, dataSource, initializer, pkGenerator, sqlLogger,
                rowReaderFactory, selectTranslator, procedureTranslator, sqlTemplateTranslator,
                insertBatchTranslator, updateBatchTranslator, deleteBatchTranslator);
    }

    static class TelemetricDataNode extends DataNode {

        private final AtomicInteger queryCounter = new AtomicInteger();
        private volatile boolean blockingQueries;

        public TelemetricDataNode(
                String name,
                DataSource dataSource,
                DataNodeInitializer initializer,
                PkGenerator pkGenerator,
                SQLLogger sqlLogger,
                RowReaderFactory rowReaderFactory,
                SelectTranslator selectTranslator,
                ProcedureTranslator procedureTranslator,
                SQLTemplateTranslator sqlTemplateTranslator,
                BatchTranslator<InsertBatchQuery> insertBatchTranslator,
                BatchTranslator<UpdateBatchQuery> updateBatchTranslator,
                BatchTranslator<DeleteBatchQuery> deleteBatchTranslator) {
            super(name, dataSource, initializer, pkGenerator, sqlLogger, rowReaderFactory, selectTranslator,
                    procedureTranslator, sqlTemplateTranslator, insertBatchTranslator, updateBatchTranslator,
                    deleteBatchTranslator);
        }

        @Override
        public void execute(List<StatementContext> statementContexts) {
            if (blockingQueries) {
                Assertions.fail("Query is unexpected: "
                        + statementContexts.stream().map(StatementContext::getQuery).toList());
            }
            super.execute(statementContexts);
            queryCounter.addAndGet(statementContexts.size());
        }

        public int getQueriesCount() {
            return queryCounter.get();
        }

        public void setBlockingQueries(boolean blockingQueries) {
            this.blockingQueries = blockingQueries;
        }
    }

}
