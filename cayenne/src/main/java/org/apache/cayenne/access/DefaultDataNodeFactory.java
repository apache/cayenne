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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.CoreModule;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.BatchTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.SQLTemplateTranslator;
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.project.DataNodeDescriptor;
import org.apache.cayenne.RuntimeProperties;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.DbAdapterDetector;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
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
    protected AdhocObjectFactory objectFactory;

    @Inject
    protected SQLTemplateTranslator sqlTemplateTranslator;

    @Inject(CoreModule.ADAPTER_DETECTORS_LIST)
    protected List<DbAdapterDetector> adapterDetectors;

    @Inject
    protected RuntimeProperties properties;

    @Override
    public DataNode createDataNode(String channelName, DataNodeDescriptor descriptor, Collection<DataMap> dataMaps) {

        DataSource dataSource = createDataSource(channelName, descriptor);

        // an explicit adapter still goes through the initializer, so that the schema update runs on first use
        DbAdapter adapter = descriptor.adapterType() != null
                ? objectFactory.newInstance(DbAdapter.class, descriptor.adapterType(), false)
                : null;
        SchemaUpdateStrategy schemaUpdateStrategy = createSchemaUpdateStrategy(descriptor.schemaUpdateStrategyType());
        DataNodeInitializer initializer =
                () -> initDataNode(descriptor.name(), dataSource, adapter, schemaUpdateStrategy, dataMaps);

        return newDataNode(descriptor.name(), dataSource, initializer, descriptor.pkGenerator());
    }

    /**
     * Creates the node from its per-node parts, wiring in the services injected into the factory. A hook for the
     * subclasses that need a DataNode subclass.
     */
    protected DataNode newDataNode(
            String name,
            DataSource dataSource,
            DataNodeInitializer initializer,
            PkGenerator pkGenerator) {

        return new DataNode(
                name,
                dataSource,
                initializer,
                pkGenerator,
                sqlLogger,
                rowReaderFactory,
                selectTranslator,
                procedureTranslator,
                sqlTemplateTranslator,
                insertBatchTranslator,
                updateBatchTranslator,
                deleteBatchTranslator);
    }

    /**
     * The first-use initialization of a node: detects the adapter off the raw DataSource, unless an explicit adapter
     * is given, and runs the schema update strategy against the DataMaps of the node.
     */
    protected DbAdapter initDataNode(
            String nodeName,
            DataSource dataSource,
            DbAdapter adapter,
            SchemaUpdateStrategy schemaUpdateStrategy,
            Collection<DataMap> dataMaps) {

        DbAdapter resolved = adapter != null ? adapter : detectAdapter(nodeName, dataSource);

        try {
            schemaUpdateStrategy.updateSchema(resolved, dataSource, dataMaps, sqlLogger);
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error updating the schema of DataNode '%s': %s", e, nodeName,
                    e.getLocalizedMessage());
        }

        return resolved;
    }

    /**
     * Detects the DB type off the DataSource with the injected {@link DbAdapterDetector}s. The detection uses a
     * connection of its own, outside the current transaction, if any.
     */
    protected DbAdapter detectAdapter(String nodeName, DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            return DbAdapterDetector.detect(c.getMetaData(), adapterDetectors);
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error detecting the DB type of DataNode '%s': %s", e, nodeName,
                    e.getLocalizedMessage());
        }
    }

    protected DataSource createDataSource(String channelName, DataNodeDescriptor descriptor) {
        return descriptor.dataSource() != null
                ? descriptor.dataSource()
                : CayenneDataSource.fromProperties(properties.toMap(), channelName + "." + descriptor.name()).build();
    }

    protected SchemaUpdateStrategy createSchemaUpdateStrategy(Class<? extends SchemaUpdateStrategy> type) {
        return type != null
                ? objectFactory.newInstance(SchemaUpdateStrategy.class, type, false)
                : new SkipSchemaUpdateStrategy();
    }
}
