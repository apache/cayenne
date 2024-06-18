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
package org.apache.cayenne.unit.di.runtime;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.translator.select.SelectTranslatorFactory;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.DataNodeFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;

public class RuntimeCaseDataNodeFactory implements DataNodeFactory {

	@Inject
	private JdbcEventLogger jdbcEventLogger;

	@Inject
	protected RowReaderFactory rowReaderFactory;

	@Inject
	protected BatchTranslatorFactory batchTranslatorFactory;

	@Inject
	protected SelectTranslatorFactory selectTranslatorFactory;

	@Inject
	private RuntimeCaseDataSourceFactory dataSourceFactory;

	@Inject
	private DbAdapter adapter;

	@Inject
	protected SQLTemplateProcessor sqlTemplateProcessor;

	@Override
	public DataNode createDataNode(DataNodeDescriptor nodeDescriptor) throws Exception {
		DataNode dataNode = new RuntimeCaseDataNode(nodeDescriptor.getName());

		dataNode.setJdbcEventLogger(jdbcEventLogger);
		dataNode.setRowReaderFactory(rowReaderFactory);
		dataNode.setBatchTranslatorFactory(batchTranslatorFactory);
		dataNode.setSelectTranslatorFactory(selectTranslatorFactory);

		// shared or dedicated DataSources can be mapped per DataMap
		dataNode.setDataSource(dataSourceFactory.getDataSource(nodeDescriptor.getName()));
		dataNode.setAdapter(adapter);
		dataNode.setSchemaUpdateStrategy(new SkipSchemaUpdateStrategy());
		dataNode.setSqlTemplateProcessor(sqlTemplateProcessor);

		return dataNode;
	}

}
