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

package org.apache.cayenne.dbsync.merge.token.db;

import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.Procedure;

/**
 * @since 4.2
 */
public class DropProcedureToDb extends AbstractToDbToken {

    private Procedure procedure;

    public DropProcedureToDb(Procedure procedure) {
        super("Drop procedure to db", 7);
        this.procedure = procedure;
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        throw new UnsupportedOperationException("Can't drop procedure to db.");
    }

    @Override
    public String getTokenValue() {
        return procedure.getName();
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createAddProcedureToModel(procedure);
    }
}
