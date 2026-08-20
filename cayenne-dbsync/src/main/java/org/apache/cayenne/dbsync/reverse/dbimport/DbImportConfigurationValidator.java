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

package org.apache.cayenne.dbsync.reverse.dbimport;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.reverse.configuration.DbAdapterFactory;
import org.apache.cayenne.di.Injector;

import java.util.Collection;
import javax.sql.DataSource;

/**
 * @since 4.0
 */
public class DbImportConfigurationValidator implements Cloneable {
    private final ReverseEngineering reverseEngineering;
    private final DbImportConfiguration config;
    private final Injector injector;

    public DbImportConfigurationValidator(ReverseEngineering reverseEngineering, DbImportConfiguration config, Injector injector) {
        this.reverseEngineering = reverseEngineering;
        this.config = config;
        this.injector = injector;
    }

    public void validate() throws Exception {
        DbAdapter adapter;

        try {
            DataSource dataSource = config.createDataSource();
            adapter = injector.getInstance(DbAdapterFactory.class).createAdapter(config.getAdapter(), dataSource);
        } catch (Exception ex) {
            throw new Exception("Error creating DataSource or DbAdapter for URL (" + config.getUrl() + ")", ex);
        }

        if (adapter != null && !adapter.supportsCatalogsOnReverseEngineering() && !isReverseEngineeringCatalogsEmpty()) {
            String message = "Your database does not support catalogs on reverse engineering. " +
                    "It allows to connect to only one at the moment. " +
                    "Please don't note catalogs in <dbimport> configuration.";
            throw new Exception(message);
        }
    }

    private boolean isReverseEngineeringCatalogsEmpty() {
        Collection<Catalog> catalogs = reverseEngineering.getCatalogs();
        if (catalogs == null || catalogs.isEmpty()) {
            return true;
        }

        for (Catalog catalog : catalogs) {
            if (catalog.getName() != null) {
                return false;
            }
        }

        return true;
    }
}