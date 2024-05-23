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
package org.apache.cayenne.reflect;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbEntity;

/**
 * A descriptor for an additional DB entity attached to the main one.
 * For now additional entity is spawn by flattened attributes.
 *
 * @see PersistentDescriptorFactory#indexAdditionalDbEntities(PersistentDescriptor)
 *
 * @since 5.0
 */
public class AdditionalDbEntityDescriptor {
    private final CayennePath path;
    private final DbEntity entity;
    private final boolean noDelete;

    /**
     * @param path relative to the root entity path
     * @param entity target of this descriptor
     * @param noDelete should row deletion of the root entity trigger deletion of the additional entity
     */
    AdditionalDbEntityDescriptor(CayennePath path, DbEntity entity, boolean noDelete) {
        this.noDelete = noDelete;
        this.entity = entity;
        this.path = path;
    }

    public DbEntity getDbEntity() {
        return entity;
    }

    public CayennePath getPath() {
        return path;
    }

    public boolean noDelete() {
        return noDelete;
    }
}
