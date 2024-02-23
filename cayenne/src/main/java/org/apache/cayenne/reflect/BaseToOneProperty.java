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

/**
 * A convenience base superclass for {@link ToOneProperty} implementors.
 * 
 * @since 3.0
 */
public abstract class BaseToOneProperty extends BaseArcProperty implements ToOneProperty {

    public BaseToOneProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            Accessor accessor, String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    public void setTarget(Object source, Object target, boolean setReverse) {
        Object oldTarget = readProperty(source);
        if (oldTarget == target) {
            return;
        }

        // TODO, Andrus, 2/9/2006 - GenericPersistentObject also invokes "willConnect" and has a
        // callback to ObjectStore to handle flattened....

        if (setReverse) {
            setReverse(source, oldTarget, target);
        }

        writeProperty(source, oldTarget, target);
    }

    @Override
    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitToOne(this);
    }
}
