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

 
package org.apache.cayenne.regression;

import java.util.Properties;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.cayenne.conf.ConnectionProperties;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.unit.CayenneTestResources;

/**
 * @author Andrei Adamchik
 */
public class TestPreferences extends Preferences {
	protected DataSourceInfo connectionInfo;
	
    /**
     * Constructor for TestPreferences.
     * @param props
     */
    public TestPreferences(Properties props) throws Exception {
        super(props);
    }

    protected boolean initProjectFile(ExtendedProperties conf) {
        String connectionName = conf.getString(CayenneTestResources.CONNECTION_NAME_KEY);
        if(connectionName == null) {
        	return false;
        }
        
        connectionInfo =
            ConnectionProperties.getInstance().getConnectionInfo(connectionName);
        return connectionInfo != null;
    }
    /**
     * Returns the connectionInfo.
     * @return DataSourceInfo
     */
    public DataSourceInfo getConnectionInfo() {
        return connectionInfo;
    }
    
    protected boolean init(ExtendedProperties conf) {
        boolean flag = super.init(conf);
        
        // change schema to the login name
        if(schema == null && connectionInfo != null) {
            schema = connectionInfo.getUserName();
        }
        
        return flag;
    }

}
