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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.log.JdbcEventLogger;

/**
 * A factory of DbAdapters that either loads user-provided adapter or guesses
 * the adapter type from the database metadata.
 *
 * @since 3.1
 */
public class DefaultDbAdapterFactory implements DbAdapterFactory {

	@Inject
	protected Injector injector;

	@Inject
	protected JdbcEventLogger jdbcEventLogger;

	@Inject
	protected AdhocObjectFactory objectFactory;

	@Inject
	protected PkGeneratorFactoryProvider pkGeneratorProvider;

	protected List<DbAdapterDetector> detectors;

	public DefaultDbAdapterFactory(@Inject(Constants.ADAPTER_DETECTORS_LIST) List<DbAdapterDetector> detectors) {
		if (detectors == null) {
			throw new NullPointerException("Null detectors list");
		}
		this.detectors = detectors;
	}

	@Override
	public DbAdapter createAdapter(DataNodeDescriptor nodeDescriptor, final DataSource dataSource) {

		String adapterType = null;

		if (nodeDescriptor != null) {
			adapterType = nodeDescriptor.getAdapterType();
		}

		// must not create AutoAdapter via objectFactory, so treat explicit
		// AutoAdapter as null and let it fall through to the default. (explicit
		// AutoAdapter is often passed from the cdbimport pligin).
		if (adapterType != null && adapterType.equals(AutoAdapter.class.getName())) {
			adapterType = null;
		}

		if (adapterType != null) {
			DbAdapter dbAdapter = objectFactory.newInstance(DbAdapter.class, adapterType);
			return setupPkGenerator(dbAdapter);
		} else {
			return new AutoAdapter(() -> detectAdapter(dataSource), jdbcEventLogger);
		}
	}

	protected DbAdapter detectAdapter(DataSource dataSource) {

		if (detectors.isEmpty()) {
			return defaultAdapter();
		}

		try (Connection c = dataSource.getConnection()) {
			return detectAdapter(c.getMetaData());
		} catch (SQLException e) {
			throw new CayenneRuntimeException("Error detecting database type: " + e.getLocalizedMessage(), e);
		}
	}

	protected DbAdapter detectAdapter(DatabaseMetaData metaData) throws SQLException {
		// iterate in reverse order to allow custom factories to take precedence
		// over the
		// default ones configured in constructor
		for (int i = detectors.size() - 1; i >= 0; i--) {
			DbAdapterDetector detector = detectors.get(i);
			DbAdapter adapter = detector.createAdapter(metaData);

			if (adapter != null) {
				jdbcEventLogger.log("Detected and installed adapter: " + adapter.getClass().getName());

				// TODO: should detector do this??
				injector.injectMembers(adapter);

				return setupPkGenerator(adapter);
			}
		}

		return defaultAdapter();
	}

	protected DbAdapter defaultAdapter() {
		jdbcEventLogger.log("Failed to detect database type, using generic adapter");
		return objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
	}

	/**
	 * Setup PK generator for the adapter
	 * @param dbAdapter to process
	 * @return db adapter
	 * @since 4.1
	 */
	protected DbAdapter setupPkGenerator(DbAdapter dbAdapter) {
		PkGenerator pkGenerator = pkGeneratorProvider.get(Objects.requireNonNull(dbAdapter));
		if(pkGenerator != null) {
			pkGenerator.setAdapter(dbAdapter);
			dbAdapter.setPkGenerator(pkGenerator);
		}
		return dbAdapter;
	}
}
