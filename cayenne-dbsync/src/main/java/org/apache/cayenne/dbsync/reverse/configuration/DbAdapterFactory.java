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

import org.apache.cayenne.dba.DbAdapter;

import javax.sql.DataSource;

/**
 * Creates a DbAdapter for a Cayenne tool, either of an explicitly requested type, or detected off the DataSource.
 *
 * @since 5.0
 */
public interface DbAdapterFactory {

    /**
     * @param adapterType a name of the DbAdapter class to use, or null to detect the adapter off the DataSource
     */
    DbAdapter createAdapter(String adapterType, DataSource dataSource);
}
