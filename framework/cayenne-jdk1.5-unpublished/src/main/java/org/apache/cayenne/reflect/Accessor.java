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

package org.apache.cayenne.reflect;

import java.io.Serializable;

/**
 * An accessor of a property value. Abstracts the actual property implementation. E.g. it
 * can be a Field, a pair of get/set methods or a map/DataObject.
 * 
 * @since 3.0
 */
public interface Accessor extends Serializable {

    /**
     * Returns property name.
     */
    String getName();

    /**
     * Returns a property value of an object without disturbing the object fault status.
     */
    Object getValue(Object object) throws PropertyException;

    /**
     * Sets a property value of an object without disturbing the object fault status. Old
     * value of the property is specified as a hint.
     */
    void setValue(Object object, Object newValue) throws PropertyException;
}
