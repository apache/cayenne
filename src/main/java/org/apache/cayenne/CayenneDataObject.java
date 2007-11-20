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

package org.apache.cayenne;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.reflect.ToManyMapProperty;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.apache.cayenne.xml.XMLDecoder;
import org.apache.cayenne.xml.XMLEncoder;
import org.apache.cayenne.xml.XMLSerializable;

/**
 * A default implementation of DataObject interface. It is normally used as a superclass
 * of Cayenne persistent objects.
 * 
 * @author Andrus Adamchik
 */
public class CayenneDataObject implements DataObject, Validating, XMLSerializable {

    protected long snapshotVersion = DEFAULT_VERSION;

    protected ObjectId objectId;
    protected transient int persistenceState = PersistenceState.TRANSIENT;
    protected transient ObjectContext objectContext;
    protected Map<String, Object> values = new HashMap<String, Object>();

    /**
     * Returns a DataContext that holds this object. Object becomes associated with a
     * DataContext either when the object is fetched using a query, or when a new object
     * is registered explicitly with a DataContext.
     * 
     * @deprecated since 3.0 use {@link #getObjectContext()}.
     */
    public DataContext getDataContext() {
        if (objectContext == null || objectContext instanceof DataContext) {
            return (DataContext) objectContext;
        }

        throw new CayenneRuntimeException("ObjectContext is not a DataContext: "
                + objectContext);
    }

    /**
     * Sets object DataContext.
     * 
     * @deprecated since 3.0 use {@link #setObjectContext(ObjectContext)}.
     */
    public void setDataContext(DataContext dataContext) {
        this.objectContext = dataContext;

        if (dataContext == null) {
            this.persistenceState = PersistenceState.TRANSIENT;
        }
    }

