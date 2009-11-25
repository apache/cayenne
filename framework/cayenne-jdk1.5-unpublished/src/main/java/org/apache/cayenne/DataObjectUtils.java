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

import java.util.Map;

import org.apache.cayenne.query.Query;

/**
 * A collection of utility methods to work with DataObjects.
 * <p>
 * <i>DataObjects and Primary Keys: All methods that allow to extract primary key values
 * or use primary keys to find objects are provided for convenience. Still the author's
 * belief is that integer sequential primary keys are meaningless in the object model and
 * are pure database artifacts. Therefore relying heavily on direct access to PK provided
 * via this class (or other such Cayenne API) is not a clean design practice in many
 * cases, and sometimes may actually lead to security issues. </i>
 * </p>
 * 
 * @since 1.1
 * @deprecated since 3.1 {@link org.apache.cayenne.Cayenne} class is used instead
 */
@Deprecated
public final class DataObjectUtils {
    
    /**
     * Returns an int primary key value for a persistent object. Only works for single
     * column numeric primary keys. If an object is transient or has an ObjectId that can
     * not be converted to an int PK, an exception is thrown.
     * 
     * @since 3.0
     */
    public static long longPKForObject(Persistent dataObject) {
        return Cayenne.longPKForObject(dataObject);
    }

    /**
     * Returns an int primary key value for a persistent object. Only works for single
     * column numeric primary keys. If an object is transient or has an ObjectId that can
     * not be converted to an int PK, an exception is thrown.
     */
    public static int intPKForObject(Persistent dataObject) {
        return Cayenne.intPKForObject(dataObject);
    }

    /**
     * Returns a primary key value for a persistent object. Only works for single column
     * primary keys. If an object is transient or has a compound ObjectId, an exception is
     * thrown.
     */
    public static Object pkForObject(Persistent dataObject) {
        return Cayenne.pkForObject(dataObject);
    }

    /**
     * Returns a primary key map for a persistent object. This method is the most generic
     * out of all methods for primary key retrieval. It will work for all possible types
     * of primary keys. If an object is transient, an exception is thrown.
     */
    public static Map<String, Object> compoundPKForObject(Persistent dataObject) {
        return Cayenne.compoundPKForObject(dataObject);
    }

    /**
     * Returns an object matching an int primary key. If the object is mapped to use
     * non-integer PK or a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static <T> T objectForPK(
            ObjectContext context,
            Class<T> dataObjectClass,
            int pk) {
        return Cayenne.objectForPK(context, dataObjectClass, pk);
    }

    /**
     * Returns an object matching an Object primary key. If the object is mapped to use a
     * compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static <T> T objectForPK(
            ObjectContext context,
            Class<T> dataObjectClass,
            Object pk) {

        return Cayenne.objectForPK(context, dataObjectClass, pk);
    }

    /**
     * Returns an object matching a primary key. PK map parameter should use database PK
     * column names as keys.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static <T> T objectForPK(
            ObjectContext context,
            Class<T> dataObjectClass,
            Map<String, ?> pk) {

        return Cayenne.objectForPK(context, dataObjectClass, pk);
    }

    /**
     * Returns an object matching an int primary key. If the object is mapped to use
     * non-integer PK or a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static Object objectForPK(ObjectContext context, String objEntityName, int pk) {
        return Cayenne.objectForPK(context, objEntityName, pk);
    }

    /**
     * Returns an object matching an Object primary key. If the object is mapped to use a
     * compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static Object objectForPK(
            ObjectContext context,
            String objEntityName,
            Object pk) {
        return Cayenne.objectForPK(context, objEntityName, pk);
    }

    /**
     * Returns an object matching a primary key. PK map parameter should use database PK
     * column names as keys.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static Object objectForPK(
            ObjectContext context,
            String objEntityName,
            Map<String, ?> pk) {
        return Cayenne.objectForPK(context, objEntityName, pk);
    }

    /**
     * Returns an object matching ObjectId. If this object is already cached in the
     * ObjectStore, it is returned without a query. Otherwise a query is built and
     * executed against the database.
     * 
     * @return A persistent object that matched the id, null if no matching objects were
     *         found
     * @throws CayenneRuntimeException if more than one object matched ObjectId.
     */
    public static Object objectForPK(ObjectContext context, ObjectId id) {
        return Cayenne.objectForPK(context, id);
    }

    /**
     * Returns an object or a DataRow that is a result of a given query. If query returns
     * more than one object, an exception is thrown. If query returns no objects, null is
     * returned.
     * 
     * @since 1.2
     */
    public static Object objectForQuery(ObjectContext context, Query query) {
        return Cayenne.objectForQuery(context, query);
    }

    // not intended for instantiation
    private DataObjectUtils() {
    }
}
