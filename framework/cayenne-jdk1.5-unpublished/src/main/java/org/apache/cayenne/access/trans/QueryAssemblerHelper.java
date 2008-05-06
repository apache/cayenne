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

package org.apache.cayenne.access.trans;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Translates parts of the query to SQL. Always works in the context of parent Translator.
 * 
 * @author Andrus Adamchik
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

    /**
     * Translates the part of parent translator's query that is supported by this
     * PartTranslator. For example, QualifierTranslator will process qualifier expression,
     * OrderingTranslator - ordering of the query. In the process of translation parent
     * translator is notified of any join tables added (so that it can update its "FROM"
     * clause). Also parent translator is consulted about table aliases to use when
     * translating columns.
     */
    public abstract String doTranslation();

    public ObjEntity getObjEntity() {
        return getQueryAssembler().getRootEntity();
    }

    public DbEntity getDbEntity() {
        return getQueryAssembler().getRootDbEntity();
    }

    /**
     * Processes parts of the OBJ_PATH expression.
     */
    protected void appendObjPath(StringBuffer buf, Expression pathExp) {

        queryAssembler.resetJoinStack();

        for (PathComponent<ObjAttribute, ObjRelationship> component : getObjEntity()
                .resolvePath(pathExp, queryAssembler.getPathAliases())) {

            ObjRelationship relationship = component.getRelationship();
            ObjAttribute attribute = component.getAttribute();

            if (relationship != null) {

                // if this is a last relationship in the path,
                // it needs special handling
                if (component.isLast()) {
                    processRelTermination(buf, relationship, component.getJoinType());
                }
                else {
                    // find and add joins ....
                    for (DbRelationship dbRel : relationship.getDbRelationships()) {
                        queryAssembler
                                .dbRelationshipAdded(dbRel, component.getJoinType());
                    }
                }
            }
            else {
                Iterator<CayenneMapEntry> dbPathIterator = attribute.getDbPathIterator();
                while (dbPathIterator.hasNext()) {
                    Object pathPart = dbPathIterator.next();

                    if (pathPart == null) {
                        throw new CayenneRuntimeException(
                                "ObjAttribute has no component: " + attribute.getName());
                    }
                    else if (pathPart instanceof DbRelationship) {
                        queryAssembler.dbRelationshipAdded(
                                (DbRelationship) pathPart,
                                JoinType.INNER);
                    }
                    else if (pathPart instanceof DbAttribute) {
                        processColumn(buf, (DbAttribute) pathPart);
                    }
                }

            }
        }
    }

    protected void appendDbPath(StringBuffer buf, Expression pathExp) {

        queryAssembler.resetJoinStack();

        for (PathComponent<DbAttribute, DbRelationship> component : getDbEntity()
                .resolvePath(pathExp, queryAssembler.getPathAliases())) {

            DbRelationship relationship = component.getRelationship();

            if (relationship != null) {

                // if this is a last relationship in the path,
                // it needs special handling
                if (component.isLast()) {
                    processRelTermination(buf, relationship, component.getJoinType());
                }
                else {
                    // find and add joins ....
                    queryAssembler.dbRelationshipAdded(relationship, component
                            .getJoinType());
                }
            }
            else {
                processColumn(buf, component.getAttribute());
            }
        }
    }

    protected void processColumn(StringBuffer buf, DbAttribute dbAttr) {
        String alias = (queryAssembler.supportsTableAliases()) ? queryAssembler
                .getCurrentAlias() : null;

        buf.append(dbAttr.getAliasedName(alias));
    }

    /**
     * Appends SQL code to the query buffer to handle <code>val</code> as a parameter to
     * the PreparedStatement being built. Adds <code>val</code> into QueryAssembler
     * parameter list.
     * <p>
     * If <code>val</code> is null, "NULL" is appended to the query.
     * </p>
     * <p>
     * If <code>val</code> is a DataObject, its primary key value is used as a
     * parameter. <i>Only objects with a single column primary key can be used.</i>
     * 
     * @param buf query buffer.
     * @param val object that should be appended as a literal to the query. Must be of one
     *            of "standard JDBC" types, null or a DataObject.
     * @param attr DbAttribute that has information on what type of parameter is being
     *            appended.
     */
    protected void appendLiteral(
            StringBuffer buf,
            Object val,
            DbAttribute attr,
            Expression parentExpression) {
        if (val == null) {
            buf.append("NULL");
        }
        else if (val instanceof Persistent) {
            ObjectId id = ((Persistent) val).getObjectId();

            // check if this id is acceptable to be a parameter
            if (id == null) {
                throw new CayenneRuntimeException(
                        "Can't use TRANSIENT object as a query parameter.");
            }

            if (id.isTemporary()) {
                throw new CayenneRuntimeException(
                        "Can't use NEW object as a query parameter.");
            }

            Map<String, Object> snap = id.getIdSnapshot();
            if (snap.size() != 1) {
                StringBuilder msg = new StringBuilder();
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
     * Appends SQL code to the query buffer to handle <code>val</code> as a parameter to
     * the PreparedStatement being built. Adds <code>val</code> into QueryAssembler
     * parameter list.
     * 
     * @param buf query buffer
     * @param val object that should be appended as a literal to the query. Must be of one
     *            of "standard JDBC" types. Can not be null.
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
     * Returns database type of expression parameters or null if it can not be determined.
     */
    protected DbAttribute paramsDbType(Expression e) {
        int len = e.getOperandCount();

        // for unary expressions, find parent binary - this is a hack mainly to support
        // ASTList
        if (len < 2) {

            if (e instanceof SimpleNode) {
                Expression parent = (Expression) ((SimpleNode) e).jjtGetParent();
                if (parent != null) {
                    return paramsDbType(parent);
                }
            }

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
                    PathComponent<ObjAttribute, ObjRelationship> last = getObjEntity()
                            .lastPathComponent(
                                    expression,
                                    queryAssembler.getPathAliases());

                    // TODO: handle EmbeddableAttribute
                    // if (last instanceof EmbeddableAttribute)
                    // break;

                    if (last.getAttribute() != null) {
                        attribute = last.getAttribute().getDbAttribute();
                        break;
                    }
                    else if (last.getRelationship() != null) {
                        List<DbRelationship> dbPath = last
                                .getRelationship()
                                .getDbRelationships();
                        if (dbPath.size() > 0) {
                            relationship = dbPath.get(dbPath.size() - 1);
                            break;
                        }
                    }
                }
                else if (expression.getType() == Expression.DB_PATH) {
                    PathComponent<DbAttribute, DbRelationship> last = getDbEntity()
                            .lastPathComponent(
                                    expression,
                                    queryAssembler.getPathAliases());
                    if (last.getAttribute() != null) {
                        attribute = last.getAttribute();
                        break;
                    }
                    else if (last.getRelationship() != null) {
                        relationship = last.getRelationship();
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
                DbJoin join = relationship.getJoins().get(0);
                return join.getSource();
            }
        }

        return null;
    }

    /**
     * Processes case when an OBJ_PATH expression ends with relationship. If this is a "to
     * many" relationship, a join is added and a column expression for the target entity
     * primary key. If this is a "to one" relationship, column expression for the source
     * foreign key is added.
     * 
     * @since 3.0
     */
    protected void processRelTermination(
            StringBuffer buf,
            ObjRelationship rel,
            JoinType joinType) {
        Iterator<DbRelationship> dbRels = rel.getDbRelationships().iterator();

        // scan DbRelationships
        while (dbRels.hasNext()) {
            DbRelationship dbRel = dbRels.next();

            // if this is a last relationship in the path,
            // it needs special handling
            if (!dbRels.hasNext()) {
                processRelTermination(buf, dbRel, joinType);
            }
            else {
                // find and add joins ....
                queryAssembler.dbRelationshipAdded(dbRel, joinType);
            }
        }
    }

    /**
     * Handles case when a DB_NAME expression ends with relationship. If this is a "to
     * many" relationship, a join is added and a column expression for the target entity
     * primary key. If this is a "to one" relationship, column expression for the source
     * foreign key is added.
     * 
     * @since 3.0
     */
    protected void processRelTermination(
            StringBuffer buf,
            DbRelationship rel,
            JoinType joinType) {
        if (rel.isToMany()) {
            // append joins
            queryAssembler.dbRelationshipAdded(rel, joinType);
        }

        // get last DbRelationship on the list
        List<DbJoin> joins = rel.getJoins();
        if (joins.size() != 1) {
            StringBuilder msg = new StringBuilder();
            msg
                    .append("OBJ_PATH expressions are only supported ")
                    .append("for a single-join relationships. ")
                    .append("This relationship has ")
                    .append(joins.size())
                    .append(" joins.");

            throw new CayenneRuntimeException(msg.toString());
        }

        DbJoin join = joins.get(0);

        DbAttribute attribute = null;

        if (rel.isToMany()) {
            DbEntity ent = (DbEntity) join.getRelationship().getTargetEntity();
            Collection<DbAttribute> pk = ent.getPrimaryKeys();
            if (pk.size() != 1) {
                StringBuilder msg = new StringBuilder();
                msg
                        .append("DB_NAME expressions can only support ")
                        .append("targets with a single column PK. ")
                        .append("This entity has ")
                        .append(pk.size())
                        .append(" columns in primary key.");

                throw new CayenneRuntimeException(msg.toString());
            }

            attribute = pk.iterator().next();
        }
        else {
            attribute = join.getSource();
        }

        processColumn(buf, attribute);
    }
}
