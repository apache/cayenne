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
package org.objectstyle.cayenne.access.trans;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/** 
 * Translates parts of the query to SQL.
 * Always works in the context of parent Translator. 
 * 
 * @author Andrei Adamchik
 */
public abstract class QueryAssemblerHelper {

    protected QueryAssembler queryAssembler;

    public QueryAssemblerHelper() {
    }

    /** Creates QueryAssemblerHelper. Sets queryAssembler property. */
    public QueryAssemblerHelper(QueryAssembler queryAssembler) {
        this.queryAssembler = queryAssembler;
    }

    /** Returns parent QueryAssembler that uses this helper. */
    public QueryAssembler getQueryAssembler() {
        return queryAssembler;
    }

    public void setQueryAssembler(QueryAssembler queryAssembler) {
        this.queryAssembler = queryAssembler;
    }

    /** Translates the part of parent translator's query that is supported 
     * by this PartTranslator. For example, QualifierTranslator will process 
     * qualifier expression, OrderingTranslator - ordering of the query. 
     * In the process of translation parent translator is notified of any 
     * join tables added (so that it can update its "FROM" clause). 
     * Also parent translator is consulted about table aliases to use 
     * when translating columns. */
    public abstract String doTranslation();

    public ObjEntity getObjEntity() {
        return getQueryAssembler().getRootEntity();
    }

    public DbEntity getDbEntity() {
        return getQueryAssembler().getRootDbEntity();
    }

    /** Processes parts of the OBJ_PATH expression. */
    protected void appendObjPath(StringBuffer buf, Expression pathExp) {

        Iterator it = getObjEntity().resolvePathComponents(pathExp);
        ObjRelationship lastRelationship = null;

        while (it.hasNext()) {
            Object pathComp = it.next();

            if (pathComp instanceof ObjRelationship) {
                ObjRelationship rel = (ObjRelationship) pathComp;

                // if this is a last relationship in the path,
                // it needs special handling
                if (!it.hasNext()) {
                    processRelTermination(buf, rel);
                }
                else {
                    // find and add joins ....
                    Iterator relit = rel.getDbRelationships().iterator();
                    while (relit.hasNext()) {
                        queryAssembler.dbRelationshipAdded((DbRelationship) relit.next());
                    }
                }
                lastRelationship = rel;
            }
            else {
                ObjAttribute objAttr = (ObjAttribute) pathComp;
                if (lastRelationship != null) {
                    List lastDbRelList = lastRelationship.getDbRelationships();
                    DbRelationship lastDbRel =
                        (DbRelationship) lastDbRelList.get(lastDbRelList.size() - 1);
                    processColumn(buf, objAttr.getDbAttribute(), lastDbRel);
                }
                else {
                    processColumn(buf, objAttr.getDbAttribute());
                }
            }
        }
    }

    protected void appendDbPath(StringBuffer buf, Expression pathExp) {
        Iterator it = getDbEntity().resolvePathComponents(pathExp);

        while (it.hasNext()) {
            Object pathComp = it.next();
            if (pathComp instanceof DbRelationship) {
                DbRelationship rel = (DbRelationship) pathComp;

                // if this is a last relationship in the path,
                // it needs special handling
                if (!it.hasNext()) {
                    processRelTermination(buf, rel);
                }
                else {
                    // find and add joins ....
                    queryAssembler.dbRelationshipAdded(rel);
                }
            }
            else {
                DbAttribute dbAttr = (DbAttribute) pathComp;
                processColumn(buf, dbAttr);
            }
        }
    }

    /** Appends column name of a column in a root entity. */
    protected void processColumn(StringBuffer buf, Expression nameExp) {
        if (queryAssembler.supportsTableAliases()) {
            String alias = queryAssembler.aliasForTable(getDbEntity());
            buf.append(alias).append('.');
        }

        buf.append(nameExp.getOperand(0));
    }

