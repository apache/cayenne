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

import java.util.Collection;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

/**
 * Enhances classes passed through the visitor, ensuring that the resulting class
 * implements {@link Persistent} interface as well as supports lazy faulting.
 * 
 * @since 3.0
 */
public abstract class PojoVisitor extends ClassAdapter {

    static String ENHANCED_INTERFACE_SIG = Type.getInternalName(Persistent.class);

    protected EnhancementHelper helper;

    public PojoVisitor(ClassVisitor visitor) {
        super(visitor);
        this.helper = new EnhancementHelper(this);
    }

    /**
     * Handles injection of additional fields and Persistent interface properties.
     */
    @Override
    public void visit(
            int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces) {

        for (String anInterface : interfaces) {
            if (ENHANCED_INTERFACE_SIG.equals(anInterface)) {
                throw new DoubleEnhanceException(name
                        + " already implements "
                        + ENHANCED_INTERFACE_SIG);
            }
        }

        helper.reset(name);
        interfaces = helper.addInterface(interfaces, Persistent.class);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        helper.createProperty(ObjectId.class, "objectId");
        helper.createProperty(ObjectContext.class, "objectContext", true);
        helper.createProperty(Integer.TYPE, "persistenceState");

        for (String property : getLazilyFaultedProperties()) {
            helper.createField(Boolean.TYPE, "faultResolved_" + property, true);
        }
    }

    protected abstract Collection<String> getLazilyFaultedProperties();
}
