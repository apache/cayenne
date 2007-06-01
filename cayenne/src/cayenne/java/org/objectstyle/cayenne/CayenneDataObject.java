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
package org.objectstyle.cayenne;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.util.PropertyComparator;
import org.objectstyle.cayenne.validation.BeanValidationFailure;
import org.objectstyle.cayenne.validation.ValidationFailure;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * A default implementation of DataObject interface. It is normally used as a superclass
 * of Cayenne persistent objects.
 * 
 * @author Andrei Adamchik
 */
public class CayenneDataObject implements DataObject {

    protected long snapshotVersion = DEFAULT_VERSION;

    protected ObjectId objectId;
    protected transient int persistenceState = PersistenceState.TRANSIENT;
    protected transient DataContext dataContext;
    protected Map values = new HashMap();

    /**
     * Returns a DataContext that holds this object. Object becomes assocaiated with a
     * DataContext either when the object is fetched using a query, or when a new object
     * is registered explicitly with a DataContext.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    public void setDataContext(DataContext dataContext) {
        this.dataContext = dataContext;

        if (dataContext == null) {
            this.persistenceState = PersistenceState.TRANSIENT;
        }
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    public int getPersistenceState() {
        return persistenceState;
    }

    public void setPersistenceState(int persistenceState) {
        this.persistenceState = persistenceState;

        if (persistenceState == PersistenceState.HOLLOW) {
            values.clear();
        }
    }

    public Object readNestedProperty(String path) {
        Object object = null;
        CayenneDataObject dataObject = this;
        String[] tokenized = tokenizePath(path);
        int length = tokenized.length;

        for (int i = 0; i < length; i++) {

            object = dataObject.readSimpleProperty(tokenized[i]);

            if (object == null) {
                return null;
            }
            else if (object instanceof CayenneDataObject) {
                dataObject = (CayenneDataObject) object;
            }
            else if (i + 1 < length) {
                throw new CayenneRuntimeException("Invalid path: " + path);
            }
        }

        return object;
    }

    private static final String[] tokenizePath(String path) {
        if (path == null) {
            throw new NullPointerException("Null property path.");
        }

        if (path.length() == 0) {
            throw new IllegalArgumentException("Empty property path.");
        }

        // take a shortcut for simple properties
        if (path.indexOf(".") < 0) {
            return new String[] {
                path
            };
        }

        StringTokenizer tokens = new StringTokenizer(path, ".");
        int length = tokens.countTokens();
        String[] tokenized = new String[length];
        for (int i = 0; i < length; i++) {
            tokenized[i] = tokens.nextToken();
        }

        return tokenized;
    }

    private final Object readSimpleProperty(String property) {
        // side effect - resolves HOLLOW object
        Object object = readProperty(property);

        // if a null value is returned, there is still a chance to
        // find a non-persistent property via reflection
        if (object == null && !values.containsKey(property)) {
            try {
                object = PropertyComparator.readProperty(property, this);
            }
            catch (IllegalAccessException e) {
                throw new CayenneRuntimeException("Error reading property '"
                        + property
                        + "'.", e);
            }
            catch (InvocationTargetException e) {
                throw new CayenneRuntimeException("Error reading property '"
                        + property
                        + "'.", e);
            }
            catch (NoSuchMethodException e) {
                // ignoring, no such property exists
            }
        }

        return object;
    }

    /**
     * @since 1.1
     */
    public void resolveFault() {
        if (getPersistenceState() == PersistenceState.HOLLOW && dataContext != null) {
            dataContext.getObjectStore().resolveHollow(this);
            if (getPersistenceState() != PersistenceState.COMMITTED) {
                throw new CayenneRuntimeException(
                        "Error resolving fault, no matching row exists in the database for ObjectId: "
                                + getObjectId());
            }
        }
    }

    public Object readProperty(String propName) {
        resolveFault();

        Object object = readPropertyDirectly(propName);

        // must resolve faults immediately
        if (object instanceof Fault) {
            object = ((Fault) object).resolveFault(this, propName);
            writePropertyDirectly(propName, object);
        }

        return object;
    }

    public Object readPropertyDirectly(String propName) {
        return values.get(propName);
    }

    public void writeProperty(String propName, Object val) {
        resolveFault();

        // 1. retain object snapshot to allow clean changes tracking
        // 2. change object state
        if (persistenceState == PersistenceState.COMMITTED) {
            persistenceState = PersistenceState.MODIFIED;
            dataContext.getObjectStore().retainSnapshot(this);
        }
        // else....
        // other persistence states can't be changed to MODIFIED

        writePropertyDirectly(propName, val);
    }

