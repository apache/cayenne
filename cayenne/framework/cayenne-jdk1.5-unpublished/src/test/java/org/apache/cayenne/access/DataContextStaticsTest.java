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

package org.apache.cayenne.access;

import java.io.InputStream;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.ResourceLocator;

/**
 * @author Andrus Adamchik
 */
public class DataContextStaticsTest extends CayenneCase {
    protected Configuration savedConfig;

    public void testCreateDataContext1() throws Exception {
        TestConfig conf = new TestConfig(getDomain());
        try {
            DataContext c1 = DataContext.createDataContext();
            assertNotNull(c1);
            assertSame(c1.getParentDataDomain(), getDomain());
            assertTrue(c1 != DataContext.createDataContext());
        } finally {
            conf.restoreConfig();
        }
    }

    public void testCreateDataContext2() throws Exception {
        TestConfig conf = new TestConfig(getDomain());
        try {
            String name = getDomain().getName();
            DataContext c1 = DataContext.createDataContext(name);
            assertNotNull(c1);
            assertSame(c1.getParentDataDomain(), getDomain());
            assertTrue(c1 != DataContext.createDataContext(name));
        } finally {
            conf.restoreConfig();
        }
    }

    class TestConfig extends Configuration {
        protected Configuration savedConfig;

        public TestConfig(DataDomain domain) {
            savedConfig = Configuration.sharedConfiguration;
            Configuration.sharedConfiguration = this;
            addDomain(domain);
        }

        public void restoreConfig() {
            Configuration.sharedConfiguration = savedConfig;
        }

		@Override
        public boolean canInitialize() {
			return true;
		}

		@Override
        public void initialize() throws Exception {
		}

		@Override
        public void didInitialize() {
		}

		@Override
        public ResourceLocator getResourceLocator() {
			return null;
		}

        @Override
        protected InputStream getDomainConfiguration() {
            return null;
        }

        @Override
        protected InputStream getMapConfiguration(String location) {
            return null;
        }
        
        @Override
        protected InputStream getViewConfiguration(String location) {
            return null;
        }
    }

}
