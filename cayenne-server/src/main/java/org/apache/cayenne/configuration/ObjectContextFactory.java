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
package org.apache.cayenne.configuration;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;

/**
 * A factory for regular and nested contexts.
 * 
 * @since 3.1
 */
public interface ObjectContextFactory {

    /**
     * Creates an ObjectContext attached to a default DataChannel.
     */
    ObjectContext createContext();

    /**
     * Creates an ObjectContext attached to a provided channel. This is often used for
     * nested context creation.
     */
    ObjectContext createContext(DataChannel parent);
}
