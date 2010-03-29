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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.NameConverter;
import org.apache.cayenne.util.Util;

/**
 * A {@link MergerToken} to add a {@link DbEntity} to a {@link DataMap}
 * 
 */
public class CreateTableToModel extends AbstractToModelToken.Entity {

    /**
     * className if {@link ObjEntity} should be generated with a
     *  special class name.
     * Setting this to <code>null</code>, because by default class name should be generated 
     */
    private String objEntityClassName = null; //CayenneDataObject.class.getName();

    public CreateTableToModel(DbEntity entity) {
        super(entity);
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

    public void execute(MergerContext mergerContext) {
        DataMap map = mergerContext.getDataMap();
        map.addDbEntity(getEntity());

        // create a ObjEntity
        String objEntityName = NameConverter.underscoredToJava(getEntity().getName(), true);
        // this loop will terminate even if no valid name is found
        // to prevent loader from looping forever (though such case is very unlikely)
        String baseName = objEntityName;
        for (int i = 1; i < 1000 && map.getObjEntity(objEntityName) != null; i++) {
            objEntityName = baseName + i;
        }

        ObjEntity objEntity = new ObjEntity(objEntityName);
        objEntity.setDbEntity(getEntity());

        // try to find a class name for the ObjEntity
        String className = objEntityClassName;
        if (className == null) {
            // we should generate a className based on the objEntityName
            String packageName = map.getDefaultPackage();
            if (Util.isEmptyString(packageName)) {
                packageName = "";
            }
            else if (!packageName.endsWith(".")) {
                packageName = packageName + ".";
            }
            className = packageName + objEntityName;
        }

        objEntity.setClassName(className);
        
        objEntity.setSuperClassName(map.getDefaultSuperclass());
        
        if (map.isClientSupported()) {
            String clientPkg = map.getDefaultClientPackage();
            if (clientPkg != null) {
                if (!clientPkg.endsWith(".")) {
                    clientPkg = clientPkg + ".";
                }

                objEntity.setClientClassName(clientPkg + objEntity.getName());
            }

            objEntity.setClientSuperClassName(map.getDefaultClientSuperclass());
        }
        
        map.addObjEntity(objEntity);

        synchronizeWithObjEntity(mergerContext, getEntity());
        
        mergerContext.getModelMergeDelegate().dbEntityAdded(getEntity());
        mergerContext.getModelMergeDelegate().objEntityAdded(objEntity);
    }

    public String getTokenName() {
        return "Create Table";
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createDropTableToDb(getEntity());
    }

}
