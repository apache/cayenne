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

import java.util.Map;

import org.apache.cayenne.enhancer.EnhancerVisitorFactory;
import org.apache.cayenne.enhancer.PersistentInterfaceVisitor;
import org.apache.cayenne.jpa.map.JpaClassDescriptor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

/**
 * Class enhancer used for JPA.
 * 
 * @author Andrus Adamchik
 */
public class JpaEnhancerVisitorFactory implements EnhancerVisitorFactory {

    private Map<String, JpaClassDescriptor> managedClasses;

    public JpaEnhancerVisitorFactory(Map<String, JpaClassDescriptor> managedClasses) {
        this.managedClasses = managedClasses;
    }

    public ClassVisitor createVisitor(String className, ClassVisitor out) {
        JpaClassDescriptor descriptor = managedClasses.get(className.replace('/', '.'));
        if (descriptor == null) {
            return null;
        }

        // from here the code is copied essentially verbatim
        // from CayenneEnhancerVisitorFactory.

        // create enhancer chain
        PersistentInterfaceVisitor e1 = new PersistentInterfaceVisitor(out);
        JpaAccessorVisitor e2 = new JpaAccessorVisitor(e1, descriptor);

        // this ensures that both enhanced and original classes have compatible serialized
        // format even if no serialVersionUID is defined by the user
        SerialVersionUIDAdder e3 = new SerialVersionUIDAdder(e2);

        return e3;
    }
}
