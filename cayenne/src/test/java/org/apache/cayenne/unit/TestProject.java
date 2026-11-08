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

package org.apache.cayenne.unit;

import org.apache.cayenne.CayenneRuntime;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataDomainProvider;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.project.Project;

import java.util.Collection;

/**
 * The authored project of a test runtime. The runtime compiles its DataMaps on startup and keeps only the compiled
 * mapping, so the tests that change the authored model and recompile it get the maps from here. Captured by a
 * DataDomain provider bound with {@link #module()}.
 */
public class TestProject {

    private Project project;

    public static Module module() {
        return b -> {
            b.bind(TestProject.class).to(TestProject.class);
            b.bind(DataDomain.class).toProvider(CapturingDataDomainProvider.class);
        };
    }

    public static TestProject of(CayenneRuntime runtime) {
        // the project is captured when the domain is created, which may not have happened yet
        runtime.getDataDomain();
        return runtime.getInjector().getInstance(TestProject.class);
    }

    public Project project() {
        return project;
    }

    public Collection<DataMap> dataMaps() {
        return project.getDataMaps();
    }

    public DataMap dataMap(String name) {
        DataMap map = project.getDataMap(name);
        if (map == null) {
            throw new IllegalArgumentException("Unknown DataMap: " + name);
        }
        return map;
    }

    /**
     * Returns the authored entity a compiled entity was compiled from.
     */
    public ObjEntity objEntity(CompiledEntity entity) {
        return dataMap(entity.map().name()).getObjEntity(entity.name());
    }

    /**
     * Returns the authored entity a compiled table was compiled from.
     */
    public DbEntity dbEntity(CompiledTable table) {
        return dataMap(table.map().name()).getDbEntity(table.name());
    }

    static class CapturingDataDomainProvider extends DataDomainProvider {

        @Inject
        private TestProject testProject;

        @Override
        protected Project loadDescriptor() {
            Project loaded = super.loadDescriptor();
            testProject.project = loaded;
            return loaded;
        }
    }
}
