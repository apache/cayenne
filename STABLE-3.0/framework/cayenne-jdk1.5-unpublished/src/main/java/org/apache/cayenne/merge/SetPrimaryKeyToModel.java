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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.event.AttributeEvent;

public class SetPrimaryKeyToModel extends AbstractToModelToken.Entity {

    private Collection<DbAttribute> primaryKeyOriginal;
    private Collection<DbAttribute> primaryKeyNew;
    private String detectedPrimaryKeyName;
    private Set<String> primaryKeyNewAttributeNames = new HashSet<String>();

    public SetPrimaryKeyToModel(DbEntity entity,
            Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew, String detectedPrimaryKeyName) {
        super(entity);
        
        this.primaryKeyOriginal = primaryKeyOriginal;
        this.primaryKeyNew = primaryKeyNew;
        this.detectedPrimaryKeyName = detectedPrimaryKeyName;
        
        for (DbAttribute attr : primaryKeyNew) {
            primaryKeyNewAttributeNames.add(attr.getName().toUpperCase());
        }
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createSetPrimaryKeyToDb(
                getEntity(),
                primaryKeyNew,
                primaryKeyOriginal,
                detectedPrimaryKeyName);
    }

    public void execute(MergerContext mergerContext) {
        DbEntity e = getEntity();

        for (DbAttribute attr : e.getAttributes()) {

            boolean wasPrimaryKey = attr.isPrimaryKey();
            boolean willBePrimaryKey = primaryKeyNewAttributeNames.contains(attr
                    .getName()
                    .toUpperCase());

            if (wasPrimaryKey != willBePrimaryKey) {
                attr.setPrimaryKey(willBePrimaryKey);
                e.dbAttributeChanged(new AttributeEvent(this, attr, e));
                mergerContext.getModelMergeDelegate().dbAttributeModified(attr);
            }

        }

    }

    public String getTokenName() {
        return "Set Primary Key";
    }

}
