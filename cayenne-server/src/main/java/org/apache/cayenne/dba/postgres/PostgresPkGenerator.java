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

package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.map.DbEntity;

/**
 * Default PK generator for PostgreSQL that uses sequences for PK generation.
 */
public class PostgresPkGenerator extends OraclePkGenerator {

    /**
     * Used by DI
     * @since 4.1
     */
    public PostgresPkGenerator() {
        super();
    }

    protected PostgresPkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    @Override
    protected String createSequenceString(DbEntity ent) {
        // note that PostgreSQL 7.4 and newer supports INCREMENT BY and START WITH
        // however 7.3 doesn't like BY and WITH, so using older more neutral
        // syntax that works with all tested versions.
        return "CREATE SEQUENCE " + sequenceName(ent) + " INCREMENT " + pkCacheSize(ent) + " START " + pkStartValue;
    }

    @Override
    protected String selectNextValQuery(String sequenceName) {
        return "SELECT nextval('" + sequenceName + "')";
    }

    @Override
    protected String selectAllSequencesQuery() {
        return "SELECT relname FROM pg_class WHERE relkind='S'";
    }
}
