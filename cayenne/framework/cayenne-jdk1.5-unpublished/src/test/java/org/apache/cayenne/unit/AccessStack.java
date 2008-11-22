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

package org.apache.cayenne.unit;

import java.util.Map;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.UnitTestDomain;

/**
 * DataDomain wrapper used for testing a specific Cayenne stack configuration.
 * 
 */
public interface AccessStack {

    AccessStackAdapter getAdapter(DataNode node);

    UnitTestDomain getDataDomain();

    void createTestData(Class<?> testCase, String testName, Map parameters) throws Exception;

    void deleteTestData() throws Exception;

    void dropSchema() throws Exception;

    void createSchema() throws Exception;

    void dropPKSupport() throws Exception;

    void createPKSupport() throws Exception;
}
