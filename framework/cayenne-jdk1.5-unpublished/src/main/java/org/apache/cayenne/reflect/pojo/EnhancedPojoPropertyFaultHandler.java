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
package org.apache.cayenne.reflect.pojo;

import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.FieldAccessor;

/**
 * Handles property fault resolving.
 * 
 * @since 3.0
 */
class EnhancedPojoPropertyFaultHandler {

    static final String FAULT_FIELD_PREFIX = "$cay_faultResolved_";

    private Accessor faultResolvedFlagAccessor;

    EnhancedPojoPropertyFaultHandler(Class<?> objectClass, String propertyName) {
        this.faultResolvedFlagAccessor = new FieldAccessor(
                objectClass,
                FAULT_FIELD_PREFIX + propertyName,
                Boolean.TYPE);
    }

    boolean isFaultProperty(Object object) {
        return !((Boolean) faultResolvedFlagAccessor.getValue(object)).booleanValue();
    }

    void setFaultProperty(Object object, boolean flag) {
        // note that we need to negate "fault" property value to get "fault resolved"
        // value stored in the object.
        faultResolvedFlagAccessor.setValue(object, flag ? Boolean.FALSE : Boolean.TRUE);
    }
}
