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

package org.apache.cayenne.dba.derby;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.map.DbEntity;

/**
 * PK generator for Derby that uses sequences.
 *
 * @since 4.0 (old one used AUTO_PK_SUPPORT table)
 */
public class DerbyPkGenerator extends OraclePkGenerator {

    /**
     * Used by DI
     * @since 4.1
     */
    public DerbyPkGenerator() {
        super();
    }

    DerbyPkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    @Override
    protected String sequenceName(DbEntity entity) {
        return super.sequenceName(entity).toUpperCase();
    }

    @Override
    protected String selectNextValQuery(String pkGeneratingSequenceName) {
        return "VALUES (NEXT VALUE FOR " + pkGeneratingSequenceName + ")";
    }

    @Override
    protected String selectAllSequencesQuery() {
        return "SELECT SEQUENCENAME FROM SYS.SYSSEQUENCES";
    }

    @Override
    protected String dropSequenceString(DbEntity entity) {
        return "DROP SEQUENCE " + sequenceName(entity) + " RESTRICT";
    }

    @Override
    protected String createSequenceString(DbEntity entity) {
        return "CREATE SEQUENCE " + sequenceName(entity) + " AS BIGINT START WITH " + pkStartValue +
                " INCREMENT BY " + getPkCacheSize() + " NO MAXVALUE NO CYCLE";
    }
}
