/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.util.MockDataDomain;
import org.objectstyle.cayenne.unit.util.MockQueryEngine;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * @author Andrei Adamchik
 */
public class ObjectStoreValidationTst extends CayenneTestCase {

    private static int id = 1;

    public void testValidateUncommittedObjects() throws Exception {
        MockupValidatingObject deleted = createObject(PersistenceState.DELETED);
        MockupValidatingObject inserted = createObject(PersistenceState.NEW);
        MockupValidatingObject updated = createObject(PersistenceState.MODIFIED);

        ObjectStore store = new ObjectStore(new DataRowStore("test"));
        store.addObject(deleted);
        store.addObject(inserted);
        store.addObject(updated);

        store.validateUncommittedObjects();

        // validateForSave should not be called on deleted
        assertFalse(deleted.validatedForSave);
        assertTrue(deleted.validatedForDelete);

        assertTrue(inserted.validatedForSave);
        assertTrue(inserted.validatedForInsert);

        assertTrue(updated.validatedForSave);
        assertTrue(updated.validatedForUpdate);
    }

    public void testValidateUncommittedObjectsConcurrency() throws Exception {
        ObjectStore store = new ObjectStore(new DataRowStore("test"));
        DataObject updated1 = createActiveValidatingObject(
                store,
                PersistenceState.MODIFIED);
        DataObject updated2 = createActiveValidatingObject(
                store,
                PersistenceState.MODIFIED);
        DataObject updated3 = createActiveValidatingObject(
                store,
                PersistenceState.MODIFIED);

        store.addObject(updated1);
        store.addObject(updated2);
        store.addObject(updated3);
        store.validateUncommittedObjects();
    }

    private MockupValidatingObject createObject(int state) {
        MockupValidatingObject object = new MockupValidatingObject();
        object.setPersistenceState(state);
        object.setObjectId(new ObjectId(Artist.class, "ARTIST_NAME", id++));
        return object;
    }

    private MockupActiveValidatingObject createActiveValidatingObject(
            ObjectStore objectStore,
            int state) {
        MockupActiveValidatingObject object = new MockupActiveValidatingObject();
        object.setPersistenceState(state);
        object.setDataContext(createMockupDataContext(objectStore));
        object.setObjectId(new ObjectId(Artist.class, "ARTIST_NAME", id++));
        return object;
    }

    private DataContext createMockupDataContext(ObjectStore objectStore) {
        MockQueryEngine qe = new MockQueryEngine();
        qe.setEntityResolver(getDomain().getEntityResolver());
        MockDataDomain dd = new MockDataDomain(qe);
        return new DataContext(dd, objectStore);
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