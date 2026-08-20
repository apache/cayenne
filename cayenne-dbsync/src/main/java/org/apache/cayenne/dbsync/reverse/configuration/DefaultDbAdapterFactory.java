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

package org.apache.cayenne.dbsync.reverse.configuration;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.runtime.DbAdapterDetector;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

/**
 * @since 5.0
 */
public class DefaultDbAdapterFactory implements DbAdapterFactory {

    private final AdhocObjectFactory objectFactory;
    private final List<DbAdapterDetector> detectors;

    public DefaultDbAdapterFactory(
            @Inject AdhocObjectFactory objectFactory,
            @Inject(Constants.ADAPTER_DETECTORS_LIST) List<DbAdapterDetector> detectors) {
        this.objectFactory = objectFactory;
        this.detectors = Objects.requireNonNull(detectors, "Null detectors list");
    }

    @Override
    public DbAdapter createAdapter(String adapterType, DataSource dataSource) {

        // AutoAdapter must not be created via the object factory, so an explicit AutoAdapter is treated the same as
        // no adapter at all (an explicit AutoAdapter is often passed from the cdbimport plugin)
        return adapterType == null || AutoAdapter.class.getName().equals(adapterType)
                ? new AutoAdapter(objectFactory, dataSource, detectors)
                : objectFactory.newInstance(DbAdapter.class, adapterType);
    }
}
