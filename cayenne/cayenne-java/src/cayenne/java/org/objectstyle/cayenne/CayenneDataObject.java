/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.property.PropertyUtils;
import org.objectstyle.cayenne.validation.BeanValidationFailure;
import org.objectstyle.cayenne.validation.ValidationFailure;
import org.objectstyle.cayenne.validation.ValidationResult;
import org.objectstyle.cayenne.xml.XMLDecoder;
import org.objectstyle.cayenne.xml.XMLEncoder;
import org.objectstyle.cayenne.xml.XMLSerializable;

/**
 * A default implementation of DataObject interface. It is normally used as a superclass
 * of Cayenne persistent objects.
 * 
 * @author Andrei Adamchik
 */
public class CayenneDataObject implements DataObject, XMLSerializable {

    protected long snapshotVersion = DEFAULT_VERSION;

    protected ObjectId objectId;
    protected transient int persistenceState = PersistenceState.TRANSIENT;
    protected transient ObjectContext objectContext;
    protected Map values = new HashMap();

    /**
     * Returns a DataContext that holds this object. Object becomes assocaiated with a
     * DataContext either when the object is fetched using a query, or when a new object
     * is registered explicitly with a DataContext.
     */
    public DataContext getDataContext() {
        if (objectContext == null || objectContext instanceof DataContext) {
            return (DataContext) objectContext;
        }

        throw new CayenneRuntimeException("ObjectContext is not a DataContext: "
                + objectContext);
    }

    /**
     * Initializes DataObject's persistence context.
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
            pathIndex += tokenized[i].length();

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

    /**
     * @since 1.1
     * @deprecated since 1.2 use 'getObjectContext().prepareForAccess(object)'
     */
    public void resolveFault() {
        if (objectContext != null) {
            objectContext.prepareForAccess(this, null);
        }
    }

    public Object readProperty(String propName) {
        if (objectContext != null) {
            objectContext.prepareForAccess(this, propName);
        }

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
        if (objectContext != null) {
            objectContext.prepareForAccess(this, propName);

            // note how we notify DataContext of change BEFORE the object is actually
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
        List relList = (List) readProperty(relName);

        // call 'recordArcDeleted' AFTER readProperty as readProperty ensures that this
        // object fault is resolved
        getDataContext().getObjectStore().recordArcDeleted(
                this,
                value != null ? value.getObjectId() : null,
                relName);

        relList.remove(value);
        if (persistenceState == PersistenceState.COMMITTED) {
            persistenceState = PersistenceState.MODIFIED;
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
        List list = (List) readProperty(relName);

        // call 'recordArcCreated' AFTER readProperty as readProperty ensures that this
        // object fault is resolved
        getDataContext().getObjectStore().recordArcCreated(
                this,
                value.getObjectId(),
                relName);

        list.add(value);

        if (value != null && setReverse) {
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

        getDataContext().getObjectStore().recordArcCreated(
                this,
                value != null ? value.getObjectId() : null,
                relationshipName);

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

        objectContext.prepareForAccess(this, relationshipName);
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
    protected void willConnect(String relationshipName, DataObject dataObject) {
        // first handle most common case - both objects are in the same
        // DataContext or target is null
        if (dataObject == null
                || this.getObjectContext() == dataObject.getObjectContext()) {
            return;
        }
        else if (this.getObjectContext() == null && dataObject.getObjectContext() != null) {
            dataObject.getDataContext().registerNewObject(this);
        }
        else if (this.getObjectContext() != null && dataObject.getObjectContext() == null) {
            this.getDataContext().registerNewObject(dataObject);
        }
        else {
            throw new CayenneRuntimeException(
                    "Cannot set object as destination of relationship "
                            + relationshipName
                            + " because it is in a different DataContext");
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
                .lookupObjEntity(objectId.getEntityName())
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

        EntityResolver resolver = objectContext.getEntityResolver();
        ObjEntity entity = resolver.lookupObjEntity(objectId.getEntityName());

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
     * @see org.objectstyle.cayenne.DataObject#fetchFinished()
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

        ObjEntity objEntity = getDataContext().getEntityResolver().lookupObjEntity(this);
        if (objEntity == null) {
            throw new CayenneRuntimeException(
                    "No ObjEntity mapping found for DataObject " + getClass().getName());
        }

        DataNode node = getDataContext().getParentDataDomain().lookupDataNode(
                objEntity.getDataMap());
        if (node == null) {
            throw new CayenneRuntimeException("No DataNode found for objEntity: "
                    + objEntity.getName());
        }

        ExtendedTypeMap types = node.getAdapter().getExtendedTypes();

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
            boolean validate = true;
            DbRelationship dbRelationship = (DbRelationship) dbRels.get(0);
            Iterator joins = dbRelationship.getJoins().iterator();
            while (joins.hasNext()) {
                DbJoin join = (DbJoin) joins.next();
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
        EntityResolver er = getDataContext().getEntityResolver();
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
        ObjEntity object = null;

        // TODO: relying on singleton Configuration is a bad idea...
        // Probably decoder itself can optionally store a DataContext or an EntityResolver
        // to provide "context" appropriate for a given environment
        for (Iterator it = Configuration
                .getSharedConfiguration()
                .getDomain()
                .getDataNodes()
                .iterator(); it.hasNext();) {
            DataNode dn = (DataNode) it.next();

            EntityResolver er = dn.getEntityResolver();
            object = er.lookupObjEntity(getClass());

            if (null != object) {
                break;
            }
        }

        for (Iterator it = object.getDeclaredAttributes().iterator(); it.hasNext();) {
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