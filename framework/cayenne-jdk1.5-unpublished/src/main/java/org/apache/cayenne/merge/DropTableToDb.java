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
package org.apache.cayenne.merge;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbEntity;

public class DropTableToDb extends AbstractToDbToken.Entity {

    public DropTableToDb(DbEntity entity) {
        super(entity);
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        List<String> sqls = new ArrayList<String>();
        // TODO: fix. some adapters drop the complete AUTO_PK_SUPPORT here
        /*
        sqls.addAll(adapter.getPkGenerator().dropAutoPkStatements(
                Collections.singletonList(entity)));
         */
        sqls.addAll(adapter.dropTableStatements(getEntity()));
        return sqls;
    }

    public String getTokenName() {
        return "Drop Table";
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createCreateTableToModel(getEntity());
    }

}
