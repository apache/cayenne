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
package org.apache.cayenne.dba.db2;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.map.DbEntity;

/**
 * A sequence-based PK generator used by {@link DB2Adapter}.
 */
public class DB2PkGenerator extends OraclePkGenerator {

    /**
     * Used by DI
     * @since 4.1
     */
    public DB2PkGenerator() {
        super();
    }

    DB2PkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    private static final String _SEQUENCE_PREFIX = "S_";

    @Override
    protected String sequenceName(DbEntity entity) {
        return super.sequenceName(entity).toUpperCase();
    }

    @Override
    protected String getSequencePrefix() {
        return _SEQUENCE_PREFIX;
    }

    @Override
    protected String selectNextValQuery(String pkGeneratingSequenceName) {
        return "SELECT NEXTVAL FOR " + pkGeneratingSequenceName + " FROM SYSIBM.SYSDUMMY1";
    }

    @Override
    protected String selectAllSequencesQuery() {
        return "SELECT SEQNAME FROM SYSCAT.SEQUENCES WHERE SEQNAME LIKE '" + _SEQUENCE_PREFIX + "%'";
    }

    @Override
    protected String dropSequenceString(DbEntity entity) {
        return "DROP SEQUENCE " + sequenceName(entity) + " RESTRICT ";
    }

    @Override
    protected String createSequenceString(DbEntity entity) {
        return "CREATE SEQUENCE " + sequenceName(entity) + " AS BIGINT START WITH " + pkStartValue +
                " INCREMENT BY " + getPkCacheSize() + " NO MAXVALUE NO CYCLE CACHE " + getPkCacheSize();
    }
}
