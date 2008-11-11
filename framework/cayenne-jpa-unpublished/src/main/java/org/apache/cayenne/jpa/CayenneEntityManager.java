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
package org.apache.cayenne.jpa;

import javax.persistence.EntityManager;

import org.apache.cayenne.DataChannel;

/**
 * An interface that allows to access Cayenne runtime hidden behind standard JPA classes.
 * To do that, simply cast an EntityManager returned by Cayenne provider to
 * "CayenneEntityManager". Note that a regular JPA application shouldn't normally attempt
 * to do that. Otherwise it will not be portable across JPA providers.
 * 
 */
public interface CayenneEntityManager extends EntityManager {

    /**
     * Returns a Cayenne {@link DataChannel} that is used to link EntityManager with
     * Cayenne runtime. DataChannel can be used for instance to obtain Cayenne metadata or
     * add listeners.
     */
    DataChannel getChannel();
}
