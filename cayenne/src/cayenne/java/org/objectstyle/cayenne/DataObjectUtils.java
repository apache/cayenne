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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.ObjectStore;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntityInheritanceTree;
import org.objectstyle.cayenne.map.ObjEntity;

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
 * @author Andrei Adamchik
 */
public final class DataObjectUtils {

    /**
     * Returns an int primary key value for a DataObject. Only works for single column
     * numeric primary keys. If a DataObjects is transient or has an ObjectId that can not
     * be converted to an int PK, an exception is thrown.
     */
    public static int intPKForObject(DataObject dataObject) {
        Object value = pkForObject(dataObject);

        if (!(value instanceof Number)) {
            throw new CayenneRuntimeException("PK is not a number: "
                    + dataObject.getObjectId());
        }

        return ((Number) value).intValue();
    }

    /**
     * Returns a primary key value for a DataObject. Only works for single column primary
     * keys. If a DataObjects is transient or has a compound ObjectId, an exception is
     * thrown.
     */
    public static Object pkForObject(DataObject dataObject) {
        ObjectId id = extractObjectId(dataObject);
        Map pk = id.getIdSnapshot();

        if (pk.size() != 1) {
            throw new CayenneRuntimeException("Compund PK: " + id);
        }

        Map.Entry pkEntry = (Map.Entry) pk.entrySet().iterator().next();
        return pkEntry.getValue();
    }

    /**
     * Returns a primary key map for a DataObject. This method is the most generic out of
     * all methods for primary key retrieval. It will work for all possible types of
     * primary keys. If a DataObjects is transient, an exception is thrown.
     */
    public static Map compoundPKForObject(DataObject dataObject) {
        ObjectId id = extractObjectId(dataObject);
        return Collections.unmodifiableMap(id.getIdSnapshot());
    }

    static ObjectId extractObjectId(DataObject dataObject) {
        if (dataObject == null) {
            throw new IllegalArgumentException("Null DataObject");
        }

        ObjectId id = dataObject.getObjectId();
        if (id.isTemporary()) {
            if (id.getReplacementId() == null) {
                throw new CayenneRuntimeException(
                        "Can't get primary key from temporary id.");
            }

            return id.getReplacementId();
        }

        return id;
    }

    /**
     * Returns an object matching an int primary key. If the object is mapped to use
     * non-integer PK or a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(DataContext, ObjectId)
     */
    public static DataObject objectForPK(
            DataContext context,
            Class dataObjectClass,
            int pk) {
        return objectForPK(context, buildId(context, dataObjectClass, new Integer(pk)));
    }

    /**
     * Returns an object matching an Object primary key. If the object is mapped to use a
     * compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(DataContext, ObjectId)
     */
    public static DataObject objectForPK(
            DataContext context,
            Class dataObjectClass,
            Object pk) {

        return objectForPK(context, buildId(context, dataObjectClass, pk));
    }

    /**
     * Returns an object matching a primary key. PK map parameter should use database PK
     * column names as keys.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(DataContext, ObjectId)
     */
    public static DataObject objectForPK(
            DataContext context,
            Class dataObjectClass,
            Map pk) {
        return objectForPK(context, new ObjectId(dataObjectClass, pk));
    }

    /**
     * Returns an object matching an int primary key. If the object is mapped to use
     * non-integer PK or a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(DataContext, ObjectId)
     */
    public static DataObject objectForPK(DataContext context, String objEntityName, int pk) {
        return objectForPK(context, buildId(context, objEntityName, new Integer(pk)));
    }

    /**
     * Returns an object matching an Object primary key. If the object is mapped to use a
     * compound PK, CayenneRuntimeException is thrown.
     * <p>
     * If this object is already cached in the ObjectStore, it is returned without a
     * query. Otherwise a query is built and executed against the database.
     * </p>
     * 
     * @see #objectForPK(DataContext, ObjectId)
     */
    public static DataObject objectForPK(
            DataContext context,
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
     * @see #objectForPK(DataContext, ObjectId)
     */
    public static DataObject objectForPK(DataContext context, String objEntityName, Map pk) {
        if (objEntityName == null) {
            throw new IllegalArgumentException("Null ObjEntity name.");
        }

        ObjEntity entity = context.getEntityResolver().getObjEntity(objEntityName);
        if (entity == null) {
            throw new CayenneRuntimeException("Non-existent ObjEntity: " + objEntityName);
        }

        Class dataObjectClass = entity.getJavaClass(Configuration.getResourceLoader());

        return objectForPK(context, new ObjectId(dataObjectClass, pk));
    }

    /**
     * Returns an object matching ObjectId. If this object is already cached in the
     * ObjectStore, it is returned without a query. Otherwise a query is built and
     * executed against the database.
     * 
     * @return A DataObject that matched the id, null if no matching objects were found
     * @throws CayenneRuntimeException if more than one object matched ObjectId.
     */
    public static DataObject objectForPK(DataContext context, ObjectId id) {
        ObjectStore objectStore = context.getObjectStore();

        // look for cached object first
        DataObject object = objectStore.getObject(id);
        if (object != null) {
            return object;
        }

        // CAY-218: check for inheritance... ObjectId maybe wrong
        // TODO: investigate moving this to the ObjectStore "getObject()" - this should
        // really be global...

        ObjEntity entity = context.getEntityResolver().lookupObjEntity(id.getObjClass());
        EntityInheritanceTree inheritanceHandler = context
                .getEntityResolver()
                .lookupInheritanceTree(entity);
        if (inheritanceHandler != null) {
            Collection children = inheritanceHandler.getChildren();
            if (!children.isEmpty()) {
                // find cached child
                Iterator it = children.iterator();
                while (it.hasNext()) {
                    EntityInheritanceTree child = (EntityInheritanceTree) it.next();
                    ObjectId childID = new ObjectId(child.getEntity().getJavaClass(
                            Configuration.getResourceLoader()), id.getIdSnapshot());

                    DataObject childObject = objectStore.getObject(childID);
                    if (childObject != null) {
                        return childObject;
                    }
                }
            }
        }

        // look in shared cache...

        // TODO: take inheritance into account...
        DataRow row = objectStore.getSnapshot(id, context);

        return (row != null)
                ? context.objectFromDataRow(id.getObjClass(), row, false)
                : null;
    }

    static ObjectId buildId(DataContext context, String objEntityName, Object pk) {
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

        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity == null) {
            throw new CayenneRuntimeException("No DbEntity for ObjEntity: "
                    + entity.getName());
        }

        List pkAttributes = dbEntity.getPrimaryKey();
        if (pkAttributes.size() != 1) {
            throw new CayenneRuntimeException("PK contains "
                    + pkAttributes.size()
                    + " columns, expected 1.");
        }

        DbAttribute attr = (DbAttribute) pkAttributes.get(0);
        return new ObjectId(entity.getJavaClass(Configuration.getResourceLoader()), attr
                .getName(), pk);
    }

    static ObjectId buildId(DataContext context, Class dataObjectClass, Object pk) {
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

        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity == null) {
            throw new CayenneRuntimeException("No DbEntity for ObjEntity: "
                    + entity.getName());
        }

        List pkAttributes = dbEntity.getPrimaryKey();
        if (pkAttributes.size() != 1) {
            throw new CayenneRuntimeException("PK contains "
                    + pkAttributes.size()
                    + " columns, expected 1.");
        }

        DbAttribute attr = (DbAttribute) pkAttributes.get(0);
        return new ObjectId(dataObjectClass, attr.getName(), pk);
    }

    // not intended for instantiation
    private DataObjectUtils() {
    }
}