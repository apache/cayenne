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
package org.objectstyle.cayenne.access.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.InsertQuery;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.UpdateQuery;

/**
 * Implements helper methods that perform different query-related operations.
 * <i>May be deprecated in the future, after its functionality is moved to the 
 * places where it is used now.</i>
 * 
 * @author Andrei Adamchik
 *
 */
public class QueryUtils {

    /**
     * @deprecated Since 1.1 Unused
     */
    private static Map putModifiedAttribute(Map aMap, String name, Object value) {
        if (aMap == null) {
            aMap = new HashMap();
        }
        aMap.put(name, value);
        return aMap;
    }

    /**
     * Returns a map of the properties of dataObject which have actually changed
     * compared to the objects commited snapshot.  Actual change is determined
     * by using equals() (true implies no change).
     * Will return null if there are no changes
     * 
     * @deprecated Since 1.1 unused
     */
    public static Map updatedProperties(DataObject dataObject) {
        // Lazily created to avoid creating too many unnecessary objects
        Map result = null;

        DataContext context = dataObject.getDataContext();
        DataRow committedSnapshot =
            context.getObjectStore().getSnapshot(dataObject.getObjectId(), context);
        DataRow currentSnapshot = dataObject.getDataContext().currentSnapshot(dataObject);

        Iterator it = currentSnapshot.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            Object newValue = currentSnapshot.get(attrName);

            // if snapshot exists, compare old values and new values,
            // only add attribute to the update clause if the value has changed
            if (committedSnapshot != null) {
                Object oldValue = committedSnapshot.get(attrName);
                if (oldValue != null && !oldValue.equals(newValue)) {
                    result = putModifiedAttribute(result, attrName, newValue);
                }
                else if (oldValue == null && newValue != null) {
                    result = putModifiedAttribute(result, attrName, newValue);
                }
            }
            // if no snapshot exists, just add the fresh value to update clause
            else {
                result = putModifiedAttribute(result, attrName, newValue);
            }
        }

