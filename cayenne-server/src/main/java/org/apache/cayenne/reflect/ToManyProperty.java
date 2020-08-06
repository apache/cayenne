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
 * A property representing a collection of objects.
 * 
 * @since 3.0
 */
public interface ToManyProperty extends ArcProperty {

    void addTarget(Object source, Object target, boolean setReverse)
            throws PropertyException;

    void removeTarget(Object source, Object target, boolean setReverse)
            throws PropertyException;
    
    /**
     * Adds value to collection, without triggering changing events
     * This method is mostly for internal use
     * @since 3.1
     */
    void addTargetDirectly(Object source, Object target)
        throws PropertyException;

    /**
     * Removes value from collection, without triggering changing events
     * This method is mostly for internal use
     * @since 3.1
     */
    void removeTargetDirectly(Object source, Object target)
        throws PropertyException;
}
