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

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.QueryEngine;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.SelectQuery;

/**
 * Implements helper methods that perform different query-related operations.
 * 
 * @author Andrus Adamchik
 * @deprecated In 1.2 Cayenne supports specialized queries such as
 *             {@link org.apache.cayenne.query.ObjectIdQuery} and
 *             {@link org.apache.cayenne.query.RelationshipQuery}, making this class
 *             redundant.
 */
public class QueryUtils {

    /**
     * Creates and returns a select query that can be used to fetch an object given an
     * ObjectId.
     */
    public static SelectQuery selectObjectForId(ObjectId oid) {
        return new SelectQuery(oid.getEntityName(), ExpressionFactory.matchAllDbExp(oid
                .getIdSnapshot(), Expression.EQUAL_TO));
    }

    /**
     * Creates and returns a select query that can be used to fetch a list of objects
     * given a list of ObjectIds. All ObjectIds must belong to the same entity.
     */
    public static SelectQuery selectQueryForIds(List oids) {
        if (oids == null || oids.size() == 0) {
            throw new IllegalArgumentException("List must contain at least one ObjectId");
        }

        SelectQuery sel = new SelectQuery();
        sel.setRoot(((ObjectId) oids.get(0)).getEntityName());

        Iterator it = oids.iterator();

        ObjectId firstId = (ObjectId) it.next();
        Expression exp = ExpressionFactory.matchAllDbExp(
                firstId.getIdSnapshot(),
                Expression.EQUAL_TO);

        while (it.hasNext()) {
            ObjectId anId = (ObjectId) it.next();
            exp = exp.orExp(ExpressionFactory.matchAllDbExp(
                    anId.getIdSnapshot(),
                    Expression.EQUAL_TO));
        }

        sel.setQualifier(exp);
        return sel;
    }

    /**
     * Generates a SelectQuery that can be used to fetch relationship destination objects
     * given a source object of a to-many relationship.
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
                throw new CayenneRuntimeException("DbRelationship '"
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
