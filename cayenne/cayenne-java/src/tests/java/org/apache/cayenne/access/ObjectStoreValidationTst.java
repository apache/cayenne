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

import org.apache.art.Artist;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.unit.CayenneTestCase;
import org.apache.cayenne.validation.ValidationResult;

/**
 * @author Andrei Adamchik
 */
public class ObjectStoreValidationTst extends CayenneTestCase {

    private static int id = 1;

    /**
     * @deprecated since 1.2
     */
    public void testValidateUncommittedObjects() {
        MockupValidatingObject deleted = createObject(PersistenceState.DELETED);
        MockupValidatingObject inserted = createObject(PersistenceState.NEW);
        MockupValidatingObject updated = createObject(PersistenceState.MODIFIED);

        ObjectStore store = new ObjectStore(new DataRowStore("test"));
        store.setContext(createDataContext());
        store.recordObjectCreated(deleted);
        store.recordObjectCreated(inserted);
        store.recordObjectCreated(updated);

        store.validateUncommittedObjects();

        // validateForSave should not be called on deleted
        assertFalse(deleted.validatedForSave);
        assertTrue(deleted.validatedForDelete);

        assertTrue(inserted.validatedForSave);
        assertTrue(inserted.validatedForInsert);

        assertTrue(updated.validatedForSave);
        assertTrue(updated.validatedForUpdate);
    }

    /**
     * @deprecated since 1.2
     */
    public void testValidateUncommittedObjectsConcurrency() {
        DataContext context = createDataContext();
        DataObject updated1 = createActiveValidatingObject(
                context,
                PersistenceState.MODIFIED);
        DataObject updated2 = createActiveValidatingObject(
                context,
                PersistenceState.MODIFIED);
        DataObject updated3 = createActiveValidatingObject(
                context,
                PersistenceState.MODIFIED);

        context.getObjectStore().recordObjectCreated(updated1);
        context.getObjectStore().recordObjectCreated(updated2);
        context.getObjectStore().recordObjectCreated(updated3);
        context.getObjectStore().validateUncommittedObjects();
    }

    private MockupValidatingObject createObject(int state) {
        MockupValidatingObject object = new MockupValidatingObject();
        object.setDataContext(createDataContext());
        object.setPersistenceState(state);
        object.setObjectId(new ObjectId("Artist", "ARTIST_NAME", id++));
        return object;
    }

    private MockupActiveValidatingObject createActiveValidatingObject(
            DataContext context,
            int state) {
        MockupActiveValidatingObject object = new MockupActiveValidatingObject();
        object.setPersistenceState(state);
        object.setDataContext(context);
        object.setObjectId(new ObjectId("Artist", "ARTIST_NAME", id++));
        return object;
    }

    class MockupActiveValidatingObject extends CayenneDataObject {

        public void validateForSave(ValidationResult validationResult) {
            // create a new object on validation... this will end up in the ObjectStore
            // so lets see how the ObjectStore can handle this operation during
            // validation....
            getDataContext().createAndRegisterNewObject(Artist.class);
        }
    }

    class MockupValidatingObject extends CayenneDataObject {

        boolean validatedForSave;
        boolean validatedForDelete;
        boolean validatedForInsert;
        boolean validatedForUpdate;

        public void validateForDelete(ValidationResult validationResult) {
            validatedForDelete = true;
            super.validateForDelete(validationResult);
        }

        public void validateForInsert(ValidationResult validationResult) {
            validatedForInsert = true;
            super.validateForInsert(validationResult);
        }

        public void validateForSave(ValidationResult validationResult) {
            validatedForSave = true;
        }

        public void validateForUpdate(ValidationResult validationResult) {
            validatedForUpdate = true;
            super.validateForUpdate(validationResult);
        }
    }
}
