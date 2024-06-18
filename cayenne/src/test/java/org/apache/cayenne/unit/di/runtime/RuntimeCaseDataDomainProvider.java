/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.unit.di.runtime;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.UnitTestDomain;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.DataDomainProvider;
import org.apache.cayenne.configuration.runtime.DataNodeFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.unit.UnitDbAdapter;

class RuntimeCaseDataDomainProvider extends DataDomainProvider {

    @Inject
    private UnitDbAdapter unitDbAdapter;

    @Inject
    protected DataNodeFactory dataNodeFactory;

    @Override
    protected DataDomain createDataDomain(String name) {
        return new UnitTestDomain(name);
    }

    @Override
    protected DataDomain createAndInitDataDomain() throws Exception {

        DataDomain domain = super.createAndInitDataDomain();
        DataNode node = null;

        for (DataMap dataMap : domain.getDataMaps()) {

            // add nodes and DataSources dynamically...
            DataNodeDescriptor descriptor = new DataNodeDescriptor(dataMap.getName());

            node = dataNodeFactory.createDataNode(descriptor);

            node.addDataMap(dataMap);

            // tweak procedures for testing...
            for (Procedure proc : dataMap.getProcedures()) {
                unitDbAdapter.tweakProcedure(proc);
            }

            // customizations from SimpleAccessStackAdapter that are not yet
            // ported...
            // those can be done better now

            // node
            // .getAdapter()
            // .getExtendedTypes()
            // .registerType(new StringET1ExtendedType());
            //

            domain.addNode(node);
        }

        if (domain.getDataMaps().size() == 1) {
            domain.setDefaultNode(node);
        }

        return domain;
    }

}
