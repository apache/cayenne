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
 */
public class OpenBaseUnitDbAdapter extends UnitDbAdapter {

    public OpenBaseUnitDbAdapter(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop)
            throws Exception {
        // avoid dropping constraints...
    }

    @Override
    public boolean supportsHaving() {
        return false;
    }

    @Override
    public boolean supportsReverseComparison() {
        // TODO: andrus, 7/1/2007 I am trying to follow up with openbase on this...
        return false;
    }

    @Override
    public boolean supportNullRowForAggregateFunctions() {
        // TODO: andrus, 7/1/2007 I am trying to follow up with openbase on this...
        return false;
    }
}
