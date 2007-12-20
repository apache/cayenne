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

package org.apache.cayenne.jpa.reflect;

import org.apache.cayenne.Fault;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.FieldAccessor;
import org.apache.cayenne.reflect.PropertyException;

class JpaCollectionFieldAccessor extends FieldAccessor {

    public JpaCollectionFieldAccessor(Class<?> objectClass, String propertyName,
            Class<?> propertyType) {
        super(objectClass, propertyName, propertyType);

        if (!Persistent.class.isAssignableFrom(objectClass)) {
            throw new IllegalArgumentException("Only supports persistent classes. Got: "
                    + objectClass);
        }
    }

    /**
     * Resolves a fault before setting the field.
     */
    @Override
    public void setValue(Object object, Object newValue) throws PropertyException {

        if (newValue instanceof Fault) {
            newValue = ((Fault) newValue).resolveFault((Persistent) object, getName());
        }

        super.setValue(object, newValue);
    }
}
