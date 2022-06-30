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

package org.apache.cayenne.dbsync.merge.token.model;

import org.apache.cayenne.dbsync.merge.context.MergerContext;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.event.AttributeEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class SetPrimaryKeyToModel extends AbstractToModelToken.Entity {

    private Collection<DbAttribute> primaryKeyOriginal;
    private Collection<DbAttribute> primaryKeyNew;
    private String detectedPrimaryKeyName;
    private Set<String> primaryKeyNewAttributeNames = new HashSet<>();
    private Function<String, String> nameConverter;

    public SetPrimaryKeyToModel(DbEntity entity,
            Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew, String detectedPrimaryKeyName, Function<String, String> nameConverter) {
        super("Set Primary Key", 105, entity);
        
        this.primaryKeyOriginal = primaryKeyOriginal;
        this.primaryKeyNew = primaryKeyNew;
        this.detectedPrimaryKeyName = detectedPrimaryKeyName;
        this.nameConverter = nameConverter;
        
        for (DbAttribute attr : primaryKeyNew) {
            primaryKeyNewAttributeNames.add(this.nameConverter.apply(attr.getName()));
        }
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createSetPrimaryKeyToDb(
                getEntity(),
                primaryKeyNew,
                primaryKeyOriginal,
                detectedPrimaryKeyName,
                nameConverter);
    }

    @Override
    public void execute(MergerContext mergerContext) {
        DbEntity e = getEntity();

        for (DbAttribute attr : e.getAttributes()) {

            boolean wasPrimaryKey = attr.isPrimaryKey();
            boolean willBePrimaryKey = primaryKeyNewAttributeNames.contains(nameConverter.apply(attr.getName()));

            if (wasPrimaryKey != willBePrimaryKey) {
                attr.setPrimaryKey(willBePrimaryKey);
                e.dbAttributeChanged(new AttributeEvent(this, attr, e));
                mergerContext.getDelegate().dbAttributeModified(attr);
            }
        }
    }
}
