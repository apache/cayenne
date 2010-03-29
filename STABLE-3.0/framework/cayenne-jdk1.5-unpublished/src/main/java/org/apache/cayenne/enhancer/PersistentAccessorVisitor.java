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
package org.apache.cayenne.enhancer;

import org.apache.cayenne.map.ObjEntity;
import org.objectweb.asm.ClassVisitor;

/**
 * Accessor enhancer that enhances getters and setters mapped in a given {@link ObjEntity}.
 * 
 * @since 3.0
 */
public class PersistentAccessorVisitor extends AccessorVisitor {

    private ObjEntity entity;

    public PersistentAccessorVisitor(ClassVisitor visitor, ObjEntity entity) {
        super(visitor);
        this.entity = entity;
    }

    @Override
    protected boolean isEnhancedProperty(String property) {
        return entity.getAttribute(property) != null
                || entity.getRelationship(property) != null;
    }

    @Override
    protected boolean isLazyFaulted(String property) {
        return entity.getRelationship(property) != null;
    }
}
