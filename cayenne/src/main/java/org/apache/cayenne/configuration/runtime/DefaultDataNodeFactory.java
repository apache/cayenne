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
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.BatchTranslator;
import org.apache.cayenne.access.translator.EJBQLTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.SQLTemplateTranslator;
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

import javax.sql.DataSource;
import java.util.List;

/**
 * @since 4.0
 */
public class DefaultDataNodeFactory implements DataNodeFactory {

    @Inject
    protected SQLLogger sqlLogger;

    @Inject
    protected RowReaderFactory rowReaderFactory;

    @Inject(BatchTranslator.INSERT)
    protected BatchTranslator<InsertBatchQuery> insertBatchTranslator;

    @Inject(BatchTranslator.UPDATE)
    protected BatchTranslator<UpdateBatchQuery> updateBatchTranslator;

    @Inject(BatchTranslator.DELETE)
    protected BatchTranslator<DeleteBatchQuery> deleteBatchTranslator;

    @Inject
    protected SelectTranslator selectTranslator;

    @Inject
    protected ProcedureTranslator procedureTranslator;

    @Inject
    protected EJBQLTranslator ejbqlTranslator;

    @Inject
    protected AdhocObjectFactory objectFactory;

    @Inject
    protected SQLTemplateTranslator sqlTemplateTranslator;

    @Inject(Constants.ADAPTER_DETECTORS_LIST)
    protected List<DbAdapterDetector> adapterDetectors;

    @Inject
    protected RuntimeProperties properties;

    @Override
    public DataNode createDataNode(String channelName, DataNodeDescriptor descriptor) {

        DataSource dataSource = createDataSource(channelName, descriptor);

        DataNode dataNode = doCreateDataNode(descriptor.name());

        dataNode.setSQLLogger(sqlLogger);
        dataNode.setRowReaderFactory(rowReaderFactory);
        dataNode.setInsertBatchTranslator(insertBatchTranslator);
        dataNode.setUpdateBatchTranslator(updateBatchTranslator);
        dataNode.setDeleteBatchTranslator(deleteBatchTranslator);
        dataNode.setSelectTranslator(selectTranslator);
        dataNode.setProcedureTranslator(procedureTranslator);
        dataNode.setEjbqlTranslator(ejbqlTranslator);
        dataNode.setSqlTemplateTranslator(sqlTemplateTranslator);

        dataNode.setDataSource(dataSource);
        dataNode.setSchemaUpdateStrategy(createSchemaUpdateStrategy(descriptor.schemaUpdateStrategyType()));
        dataNode.setAdapter(createDbAdapter(descriptor.adapterType(), dataSource));

        return dataNode;
    }

    protected DataNode doCreateDataNode(String name) {
        return new DataNode(name);
    }

    protected DataSource createDataSource(String channelName, DataNodeDescriptor descriptor) {
        return descriptor.dataSource() != null
                ? descriptor.dataSource()
                : CayenneDataSource.fromProperties(properties.toMap(), channelName + "." + descriptor.name()).build();
    }

    protected DbAdapter createDbAdapter(Class<? extends DbAdapter> type, DataSource dataSource) {
        return type != null
                ? objectFactory.newInstance(DbAdapter.class, type, false)
                : new AutoAdapter(objectFactory, dataSource, adapterDetectors);
    }

    protected SchemaUpdateStrategy createSchemaUpdateStrategy(Class<? extends SchemaUpdateStrategy> type) {
        return type != null
                ? objectFactory.newInstance(SchemaUpdateStrategy.class, type, false)
                : new SkipSchemaUpdateStrategy();
    }
}