    /**
     * Returns mapped ObjEntity for this object. If an object is transient or is not
     * mapped returns null.
     * 
     * @since 1.2
     */
    public ObjEntity getObjEntity() {
        return (getObjectContext() != null) ? getObjectContext()
                .getEntityResolver()
                .lookupObjEntity(this) : null;
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

        int pathIndex = 0;

        for (int i = 0; i < length; i++) {
            pathIndex += tokenized[i].length() + 1;

            object = dataObject.readSimpleProperty(tokenized[i]);

            if (object == null) {
                return null;
            }
            else if (object instanceof CayenneDataObject) {
                dataObject = (CayenneDataObject) object;
            }
            else if (i + 1 < length) {
                // read the rest of the path via introspection
                return PropertyUtils.getProperty(object, path.substring(pathIndex));
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
            object = PropertyUtils.getProperty(this, property);
        }

        return object;
    }

    public Object readProperty(String propertyName) {
        if (objectContext != null) {
            // will resolve faults ourselves below as checking class descriptors for the
            // "lazyFaulting" flag is inefficient. Passing "false" here to suppress fault
            // processing
            objectContext.prepareForAccess(this, propertyName, false);
        }

        Object object = readPropertyDirectly(propertyName);

        if (object instanceof Fault) {
            object = ((Fault) object).resolveFault(this, propertyName);
            writePropertyDirectly(propertyName, object);
        }

        return object;
    }

    public Object readPropertyDirectly(String propName) {
        return values.get(propName);
    }

    public void writeProperty(String propName, Object val) {
        if (objectContext != null) {
            // pass "false" to avoid unneeded fault processing
            objectContext.prepareForAccess(this, propName, false);

            // note how we notify ObjectContext of change BEFORE the object is actually
            // changed... this is needed to take a valid current snapshot
            Object oldValue = readPropertyDirectly(propName);
            objectContext.propertyChanged(this, propName, oldValue, val);
        }

        writePropertyDirectly(propName, val);
    }

    public void writePropertyDirectly(String propName, Object val) {
        values.put(propName, val);
    }

    public void removeToManyTarget(String relName, DataObject value, boolean setReverse) {

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        Object holder = readProperty(relName);

        // call 'propertyChanged' AFTER readProperty as readProperty ensures that this
        // object fault is resolved
        getObjectContext().propertyChanged(this, relName, value, null);

        // TODO: andrus 8/20/2007 - can we optimize this somehow, avoiding type checking??
        if (holder instanceof Collection) {
            ((Collection) holder).remove(value);
        }
        else if (holder instanceof Map) {
            ((Map) holder).remove(getMapKey(relName, value));
        }

        if (value != null && setReverse) {
            unsetReverseRelationship(relName, value);
        }
    }

    public void addToManyTarget(String relName, DataObject value, boolean setReverse) {
        if (value == null) {
            throw new NullPointerException("Attempt to add null target DataObject.");
        }

        willConnect(relName, value);

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        Object holder = readProperty(relName);

        // call 'propertyChanged' AFTER readProperty as readProperty ensures that this
        // object fault is resolved
        getObjectContext().propertyChanged(this, relName, null, value);

        // TODO: andrus 8/20/2007 - can we optimize this somehow, avoiding type checking??
        if (holder instanceof Collection) {
            ((Collection) holder).add(value);
        }
        else if (holder instanceof Map) {
            ((Map) holder).put(getMapKey(relName, value), value);
        }

        if (setReverse) {
            setReverseRelationship(relName, value);
        }
    }

    public void setToOneTarget(
            String relationshipName,
            DataObject value,
            boolean setReverse) {

        willConnect(relationshipName, value);

        Object oldTarget = readProperty(relationshipName);
        if (oldTarget == value) {
            return;
        }

        getObjectContext().propertyChanged(this, relationshipName, oldTarget, value);

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

        objectContext.prepareForAccess(this, relationshipName, false);
        writePropertyDirectly(relationshipName, value);
    }

    /**
     * Called before establishing a relationship with another object. Applies "persistence
     * by reachability" logic, pulling one of the two objects to a DataConext of another
     * object in case one of the objects is transient. If both objects are persistent, and
     * they don't have the same DataContext, CayenneRuntimeException is thrown.
     * 
     * @since 1.2
     */
    protected void willConnect(String relationshipName, Persistent object) {
        // first handle most common case - both objects are in the same
        // ObjectContext or target is null
        if (object == null || this.getObjectContext() == object.getObjectContext()) {
            return;
        }
        else if (this.getObjectContext() == null && object.getObjectContext() != null) {
            object.getObjectContext().registerNewObject(this);
        }
        else if (this.getObjectContext() != null && object.getObjectContext() == null) {
            this.getObjectContext().registerNewObject(object);
        }
        else {
            throw new CayenneRuntimeException(
                    "Cannot set object as destination of relationship "
                            + relationshipName
                            + " because it is in a different ObjectContext");
        }
    }

    /**
     * Initializes reverse relationship from object <code>val</code> to this object.
     * 
     * @param relName name of relationship from this object to <code>val</code>.
     */
    protected void setReverseRelationship(String relName, DataObject val) {
        ObjRelationship rel = (ObjRelationship) objectContext
                .getEntityResolver()
                .getObjEntity(objectId.getEntityName())
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
     * Returns a map key for a given object and relationship.
     * 
     * @since 3.0
     */
    private Object getMapKey(String relationshipName, Object value) {

        EntityResolver resolver = objectContext.getEntityResolver();
        ClassDescriptor descriptor = resolver
                .getClassDescriptor(objectId.getEntityName());

        if (descriptor == null) {
            throw new IllegalStateException("DataObject's entity is unmapped, objectId: "
                    + objectId);
        }

        Property property = descriptor.getProperty(relationshipName);
        if (property instanceof ToManyMapProperty) {
            return ((ToManyMapProperty) property).getMapKey(value);
        }

        throw new IllegalArgumentException("Relationship '"
                + relationshipName
                + "' is not a to-many Map");
    }

    /**
     * Removes current object from reverse relationship of object <code>val</code> to
     * this object.
     */
    protected void unsetReverseRelationship(String relName, DataObject val) {

        EntityResolver resolver = objectContext.getEntityResolver();
        ObjEntity entity = resolver.getObjEntity(objectId.getEntityName());

        if (entity == null) {
            throw new IllegalStateException("DataObject's entity is unmapped, objectId: "
                    + objectId);
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
     * A variation of "toString" method, that may be more efficient in some cases. For
     * example when printing a list of objects into the same String.
     */
    public StringBuffer toStringBuffer(StringBuffer buffer, boolean fullDesc) {
        String id = (objectId != null) ? objectId.toString() : "<no id>";
        String state = PersistenceState.persistenceStateName(persistenceState);

        buffer.append('{').append(id).append("; ").append(state).append("; ");

        if (fullDesc) {
            appendProperties(buffer);
        }

        buffer.append("}");
        return buffer;
    }

    protected void appendProperties(StringBuffer buffer) {
        buffer.append("[");
        Iterator it = values.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            buffer.append(entry.getKey()).append("=>");
            Object value = entry.getValue();

            if (value instanceof Persistent) {
                buffer.append('{').append(((Persistent) value).getObjectId()).append('}');
            }
            else if (value instanceof Collection) {
                buffer.append("(..)");
            }
            else if (value instanceof Fault) {
                buffer.append('?');
            }
            else {
                buffer.append(value);
            }

            if (it.hasNext()) {
                buffer.append("; ");
            }
        }

        buffer.append("]");
    }

    public String toString() {
        return toStringBuffer(new StringBuffer(), true).toString();
    }

    /**
     * Default implementation does nothing.
     * 
     * @deprecated since 3.0 use callbacks.
     * @see LifecycleListener
     */
    public void fetchFinished() {
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(persistenceState);

        switch (persistenceState) {
            // New, modified or transient or deleted - write the whole shebang
            // The other states (committed, hollow) all need just ObjectId
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
                // props will be populated when required (readProperty called)
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

        ObjEntity objEntity = getObjectContext()
                .getEntityResolver()
                .lookupObjEntity(this);
        if (objEntity == null) {
            throw new CayenneRuntimeException(
                    "No ObjEntity mapping found for DataObject " + getClass().getName());
        }

        // validate mandatory attributes

        // handling a special case - meaningful mandatory FK... defer failures until
        // relationship validation is done... This is just a temporary solution, as
        // handling meaningful keys within the object lifecycle requires something more,
        // namely read/write methods for relationships and direct values should be
        // synchronous with each other..
        Map failedDbAttributes = null;

        Iterator attributes = objEntity.getAttributes().iterator();
        while (attributes.hasNext()) {
            Object next = attributes.next();

            // TODO: andrus, 2/20/2007 - handle embedded attribute
            if (!(next instanceof ObjAttribute)) {
                continue;
            }

            ObjAttribute objAttribute = (ObjAttribute) next;
            DbAttribute dbAttribute = objAttribute.getDbAttribute();

            // pk may still be generated
            if (dbAttribute.isPrimaryKey()) {
                continue;
            }

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

            // validate length
            if (value != null && dbAttribute.getMaxLength() > 0) {

                if (value.getClass().isArray()) {
                    int len = Array.getLength(value);
                    if (len > dbAttribute.getMaxLength()) {
                        String message = "\""
                                + objAttribute.getName()
                                + "\" exceeds maximum allowed length ("
                                + dbAttribute.getMaxLength()
                                + " bytes): "
                                + len;
                        validationResult.addFailure(new BeanValidationFailure(
                                this,
                                objAttribute.getName(),
                                message));
                    }
                }
                else if (value instanceof CharSequence) {
                    int len = ((CharSequence) value).length();
                    if (len > dbAttribute.getMaxLength()) {
                        String message = "\""
                                + objAttribute.getName()
                                + "\" exceeds maximum allowed length ("
                                + dbAttribute.getMaxLength()
                                + " chars): "
                                + len;
                        validationResult.addFailure(new BeanValidationFailure(
                                this,
                                objAttribute.getName(),
                                message));
                    }
                }
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
            boolean validate = true;
            DbRelationship dbRelationship = (DbRelationship) dbRels.get(0);
            for (DbJoin join : dbRelationship.getJoins()) {
                DbAttribute source = join.getSource();

                if (source.isMandatory()) {
                    // clear attribute failures...
                    if (failedDbAttributes != null && !failedDbAttributes.isEmpty()) {
                        failedDbAttributes.remove(source.getName());

                        // loop through all joins if there were previous mandatory

                        // attribute failures....
                        if (!failedDbAttributes.isEmpty()) {
                            continue;
                        }
                    }
                }
                else {
                    // do not validate if the relation is based on
                    // multiple keys with some that can be nullable.
                    validate = false;
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

    /**
     * Encodes object to XML using provided encoder.
     * 
     * @since 1.2
     */
    public void encodeAsXML(XMLEncoder encoder) {
        EntityResolver er = getObjectContext().getEntityResolver();
        ObjEntity object = er.lookupObjEntity(getClass());

        String[] fields = this.getClass().getName().split("\\.");
        encoder.setRoot(fields[fields.length - 1], this.getClass().getName());

        for (Iterator it = object.getDeclaredAttributes().iterator(); it.hasNext();) {
            ObjAttribute att = (ObjAttribute) it.next();
            String name = att.getName();
            encoder.encodeProperty(name, readNestedProperty(name));
        }
    }

    public void decodeFromXML(XMLDecoder decoder) {

        // TODO: (andrus, long time ago) relying on singleton Configuration and a single
        // DataDomain is a very bad idea... Decoder itself can optionally store a
        // DataContext or an EntityResolver to provide the context appropriate for a given
        // environment
        EntityResolver resolver = Configuration
                .getSharedConfiguration()
                .getDomain()
                .getEntityResolver();
        ObjEntity objectEntity = resolver.lookupObjEntity(getClass());

        for (Iterator it = objectEntity.getDeclaredAttributes().iterator(); it.hasNext();) {
            ObjAttribute att = (ObjAttribute) it.next();
            String name = att.getName();
            writeProperty(name, decoder.decodeObject(name));
        }
    }

    /**
     * Returns this object's DataContext.
     * 
     * @since 1.2
     */
    public ObjectContext getObjectContext() {
        return objectContext;
    }

    /**
     * @since 1.2
     */
    public void setObjectContext(ObjectContext objectContext) {
        this.objectContext = objectContext;

        if (objectContext == null) {
            this.persistenceState = PersistenceState.TRANSIENT;
        }
    }
}
