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
package org.apache.cayenne.jpa.enhancer;

import org.apache.cayenne.enhancer.EmbeddableVisitor;
import org.apache.cayenne.enhancer.EnhancerVisitorFactory;
import org.apache.cayenne.enhancer.PojoVisitor;
import org.apache.cayenne.jpa.map.JpaEmbeddable;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

/**
 * EnhancerVisitorFactory implementation based on JPA mapping.
 * 
 * @since 3.0
 */
public class JpaEnhancerVisitorFactory implements EnhancerVisitorFactory {

    private JpaEntityMap entityMap;

    public JpaEnhancerVisitorFactory(JpaEntityMap entityMap) {
        this.entityMap = entityMap;
    }

    public ClassVisitor createVisitor(String className, ClassVisitor out) {

        String key = className.replace('/', '.');

        JpaEntity entity = entityMap.entityForClass(key);
        if (entity != null) {

            // create enhancer chain
            PojoVisitor e1 = new JpaPojoVisitor(out, entity);
            JpaAccessorVisitor e2 = new JpaAccessorVisitor(e1, entity);

            // this ensures that both enhanced and original classes have compatible
            // serialized format even if no serialVersionUID is defined by the user
            SerialVersionUIDAdder e3 = new SerialVersionUIDAdder(e2);

            return e3;
        }

        JpaEmbeddable embeddable = entityMap.embeddableForClass(key);
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
