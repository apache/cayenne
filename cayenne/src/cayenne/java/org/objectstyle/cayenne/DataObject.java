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

import java.util.Map;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.validation.ValidationResult;

/** 
 * Defines basic methods for a persistent object in Cayenne.
 * 
 * @author Andrei Adamchik
 */
public interface DataObject extends java.io.Serializable {
    public static final long DEFAULT_VERSION = Long.MIN_VALUE;

    /** 
     * Returns a data context this object is registered with, or null
     * if this object has no associated DataContext.
     */
    public DataContext getDataContext();

    /** 
     * Sets object DataContext. 
     */
    public void setDataContext(DataContext ctxt);

    /** 
     * Returns ObjectId for this data object - piece that
     * uniquely identifies this data object for persistence purposes.
     */
    public ObjectId getObjectId();

    /** Sets ObjectId for this data object - piece that uniquely 
     *  identifies this data object for persistence purposes.
     */
    public void setObjectId(ObjectId objectId);

    /** 
     * Returns current persistence state. Acceptable values are defined in
     * {@link PersistenceState} class.
     */
    public int getPersistenceState();

    /** 
     * Modifies persistence state of the object. Acceptable values are defined in
     * {@link PersistenceState} class.
     */
    public void setPersistenceState(int newState);

    /** 
     * Modifies a value of a named property without altering the object state in any way,
     * and without triggering any database operations. This method is intended mostly for internal
     * use by Cayenne framework, and shouldn't be called from the application code. 
     */
    public void writePropertyDirectly(String propertyName, Object val);

    /** 
     * Returns mapped property value as curently stored in the DataObject.
     * Returned value maybe a fault or a real value. This method will not attempt 
     * to resolve faults, or to read unmapped properties.
     */
    public Object readPropertyDirectly(String propertyName);

    /**
     * Returns a value of the property identified by a property path. Supports reading both
     * mapped and unmapped properties. Unmapped properties are accessed in a manner consistent 
     * with JavaBeans specification.
     * 
     * <p>
     * Property path (or nested property) is a 
     * dot-separated path used to traverse object relationships until the final object 
     * is found. If a null object found while traversing path, null is returned. If a 
     * list is encountered in the middle of the path, CayenneRuntimeException is thrown.
     * Unlike {@link #readPropertyDirectly(String)}, this method will resolve an object
     * if it is HOLLOW.
     *
     * <p>Examples:</p>
     * <ul>
     *    <li>Read this object property:<br>
     *    <code>String name = (String)artist.readNestedProperty("name");</code><br><br></li>
     *
     *    <li>Read an object related to this object:<br>
     *    <code>Gallery g = (Gallery)paintingInfo.readNestedProperty("toPainting.toGallery");</code>
     *    <br><br></li>
     *
     *    <li>Read a property of an object related to this object: <br>
     *    <code>String name = (String)painting.readNestedProperty("toArtist.artistName");</code>
     *    <br><br></li>
     *
     *    <li>Read to-many relationship list:<br>
     *    <code>List exhibits = (List)painting.readNestedProperty("toGallery.exhibitArray");</code>
     *    <br><br></li>
     *
     *    <li>Read to-many relationship in the middle of the path <b>(throws exception)</b>:<br>
     *    <code>String name = (String)artist.readNestedProperty("paintingArray.paintingName");</code>
     *   <br><br></li>
     * </ul>
     * 
     * @since 1.0.5
     *
     */
    public Object readNestedProperty(String path);
    
    /**
    * Returns a value of the property identified by propName. Resolves faults if needed.
    * This method can safely be used instead of or in addition to the auto-generated property
    * accessors in subclasses of CayenneDataObject.
    */
    public Object readProperty(String propName);

    /**
    * Sets the property with the name propName to the new value val. Resolves faults if needed
    * This method can safely be used instead of or in addition to the auto-generated property
    * modifiers in subclasses of CayenneDataObject.
    */
    public void writeProperty(String propName, Object val);

    /**
     * @deprecated Since 1.0.1 this method is no longer needed.
     */
    public DataObject readToOneDependentTarget(String relName);

    public void addToManyTarget(String relName, DataObject val, boolean setReverse);

    public void removeToManyTarget(String relName, DataObject val, boolean setReverse);

    public void setToOneTarget(String relName, DataObject val, boolean setReverse);

    /**
     * @deprecated Since 1.0.1 this method is no longer needed, since 
     * "setToOneTarget(String, DataObject, boolean)" supports dependent targets 
     * as well.
     */
    public void setToOneDependentTarget(String relName, DataObject val);

    /**
     * Returns a snapshot for this object corresponding to the state 
     * of the database when object was last fetched or committed. 
     * 
     * @deprecated Since 1.1 use 
     * getDataContext().getObjectStore().getSnapshot(this.getObjectId(), getDataContext())
     */
    public Map getCommittedSnapshot();

    /**
     * Returns a snapshot of object current values.
     * 
     * @deprecated Since 1.1 use getDataContext().currentSnapshot(this)
     */
    public Map getCurrentSnapshot();

    /**
     * Notification method called by DataContext after the object 
     * was read from the database.
     */
    public void fetchFinished();

    /**
     * Returns a version of a DataRow snapshot that was used to 
     * create this object.
     * 
     * @since 1.1
     */
    public long getSnapshotVersion();

    /**
     * @since 1.1
     */
    public void setSnapshotVersion(long snapshotVersion);

    /**
     * Initializes object with data from cache or from the database,
     * if this object is not fully resolved.
     * 
     * @since 1.1
     */
    public void resolveFault();

    /**
     * Performs property validation of the NEW object, appending any validation failures
     * to the provided validationResult object. This method is invoked by DataContext
     * before committing a NEW object to the database.
     * 
     * @since 1.1
     */
    public void validateForInsert(ValidationResult validationResult);

    /**
     * Performs property validation of the MODIFIED object, appending any validation failures
     * to the provided validationResult object. This method is invoked by DataContext
     * before committing a MODIFIED object to the database.
     * 
     * @since 1.1
     */
    public void validateForUpdate(ValidationResult validationResult);

    /**
     * Performs property validation of the DELETED object, appending any validation failures
     * to the provided validationResult object. This method is invoked by DataContext
     * before committing a DELETED object to the database.
     * 
     * @since 1.1
     */
    public void validateForDelete(ValidationResult validationResult);
}
