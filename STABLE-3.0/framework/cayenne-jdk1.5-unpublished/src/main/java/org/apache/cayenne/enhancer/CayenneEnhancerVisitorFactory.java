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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

/**
 * EnhancerVisitorFactory implementation based on Cayenne mapping.
 * 
 * @since 3.0
 */
public class CayenneEnhancerVisitorFactory implements EnhancerVisitorFactory {

    protected Map<String, ObjEntity> entitiesByClass;
    protected Map<String, Embeddable> embeddablesByClass;

    public CayenneEnhancerVisitorFactory(EntityResolver entityResolver) {
        indexEntities(entityResolver);
    }

    protected void indexEntities(EntityResolver entityResolver) {
        // EntityResolver doesn't have an index by class name, (let alone using
        // "internal" class names with slashes as keys), so we have to build it
        // manually

        this.entitiesByClass = new HashMap<String, ObjEntity>();
        for (ObjEntity entity : entityResolver.getObjEntities()) {
            entitiesByClass.put(entity.getClassName(), entity);
        }

        this.embeddablesByClass = new HashMap<String, Embeddable>();
        for (Embeddable embeddable : entityResolver.getEmbeddables()) {
            embeddablesByClass.put(embeddable.getClassName(), embeddable);
        }
    }

    public ClassVisitor createVisitor(String className, ClassVisitor out) {
        String key = className.replace('/', '.');

        ObjEntity entity = entitiesByClass.get(key);
        if (entity != null) {

            // create enhancer chain
            PojoVisitor e1 = new CayennePojoVisitor(out, entity);
            PersistentAccessorVisitor e2 = new PersistentAccessorVisitor(e1, entity);

            // this ensures that both enhanced and original classes have compatible
            // serialized
            // format even if no serialVersionUID is defined by the user
            SerialVersionUIDAdder e3 = new SerialVersionUIDAdder(e2);
            return e3;
        }

        Embeddable embeddable = embeddablesByClass.get(key);
        if (embeddable != null) {
            // create enhancer chain
            EmbeddableVisitor e1 = new EmbeddableVisitor(out);

            // this ensures that both enhanced and original classes have compatible
            // serialized
            // format even if no serialVersionUID is defined by the user
            SerialVersionUIDAdder e2 = new SerialVersionUIDAdder(e1);
            return e2;
        }

        return null;
    }
}
