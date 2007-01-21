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


package org.apache.cayenne.jpa.cspi;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.jpa.JpaEntityManagerFactory;

/**
 * A default EntityManagerFactory used by Cayenne JPA provider.
 * <h3>Cayenne Compatibility Note</h3>
 * <p>
 * CjpaEntityManagerFactory wraps a DataDomain that maps to a persistence unit.
 * </p>
 * 
 * @author Andrus Adamchik
 */
public class CjpaEntityManagerFactory extends JpaEntityManagerFactory {

    protected DataDomain domain;

    public CjpaEntityManagerFactory(DataDomain domain, PersistenceUnitInfo unitInfo) {
        super(unitInfo);
        this.domain = domain;
    }

    @Override
    protected EntityManager createEntityManagerInternal(Map parameters) {
        CjpaEntityManager manager = new CjpaEntityManager(
                domain.createDataContext(),
                this,
                parameters);
        manager.setDelegate(getDelegate());
        return manager;
    }
}
