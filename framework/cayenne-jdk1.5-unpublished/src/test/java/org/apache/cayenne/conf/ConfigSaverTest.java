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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Test cases for DomainHelper class.
 * 
 */
public class ConfigSaverTest extends CayenneCase {
	protected ConfigSaver saver;

	public void testStoreFullDataNode() throws Exception {
		DataSourceInfo info = new DataSourceInfo();
		info.setDataSourceUrl("s1");
		info.setJdbcDriver("s2");
		info.setPassword("s3");
		info.setUserName("s4");
		
		info.setMaxConnections(35);
		info.setMinConnections(22);
		assertSaved(info);
	}
	
	public void testStoreDataNodeNoUserName() throws Exception {
		DataSourceInfo info = new DataSourceInfo();
		info.setDataSourceUrl("s1");
		info.setJdbcDriver("s2");
		info.setPassword("s3");
		
		info.setMaxConnections(35);
		info.setMinConnections(22);
		assertSaved(info);
	}
	
	public void testStoreDataNodeNoPassword() throws Exception {
		DataSourceInfo info = new DataSourceInfo();
		info.setDataSourceUrl("s1");
		info.setJdbcDriver("s2");
		info.setUserName("s4");
		
		info.setMaxConnections(35);
		info.setMinConnections(22);
		assertSaved(info);
	}


	protected void assertSaved(DataSourceInfo info) throws Exception {
		StringWriter str = new StringWriter();
		PrintWriter out = new PrintWriter(str);

	    saver.storeDataNode(out, null, info);

		out.close();
		str.close();

		StringBuffer buf = str.getBuffer();

		// perform assertions
		if (info.getDataSourceUrl() != null) {
			assertTrue(
				"URL not saved: " + info.getDataSourceUrl(),
                    buf.toString().contains("<url value=\"" + info.getDataSourceUrl() + "\"/>"));
		}
		
		if (info.getJdbcDriver() != null) {
			assertTrue(
				"Driver not saved: " + info.getJdbcDriver(),
                    buf.toString().contains("<driver project-version=\"" + Project.CURRENT_PROJECT_VERSION + "\" class=\"" + info.getJdbcDriver() + "\">"));
		}
		
		if (info.getUserName() != null) {
			assertTrue(
				"User name not saved: " + info.getUserName(),
                    buf.toString().contains("userName=\"" + info.getUserName() + "\""));
		}
		
		if (info.getPassword() != null) {
			assertTrue(
				"Password not saved: " + info.getPassword(),
                    buf.toString().contains("password=\"" + info.getPassword() + "\""));
		}
	}
	
    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        saver = new ConfigSaver();
    }
}
