/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.map.DataMap;

/**
 * @author Andrus Adamchik
 */
public class FrontBaseStackAdapter extends AccessStackAdapter {

    public FrontBaseStackAdapter(DbAdapter adapter) {
        super(adapter);
    }

    public boolean supportsLobs() {
        return true;
    }

    public boolean supportsLobInsertsAsStrings() {
        return false;
    }

    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop)
            throws Exception {
        // avoid dropping constraints...
    }

    public boolean supportsBatchPK() {
        return false;
    }

    public boolean supportsHaving() {
        // FrontBase DOES support HAVING, however it doesn't support aggegate expressions
        // in HAVING, and requires using column aliases instead. As HAVING is used for old
        // and ugly derived DbEntities, no point in implementing special support at the
        // adapter level.
        return false;
    }

    public boolean supportsCaseInsensitiveOrder() {
        // TODO, Andrus 11/8/2005: FrontBase does support UPPER() in ordering clause,
        // however it does not
        // support table aliases inside UPPER... Not sure what to do about it.

        return false;
    }
}
