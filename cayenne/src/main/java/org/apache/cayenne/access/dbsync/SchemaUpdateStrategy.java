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
package org.apache.cayenne.access.dbsync;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.DataMap;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Defines a strategy to update a DB schema to match the DataMaps of a {@link DataNode}. Run once per node, as a part
 * of the node initialization on its first use.
 *
 * @since 3.0
 */
public interface SchemaUpdateStrategy {

    /**
     * Checks and possibly updates the schema of the database behind the DataSource to match the DataMaps.
     *
     * @since 6.0
     */
    void updateSchema(DbAdapter adapter, DataSource dataSource, Collection<DataMap> dataMaps, SQLLogger logger) throws SQLException;
}
