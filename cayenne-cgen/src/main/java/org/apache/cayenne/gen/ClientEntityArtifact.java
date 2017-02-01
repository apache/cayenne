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
package org.apache.cayenne.gen;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.map.ObjEntity;

/**
 * Client code generation artifact based on ObjEntity.
 * 
 * @since 3.0
 */
public class ClientEntityArtifact extends EntityArtifact {

    public ClientEntityArtifact(ObjEntity entity) {
        super(entity);
    }

    @Override
    public String getQualifiedBaseClassName() {
        return (entity.getClientSuperClassName() != null) ? entity
                .getClientSuperClassName() : PersistentObject.class.getName();
    }

    @Override
    public String getQualifiedClassName() {
        return entity.getClientClassName();
    }
}
