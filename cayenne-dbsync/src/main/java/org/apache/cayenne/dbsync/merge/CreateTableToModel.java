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
package org.apache.cayenne.dbsync.merge;

import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;

/**
 * A {@link MergerToken} to add a {@link DbEntity} to a {@link DataMap}
 */
public class CreateTableToModel extends AbstractToModelToken.Entity {

    /**
     * className if {@link ObjEntity} should be generated with a
     * special class name.
     * Setting this to <code>null</code>, because by default class name should be generated
     */
    private String objEntityClassName;

    public CreateTableToModel(DbEntity entity) {
        super("Create Table", entity);
    }

    /**
     * Set the {@link ObjEntity} className if {@link ObjEntity} should be generated with a
     * special class name. Set to null if the {@link ObjEntity} should be created with a
     * name based on {@link DataMap#getDefaultPackage()} and {@link ObjEntity#getName()}
     * <p>
     * The default value is <code>null</code>
     */
    public void setObjEntityClassName(String n) {
        objEntityClassName = n;
    }

    @Override
    public void execute(MergerContext context) {
        DbEntity dbEntity = getEntity();

        DataMap map = context.getDataMap();
        map.addDbEntity(dbEntity);

        // create a ObjEntity
        ObjEntity objEntity = new ObjEntity();

        objEntity.setName(NameBuilder
                .builder(objEntity, dbEntity.getDataMap())
                .baseName(context.getNameGenerator().objEntityName(dbEntity))
                .name());
        objEntity.setDbEntity(getEntity());

        // try to find a class name for the ObjEntity
        String className = objEntityClassName;
        if (className == null) {
            // generate a className based on the objEntityName
            className = map.getNameWithDefaultPackage(objEntity.getName());
        }

        objEntity.setClassName(className);
        objEntity.setSuperClassName(map.getDefaultSuperclass());

        if (map.isClientSupported()) {
            objEntity.setClientClassName(map.getNameWithDefaultClientPackage(objEntity.getName()));
            objEntity.setClientSuperClassName(map.getDefaultClientSuperclass());
        }

        map.addObjEntity(objEntity);

        // presumably there are no other ObjEntities pointing to this DbEntity, so syncing just this one...
        context.getEntityMergeSupport().synchronizeWithDbEntity(objEntity);

        context.getDelegate().dbEntityAdded(getEntity());
        context.getDelegate().objEntityAdded(objEntity);
    }

    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createDropTableToDb(getEntity());
    }

}
