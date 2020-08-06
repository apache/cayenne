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

package org.apache.cayenne.unit;

import java.sql.Connection;
import java.util.Collection;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.exp.parser.ASTExtract;
import org.apache.cayenne.map.DataMap;

/**
 */
public class FrontBaseUnitDbAdapter extends UnitDbAdapter {

    public FrontBaseUnitDbAdapter(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public boolean supportsLobs() {
        return true;
    }

    @Override
    public boolean supportsLobInsertsAsStrings() {
        return false;
    }
    
    @Override
    public boolean supportsEqualNullSyntax() {
        return false;
    }

    @Override
    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop)
            throws Exception {
        // avoid dropping constraints...
    }

    @Override
    public boolean supportsBatchPK() {
        return false;
    }

    @Override
    public boolean supportsHaving() {
        // FrontBase DOES support HAVING, however it doesn't support aggegate expressions
        // in HAVING, and requires using column aliases instead. As HAVING is used for old
        // and ugly derived DbEntities, no point in implementing special support at the
        // adapter level.
        return false;
    }

    @Override
    public boolean supportsCaseInsensitiveOrder() {
        // TODO, Andrus 11/8/2005: FrontBase does support UPPER() in ordering clause,
        // however it does not
        // support table aliases inside UPPER... Not sure what to do about it.

        return false;
    }

    @Override
    public boolean supportsEscapeInLike() {
        return false;
    }

    @Override
    public boolean supportsExpressionInHaving() {
        return false;
    }

    public boolean supportsExtractPart(ASTExtract.DateTimePart part) {
        switch (part) {
            case WEEK:
            case DAY_OF_YEAR:
            case DAY_OF_WEEK:
                return false;
        }
        return true;
    }
}
