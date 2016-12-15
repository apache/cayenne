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

package org.apache.cayenne.dbsync.merge.token.db;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.DummyReverseToken;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.ValueForNullProvider;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.util.List;


public class SetValueForNullToDb extends AbstractToDbToken.EntityAndColumn {
    
    private ValueForNullProvider valueForNullProvider;

    public SetValueForNullToDb(DbEntity entity, DbAttribute column, ValueForNullProvider valueForNullProvider) {
        super("Set value for null", 80, entity, column);
        this.valueForNullProvider = valueForNullProvider;
    }
    
    @Override
    public List<String> createSql(DbAdapter adapter) {
        return valueForNullProvider.createSql(getEntity(), getColumn());
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return new DummyReverseToken(this);
    }
    
}
