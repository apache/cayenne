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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.PropertyUtils;

/**
 * Various utils for processing persistent objects and their properties
 * <p>
 * <i>DataObjects and Primary Keys: All methods that allow to extract primary
 * key values or use primary keys to find objects are provided for convenience.
 * Still the author's belief is that integer sequential primary keys are
 * meaningless in the object model and are pure database artifacts. Therefore
 * relying heavily on direct access to PK provided via this class (or other such
 * Cayenne API) is not a clean design practice in many cases, and sometimes may
 * actually lead to security issues. </i>
 * </p>
 * 
 * @since 3.1 its predecessor was called DataObjectUtils
 */
public class Cayenne {

    /**
     * A special property denoting a size of the to-many collection, when
     * encountered at the end of the path</p>
     */
    final static String PROPERTY_COLLECTION_SIZE = "@size";

    /**
     * Returns mapped ObjEntity for object. If an object is transient or is not
     * mapped returns null.
     */
    public static ObjEntity getObjEntity(Persistent p) {
        return (p.getObjectContext() != null) ? p.getObjectContext().getEntityResolver().getObjEntity(p) : null;
    }

    /**
     * Returns class descriptor for the object or null if the object is not
     * registered with an ObjectContext or descriptor was not found.
     */
    public static ClassDescriptor getClassDescriptor(Persistent object) {

        ObjectContext context = object.getObjectContext();

        if (context == null) {
            return null;
        }

        return context.getEntityResolver().getClassDescriptor(object.getObjectId().getEntityName());
    }

