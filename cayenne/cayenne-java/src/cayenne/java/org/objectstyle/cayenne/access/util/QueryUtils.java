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
package org.objectstyle.cayenne.access.util;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Implements helper methods that perform different query-related operations.
 * 
 * @author Andrus Adamchik
 * @deprecated In 1.2 Cayenne supports specialized queries such as
 *             {@link org.objectstyle.cayenne.query.ObjectIdQuery} and
 *             {@link org.objectstyle.cayenne.query.RelationshipQuery}, making this class
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