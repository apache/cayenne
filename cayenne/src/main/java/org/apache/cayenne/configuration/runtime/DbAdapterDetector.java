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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.cayenne.dba.DbAdapter;

/**
 * A factory interface providing DbAdapter based on JDBC metadata. It allows custom
 * DbAdapters to contribute database detection algorithms to
 * {@link DefaultDbAdapterFactory}.
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
}
