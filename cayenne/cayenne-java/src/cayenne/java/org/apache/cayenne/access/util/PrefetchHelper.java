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

package org.apache.cayenne.access.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTIn;
import org.apache.cayenne.exp.parser.ASTList;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.SelectQuery;

/**
 * A helper class to resolve relationships in batches for the lists of objects. Used for
 * performance tuning.
 * 
 * @author Arndt Brenschede
 * @deprecated Since 1.2. This class has a number of limitations and keeping it in Cayenne
 *             doesn't look like a wise idea.
 */
public class PrefetchHelper {

    /**
     * Resolves a to-one relationship for a list of objects.
     */
    public static void resolveToOneRelations(
            DataContext context,
            List objects,
            String relName) {

        int len = objects.size();
        if (len == 0) {
            return;
        }

        List oids = null;

        for (int i = 0; i < len; i++) {
            DataObject sourceObject = (DataObject) objects.get(i);
            DataObject targetObject = (DataObject) sourceObject.readProperty(relName);

            if (targetObject.getPersistenceState() == PersistenceState.HOLLOW) {
                ObjectId oid = targetObject.getObjectId();

                if (oids == null) {
                    oids = new ArrayList(len);
                }

                oids.add(oid);
            }
        }

        if (oids != null) {
            // this maybe suboptimal, cause it uses an OR .. OR .. OR .. expression
            // instead of IN (..) - to be compatble with compound keys -
            // however, it seems to be quite fast as well
            SelectQuery sel = QueryUtils.selectQueryForIds(oids);
            context.performQuery(sel);
        }
    }

    /**
     * Resolves a toMany relation for a list of objects.
     * <p>
     * WARNING: this is a bit of a hack - it works for my toMany's, but it possibly
     * doesn't work in all cases.
     * </p>
     * <p>
     * *** It definitly does not work for compound keys ***
     * </p>
     */
    public static void resolveToManyRelations(
            DataContext context,
            List objects,
            String relName) {

        int nobjects = objects.size();
        if (nobjects == 0)
            return;

        String dbKey = null;
        Map listMap = new HashMap(nobjects);

        // put the object-ids in a map for later assignment of the
        // query results

        for (int i = 0; i < nobjects; i++) {
            DataObject object = (DataObject) objects.get(i);
            ObjectId oid = object.getObjectId();
            if (dbKey == null) {
                Map id = oid.getIdSnapshot();
                if (id.size() != 1) {
                    throw new CayenneRuntimeException(
                            "resolveToManyRelations expects single keys for now...");
                }
                dbKey = (String) id.keySet().iterator().next();
            }
            listMap.put(oid.getValueForAttribute(dbKey), new ArrayList());
        }

        ObjEntity ent = context.getEntityResolver().lookupObjEntity(
                (DataObject) objects.get(0));
        ObjRelationship rel = (ObjRelationship) ent.getRelationship(relName);
        ObjEntity destEnt = (ObjEntity) rel.getTargetEntity();

        List dbRels = rel.getDbRelationships();

        // sanity check
        if (dbRels == null || dbRels.size() == 0) {
            throw new CayenneRuntimeException("ObjRelationship '"
                    + rel.getName()
                    + "' is unmapped.");
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
                throw new CayenneRuntimeException("DbRelatitionship '"
                        + dbRel.getName()
                        + "' has no reverse relationship");
            }

            buf.append(reverse.getName());
        }

        // do the query

        ASTDbPath dbpath = new ASTDbPath(buf.toString());
        ASTList listExp = new ASTList(objects);
        SelectQuery sel = new SelectQuery(destEnt, new ASTIn(dbpath, listExp));
        sel.setFetchingDataRows(true);
        List results = context.performQuery(sel);

        // sort the resulting objects into individual lists for each source object

        List destObjects = context.objectsFromDataRows(destEnt, results, false, false);
        int nrows = destObjects.size();
        for (int k = 0; k < nrows; k++) {
            Map row = (Map) results.get(k);
            ((List) listMap.get(row.get(dbKey))).add(destObjects.get(k));
        }

        // and finally set these lists in the relation targets
        for (int i = 0; i < nobjects; i++) {
            DataObject object = (DataObject) objects.get(i);
            ObjectId oid = object.getObjectId();
            Object list = listMap.get(oid.getValueForAttribute(dbKey));

            ((ValueHolder) object.readPropertyDirectly(relName)).setValueDirectly(list);
        }
    }
}
