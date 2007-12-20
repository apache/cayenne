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

import org.apache.cayenne.enhancer.AccessorVisitor;
import org.apache.cayenne.jpa.map.AccessType;
import org.apache.cayenne.jpa.map.JpaClassDescriptor;
import org.apache.cayenne.jpa.map.JpaPropertyDescriptor;
import org.objectweb.asm.ClassVisitor;

/**
 * @author Andrus Adamchik
 */
class JpaAccessorVisitor extends AccessorVisitor {

    private JpaClassDescriptor descriptor;

    public JpaAccessorVisitor(ClassVisitor visitor, JpaClassDescriptor descriptor) {
        super(visitor);
        this.descriptor = descriptor;
    }

    @Override
    protected boolean isEnhancedProperty(String property) {
        return descriptor.getAccess() != AccessType.PROPERTY
                && descriptor.getProperty(property) != null;
    }

    @Override
    protected boolean isLazyFaulted(String property) {
        JpaPropertyDescriptor propertyDescriptor = descriptor.getProperty(property);

        // TODO: andrus, 10/14/2006 - this should access Jpa LAZY vs. EAGER flag
        // instead of using Cayenne default logic of lazy relationships
        return propertyDescriptor != null
                && propertyDescriptor.getTargetEntityType() != null;
    }
}
