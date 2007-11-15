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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;

/**
 * A {@link MergerToken} to add a {@link DbEntity} to a {@link DataMap}
 * 
 * @author halset
 */
public class CreateTableToModel extends AbstractToModelToken {

    private DbEntity entity;

    public CreateTableToModel(DbEntity entity) {
        this.entity = entity;
    }

    public void execute(MergerContext mergerContext) {
        mergerContext.getDataMap().addDbEntity(entity);
    }

    public String getTokenName() {
        return "Create Table";
    }

    public String getTokenValue() {
        return entity.getName();
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createDropTableToDb(entity);
    }

}
