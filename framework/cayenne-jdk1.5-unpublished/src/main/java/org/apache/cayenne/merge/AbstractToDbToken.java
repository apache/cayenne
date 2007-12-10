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

import java.util.List;

import org.apache.cayenne.dba.DbAdapter;

/**
 * Common abstract superclass for all {@link MergerToken}s going from the model to the
 * database.
 * 
 * @author halset
 */
public abstract class AbstractToDbToken implements MergerToken {

    public MergeDirection getDirection() {
        return MergeDirection.TO_DB;
    }

    public void execute(MergerContext mergerContext) {
        for (String sql : createSql(mergerContext.getAdapter())) {
            mergerContext.executeSql(sql);
        }
    }

    public abstract List<String> createSql(DbAdapter adapter);

}
