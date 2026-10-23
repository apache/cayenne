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
 * A callback interface to listen to persistent object lifecycle events. Note that
 * listeners ARE NOT REQUIRED to implement this interface, and in fact most won't. It
 * exists for type safety and for simplifying listener registration.
 * 
 * @since 3.0
 */
public interface LifecycleListener {

    void postAdd(Persistent object);
    
    void prePersist(Persistent object);

    void postPersist(Persistent object);

    void preRemove(Persistent object);

    void postRemove(Persistent object);

    void preUpdate(Persistent object);

    void postUpdate(Persistent object);

    void postLoad(Persistent object);
}
