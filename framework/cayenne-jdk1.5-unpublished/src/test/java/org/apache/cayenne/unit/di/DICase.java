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
package org.apache.cayenne.unit.di;

import junit.framework.TestCase;

import org.apache.cayenne.di.Injector;

/**
 * A unit test superclass that supports injection of members based on the standard unit
 * test container.
 */
public abstract class DICase extends TestCase {

    protected abstract Injector getUnitTestInjector();

    @Override
    protected final void setUp() throws Exception {
        getUnitTestInjector().getInstance(UnitTestLifecycleManager.class).setUp(this);

        try {
            setUpAfterInjection();
        }
        catch (Exception e) {

            // must stop the lifecycle manager (do the same thing we'd normally do in
            // 'tearDown' ), otherwise following tests will end up in
            // a bad state

            try {
                getUnitTestInjector()
                        .getInstance(UnitTestLifecycleManager.class)
                        .tearDown(this);
            }
            catch (Exception x) {
                // swallow...
            }

            throw e;
        }
    }

    @Override
    protected final void tearDown() throws Exception {

        try {
            tearDownBeforeInjection();
        }
        finally {
            getUnitTestInjector().getInstance(UnitTestLifecycleManager.class).tearDown(
                    this);
        }
    }

    protected void setUpAfterInjection() throws Exception {
        // noop
    }

    protected void tearDownBeforeInjection() throws Exception {
        // noop
    }
}
