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
package org.apache.cayenne;

import org.apache.cayenne.reflect.LifecycleCallbackRegistry;

/**
 * A callback interface to listen to persistent object lifecycle events. Note that
 * listeners ARE NOT REQUIRED to implement this interface, and in fact most won't. It
 * exists for type safety and for simplifying listener registration. See
 * {@link LifecycleCallbackRegistry} for details on how to register callbacks.
 * 
 * @since 3.0
 */
public interface LifecycleListener {

    void postAdd(Object entity);
    
    void prePersist(Object entity);

    void postPersist(Object entity);

    void preRemove(Object entity);

    void postRemove(Object entity);

    void preUpdate(Object entity);

    void postUpdate(Object entity);

    void postLoad(Object entity);
}
