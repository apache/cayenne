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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbEntity;

public class FirebirdUnitDbAdapter extends UnitDbAdapter {

    public FirebirdUnitDbAdapter(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public boolean supportsBoolean() {
        return true;
    }

    @Override
    public boolean supportsLobs() {
        return true;
    }

    @Override
    public boolean supportsFKConstraints(DbEntity entity) {
        return !entity.getName().contains("CLOB");
    }

    @Override
    public boolean supportsBinaryPK() {
        return false;
    }

    @Override
    public boolean supportsPKGeneratorConcurrency() {
        return false;
    }

    @Override
    public boolean supportsSelectBooleanExpression() {
        return false;
    }
}