    public void writePropertyDirectly(String propName, Object val) {
        values.put(propName, val);
    }

    /**
     * @deprecated Since 1.0.1 this method is no longer needed, since
     *             "readProperty(String)" supports to-one dependent targets.
     */
    public DataObject readToOneDependentTarget(String relName) {
        return (DataObject) readProperty(relName);
    }

    public void removeToManyTarget(String relName, DataObject value, boolean setReverse) {

        ObjRelationship relationship = this.getRelationshipNamed(relName);

        if (relationship == null) {
            throw new NullPointerException("Can't find relationship: " + relName);
        }

        // if "setReverse" is false, avoid unneeded processing of flattened relationship
        getDataContext().getObjectStore().objectRelationshipUnset(
                this,
                value,
                relationship,
                setReverse);

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        List relList = (List) readProperty(relName);
        relList.remove(value);
        if (persistenceState == PersistenceState.COMMITTED) {
            persistenceState = PersistenceState.MODIFIED;
        }

        if (value != null && setReverse) {
            unsetReverseRelationship(relName, value);
        }
    }

    public void addToManyTarget(String relName, DataObject value, boolean setReverse) {
        if ((value != null) && (dataContext != value.getDataContext())) {
            throw new CayenneRuntimeException("Cannot add object to relationship "
                    + relName
                    + " because it is in a different DataContext");
        }

        ObjRelationship relationship = this.getRelationshipNamed(relName);
        if (relationship == null) {
            throw new NullPointerException("Can't find relationship: " + relName);
        }

        getDataContext().getObjectStore().objectRelationshipSet(
                this,
                value,
                relationship,
                setReverse);

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        List list = (List) readProperty(relName);
        list.add(value);
        if (persistenceState == PersistenceState.COMMITTED) {
            persistenceState = PersistenceState.MODIFIED;

            // retaining a snapshot here is wasteful, but we have to do this for
            // consistency (see CAY-213)
            dataContext.getObjectStore().retainSnapshot(this);
        }

        if (value != null && setReverse) {
            setReverseRelationship(relName, value);
        }
    }

    /**
     * @deprecated Since 1.0.1 this method is no longer needed, since
     *             "setToOneTarget(String, DataObject, boolean)" supports dependent
     *             targets as well.
     */
    public void setToOneDependentTarget(String relName, DataObject val) {
        setToOneTarget(relName, val, true);
    }

    public void setToOneTarget(
            String relationshipName,
            DataObject value,
            boolean setReverse) {
        if ((value != null) && (dataContext != value.getDataContext())) {
            throw new CayenneRuntimeException(
                    "Cannot set object as destination of relationship "
                            + relationshipName
                            + " because it is in a different DataContext");
        }

        Object oldTarget = readProperty(relationshipName);
        if (oldTarget == value) {
            return;
        }

        ObjRelationship relationship = this.getRelationshipNamed(relationshipName);
        if (relationship == null) {
            throw new NullPointerException("Can't find relationship: " + relationshipName);
        }

        // if "setReverse" is false, avoid unneeded processing of flattened relationship
        getDataContext().getObjectStore().objectRelationshipSet(
                this,
                value,
                relationship,
                setReverse);

        if (setReverse) {
            // unset old reverse relationship
            if (oldTarget instanceof DataObject) {
                unsetReverseRelationship(relationshipName, (DataObject) oldTarget);
            }

            // set new reverse relationship
            if (value != null) {
                setReverseRelationship(relationshipName, value);
            }
        }

        writeProperty(relationshipName, value);
    }

    private ObjRelationship getRelationshipNamed(String relName) {
        return (ObjRelationship) dataContext
                .getEntityResolver()
                .lookupObjEntity(this)
                .getRelationship(relName);
    }

    /**
     * Initializes reverse relationship from object <code>val</code> to this object.
     * 
     * @param relName name of relationship from this object to <code>val</code>.
     */
    protected void setReverseRelationship(String relName, DataObject val) {
        ObjRelationship rel = (ObjRelationship) dataContext
                .getEntityResolver()
                .lookupObjEntity(objectId.getObjClass())
                .getRelationship(relName);
        ObjRelationship revRel = rel.getReverseRelationship();
        if (revRel != null) {
            if (revRel.isToMany())
                val.addToManyTarget(revRel.getName(), this, false);
            else
                val.setToOneTarget(revRel.getName(), this, false);
        }
    }

