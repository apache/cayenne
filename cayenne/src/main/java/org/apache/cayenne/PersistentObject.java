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

package org.apache.cayenne;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.reflect.ToManyMapProperty;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base implementation of {@link Persistent}, have no assumption about how data is actually stored.
 * Provides implementation of properties declared in Persistent interface.
 * <p>
 * Three variants are currently supported:
 * <ul>
 *  <li> field based storage, e.g. each entity class will directly define fields to store data
 *  <li> {@link Map} based storage, e.g. values will be stored in general Map ({@link GenericPersistentObject})
 *  <li> mixed fields and generic Map to store runtime attributes ({@link HybridPersistentObject})
 * </ul>
 * <p>
 * This class can be used directly as superclass for field-based data objects.
 * <p>
 * To create own implementation of {@link Persistent} with custom field storage logic it is enough
 * to implement {@link #readPropertyDirectly(String)} and {@link #writePropertyDirectly(String, Object)} methods
 * and serialization support if needed (helper methods {@link #writeState(ObjectOutputStream)}
 * and {@link #readState(ObjectInputStream)} are provided).
 * <h1>POJO Note</h1>
 * <p>
 * If having PersistentObject as a superclass presents a problem in an application, source
 * code of this class can be copied verbatim to a custom class generation template.
 * Desired superclass can be set in CayenneModeler.
 * </p>
 *
 * @since 1.2
 */
public abstract class PersistentObject implements Persistent, Validating {

    protected ObjectId objectId;
    protected int persistenceState;
    protected transient ObjectContext objectContext;
    protected long snapshotVersion = DEFAULT_VERSION;

    /**
     * Creates a new transient object.
     */
    public PersistentObject() {
        this.persistenceState = PersistenceState.TRANSIENT;
    }

    public int getPersistenceState() {
        return persistenceState;
    }

    public ObjectContext getObjectContext() {
        return objectContext;
    }

    /**
     * @since 1.2
     */
    @Override
    public void setObjectContext(ObjectContext objectContext) {
        this.objectContext = objectContext;

        if (objectContext == null) {
            this.persistenceState = PersistenceState.TRANSIENT;
        }
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    /**
     * Returns a map key for a given to-many map relationship and a target object.
     *
     * @since 3.0
     */
    protected Object getMapKey(String relationshipName, Object value) {

        EntityResolver resolver = objectContext.getEntityResolver();
        ClassDescriptor descriptor = resolver
                .getClassDescriptor(objectId.getEntityName());

        if (descriptor == null) {
            throw new IllegalStateException("Persistent's entity is unmapped, objectId: "
                    + objectId);
        }

        PropertyDescriptor property = descriptor.getProperty(relationshipName);
        if (property instanceof ToManyMapProperty) {
            return ((ToManyMapProperty) property).getMapKey(value);
        }

        throw new IllegalArgumentException("Relationship '"
                + relationshipName
                + "' is not a to-many Map");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        // return null by default to keep compatibility with pre 4.1 versions
        return null;
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        throw new IllegalArgumentException("Unknown property: " + propName);
    }

    protected void beforePropertyRead(String propName) {
        if (objectContext != null) {
            // will resolve faults ourselves below as checking class descriptors
            // for the "lazyFaulting" flag is inefficient. Passing "false" here to
            // suppress fault processing
            objectContext.prepareForAccess(this, propName, false);
        }
    }

    protected void beforePropertyWrite(String propName, Object oldValue, Object newValue) {
        if (objectContext != null) {
            objectContext.prepareForAccess(this, propName, false);
            objectContext.propertyChanged(this, propName, oldValue, newValue);
        }
    }

    @Override
    public Object readProperty(String propertyName) {
        beforePropertyRead(propertyName);

        Object object = readPropertyDirectly(propertyName);

        if (object instanceof Fault) {
            object = ((Fault) object).resolveFault(this, propertyName);
            writePropertyDirectly(propertyName, object);
        }

        return object;
    }

    /**
     * Returns a value of the property identified by a property path. Supports
     * reading both mapped and unmapped properties. Unmapped properties are
     * accessed in a manner consistent with JavaBeans specification.
     * <p>
     * Property path (or nested property) is a dot-separated path used to
     * traverse object relationships until the final object is found. If a null
     * object found while traversing path, null is returned. If a list is
     * encountered in the middle of the path, CayenneRuntimeException is thrown.
     * Unlike {@link #readPropertyDirectly(String)}, this method will resolve an
     * object if it is HOLLOW.
     * <p>
     * Examples:
     * </p>
     * <ul>
     * <li>Read this object property:<br>
     * <code>String name = (String)artist.readNestedProperty("name");</code><br>
     * <br>
     * </li>
     * <li>Read an object related to this object:<br>
     * <code>Gallery g = (Gallery)paintingInfo.readNestedProperty("toPainting.toGallery");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read a property of an object related to this object: <br>
     * <code>String name = (String)painting.readNestedProperty("toArtist.artistName");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read to-many relationship list:<br>
     * <code>List exhibits = (List)painting.readNestedProperty("toGallery.exhibitArray");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read to-many relationship in the middle of the path:<br>
     * <code>List&lt;String&gt; names = (List&lt;String&gt;)artist.readNestedProperty("paintingArray.paintingName");</code>
     * <br>
     * <br>
     * </li>
     * </ul>
     *
     * @since 1.0.5
     */
    @Override
    public Object readNestedProperty(String path) {
        return readNestedProperty(CayennePath.of(path));
    }

    /**
     * @inheritDoc
     */
    @Override
    public Object readNestedProperty(CayennePath path) {
        if ((null == path) || path.isEmpty()) {
            throw new IllegalArgumentException("the path must be supplied in order to lookup a nested property");
        }

        String firstSegment = path.first().value();
        Object property = readSimpleProperty(firstSegment);
        if (property == null) {
            return null;
        }

        if (path.length() == 1) {
            return property;
        }

        CayennePath pathRemainder = path.tail(1);
        if (property instanceof Persistent) {
            return ((Persistent) property).readNestedProperty(pathRemainder);
        } else {
            return Cayenne.readNestedProperty(property, pathRemainder);
        }
    }

    Object readSimpleProperty(String property) {

        // side effect - resolves HOLLOW object
        Object object = readProperty(property);

        // if a null value is returned, there is still a chance to
        // find a non-persistent property via reflection
        if (object == null) {
            object = PropertyUtils.getProperty(this, property);
        }

        return object;
    }

    @Override
    public void writeProperty(String propName, Object val) {
        Object oldValue = readPropertyDirectly(propName);
        beforePropertyWrite(propName, oldValue, val);

        writePropertyDirectly(propName, val);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeToManyTarget(String relName, Persistent value, boolean setReverse) {

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        Object holder = readProperty(relName);

        // call 'propertyChanged' AFTER readProperty as readProperty ensures
        // that this object fault is resolved
        objectContext.propertyChanged(this, relName, value, null);

        // TODO: andrus 8/20/2007 - can we optimize this somehow, avoiding type checking??
        if (holder instanceof Collection) {
            ((Collection<Object>) holder).remove(value);
        } else if (holder instanceof Map) {
            ((Map<Object, Object>) holder).remove(getMapKey(relName, value));
        }

        if (value != null && setReverse) {
            unsetReverseRelationship(relName, value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addToManyTarget(String relName, Persistent value, boolean setReverse) {
        if (value == null) {
            throw new NullPointerException("Attempt to add null target Persistent.");
        }

        willConnect(relName, value);

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        Object holder = readProperty(relName);

        // call 'propertyChanged' AFTER readProperty as readProperty ensures
        // that this object fault is resolved
        objectContext.propertyChanged(this, relName, null, value);

        // TODO: andrus 8/20/2007 - can we optimize this somehow, avoiding type checking??
        if (holder instanceof Collection) {
            ((Collection<Object>) holder).add(value);
        } else if (holder instanceof Map) {
            ((Map<Object, Object>) holder).put(getMapKey(relName, value), value);
        }

        if (setReverse) {
            setReverseRelationship(relName, value);
        }
    }

    /**
     * Sets the relationships to the specified <code>Persistent</code> objects.
     *
     * <p>
     * New relationships will be created with
     * {@link #addToManyTarget(String, org.apache.cayenne.Persistent, boolean)},
     * already established relationships stay untouched. Missing relationships
     * will be removed with
     * {@link #removeToManyTarget(String, org.apache.cayenne.Persistent, boolean)}
     * and returned as List. You may delete them manually.
     * </p>
     *
     * <p>
     * Notice: Moving an object relationship to another object, is still needing
     * an manually "unregister" from the first object by
     * {@link #removeToManyTarget(String, org.apache.cayenne.Persistent, boolean)}
     * </p>
     *
     * @param relName    name of the relation
     * @param values     <code>Persistent</code> objects of this
     *                   <code>Collection</code> are set to the object. No changes will
     *                   be made to the the <code>Collection</code>, a copy is used. It
     *                   is safe to pass a persisted <code>Collection</code> of another
     *                   object.
     * @param setReverse update reverse relationships
     * @return <code>List&lt;? extends Persistent&gt;</code> of unrelated Persistent objects.
     * If no relationship was removed an empty List is returned.
     * @throws IllegalArgumentException      if no relationship could be read by relName, or if the passed
     *                                       <code>Collection</code> is null. To clear all relationships
     *                                       use an empty <code>Collection</code>
     * @throws UnsupportedOperationException if the relation Collection Type is neither
     *                                       <code>java.util.Collection</code> nor
     *                                       <code>java.util.Map</code>
     * @since 4.0
     */
    @SuppressWarnings("unchecked")
    public List<? extends Persistent> setToManyTarget(String relName, Collection<? extends Persistent> values,
                                                      boolean setReverse) {
        if (values == null) {
            throw new IllegalArgumentException("values Collection is null. To clear all relationships use an empty Collection");
        }

        Object property = readProperty(relName);
        if (property == null) {
            throw new IllegalArgumentException("unknown relName " + relName);
        }

        Collection<Persistent> old;
        if (property instanceof Map) {
            old = ((Map<?, Persistent>) property).values();
        } else if (property instanceof Collection) {
            old = (Collection<Persistent>) property;
        } else {
            throw new UnsupportedOperationException("setToManyTarget operates only with Map or Collection types");
        }

        // operate on a copy of passed collection
        values = new ArrayList<>(values);

        List<Persistent> removedObjects = new ArrayList<>();

        // remove all relationships, which are missing in passed collection
        Persistent[] oldValues = old.toArray(new Persistent[0]);
        for (Persistent obj : oldValues) {
            if (!values.contains(obj)) {
                removeToManyTarget(relName, obj, setReverse);
                // collect objects whose relationship was removed
                removedObjects.add(obj);
            }
        }

        // don't add elements which are already present
        for (Persistent obj : old) {
            values.remove(obj);
        }

        // add new elements
        for (Persistent obj : values) {
            addToManyTarget(relName, obj, setReverse);
        }

        return removedObjects;
    }

    @Override
    public void setToOneTarget(String relationshipName, Persistent value, boolean setReverse) {

        willConnect(relationshipName, value);

        Object oldTarget = readProperty(relationshipName);
        if (oldTarget == value) {
            return;
        }

        getObjectContext().propertyChanged(this, relationshipName, oldTarget, value);

        if (setReverse) {
            // unset old reverse relationship
            if (oldTarget instanceof Persistent) {
                unsetReverseRelationship(relationshipName, (Persistent) oldTarget);
            }

            // set new reverse relationship
            if (value != null) {
                setReverseRelationship(relationshipName, value);
            }
        }

        // readProperty will call this
        //objectContext.prepareForAccess(this, relationshipName, false);
        writePropertyDirectly(relationshipName, value);
    }

    /**
     * Called before establishing a relationship with another object. Applies
     * "persistence by reachability" logic, pulling one of the two objects to a
     * DataConext of another object in case one of the objects is transient. If
     * both objects are persistent, and they don't have the same DataContext,
     * CayenneRuntimeException is thrown.
     *
     * @since 1.2
     */
    protected void willConnect(String relationshipName, Persistent object) {
        // first handle most common case - both objects are in the same
        // ObjectContext or target is null
        if (object == null || this.getObjectContext() == object.getObjectContext()) {
            return;
        } else if (this.getObjectContext() == null && object.getObjectContext() != null) {
            object.getObjectContext().registerNewObject(this);
        } else if (this.getObjectContext() != null && object.getObjectContext() == null) {
            this.getObjectContext().registerNewObject(object);
        } else {
            throw new CayenneRuntimeException("Cannot set object as destination of relationship %s"
                    + " because it is in a different ObjectContext", relationshipName);
        }
    }

    /**
     * Initializes reverse relationship from object <code>val</code> to this
     * object.
     *
     * @param relName name of relationship from this object to <code>val</code>.
     */
    protected void setReverseRelationship(String relName, Persistent val) {
        ObjRelationship rel = objectContext.getEntityResolver().getObjEntity(objectId.getEntityName())
                .getRelationship(relName);
        ObjRelationship revRel = rel.getReverseRelationship();
        if (revRel != null) {
            Object oldTarget = val.readProperty(revRel.getName());
            if (oldTarget != this && oldTarget instanceof Persistent && val instanceof PersistentObject) {
                ((PersistentObject)val).unsetReverseRelationship(revRel.getName(), (Persistent) oldTarget);
            }
            if (revRel.isToMany()) {
                val.addToManyTarget(revRel.getName(), this, false);
            } else {
                val.setToOneTarget(revRel.getName(), this, false);
            }
        }
    }

    /**
     * Removes current object from reverse relationship of object
     * <code>val</code> to this object.
     */
    protected void unsetReverseRelationship(String relName, Persistent val) {

        EntityResolver resolver = objectContext.getEntityResolver();
        ObjEntity entity = resolver.getObjEntity(objectId.getEntityName());

        if (entity == null) {
            throw new IllegalStateException("Persistent's entity is unmapped, objectId: " + objectId);
        }

        ObjRelationship rel = entity.getRelationship(relName);
        ObjRelationship revRel = rel.getReverseRelationship();
        if (revRel != null) {
            if (revRel.isToMany()) {
                val.removeToManyTarget(revRel.getName(), this, false);
            } else {
                val.setToOneTarget(revRel.getName(), null, false);
            }
        }
    }

    @Override
    public void setPersistenceState(int persistenceState) {
        this.persistenceState = persistenceState;
    }

    /**
     * @since 1.1
     */
    @Override
    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    /**
     * @since 1.1
     */
    @Override
    public void setSnapshotVersion(long snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    /**
     * Performs property validation of the object, appending any validation
     * failures to the provided validationResult object. This method is invoked
     * from "validateFor.." before committing a NEW or MODIFIED object to the
     * database. Validation includes checking for null values and value sizes.
     * PersistentObject subclasses may override this method, calling super.
     *
     * @since 1.1
     */
    protected void validateForSave(ValidationResult validationResult) {

        ObjEntity objEntity = getObjectContext().getEntityResolver().getObjEntity(this);
        if (objEntity == null) {
            throw new CayenneRuntimeException("No ObjEntity mapping found for Persistent %s", getClass().getName());
        }

        // validate mandatory attributes

        Map<String, ValidationFailure> failedDbAttributes = null;

        for (ObjAttribute next : objEntity.getAttributes()) {
            // TODO: andrus, 2/20/2007 - handle embedded attribute
            if (next instanceof EmbeddedAttribute) {
                continue;
            }

            DbAttribute dbAttribute = next.getDbAttribute();
            if (dbAttribute == null) {
                throw new CayenneRuntimeException("ObjAttribute '%s"
                        + "' does not have a corresponding DbAttribute", next.getName());
            }

            // pk may still be generated
            if (dbAttribute.isPrimaryKey()) {
                continue;
            }

            Object value = this.readPropertyDirectly(next.getName());
            if (dbAttribute.isMandatory()) {
                ValidationFailure failure = BeanValidationFailure.validateNotNull(this, next.getName(), value);

                if (failure != null) {

                    if (failedDbAttributes == null) {
                        failedDbAttributes = new HashMap<>();
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
                        String message = "\"" + next.getName() + "\" exceeds maximum allowed length ("
                                + dbAttribute.getMaxLength() + " bytes): " + len;
                        validationResult.addFailure(new BeanValidationFailure(this, next.getName(), message));
                    }
                } else if (value instanceof CharSequence) {
                    int len = ((CharSequence) value).length();
                    if (len > dbAttribute.getMaxLength()) {
                        String message = "\"" + next.getName() + "\" exceeds maximum allowed length ("
                                + dbAttribute.getMaxLength() + " chars): " + len;
                        validationResult.addFailure(new BeanValidationFailure(this, next.getName(), message));
                    }
                }
            }
        }

        // validate mandatory relationships
        for (final ObjRelationship relationship : objEntity.getRelationships()) {

            List<DbRelationship> dbRels = relationship.getDbRelationships();
            if (dbRels.isEmpty()) {
                continue;
            }

            // skip db relationships that we can't validate or that can't be invalid here
            // can't handle paths longer than two db relationships
            // see ObjRelationship.recalculateReadOnlyValue() for more info
            if (relationship.isSourceIndependentFromTargetChange()) {
                continue;
            }

            // if db relationship is not based on a PK and is based on mandatory
            // attributes, see if we have a target object set
            // relationship will be validated only if all db path has mandatory
            // db relationships
            boolean validate = true;
            for (DbRelationship dbRelationship : dbRels) {
                for (DbJoin join : dbRelationship.getJoins()) {
                    DbAttribute source = join.getSource();
                    if (source.isMandatory()) {
                        // clear attribute failures...
                        if (failedDbAttributes != null && !failedDbAttributes.isEmpty()) {
                            failedDbAttributes.remove(source.getName());
                        }
                    } else {
                        // do not validate if the relation is based on
                        // multiple keys with some that can be nullable.
                        validate = false;
                    }
                }
            }

            if (validate) {
                Object value = this.readPropertyDirectly(relationship.getName());
                ValidationFailure failure = BeanValidationFailure.validateNotNull(this, relationship.getName(), value);

                if (failure != null) {
                    validationResult.addFailure(failure);
                }
            }
        }

        // deal with previously found attribute failures...
        if (failedDbAttributes != null && !failedDbAttributes.isEmpty()) {
            for (ValidationFailure failure : failedDbAttributes.values()) {
                validationResult.addFailure(failure);
            }
        }
    }

    /**
     * Calls {@link #validateForSave(ValidationResult)}. PersistentObject
     * subclasses may override it providing validation logic that should be
     * executed for the newly created objects before saving them.
     *
     * @since 1.1
     */
    @Override
    public void validateForInsert(ValidationResult validationResult) {
        validateForSave(validationResult);
    }

    /**
     * Calls {@link #validateForSave(ValidationResult)}. PersistentObject
     * subclasses may override it providing validation logic that should be
     * executed for the modified objects before saving them.
     *
     * @since 1.1
     */
    @Override
    public void validateForUpdate(ValidationResult validationResult) {
        validateForSave(validationResult);
    }

    /**
     * This implementation does nothing. PersistentObject subclasses may
     * override it providing validation logic that should be executed for the
     * deleted objects before committing them.
     *
     * @since 1.1
     */
    @Override
    public void validateForDelete(ValidationResult validationResult) {
        // does nothing
    }

    /**
     * Serialization support.
     * Will write down persistenceState and objectId, delegating data serialization down to sub-classes.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    /**
     * Serialization support.
     * Will read persistenceState and objectId, delegating data serialization down to sub-classes.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    protected void writeSerialized(ObjectOutputStream out) throws IOException {
        out.writeInt(persistenceState);
        out.writeObject(objectId);

        if (persistenceState == PersistenceState.COMMITTED
                || persistenceState == PersistenceState.HOLLOW) {
            return;
        }

        writeState(out);
    }

    protected void readSerialized(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.persistenceState = in.readInt();
        this.objectId = (ObjectId) in.readObject();

        if (persistenceState == PersistenceState.COMMITTED
                || persistenceState == PersistenceState.HOLLOW) {
            persistenceState = PersistenceState.HOLLOW;
            return;
        }

        readState(in);
    }

    protected void writeState(ObjectOutputStream out) throws IOException {
        // no additional info for base class
    }

    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // no additional info for base class
    }

    /**
     * A variation of "toString" method, that may be more efficient in some
     * cases. For example when printing a list of objects into the same String.
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
    }

    @Override
    public String toString() {
        return toStringBuffer(new StringBuffer(), true).toString();
    }
}
