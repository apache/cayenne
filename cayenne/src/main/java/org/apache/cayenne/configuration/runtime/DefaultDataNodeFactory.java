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
package org.apache.cayenne.configuration.runtime;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategyFactory;
import org.apache.cayenne.access.translator.sqltemplate.SQLTemplateTranslator;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslator;
import org.apache.cayenne.access.translator.select.SelectTranslator;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;

import javax.sql.DataSource;

/**
 * @since 4.0
 */
public class DefaultDataNodeFactory implements DataNodeFactory {

    @Inject
    protected JdbcEventLogger jdbcEventLogger;

    @Inject
    protected RowReaderFactory rowReaderFactory;

    @Inject
    protected DataSourceFactory dataSourceFactory;

    @Inject(BatchTranslator.INSERT)
    protected BatchTranslator<InsertBatchQuery> insertBatchTranslator;

    @Inject(BatchTranslator.UPDATE)
    protected BatchTranslator<UpdateBatchQuery> updateBatchTranslator;

    @Inject(BatchTranslator.DELETE)
    protected BatchTranslator<DeleteBatchQuery> deleteBatchTranslator;

    @Inject
    protected SelectTranslator selectTranslator;

    @Inject
    protected DbAdapterFactory adapterFactory;

    @Inject
    protected AdhocObjectFactory objectFactory;

    @Inject
    protected SchemaUpdateStrategyFactory schemaUpdateStrategyFactory;
    
    @Inject
    protected SQLTemplateTranslator sqlTemplateTranslator;

    @Override
    public DataNode createDataNode(DataNodeDescriptor nodeDescriptor) {

        DataNode dataNode = doCreateDataNode(nodeDescriptor.getName());

        dataNode.setJdbcEventLogger(jdbcEventLogger);
        dataNode.setRowReaderFactory(rowReaderFactory);
        dataNode.setInsertBatchTranslator(insertBatchTranslator);
        dataNode.setUpdateBatchTranslator(updateBatchTranslator);
        dataNode.setDeleteBatchTranslator(deleteBatchTranslator);
        dataNode.setSelectTranslator(selectTranslator);
        dataNode.setSqlTemplateTranslator(sqlTemplateTranslator);

        DataSource dataSource = dataSourceFactory.getDataSource(nodeDescriptor);

        dataNode.setDataSourceFactory(nodeDescriptor.getDataSourceFactoryType());
        dataNode.setDataSource(dataSource);

        dataNode.setSchemaUpdateStrategy(schemaUpdateStrategyFactory.create(nodeDescriptor));

        dataNode.setAdapter(adapterFactory.createAdapter(nodeDescriptor, dataSource));

        return dataNode;
    }

    // keeping a protected method for the sake of tests that would override it
    protected DataNode doCreateDataNode(String name) {
        return new DataNode(name);
    }
}