    /**
     * Removes current object from reverse relationship of object <code>val</code> to
     * this object.
     */
    protected void unsetReverseRelationship(String relName, DataObject val) {
        Class aClass = objectId.getObjClass();
        EntityResolver resolver = dataContext.getEntityResolver();
        ObjEntity entity = resolver.lookupObjEntity(aClass);

        if (entity == null) {
            String className = (aClass != null) ? aClass.getName() : "<null>";
            throw new IllegalStateException("DataObject's class is unmapped: "
                    + className);
        }

        ObjRelationship rel = (ObjRelationship) entity.getRelationship(relName);
        ObjRelationship revRel = rel.getReverseRelationship();
        if (revRel != null) {
            if (revRel.isToMany())
                val.removeToManyTarget(revRel.getName(), this, false);
            else
                val.setToOneTarget(revRel.getName(), null, false);
        }
    }

    /**
     * @deprecated Since 1.1 use
     *             getDataContext().getObjectStore().getSnapshot(this.getObjectId(),
     *             getDataContext())
     */
    public Map getCommittedSnapshot() {
        return dataContext.getObjectStore().getSnapshot(getObjectId(), dataContext);
    }

    /**
     * @deprecated Since 1.1 use getDataContext().currentSnapshot(this)
     */
    public Map getCurrentSnapshot() {
        return dataContext.currentSnapshot(this);
    }

    /**
     * A variation of "toString" method, that may be more efficient in some cases. For
     * example when printing a list of objects into the same String.
     */
    public StringBuffer toStringBuffer(StringBuffer buf, boolean fullDesc) {
        // log all properties
        buf.append('{');

        if (fullDesc)
            appendProperties(buf);

        buf.append("<oid: ").append(objectId).append("; state: ").append(
                PersistenceState.persistenceStateName(persistenceState)).append(">}\n");
        return buf;
    }

    protected void appendProperties(StringBuffer buf) {
        buf.append("[");
        Iterator it = values.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            buf.append('\t').append(key).append(" => ");
            Object val = values.get(key);

            if (val instanceof CayenneDataObject) {
                ((CayenneDataObject) val).toStringBuffer(buf, false);
            }
            else if (val instanceof List) {
                buf.append('(').append(val.getClass().getName()).append(')');
            }
            else
                buf.append(val);

            buf.append('\n');
        }

