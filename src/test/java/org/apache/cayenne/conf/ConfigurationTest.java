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

package org.apache.cayenne.conf;

import java.io.File;
import java.io.InputStream;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.project.ProjectDataSourceFactory;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.ResourceLocator;

public class ConfigurationTest extends CayenneCase {

    public void testDomain() throws java.lang.Exception {
        Configuration cfg = new Config();

        DataDomain d1 = new DataDomain("d1");
        cfg.addDomain(d1);
        assertSame(d1, cfg.getDomain(d1.getName()));

        cfg.removeDomain(d1.getName());
        assertNull(cfg.getDomain(d1.getName()));
    }
    
	public void testOverrideFactory() throws java.lang.Exception {
		Configuration cfg = new Config();

        assertNull(cfg.getDataSourceFactory());
		ProjectDataSourceFactory factory = new ProjectDataSourceFactory(null);
        cfg.setDataSourceFactory(factory);
        assertSame(factory, cfg.getDataSourceFactory());
	}

	public void testDefaultConfigurationConstructorWithNullName() {
		try {
			new DefaultConfiguration(null);
			fail("expected ConfigurationException!");
		}
		catch (ConfigurationException ex) {
			// OK
		}
	}

	public void testFileConfigurationConstructorWithNullFile() {
		try {
			new FileConfiguration((File)null);
			fail("expected ConfigurationException!");
		}
		catch (ConfigurationException ex) {
			// OK
		}
	}

	public void testFileConfigurationConstructorWithNullName() {
		try {
			new FileConfiguration((String)null);
			fail("expected ConfigurationException!");
		}
		catch (ConfigurationException ex) {
			// OK
		}
	}

    /** Concrete Configuration subclass used for tests. */
    public static class Config extends Configuration {

		public boolean canInitialize() {
			return true;
		}

		public void initialize() throws Exception {
		}

		public void didInitialize() {
		}

		public ResourceLocator getResourceLocator() {
			return null;
		}

		protected InputStream getDomainConfiguration() {
            return null;
        }

		protected InputStream getMapConfiguration(String location) {
            return null;
        }
        
        protected InputStream getViewConfiguration(String location) {
            return null;
        }
    }
}