    protected void processColumn(
        StringBuffer buf,
        DbAttribute dbAttr,
        DbRelationship relationship) {
        String alias = null;

        if (queryAssembler.supportsTableAliases()) {

            if (relationship != null) {
                alias = queryAssembler.aliasForTable(
                        (DbEntity) dbAttr.getEntity(),
                        relationship);
            }

            // sometimes lookup for relationship fails (any specific case other than
            // relationship being null?), so lookup by entity. Note that as CAY-194
            // shows, lookup by DbEntity may produce incorrect results for 
            // reflexive relationships.
            if (alias == null) {
                alias = queryAssembler.aliasForTable((DbEntity) dbAttr.getEntity());
            }
        }

        buf.append(dbAttr.getAliasedName(alias));
    }

    protected void processColumn(StringBuffer buf, DbAttribute dbAttr) {
        String alias =
            (queryAssembler.supportsTableAliases())
                ? queryAssembler.aliasForTable((DbEntity) dbAttr.getEntity())
                : null;

        buf.append(dbAttr.getAliasedName(alias));
    }

    /**
     * Appends SQL code to the query buffer to handle <code>val</code> as a
     * parameter to the PreparedStatement being built. Adds <code>val</code>
     * into QueryAssembler parameter list. 
     * 
     * <p>If <code>val</code> is null, "NULL" is appended to the query. </p>
     * 
     * <p>If <code>val</code> is a DataObject, its  primary key value is 
     * used as a parameter. <i>Only objects with a single column primary key 
     * can be used.</i>
     * 
     * @param buf query buffer.
     * 
     * @param val object that should be appended as a literal to the query.
     * Must be of one of "standard JDBC" types, null or a DataObject.
     * 
     * @param attr DbAttribute that has information on what type of parameter
     * is being appended.
     * 
     */
    protected void appendLiteral(
        StringBuffer buf,
        Object val,
        DbAttribute attr,
        Expression parentExpression) {
        if (val == null) {
            buf.append("NULL");
        }
        else if (val instanceof DataObject) {
            ObjectId id = ((DataObject) val).getObjectId();

            // check if this id is acceptable to be a parameter
            if (id == null) {
                throw new CayenneRuntimeException("Can't use TRANSIENT object as a query parameter.");
            }

            if (id.isTemporary()) {
                throw new CayenneRuntimeException("Can't use NEW object as a query parameter.");
            }

            Map snap = id.getIdSnapshot();
            if (snap.size() != 1) {
                StringBuffer msg = new StringBuffer();
                msg
                    .append("Object must have a single primary key column ")
                    .append("to serve as a query parameter. ")
                    .append("This object has ")
                    .append(snap.size())
                    .append(": ")
                    .append(snap);

                throw new CayenneRuntimeException(msg.toString());
            }

            // checks have been passed, use id value
            appendLiteralDirect(
                buf,
                snap.get(snap.keySet().iterator().next()),
                attr,
                parentExpression);
        }
        else {
            appendLiteralDirect(buf, val, attr, parentExpression);
        }
    }

    /**
     * Appends SQL code to the query buffer to handle <code>val</code> as a
     * parameter to the PreparedStatement being built. Adds <code>val</code>
     * into QueryAssembler parameter list. 
     * 
     * 
     * @param buf query buffer
     * @param val object that should be appended as a literal to the query. 
     * Must be of one of "standard JDBC" types. Can not be null.
     */
    protected void appendLiteralDirect(
        StringBuffer buf,
        Object val,
        DbAttribute attr,
        Expression parentExpression) {
        buf.append('?');

        // we are hoping that when processing parameter list, 
        // the correct type will be
        // guessed without looking at DbAttribute...
        queryAssembler.addToParamList(attr, val);
    }

