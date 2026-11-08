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

package org.apache.cayenne.dba.h2;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.SequencePkGenerator;
import org.apache.cayenne.map.compiled.CompiledTable;

/**
 * Default PK generator for H2 that uses sequences for PK generation.
 *
 * @since 4.0
 */
public class H2PkGenerator extends SequencePkGenerator {

    public H2PkGenerator() {
    }

    @Override
    protected String createSequenceString(DbAdapter adapter, CompiledTable table) {
        return "CREATE SEQUENCE " + adapter.getQuotingStrategy(table).quotedFQN(table.catalog(), table.schema(),
                sequenceName(table)) + " START WITH " + pkStartValue + " INCREMENT BY " + pkCacheSize(table) + " CACHE 1";
    }

    @Override
    protected String selectNextValQuery(String sequenceName) {
        return "SELECT NEXT VALUE FOR " + sequenceName;
    }

    @Override
    protected String selectAllSequencesQuery() {
        return "SELECT LOWER(sequence_name) FROM Information_Schema.Sequences";
    }
}
