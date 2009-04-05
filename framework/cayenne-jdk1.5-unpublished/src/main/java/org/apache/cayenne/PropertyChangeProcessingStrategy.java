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
 * Defines how ObjectContext should behave in response to
 * {@link ObjectContext#propertyChanged(Persistent, String, Object, Object)} calls.
 * 
 * @since 3.0
 */
enum PropertyChangeProcessingStrategy {

    /**
     * A strategy that does no extra processing of property changes. Usually used when
     * already committed changes are merged from a downstream channel.
     */
    IGNORE,

    /**
     * A strategy that records the change in the change log and marks participating
     * objects as dirty, but no attempt is made to process reverse relationships. Usually
     * used when processing changes from an upstream (child) context.
     */
    RECORD,

    /**
     * A strategy that records the change in the change log and marks participating
     * objects as dirty, and then initiates a complimentary change to the reverse
     * relationships. The default operation mode used for changes initiated by the user.
     */
    RECORD_AND_PROCESS_REVERSE_ARCS;
}