        buf.append("]");
    }

    public String toString() {
        return toStringBuffer(new StringBuffer(), true).toString();
    }

    /**
     * Default implementation does nothing.
     * 
     * @see org.objectstyle.cayenne.DataObject#fetchFinished()
     */
    public void fetchFinished() {
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(persistenceState);

        switch (persistenceState) {
            //New, modified or transient or deleted - write the whole shebang
            //The other states (committed, hollow) all need just ObjectId
            case PersistenceState.TRANSIENT:
            case PersistenceState.NEW:
            case PersistenceState.MODIFIED:
            case PersistenceState.DELETED:
                out.writeObject(values);
                break;
        }

        out.writeObject(objectId);
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        this.persistenceState = in.readInt();

        switch (persistenceState) {
            case PersistenceState.TRANSIENT:
            case PersistenceState.NEW:
            case PersistenceState.MODIFIED:
            case PersistenceState.DELETED:
                values = (Map) in.readObject();
                break;
            case PersistenceState.COMMITTED:
            case PersistenceState.HOLLOW:
                this.persistenceState = PersistenceState.HOLLOW;
                //props will be populated when required (readProperty called)
                values = new HashMap();
                break;
        }

        this.objectId = (ObjectId) in.readObject();

        // DataContext will be set *IF* the DataContext it came from is also
        // deserialized. Setting of DataContext is handled by the DataContext
        // itself
    }

    /**
     * Returns a version of a DataRow snapshot that was used to create this object.
     * 
     * @since 1.1
     */
    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    /**
     * @since 1.1
     */
    public void setSnapshotVersion(long snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    /**
     * Performs property validation of the object, appending any validation failures to
     * the provided validationResult object. This method is invoked from "validateFor.."
     * before committing a NEW or MODIFIED object to the database. Validation includes
     * checking for null values and value sizes. CayenneDataObject subclasses may override
     * this method, calling super.
     * 
     * @since 1.1
     */
    protected void validateForSave(ValidationResult validationResult) {

        ObjEntity objEntity = getDataContext().getEntityResolver().lookupObjEntity(this);
        ExtendedTypeMap types = getDataContext()
                .lookupDataNode(objEntity.getDataMap())
                .getAdapter()
                .getExtendedTypes();

        // validate mandatory attributes

        // handling a special case - meaningful mandatory FK... defer failures until
        // relationship validation is done... This is just a temporary solution, as
        // handling meaningful keys within the object lifecycle requires something more,
        // namely read/write methods for relationships and direct values should be
        // synchronous with each other..
        Map failedDbAttributes = null;

        Iterator attributes = objEntity.getAttributes().iterator();
        while (attributes.hasNext()) {
            ObjAttribute objAttribute = (ObjAttribute) attributes.next();
            DbAttribute dbAttribute = objAttribute.getDbAttribute();

            Object value = this.readPropertyDirectly(objAttribute.getName());
            if (dbAttribute.isMandatory()) {
                ValidationFailure failure = BeanValidationFailure.validateNotNull(
                        this,
                        objAttribute.getName(),
                        value);

                if (failure != null) {

                    if (failedDbAttributes == null) {
                        failedDbAttributes = new HashMap();
                    }

                    failedDbAttributes.put(dbAttribute.getName(), failure);
                    continue;
                }
            }

            if (value != null) {

                // TODO: should we pass null values for validation as well?
                // if so, class can be obtained from ObjAttribute...

                types.getRegisteredType(value.getClass()).validateProperty(
                        this,
                        objAttribute.getName(),
                        value,
                        dbAttribute,
                        validationResult);
            }
        }

        // validate mandatory relationships
        Iterator relationships = objEntity.getRelationships().iterator();
        while (relationships.hasNext()) {
            ObjRelationship relationship = (ObjRelationship) relationships.next();

            if (relationship.isSourceIndependentFromTargetChange()) {
                continue;
            }

            List dbRels = relationship.getDbRelationships();
            if (dbRels.isEmpty()) {
                // Wha?
                continue;
            }

            // if db relationship is not based on a PK and is based on mandatory
            // attributes, see if we have a target object set
            boolean validate = false;
            DbRelationship dbRelationship = (DbRelationship) dbRels.get(0);
            Iterator joins = dbRelationship.getJoins().iterator();
            while (joins.hasNext()) {
                DbJoin join = (DbJoin) joins.next();
                DbAttribute source = join.getSource();

                if (source.isMandatory()) {
                    validate = true;

                    // clear attribute failures...
                    if (failedDbAttributes != null && !failedDbAttributes.isEmpty()) {
                        failedDbAttributes.remove(source.getName());

                        // loop through all joins if there were previous mandatory
                        // attribute failures.... otherwise we can safely break away
                        if (!failedDbAttributes.isEmpty()) {
                            continue;
                        }
                    }

                    break;
                }
            }

            if (validate) {
                Object value = this.readPropertyDirectly(relationship.getName());
                ValidationFailure failure = BeanValidationFailure.validateNotNull(
                        this,
                        relationship.getName(),
                        value);

                if (failure != null) {
                    validationResult.addFailure(failure);
                    continue;
                }
            }

        }

        // deal with previously found attribute failures...
        if (failedDbAttributes != null && !failedDbAttributes.isEmpty()) {
            Iterator failedAttributes = failedDbAttributes.values().iterator();
            while (failedAttributes.hasNext()) {
                validationResult.addFailure((ValidationFailure) failedAttributes.next());
            }
        }
    }

    /**
     * Calls {@link #validateForSave(ValidationResult)}. CayenneDataObject subclasses may
     * override it providing validation logic that should be executed for the newly
     * created objects before saving them.
     * 
     * @since 1.1
     */
    public void validateForInsert(ValidationResult validationResult) {
        validateForSave(validationResult);
    }

    /**
     * Calls {@link #validateForSave(ValidationResult)}. CayenneDataObject subclasses may
     * override it providing validation logic that should be executed for the modified
     * objects before saving them.
     * 
     * @since 1.1
     */
    public void validateForUpdate(ValidationResult validationResult) {
        validateForSave(validationResult);
    }

    /**
     * This implementation does nothing. CayenneDataObject subclasses may override it
     * providing validation logic that should be executed for the deleted objects before
     * committing them.
     * 
     * @since 1.1
     */
    public void validateForDelete(ValidationResult validationResult) {
        // does nothing
    }
}