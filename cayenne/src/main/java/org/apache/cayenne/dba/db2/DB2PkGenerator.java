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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.SequencePkGenerator;
import org.apache.cayenne.map.compiled.CompiledTable;

/**
 * A sequence-based PK generator used by {@link DB2Adapter}.
 */
public class DB2PkGenerator extends SequencePkGenerator {

    private static final String _SEQUENCE_PREFIX = "S_";

    public DB2PkGenerator() {
    }

    @Override
    protected String sequenceName(CompiledTable table) {
        return super.sequenceName(table).toUpperCase();
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
    protected String dropSequenceString(DbAdapter adapter, CompiledTable table) {
        return "DROP SEQUENCE " + adapter.getQuotingStrategy(table).quotedFQN(table.catalog(), table.schema(),
                sequenceName(table)) + " RESTRICT ";
    }

    @Override
    protected String createSequenceString(DbAdapter adapter, CompiledTable table) {
        return "CREATE SEQUENCE " + adapter.getQuotingStrategy(table).quotedFQN(table.catalog(),
                table.schema(), sequenceName(table)) + " AS BIGINT START WITH "
                + pkStartValue + " INCREMENT BY " + getPkCacheSize() + " NO MAXVALUE NO CYCLE CACHE " + getPkCacheSize();
    }
}
