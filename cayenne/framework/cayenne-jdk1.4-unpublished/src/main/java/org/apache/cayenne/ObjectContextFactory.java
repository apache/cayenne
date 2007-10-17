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

import java.util.Map;

/**
 * Defines a factory interface for creating object contexts.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public interface ObjectContextFactory {

    /**
     * Creates an instance of ObjectContext accessing the database via the default
     * DataChannel.
     */
    ObjectContext createObjectContext(Map properties);

    /**
     * Creates an instance of ObjectContext accessing the database via the provided
     * DataChannel.
     */
    ObjectContext createObjectContext(DataChannel parent, Map properties);
}
