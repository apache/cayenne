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
package org.apache.cayenne.dba;

import org.apache.cayenne.CayenneRuntimeException;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * A factory interface providing DbAdapter based on JDBC metadata.
 *
 * @since 3.1
 */
public interface DbAdapterDetector {

    /**
     * Returns an instance of DbAdapter if the factory detects that it knows how to handle
     * the database or null if the database is not known to the factory, thus allowing
     * multiple factories to be chained.
     */
    DbAdapter createAdapter(DatabaseMetaData md) throws SQLException;

    /**
     * Returns the adapter produced by the first detector matching the database described by the metadata, trying the
     * detectors in reverse order.
     *
     * @since 6.0
     */
    static DbAdapter detect(DatabaseMetaData metaData, List<DbAdapterDetector> detectors) {

        try {
            // iterate in reverse order to allow custom detectors to take precedence over the default ones, and let
            // the first JdbcAdapter detector to serve a fallback
            for (int i = detectors.size() - 1; i >= 0; i--) {
                DbAdapter adapter = detectors.get(i).createAdapter(metaData);
                if (adapter != null) {
                    LoggerFactory.getLogger(DbAdapterDetector.class).info("Detected database '{}', installed adapter: {}",
                            metaData.getDatabaseProductName(), adapter.getClass().getName());
                    return adapter;
                }
            }

            throw new CayenneRuntimeException("No DbAdapterDetector matched the database: %s %s",
                    metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error detecting database type: " + e.getLocalizedMessage(), e);
        }
    }
}
