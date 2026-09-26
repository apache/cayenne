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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.GenericPersistentObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.OutParametersResult;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLExec;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.tx.TransactionDescriptor;
import org.apache.cayenne.tx.TransactionPropagation;
import org.apache.cayenne.tx.TransactionalOperation;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ObjectContextTest extends BaseTest {

    private long count() {
        return ObjectSelect.query(Artist.class).selectCount(runtime.newContext());
    }

    @Test
    public void newContext() {
        // tag::newContext[]
        ObjectContext context = runtime.newContext();
        // end::newContext[]

        assertNotNull(context);
    }

    @Test
    public void selectModifyCommit() {
        createArtistsDataSet();

        // tag::select[]
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .select(context);
        // end::select[]

        // tag::modify[]
        Artist selectedArtist = artists.get(0);
        selectedArtist.setName("Dali");
        // end::modify[]

        // tag::commit[]
        context.commitChanges();
        // end::commit[]

        assertEquals(PersistenceState.COMMITTED, selectedArtist.getPersistenceState());
    }

    @Test
    public void rollback() {
        createArtist("Dali", null);

        // tag::rollback[]
        context.rollbackChanges();
        // end::rollback[]

        assertEquals(0, count());
    }

    @Test
    public void execute() {
        createArtistsDataSet();
        Query query = SQLExec.query("UPDATE ARTIST SET NAME = 'Unknown'");

        // tag::execute[]
        List<QueryResult> result = context.execute(query);
        for (QueryResult item : result) {
            switch (item) {
                case SelectResult<?> select -> process(select.objects());
                case UpdateResult update -> process(update.counts());
                case ResultIterator<?> iterator -> process(iterator);
                case OutParametersResult out -> process(out.values());
            }
        }
        // end::execute[]

        assertEquals(1, result.size());
    }

    private void process(Object result) {
    }

    @Test
    public void executeInTransaction() {
        createArtistsDataSet();

        // tag::executeInTransaction[]
        Collection<Query> queries = List.of( // multiple queries that need to be run together
                SQLExec.query("DELETE FROM PAINTING"),
                SQLExec.query("DELETE FROM ARTIST"));

        runtime.performInTransaction(() -> {
            queries.forEach(context::execute);
            return null;
        });
        // end::executeInTransaction[]

        assertEquals(0, count());
    }

    @Test
    public void newObject() {
        // tag::newObject[]
        Artist newArtist = context.newObject(Artist.class);
        newArtist.setName("Picasso");
        // end::newObject[]

        assertEquals(PersistenceState.NEW, newArtist.getPersistenceState());
    }

    @Test
    public void deleteObjects() {
        Artist artist1 = createArtist("a1", null);
        Artist artist2 = createArtist("a2", null);
        Artist artist3 = createArtist("a3", null);
        Artist artist4 = createArtist("a4", null);
        context.commitChanges();

        // tag::deleteObjects[]
        context.deleteObjects(artist1);
        context.deleteObjects(artist2, artist3, artist4);
        // end::deleteObjects[]

        context.commitChanges();
        assertEquals(0, count());
    }

    @Test
    public void localObject() {
        Artist artist = createArtist("a1", null);
        context.commitChanges();

        // tag::localObject[]
        ObjectContext editingContext = runtime.newContext();
        Artist localArtist = editingContext.localObject(artist);
        // end::localObject[]

        assertNotSame(artist, localArtist);
        assertEquals(artist.getObjectId(), localArtist.getObjectId());
    }

    @Test
    public void entityResolver() {
        // tag::entityResolver[]
        EntityResolver resolver = context.getEntityResolver();
        // end::entityResolver[]

        assertNotNull(resolver.getObjEntity("Artist"));
    }

    @Test
    public void pk() {
        SQLExec.query("INSERT INTO ARTIST (ID, NAME) VALUES (34579, 'Dali')").update(context);

        // tag::objectForPK[]
        Artist artist = context.objectForPK(Artist.class, 34579);
        // end::objectForPK[]

        // tag::pkForObject[]
        long pk = Cayenne.longPKForObject(artist);
        // end::pkForObject[]

        assertEquals(34579L, pk);
    }

    @Test
    public void nesting() {
        // tag::nesting[]
        ObjectContext parent = runtime.newContext();
        ObjectContext nested = runtime.newContext(parent);
        // end::nesting[]

        Artist artist = nested.newObject(Artist.class);
        artist.setName("a1");

        // tag::nestedCommit[]
        // merges nested context changes into the parent context
        nested.commitChangesToParent();

        // end::nestedCommit[]
        assertEquals(0, count());
        artist.setName("a1.1");
        // tag::nestedCommit[]
        // regular 'commitChanges' cascades commit through the chain
        // of parent contexts all the way to the database
        nested.commitChanges();
        // end::nestedCommit[]

        assertEquals(1, count());
        artist.setName("a2");

        // tag::nestedRollback[]
        // unrolls all local changes, getting context in a state identical to parent
        nested.rollbackChangesLocally();

        // end::nestedRollback[]
        assertEquals("a1.1", artist.getName());
        artist.setName("a3");
        // tag::nestedRollback[]
        // regular 'rollbackChanges' cascades rollback through the chain of contexts
        // all the way to the topmost parent
        nested.rollbackChanges();
        // end::nestedRollback[]

        assertEquals("a1.1", artist.getName());
    }

    @Test
    public void genericNewObject() {
        // tag::genericNewObject[]
        Persistent generic = context.newObject("GenericEntity");
        // end::genericNewObject[]

        // tag::genericProperties[]
        String name = (String) generic.readProperty("name");
        generic.writeProperty("name", "New Name");
        // end::genericProperties[]

        // tag::genericEntityName[]
        String entityName = generic.getObjectId().getEntityName();
        // end::genericEntityName[]

        context.commitChanges();
        assertEquals("GenericEntity", entityName);

        // tag::genericSelect[]
        ObjectSelect<Persistent> query = ObjectSelect.query(Persistent.class, "GenericEntity");
        // end::genericSelect[]

        assertSame(generic, query.selectOne(context));
    }

    @Test
    public void genericRegisterNewObject() {
        // tag::genericRegisterNewObject[]
        Persistent generic = new GenericPersistentObject();
        generic.setObjectId(ObjectId.of("GenericEntity"));
        context.registerNewObject(generic);
        // end::genericRegisterNewObject[]

        assertEquals(PersistenceState.NEW, generic.getPersistenceState());
    }

    @Test
    public void performInTransaction() {
        ObjectContext context1 = runtime.newContext();
        ObjectContext context2 = runtime.newContext();
        context1.newObject(Artist.class).setName("a1");
        context2.newObject(Artist.class).setName("a2");

        // tag::performInTransaction[]
        Integer result = runtime.performInTransaction(() -> {
            // commit one or more contexts
            context1.commitChanges();
            context2.commitChanges();

            // after changing some objects in context1, commit again
            context1.newObject(Artist.class).setName("a3");
            context1.commitChanges();

            // return an arbitrary result or null if we don't care about the result
            return 5;
        });
        // end::performInTransaction[]

        assertEquals(5, result);
        assertEquals(3, count());
    }

    @Test
    public void threadTransaction() {
        Transaction inTx = runtime.performInTransaction(() -> {
            // tag::threadTransaction[]
            Transaction tx = BaseTransaction.getThreadTransaction();
            // end::threadTransaction[]
            return tx;
        });

        assertNotNull(inTx);
    }

    @Test
    public void transactionDescriptor() {
        TransactionalOperation<Long> transactionalOperation = this::count;

        // tag::transactionDescriptor[]
        TransactionDescriptor descriptor = TransactionDescriptor.builder()
                .isolation(Connection.TRANSACTION_SERIALIZABLE)
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .build();
        runtime.performInTransaction(transactionalOperation, descriptor);
        // end::transactionDescriptor[]
    }
}