    /** 
     * Returns database type of expression parameters or
     * null if it can not be determined.
     */
    protected DbAttribute paramsDbType(Expression e) {
        int len = e.getOperandCount();
        // ignore unary expressions
        if (len < 2) {
            return null;
        }

        // naive algorithm:

        // if at least one of the sibling operands is a
        // OBJ_PATH or DB_PATH expression, use its attribute type as
        // a final answer.

        // find attribute or relationship matching the value
        DbAttribute attribute = null;
        DbRelationship relationship = null;
        for (int i = 0; i < len; i++) {
            Object op = e.getOperand(i);

            if (op instanceof Expression) {
                Expression expression = (Expression) op;
                if (expression.getType() == Expression.OBJ_PATH) {
                    Object last = getObjEntity().lastPathComponent(expression);
                    if (last instanceof ObjAttribute) {
                        attribute = ((ObjAttribute) last).getDbAttribute();
                        break;
                    }
                    else if (last instanceof ObjRelationship) {
                        ObjRelationship objRelationship = (ObjRelationship) last;
                        List dbPath = objRelationship.getDbRelationships();
                        if (dbPath.size() > 0) {
                            relationship = (DbRelationship) dbPath.get(dbPath.size() - 1);
                            break;
                        }
                    }
                }
                else if (expression.getType() == Expression.DB_PATH) {
                    Object last = getDbEntity().lastPathComponent(expression);
                    if (last instanceof DbAttribute) {
                        attribute = (DbAttribute) last;
                        break;
                    }
                    else if (last instanceof DbRelationship) {
                        relationship = (DbRelationship) last;
                        break;
                    }
                }
            }
        }

        if (attribute != null) {
            return attribute;
        }

        if (relationship != null) {
            // Can't properly handle multiple joins....
            if (relationship.getJoins().size() == 1) {
                DbJoin join = (DbJoin) relationship.getJoins().get(0);
                return join.getSource();
            }
        }

        return null;
    }

    /** Processes case when an OBJ_PATH expression ends with relationship.
      * If this is a "to many" relationship, a join is added and a column
      * expression for the target entity primary key. If this is a "to one"
      * relationship, column expresion for the source foreign key is added.
      */
    protected void processRelTermination(StringBuffer buf, ObjRelationship rel) {

        Iterator dbRels = rel.getDbRelationships().iterator();

        // scan DbRelationships
        while (dbRels.hasNext()) {
            DbRelationship dbRel = (DbRelationship) dbRels.next();

            // if this is a last relationship in the path,
            // it needs special handling
            if (!dbRels.hasNext()) {
                processRelTermination(buf, dbRel);
            }
            else {
                // find and add joins ....
                queryAssembler.dbRelationshipAdded(dbRel);
            }
        }
    }

    /** 
     * Handles case when a DB_NAME expression ends with relationship.
     * If this is a "to many" relationship, a join is added and a column
     * expression for the target entity primary key. If this is a "to one"
     * relationship, column expresion for the source foreign key is added.
     */
    protected void processRelTermination(StringBuffer buf, DbRelationship rel) {

        if (rel.isToMany()) {
            // append joins
            queryAssembler.dbRelationshipAdded(rel);
        }

        // get last DbRelationship on the list
        List joins = rel.getJoins();
        if (joins.size() != 1) {
            StringBuffer msg = new StringBuffer();
            msg
                .append("OBJ_PATH expressions are only supported ")
                .append("for a single-join relationships. ")
                .append("This relationship has ")
                .append(joins.size())
                .append(" joins.");

            throw new CayenneRuntimeException(msg.toString());
        }

        DbJoin join = (DbJoin) joins.get(0);

        DbAttribute att = null;

        if (rel.isToMany()) {
            DbEntity ent = (DbEntity) join.getRelationship().getTargetEntity();
            List pk = ent.getPrimaryKey();
            if (pk.size() != 1) {
                StringBuffer msg = new StringBuffer();
                msg
                    .append("DB_NAME expressions can only support ")
                    .append("targets with a single column PK. ")
                    .append("This entity has ")
                    .append(pk.size())
                    .append(" columns in primary key.");

                throw new CayenneRuntimeException(msg.toString());
            }

            att = (DbAttribute) pk.get(0);
        }
        else {
            att = join.getSource();
        }

        processColumn(buf, att);
    }
}