    /**
     * Returns property descriptor for specified property.
     * 
     * @param properyName
     *            path to the property
     * @return property descriptor, <code>null</code> if not found
     */
    public static PropertyDescriptor getProperty(Persistent object, String properyName) {
        ClassDescriptor descriptor = getClassDescriptor(object);
        if (descriptor == null) {
            return null;
        }
        return descriptor.getProperty(properyName);
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
     * Unlike {@link DataObject#readPropertyDirectly(String)}, this method will resolve an
     * object if it is HOLLOW.
     * <p>
     * Examples:
     * </p>
     * <ul>
     * <li>Read this object property:<br>
     * <code>String name = (String)Cayenne.readNestedProperty(artist, "name");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read an object related to this object:<br>
     * <code>Gallery g = (Gallery)Cayenne.readNestedProperty(paintingInfo, "toPainting.toGallery");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read a property of an object related to this object: <br>
     * <code>String name = (String)Cayenne.readNestedProperty(painting, "toArtist.artistName");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read to-many relationship list:<br>
     * <code>List exhibits = (List)Cayenne.readNestedProperty(painting, "toGallery.exhibitArray");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read to-many relationship in the middle of the path:<br>
     * <code>List&lt;String&gt; names = (List&lt;String&gt;)Cayenne.readNestedProperty(artist, "paintingArray.paintingName");</code>
     * <br>
     * <br>
     * </li>
     * </ul>
     */
    public static Object readNestedProperty(Object o, String path) {

        if (o == null) {
            return null;
        } else if (o instanceof DataObject) {
            return ((DataObject) o).readNestedProperty(path);
        } else if (o instanceof Collection<?>) {

            // This allows people to put @size at the end of a property
            // path and be able to find out the size of a relationship.

            Collection<?> collection = (Collection<?>) o;

            if (path.equals(PROPERTY_COLLECTION_SIZE)) {
                return collection.size();
            }

            // Support for collection property in the middle of the path
            Collection<Object> result = o instanceof List<?> ? new ArrayList<Object>() : new HashSet<Object>();

            for (Object item : collection) {
                if (item instanceof DataObject) {
                    DataObject cdo = (DataObject) item;
                    Object rest = cdo.readNestedProperty(path);

                    if (rest instanceof Collection<?>) {

                        // We don't want nested collections. E.g.
                        // readNestedProperty("paintingArray.paintingTitle")
                        // should return
                        // List<String>
                        result.addAll((Collection<?>) rest);
                    } else {
                        result.add(rest);
                    }
                }
            }

            return result;
        }

        if ((null == path) || (0 == path.length())) {
            throw new IllegalArgumentException("the path must be supplied in order to lookup a nested property");
        }

        int dotIndex = path.indexOf('.');

        if (0 == dotIndex) {
            throw new IllegalArgumentException("the path is invalid because it starts with a period character");
        }

        if (dotIndex == path.length() - 1) {
            throw new IllegalArgumentException("the path is invalid because it ends with a period character");
        }

        if (-1 == dotIndex) {
            return readSimpleProperty(o, path);
        }

        String path0 = path.substring(0, dotIndex);
        String pathRemainder = path.substring(dotIndex + 1);

        // this is copied from the old code where the placement of a plus
        // character at the end of a segment of a property path would
        // simply strip out the plus. I am not entirely sure why this is
        // done. See unit test 'testReadNestedPropertyToManyInMiddle1'.

        if ('+' == path0.charAt(path0.length() - 1)) {
            path0 = path0.substring(0, path0.length() - 1);
        }

        Object property = readSimpleProperty(o, path0);
        return readNestedProperty(property, pathRemainder);
    }

    private static final Object readSimpleProperty(Object o, String propertyName) {
        if (o instanceof Persistent) {

            PropertyDescriptor property = getProperty((Persistent) o, propertyName);

            if (property != null) {
                return property.readProperty(o);
            }
        }

        // handling non-persistent property
        return PropertyUtils.getProperty(o, propertyName);
    }

    /**
     * Constructs a dotted path from a list of strings. Useful for creating more
     * complex paths while preserving compilation safety. For example, instead
     * of saying:
     * <p>
     * 
     * <pre>
     * orderings.add(new Ordering(&quot;department.name&quot;, SortOrder.ASCENDING));
     * </pre>
     * <p>
     * You can use makePath() with the constants generated by Cayenne Modeler:
     * <p>
     * 
     * <pre>
     * orderings.add(new Ordering(Cayenne.makePath(USER.DEPARTMENT_PROPERTY, Department.NAME_PROPERTY), SortOrder.ASCENDING));
     * </pre>
     * <p>
     * 
     * @param pathParts
     *            The varargs list of paths to join.
     * @return A string of all the paths joined by a "." (used by Cayenne in
     *         queries and orderings).
     *         <p>
     * @since 3.1
     */
    public static String makePath(String... pathParts) {
        StringBuilder builder = new StringBuilder();
        String separator = "";

        for (String path : pathParts) {
            builder.append(separator).append(path);
            separator = ".";
        }

        return builder.toString();
    }

    /**
     * Returns an int primary key value for a persistent object. Only works for
     * single column numeric primary keys. If an object is transient or has an
     * ObjectId that can not be converted to an int PK, an exception is thrown.
     */
    public static long longPKForObject(Persistent dataObject) {
        Object value = pkForObject(dataObject);

        if (!(value instanceof Number)) {
            throw new CayenneRuntimeException("PK is not a number: " + dataObject.getObjectId());
        }

        return ((Number) value).longValue();
    }

    /**
     * Returns an int primary key value for a persistent object. Only works for
     * single column numeric primary keys. If an object is transient or has an
     * ObjectId that can not be converted to an int PK, an exception is thrown.
     */
    public static int intPKForObject(Persistent dataObject) {
        Object value = pkForObject(dataObject);

        if (!(value instanceof Number)) {
            throw new CayenneRuntimeException("PK is not a number: " + dataObject.getObjectId());
        }

        return ((Number) value).intValue();
    }

    /**
     * Returns a primary key value for a persistent object. Only works for
     * single column primary keys. If an object is transient or has a compound
     * ObjectId, an exception is thrown.
     */
    public static Object pkForObject(Persistent dataObject) {
        Map<String, Object> pk = extractObjectId(dataObject);

        if (pk.size() != 1) {
            throw new CayenneRuntimeException("Expected single column PK, got " + pk.size() + " columns, ID: " + pk);
        }

        return pk.entrySet().iterator().next().getValue();
    }

    /**
     * Returns a primary key map for a persistent object. This method is the
     * most generic out of all methods for primary key retrieval. It will work
     * for all possible types of primary keys. If an object is transient, an
     * exception is thrown.
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
            ObjEntity objEntity = dataObject.getObjectContext().getEntityResolver().getObjEntity(dataObject);

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
     * Returns an object matching an int primary key. If the object is mapped to
     * use non-integer PK or a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned
     * without a query. Otherwise a query is built and executed against the
     * database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static <T> T objectForPK(ObjectContext context, Class<T> dataObjectClass, int pk) {
        return (T) objectForPK(context, buildId(context, dataObjectClass, Integer.valueOf(pk)));
    }

    /**
     * Returns an object matching an Object primary key. If the object is mapped
     * to use a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned
     * without a query. Otherwise a query is built and executed against the
     * database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static <T> T objectForPK(ObjectContext context, Class<T> dataObjectClass, Object pk) {

        return (T) objectForPK(context, buildId(context, dataObjectClass, pk));
    }

    /**
     * Returns an object matching a primary key. PK map parameter should use
     * database PK column names as keys.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned
     * without a query. Otherwise a query is built and executed against the
     * database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static <T> T objectForPK(ObjectContext context, Class<T> dataObjectClass, Map<String, ?> pk) {

        ObjEntity entity = context.getEntityResolver().getObjEntity(dataObjectClass);
        if (entity == null) {
            throw new CayenneRuntimeException("Non-existent ObjEntity for class: " + dataObjectClass);
        }

        return (T) objectForPK(context, new ObjectId(entity.getName(), pk));
    }

    /**
     * Returns an object matching an int primary key. If the object is mapped to
     * use non-integer PK or a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned
     * without a query. Otherwise a query is built and executed against the
     * database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static Object objectForPK(ObjectContext context, String objEntityName, int pk) {
        return objectForPK(context, buildId(context, objEntityName, Integer.valueOf(pk)));
    }

    /**
     * Returns an object matching an Object primary key. If the object is mapped
     * to use a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned
     * without a query. Otherwise a query is built and executed against the
     * database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static Object objectForPK(ObjectContext context, String objEntityName, Object pk) {
        return objectForPK(context, buildId(context, objEntityName, pk));
    }

    /**
     * Returns an object matching a primary key. PK map parameter should use
     * database PK column names as keys.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned
     * without a query. Otherwise a query is built and executed against the
     * database.
     * </p>
     * 
     * @see #objectForPK(ObjectContext, ObjectId)
     */
    public static Object objectForPK(ObjectContext context, String objEntityName, Map<String, ?> pk) {
        if (objEntityName == null) {
            throw new IllegalArgumentException("Null ObjEntity name.");
        }

        return objectForPK(context, new ObjectId(objEntityName, pk));
    }

    /**
     * Returns an object matching ObjectId. If this object is already cached in
     * the ObjectStore, it is returned without a query. Otherwise a query is
     * built and executed against the database.
     * 
     * @return A persistent object that matched the id, null if no matching
     *         objects were found
     * @throws CayenneRuntimeException
     *             if more than one object matched ObjectId.
     */
    public static Object objectForPK(ObjectContext context, ObjectId id) {
        return objectForQuery(context, new ObjectIdQuery(id, false, ObjectIdQuery.CACHE));
    }

    /**
     * Returns an object or a DataRow that is a result of a given query. If
     * query returns more than one object, an exception is thrown. If query
     * returns no objects, null is returned.
     */
    public static Object objectForQuery(ObjectContext context, Query query) {
        List<?> objects = context.performQuery(query);

        if (objects.size() == 0) {
            return null;
        } else if (objects.size() > 1) {
            throw new CayenneRuntimeException("Expected zero or one object, instead query matched: " + objects.size());
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
            throw new CayenneRuntimeException("PK contains " + pkAttributes.size() + " columns, expected 1.");
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

        ObjEntity entity = context.getEntityResolver().getObjEntity(dataObjectClass);
        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped DataObject Class: " + dataObjectClass.getName());
        }

        Collection<String> pkAttributes = entity.getPrimaryKeyNames();
        if (pkAttributes.size() != 1) {
            throw new CayenneRuntimeException("PK contains " + pkAttributes.size() + " columns, expected 1.");
        }

        String attr = pkAttributes.iterator().next();
        return new ObjectId(entity.getName(), attr, pk);
    }

    private Cayenne() {
    }
}
