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

import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

/**
 * A base implementation of a non-JTA EntityManagerFactory.
 * 
 * @author Andrus Adamchik
 */
public abstract class JpaEntityManagerFactory implements EntityManagerFactory {

    protected boolean open;
    protected PersistenceUnitInfo unitInfo;
    protected Object delegate;

    public JpaEntityManagerFactory(PersistenceUnitInfo unitInfo) {
        this.unitInfo = unitInfo;
        this.open = true;
    }
    
    protected PersistenceUnitInfo getPersistenceUnitInfo() {
        return unitInfo;
    }

    /**
     * Indicates whether the factory is open. Returns true until the factory has been
     * closed.
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Close the factory, releasing any resources that it holds. After a factory instance
     * is closed, all methods invoked on it will throw an IllegalStateException, except
     * for isOpen, which will return false.
     */
    public void close() {
        checkClosed();
        this.open = false;
    }

    /**
     * Create a new EntityManager. Returns a new EntityManager instance every time it is
     * invoked. The {@link EntityManager#isOpen()} method will return true of the returned
     * instance.
     * 
     * @return a new EntityManager instance.
     */
    public EntityManager createEntityManager() {
        return createEntityManager(Collections.EMPTY_MAP);
    }

    /**
     * Create a new EntityManager with the specified map of properties. Returns a new
     * EntityManager instance every time it is invoked. The {@link EntityManager#isOpen()}
     * method will return true of the returned instance.
     * 
     * @return a new EntityManager instance.
     */
    public EntityManager createEntityManager(Map parameters) {
        checkClosed();
        return createEntityManagerInternal(parameters);
    }

    protected abstract EntityManager createEntityManagerInternal(Map parameters);

    /**
     * A convenience method that throws an exception if called on closed factory.
     */
    void checkClosed() throws IllegalStateException {
        if (!isOpen()) {
            throw new IllegalStateException(
                    "An attempt to access closed EntityManagerFactory.");
        }
    }

    /**
     * Returns a "delegate" object which is usually a parent persistence provider.
     */
    public Object getDelegate() {
        return delegate;
    }

    /**
     * Sets a "delegate" object which is usually a parent persistence provider.
     */
    public void setDelegate(Object delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns PersistenceUnitInfo used by this factory.
     */
    PersistenceUnitInfo getUnitInfo() {
        return unitInfo;
    }
}
