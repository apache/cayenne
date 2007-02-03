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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.cayenne.jpa.JpaPersistenceProvider;
import org.apache.cayenne.jpa.JpaUnit;

import junit.framework.TestCase;

public class JpaUnitTest extends TestCase {

    public void testTransactionType() {

        JpaUnit info = new JpaUnit();
        info.addProperties(Collections.singletonMap(
                JpaPersistenceProvider.TRANSACTION_TYPE_PROPERTY,
                PersistenceUnitTransactionType.JTA.name()));
        assertSame(PersistenceUnitTransactionType.JTA, info.getTransactionType());

        info.addProperties(Collections.singletonMap(
                JpaPersistenceProvider.TRANSACTION_TYPE_PROPERTY,
                PersistenceUnitTransactionType.RESOURCE_LOCAL.name()));
        assertSame(PersistenceUnitTransactionType.RESOURCE_LOCAL, info
                .getTransactionType());
    }

    public void testClassLoader() {
        JpaUnit info = new JpaUnit();

        ClassLoader topLoader = new URLClassLoader(new URL[0], Thread
                .currentThread()
                .getContextClassLoader());
        info.setClassLoader(topLoader);
        assertSame(topLoader, info.getClassLoader());
    }

    public void testGetNewTempClassLoader() {
        JpaUnit info = new JpaUnit();

        ClassLoader topLoader = new URLClassLoader(new URL[0], Thread
                .currentThread()
                .getContextClassLoader());
        info.setClassLoader(topLoader);

        ClassLoader tmp1 = info.getNewTempClassLoader();
        ClassLoader tmp2 = info.getNewTempClassLoader();
        assertNotSame(topLoader, tmp1);
        assertNotSame(topLoader, tmp2);
        assertNotSame(tmp1, tmp2);

        // presumably spec requirements that ".. scope and classpath of this loader is
        // exactly the same as that of the loader returned by
        // PersistenceUnitInfo.getClassLoader..." means that it has to be a child of the
        // main ClassLoader
        assertSame(topLoader, tmp1.getParent());
        assertSame(topLoader, tmp2.getParent());
    }
}
