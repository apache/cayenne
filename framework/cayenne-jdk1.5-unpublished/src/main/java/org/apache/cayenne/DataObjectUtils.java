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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectIdQuery;
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
 */
public final class DataObjectUtils {
    
    /**
     * Returns an int primary key value for a persistent object. Only works for single
     * column numeric primary keys. If an object is transient or has an ObjectId that can
     * not be converted to an int PK, an exception is thrown.
     * 
     * @since 3.0
     */
    public static long longPKForObject(Persistent dataObject) {
        Object value = pkForObject(dataObject);

        if (!(value instanceof Number)) {
            throw new CayenneRuntimeException("PK is not a number: "
                    + dataObject.getObjectId());
        }

        return ((Number) value).longValue();
    }

    /**
     * Returns an int primary key value for a persistent object. Only works for single
     * column numeric primary keys. If an object is transient or has an ObjectId that can
     * not be converted to an int PK, an exception is thrown.
     */
    public static int intPKForObject(Persistent dataObject) {
        Object value = pkForObject(dataObject);

        if (!(value instanceof Number)) {
            throw new CayenneRuntimeException("PK is not a number: "
                    + dataObject.getObjectId());
        }

        return ((Number) value).intValue();
    }

    /**
     * Returns a primary key value for a persistent object. Only works for single column
     * primary keys. If an object is transient or has a compound ObjectId, an exception is
     * thrown.
     */
    public static Object pkForObject(Persistent dataObject) {
        Map<String, Object> pk = extractObjectId(dataObject);

        if (pk.size() != 1) {
            throw new CayenneRuntimeException("Expected single column PK, got "
                    + pk.size()
                    + " columns, ID: "
                    + pk);
        }

        return pk.entrySet().iterator().next().getValue();
    }

    /**
     * Returns a primary key map for a persistent object. This method is the most generic
     * out of all methods for primary key retrieval. It will work for all possible types
     * of primary keys. If an object is transient, an exception is thrown.
     */
    public static Map<String, Object> compoundPKForObject(Persistent dataObject) {
        return Collections.unmodifiableMap(extractObjectId(dataObject));
    }

    static Map<String, Object> extractObjectId(Persistent dataObject) {
        if (dataObject == null) {
            throw new IllegalArgumentException("Null DataObject");
        }

        ObjectId id = dataObject.getObjectId();
        if (!id.isTemporary()) {
            return id.getIdSnapshot();
        }

        // replacement ID is more tricky... do some sanity check...
        if (id.isReplacementIdAttached()) {
            ObjEntity objEntity = dataObject
                    .getObjectContext()
                    .getEntityResolver()
                    .lookupObjEntity(dataObject);

            if (objEntity != null) {
                DbEntity entity = objEntity.getDbEntity();
                if (entity != null && entity.isFullReplacementIdAttached(id)) {
                    return id.getReplacementIdMap();
                }
            }
        }

        throw new CayenneRuntimeException("Can't get primary key from temporary id.");
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
        return (T) objectForPK(
                context,
                buildId(context, dataObjectClass, Integer.valueOf(pk)));
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

        return (T) objectForPK(context, buildId(context, dataObjectClass, pk));
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

        ObjEntity entity = context.getEntityResolver().lookupObjEntity(dataObjectClass);
        if (entity == null) {
            throw new CayenneRuntimeException("Non-existent ObjEntity for class: "
                    + dataObjectClass);
        }

        return (T) objectForPK(context, new ObjectId(entity.getName(), pk));
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
        return objectForPK(context, buildId(context, objEntityName, Integer.valueOf(pk)));
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
        return objectForPK(context, buildId(context, objEntityName, pk));
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
        if (objEntityName == null) {
            throw new IllegalArgumentException("Null ObjEntity name.");
        }

        return objectForPK(context, new ObjectId(objEntityName, pk));
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
        return DataObjectUtils.objectForQuery(context, new ObjectIdQuery(
                id,
                false,
                ObjectIdQuery.CACHE));
    }

    /**
     * Returns an object or a DataRow that is a result of a given query. If query returns
     * more than one object, an exception is thrown. If query returns no objects, null is
     * returned.
     * 
     * @since 1.2
     */
    public static Object objectForQuery(ObjectContext context, Query query) {
        List<?> objects = context.performQuery(query);

        if (objects.size() == 0) {
            return null;
        }
        else if (objects.size() > 1) {
            throw new CayenneRuntimeException(
                    "Expected zero or one object, instead query matched: "
                            + objects.size());
        }

        return objects.get(0);
    }

    static ObjectId buildId(ObjectContext context, String objEntityName, Object pk) {
        if (pk == null) {
            throw new IllegalArgumentException("Null PK");
        }

        if (objEntityName == null) {
            throw new IllegalArgumentException("Null ObjEntity name.");
        }

        ObjEntity entity = context.getEntityResolver().getObjEntity(objEntityName);
        if (entity == null) {
            throw new CayenneRuntimeException("Non-existent ObjEntity: " + objEntityName);
        }

        Collection<String> pkAttributes = entity.getPrimaryKeyNames();
        if (pkAttributes.size() != 1) {
            throw new CayenneRuntimeException("PK contains "
                    + pkAttributes.size()
                    + " columns, expected 1.");
        }

        String attr = pkAttributes.iterator().next();
        return new ObjectId(objEntityName, attr, pk);
    }

    static ObjectId buildId(ObjectContext context, Class<?> dataObjectClass, Object pk) {
        if (pk == null) {
            throw new IllegalArgumentException("Null PK");
        }

        if (dataObjectClass == null) {
            throw new IllegalArgumentException("Null DataObject class.");
        }

        ObjEntity entity = context.getEntityResolver().lookupObjEntity(dataObjectClass);
        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped DataObject Class: "
                    + dataObjectClass.getName());
        }

        Collection<String> pkAttributes = entity.getPrimaryKeyNames();
        if (pkAttributes.size() != 1) {
            throw new CayenneRuntimeException("PK contains "
                    + pkAttributes.size()
                    + " columns, expected 1.");
        }

        String attr = pkAttributes.iterator().next();
        return new ObjectId(entity.getName(), attr, pk);
    }

    // not intended for instantiation
    private DataObjectUtils() {
    }
}
