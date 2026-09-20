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
package org.apache.cayenne.docs;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.commitlog.CommitLogListener;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.docs.commitlog.AuditListener;
import org.apache.cayenne.docs.commitlog.MyEntityFactory;
import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.docs.persistent.Painting;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CommitLogTest {

    /**
     * Commits an Artist and a Painting, and returns the names of the entities that were reported to the listener.
     */
    private List<String> commitAndCollectChanges(Module commitLogModule) {

        List<String> changed = new ArrayList<>();
        CommitLogListener collector = (c, changes) -> changes.getUniqueChanges()
                .forEach(ch -> changed.add(ch.getPostCommitId().getEntityName()));

        DataNodeDescriptor dataNode = DataNodeDescriptor.of("commitlog")
                .dataSource(CayenneDataSource.of("jdbc:hsqldb:mem:commitlog").build())
                .createSchemaIfNeeded()
                .build();

        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig("cayenne-project.xml")
                .defaultDataNode(dataNode)
                .addModule(commitLogModule)
                .addModule(b -> CoreModule.extend(b).addCommitLogListener(collector))
                .build();

        try {
            ObjectContext context = runtime.newContext();
            context.newObject(Artist.class).setName("a1");
            context.newObject(Painting.class).setTitle("p1");
            context.commitChanges();
        } finally {
            runtime.shutdown();
        }

        changed.sort(null);
        return changed;
    }

    @Test
    public void register() {
        // tag::register[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig("cayenne-project.xml")
                .addModule(binder -> CoreModule.extend(binder)
                        .addCommitLogListener(AuditListener.class))
                .build();
        // end::register[]

        assertNotNull(runtime.getDataDomain());
        runtime.shutdown();
    }

    @Test
    public void listener() {
        List<String> changed = commitAndCollectChanges(b -> CoreModule.extend(b)
                .addCommitLogListener(AuditListener.class));

        assertEquals(List.of("Artist", "Painting"), changed);
    }

    @Test
    public void annotationEntitiesOnly() {
        // no entities in the docs model are annotated with @CommitLog
        assertEquals(List.of(), commitAndCollectChanges(this::annotationEntitiesOnly));
    }

    private void annotationEntitiesOnly(Binder binder) {
        // tag::annotationEntitiesOnly[]
        CoreModule.extend(binder)
                .commitLogAnnotationEntitiesOnly()
                .addCommitLogListener(AuditListener.class);
        // end::annotationEntitiesOnly[]
    }

    @Test
    public void excludeFromTransaction() {
        assertEquals(List.of("Artist", "Painting"), commitAndCollectChanges(this::excludeFromTransaction));
    }

    private void excludeFromTransaction(Binder binder) {
        // tag::excludeFromTransaction[]
        CoreModule.extend(binder)
                .excludeCommitLogFromTransaction()
                .addCommitLogListener(AuditListener.class);
        // end::excludeFromTransaction[]
    }

    @Test
    public void entityFactory() {
        assertEquals(List.of("Artist"), commitAndCollectChanges(this::entityFactory));
    }

    private void entityFactory(Binder binder) {
        // tag::entityFactory[]
        CoreModule.extend(binder)
                .commitLogEntityFactory(MyEntityFactory.class)
                .addCommitLogListener(AuditListener.class);
        // end::entityFactory[]
    }
}
