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

import java.io.Serializable;

/**
 * Defines minimal API of an object that can be persisted via Cayenne.
 *
 * @since 1.2
 */
public interface Persistent extends Serializable {

    long DEFAULT_VERSION = Long.MIN_VALUE;

    ObjectId getObjectId();

    void setObjectId(ObjectId id);

    int getPersistenceState();

    void setPersistenceState(int state);

    ObjectContext getObjectContext();

    void setObjectContext(ObjectContext objectContext);

    /**
     * Modifies a value of a named property without altering the object state in any way,
     * and without triggering any database operations. This method is intended mostly for
     * internal use by Cayenne framework, and shouldn't be called from the application
     * code.
     */
    void writePropertyDirectly(String propertyName, Object val);

    /**
     * Returns mapped property value as currently stored in the Persistent object. Returned value
     * maybe a fault or a real value. This method will not attempt to resolve faults, or
     * to read unmapped properties.
     */
    Object readPropertyDirectly(String propertyName);

    /**
     * Returns a value of the property identified by a property path. Supports reading
     * both mapped and unmapped properties. Unmapped properties are accessed in a manner
     * consistent with JavaBeans specification.
     * <p>
     * Property path (or nested property) is a dot-separated path used to traverse object
     * relationships until the final object is found. If a null object found while
     * traversing path, null is returned. If a list is encountered in the middle of the
     * path, CayenneRuntimeException is thrown. Unlike
     * {@link #readPropertyDirectly(String)}, this method will resolve an object if it is
     * HOLLOW.
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
     * @see #readNestedProperty(CayennePath)
     * @since 1.0.5
     */
    Object readNestedProperty(String path);

    /**
     * Returns a value of the property identified by a property path. Supports reading
     * both mapped and unmapped properties. Unmapped properties are accessed in a manner
     * consistent with JavaBeans specification.
     * <p>
     * Property path (or nested property) is a dot-separated path used to traverse object
     * relationships until the final object is found. If a null object found while
     * traversing path, null is returned. If a list is encountered in the middle of the
     * path, CayenneRuntimeException is thrown.
     * Unlike {@link #readPropertyDirectly(String)}, this method will resolve an object if it is HOLLOW.
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
     * @param path a dot-separated path
     * @return a value of the property identified by a property path
     * @see #readNestedProperty(String)
     * @since 5.0
     */
    Object readNestedProperty(CayennePath path);

    /**
     * Returns a value of the property identified by propName. Resolves faults if needed.
     * This method can safely be used instead of or in addition to the auto-generated
     * property accessors in subclasses of the Persistent object.
     */
    Object readProperty(String propName);

    /**
     * Sets the property to the new value. Resolves faults if needed. This method can be
     * safely used instead of or in addition to the auto-generated property modifiers to
     * set simple properties. Note that to set to-one relationships use
     * {@link #setToOneTarget(String, Persistent, boolean)}.
     *
     * @param propertyName a name of the bean property being modified.
     * @param value        a new value of the property.
     */
    void writeProperty(String propertyName, Object value);

    /**
     * Adds an object to a to-many relationship.
     */
    void addToManyTarget(
            String relationshipName,
            Persistent target,
            boolean setReverse);

    /**
     * Removes an object from a to-many relationship.
     */
    void removeToManyTarget(
            String relationshipName,
            Persistent target,
            boolean unsetReverse);

    /**
     * Sets to-one relationship to a new value. Resolves faults if needed. This method can
     * safely be used instead of or in addition to the auto-generated property modifiers
     * to set properties that are to-one relationships.
     *
     * @param relationshipName a name of the bean property being modified - same as the
     *                         name of ObjRelationship.
     * @param value            a new value of the property.
     * @param setReverse       whether to update the reverse relationship pointing from the old
     *                         and new values of the property to this object.
     */
    void setToOneTarget(
            String relationshipName,
            Persistent value,
            boolean setReverse);

    /**
     * Returns a version of a DataRow snapshot that was used to create this object.
     *
     * @since 1.1
     */
    long getSnapshotVersion();

    /**
     * @since 1.1
     */
    void setSnapshotVersion(long snapshotVersion);
}
