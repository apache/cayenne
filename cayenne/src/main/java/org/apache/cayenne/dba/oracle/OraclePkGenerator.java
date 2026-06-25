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

package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.SequencePkGenerator;

/**
 * Sequence-based primary key generator implementation for Oracle. Uses Oracle sequences to generate primary key values.
 */
public class OraclePkGenerator extends SequencePkGenerator {

    /**
     * Used by DI
     *
     * @since 4.1
     */
    public OraclePkGenerator() {
        super();
    }

    protected OraclePkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    @Override
    protected String selectNextValQuery(String pkGeneratingSequenceName) {
        return "SELECT " + pkGeneratingSequenceName + ".nextval FROM DUAL";
    }

    @Override
    protected String selectAllSequencesQuery() {
        return "SELECT LOWER(SEQUENCE_NAME) FROM ALL_SEQUENCES";
    }
}
