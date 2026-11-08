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

package org.apache.cayenne;

/**
 * A placeholder for the unresolved value of a property of a persistent object, resolved with
 * {@link #resolveFault(Persistent)}. A fault stands for one compiled property, a relationship or a lazy attribute,
 * and is created with it when the mapping is compiled.
 *
 * @since 1.1
 */
public interface Fault {

    /**
     * Returns the value of the property this fault stands for, read for the given object.
     *
     * @since 6.0 takes the object alone; the fault knows its property
     */
    Object resolveFault(Persistent sourceObject);
}