        // original snapshot can have extra keys that are missing in the
        // current snapshot; process those
        if (committedSnapshot != null) {
            Iterator origit = committedSnapshot.keySet().iterator();
            while (origit.hasNext()) {
                String attrName = (String) origit.next();
                if (currentSnapshot.containsKey(attrName))
                    continue;

                Object oldValue = committedSnapshot.get(attrName);
                if (oldValue == null)
                    continue;
                result = putModifiedAttribute(result, attrName, null);
            }
        }
        return result; //Might be null if nothing was actually changed
    }

    /** Returns an update query for the DataObject that can be used to commit
     *  object state changes to the database. If no changes are found, null is returned.
     *  
     * @deprecated Unused since 1.1
     */
    public static UpdateQuery updateQuery(DataObject dataObject) {
        UpdateQuery upd = new UpdateQuery();

        ObjectId id = dataObject.getObjectId();
        upd.setRoot(dataObject.getClass());
        Map modifiedProperties = updatedProperties(dataObject);
        if ((modifiedProperties != null) && (modifiedProperties.size() > 0)) {
            Iterator keyIterator = modifiedProperties.keySet().iterator();
            while (keyIterator.hasNext()) {
                String key = (String) keyIterator.next();
                upd.addUpdAttribute(key, modifiedProperties.get(key));
            }
            // set qualifier
            upd.setQualifier(
                ExpressionFactory.matchAllDbExp(id.getIdSnapshot(), Expression.EQUAL_TO));
            return upd;
        }

        return null;
    }

    /** 
     * Generates a delete query for a specified data object.
     * 
     * @deprecated Unused since 1.1
     */
    public static DeleteQuery deleteQuery(DataObject dataObject) {
        DeleteQuery del = new DeleteQuery();
        ObjectId id = dataObject.getObjectId();
        del.setRoot(dataObject.getClass());
        del.setQualifier(
            ExpressionFactory.matchAllDbExp(id.getIdSnapshot(), Expression.EQUAL_TO));
        return del;
    }

    /** 
     * Generates an insert query for a specified data object.
     *
     * @deprecated Unused since 1.1
     */
    public static InsertQuery insertQuery(Map objectSnapshot, ObjectId permId) {
        InsertQuery ins = new InsertQuery();
        ins.setRoot(permId.getObjClass());
        ins.setObjectSnapshot(objectSnapshot);
        ins.setObjectId(permId);
        return ins;
    }

    /**
     * @deprecated Since 1.0.1 FlattenedObjectId is deprecated, so this 
     * method is deprecated too.
     */
    public static SelectQuery selectObjectForFlattenedObjectId(
        QueryEngine e,
        org.objectstyle.cayenne.FlattenedObjectId oid) {
        //Create a selectquery using the relationship name
        // and source id snapshot of the FlattenedObjectId
        SelectQuery sel = new SelectQuery();
        sel.setRoot(oid.getObjClass());

        DataObject sourceObject = oid.getSourceObject();
        Expression sourceExpression =
            ExpressionFactory.matchAllDbExp(
                sourceObject.getObjectId().getIdSnapshot(),
                Expression.EQUAL_TO);
        ObjEntity ent = e.getEntityResolver().lookupObjEntity(sourceObject);

        sel.setQualifier(
            ent.translateToRelatedEntity(sourceExpression, oid.getRelationshipName()));

        return sel;
    }

    /** 
     * Creates and returns a select query that can be used to 
     * fetch an object given an ObjectId.
     */
    public static SelectQuery selectObjectForId(ObjectId oid) {
        return new SelectQuery(
            oid.getObjClass(),
            ExpressionFactory.matchAllDbExp(oid.getIdSnapshot(), Expression.EQUAL_TO));
    }

    /** 
     * Creates and returns a select query that can be used to 
     * fetch a list of objects given a list of ObjectIds.
     * All ObjectIds must belong to the same entity.
     */
    public static SelectQuery selectQueryForIds(List oids) {
        if (oids == null || oids.size() == 0) {
            throw new IllegalArgumentException("List must contain at least one ObjectId");
        }

        SelectQuery sel = new SelectQuery();
        sel.setRoot(((ObjectId) oids.get(0)).getObjClass());

        Iterator it = oids.iterator();

        ObjectId firstId = (ObjectId) it.next();
        Expression exp =
            ExpressionFactory.matchAllDbExp(firstId.getIdSnapshot(), Expression.EQUAL_TO);

        while (it.hasNext()) {
            ObjectId anId = (ObjectId) it.next();
            exp =
                exp.orExp(
                    ExpressionFactory.matchAllDbExp(
                        anId.getIdSnapshot(),
                        Expression.EQUAL_TO));
        }

        sel.setQualifier(exp);
        return sel;
    }

    /** 
     * Creates and returns SelectQuery for a given SelectQuery and
     * relationship prefetching path.
     * 
     * @deprecated Since 1.1 Use PrefetchSelectQuery constructor.
     */
    public static PrefetchSelectQuery selectPrefetchPath(
        QueryEngine e,
        SelectQuery q,
        String prefetchPath) {
        return new PrefetchSelectQuery(e.getEntityResolver(), q, prefetchPath);
    }

    /**
     * Translates qualifier applicable for one ObjEntity into a
     * qualifier for a related ObjEntity.
     * @param ent the entity to which the original qualifier (<code>qual</code>) 
     * applies
     * @param qual the qualifier on <code>ent</code>
     * @param relPath a relationship path from <code>ent</code> to some target entity
     * @return Expression which, when applied to the target entity of relPath, 
     * will give the union of the objects that would be obtained by following 
     * relPath from all of the objects in <code>ent</code> that match <code>qual</code>
     * 
     * @deprecated Since 1.1 use Entity.translatedForRelatedEntity(Expression, String)
     */
    public static Expression transformQualifier(
        ObjEntity ent,
        Expression qual,
        String relPath) {

        if (qual == null) {
            return null;
        }

        return ent.translateToRelatedEntity(qual, relPath);
    }

    /**
     * Generates a SelectQuery that can be used to fetch 
     * relationship destination objects given a source object
     * of a to-many relationship. 
     */
    public static SelectQuery selectRelationshipObjects(
        QueryEngine e,
        DataObject source,
        String relName) {

        ObjEntity ent = e.getEntityResolver().lookupObjEntity(source);
        ObjRelationship rel = (ObjRelationship) ent.getRelationship(relName);
        ObjEntity destEnt = (ObjEntity) rel.getTargetEntity();

        List dbRels = rel.getDbRelationships();

        // sanity check
        if (dbRels == null || dbRels.size() == 0) {
            throw new CayenneRuntimeException(
                "ObjRelationship '" + rel.getName() + "' is unmapped.");
        }

        // build a reverse DB path
        // ...while reverse ObjRelationship may be absent,
        // reverse DB must always be there...
        StringBuffer buf = new StringBuffer();
        ListIterator it = dbRels.listIterator(dbRels.size());
        while (it.hasPrevious()) {
            if (buf.length() > 0) {
                buf.append(".");
            }
            DbRelationship dbRel = (DbRelationship) it.previous();
            DbRelationship reverse = dbRel.getReverseRelationship();

            // another sanity check
            if (reverse == null) {
                throw new CayenneRuntimeException(
                    "DbRelationship '"
                        + dbRel.getName()
                        + "' has no reverse relationship");
            }

            buf.append(reverse.getName());
        }

        SelectQuery select = new SelectQuery(destEnt);
        select.setQualifier(ExpressionFactory.matchDbExp(buf.toString(), source));
        // resolve inheritance
        select.setResolvingInherited(true);
        return select;
    }
}