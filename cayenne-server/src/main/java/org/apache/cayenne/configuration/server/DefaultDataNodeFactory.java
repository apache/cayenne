/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.configuration.server;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;

/**
 * @since 3.2
 */
public class DefaultDataNodeFactory implements DataNodeFactory {

    @Inject
    protected JdbcEventLogger jdbcEventLogger;

    @Inject
    protected RowReaderFactory rowReaderFactory;

    @Inject
    protected DataSourceFactory dataSourceFactory;

    @Inject
    protected BatchTranslatorFactory batchTranslatorFactory;

    @Inject
    protected DbAdapterFactory adapterFactory;

    @Inject
    protected AdhocObjectFactory objectFactory;

    @Inject
    protected SchemaUpdateStrategy defaultSchemaUpdateStrategy;

    @Override
    public DataNode createDataNode(DataNodeDescriptor nodeDescriptor) throws Exception {

        DataNode dataNode = new DataNode(nodeDescriptor.getName());

        dataNode.setJdbcEventLogger(jdbcEventLogger);
        dataNode.setRowReaderFactory(rowReaderFactory);
        dataNode.setBatchTranslatorFactory(batchTranslatorFactory);

        dataNode.setDataSourceLocation(nodeDescriptor.getParameters());

        DataSource dataSource = dataSourceFactory.getDataSource(nodeDescriptor);

        dataNode.setDataSourceFactory(nodeDescriptor.getDataSourceFactoryType());
        dataNode.setDataSource(dataSource);

        // schema update strategy
        String schemaUpdateStrategyType = nodeDescriptor.getSchemaUpdateStrategyType();

        if (schemaUpdateStrategyType == null) {
            dataNode.setSchemaUpdateStrategy(defaultSchemaUpdateStrategy);
            dataNode.setSchemaUpdateStrategyName(defaultSchemaUpdateStrategy.getClass().getName());
        } else {
            SchemaUpdateStrategy strategy = objectFactory.newInstance(SchemaUpdateStrategy.class,
                    schemaUpdateStrategyType);
            dataNode.setSchemaUpdateStrategyName(schemaUpdateStrategyType);
            dataNode.setSchemaUpdateStrategy(strategy);
        }

        // DbAdapter
        dataNode.setAdapter(adapterFactory.createAdapter(nodeDescriptor, dataSource));

        return dataNode;
    }

}
