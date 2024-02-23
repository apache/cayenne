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

/**
 *
 * @since 4.2
 */
public interface EmbeddableObject {

    /**
     * Modifies a value of a named property without altering the object state in any way,
     * and without triggering any database operations. This method is intended mostly for
     * internal use by Cayenne framework, and shouldn't be called from the application
     * code.
     */
    void writePropertyDirectly(String propertyName, Object val);

    /**
     * Returns mapped property value as currently stored in the PersistentObject. Returned value
     * maybe a fault or a real value. This method will not attempt to resolve faults, or
     * to read unmapped properties.
     */
    Object readPropertyDirectly(String propertyName);

}
