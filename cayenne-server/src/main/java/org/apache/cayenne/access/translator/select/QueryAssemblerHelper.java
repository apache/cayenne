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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.PatternMatchNode;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.*;
import org.apache.cayenne.util.CayenneMapEntry;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Translates parts of the query to SQL. Always works in the context of parent
 * Translator.
 */
public abstract class QueryAssemblerHelper {

	protected QueryAssembler queryAssembler;
	protected StringBuilder out;
	protected QuotingStrategy strategy;

	/**
	 * Force joining tables for all relations, not only for toMany
	 * @since 4.0
	 */
	private boolean forceJoinForRelations;

	/**
	 * Creates QueryAssemblerHelper initializing with parent
	 * {@link QueryAssembler} and output buffer object.
	 */
	public QueryAssemblerHelper(QueryAssembler queryAssembler) {
		this.queryAssembler = queryAssembler;
		strategy = queryAssembler.getAdapter().getQuotingStrategy();
	}

	public ObjEntity getObjEntity() {
		return queryAssembler.getQueryMetadata().getObjEntity();
	}

	public DbEntity getDbEntity() {
		return queryAssembler.getQueryMetadata().getDbEntity();
	}

	/**
	 * @since 3.0
	 */
	public StringBuilder appendPart(StringBuilder out) {
		this.out = out;
		doAppendPart();
		return out;
	}

	/**
	 * Sets ouput buffer
	 */
	void setOut(StringBuilder out) {
		this.out = out;
	}

	/**
	 * @return output buffer
	 */
	StringBuilder getOut() {
		return out;
	}

	/**
	 * @since 3.0
	 */
	protected abstract void doAppendPart();

	/**
	 * <p>
	 * Outputs the standard JDBC (database agnostic) expression for supplying
	 * the escape character to the database server when supplying a LIKE clause.
	 * This has been factored-out because some database adaptors handle LIKE
	 * differently and they need access to this common method in order not to
	 * repeat this code.
	 * <p>
	 * If there is no escape character defined then this method will not output
	 * anything. An escape character of 0 will mean no escape character.
	 * 
	 * @since 3.1
	 */
	protected void appendLikeEscapeCharacter(PatternMatchNode patternMatchNode) throws IOException {
		char escapeChar = patternMatchNode.getEscapeChar();

		if ('?' == escapeChar) {
			throw new CayenneRuntimeException("the escape character of '?' is illegal for LIKE clauses.");
		}

		if (0 != escapeChar) {
			out.append(" {escape '");
			out.append(escapeChar);
			out.append("'}");
		}
	}

	/**
	 * Processes parts of the OBJ_PATH expression.
	 */
	protected void appendObjPath(Expression pathExp) {

		queryAssembler.resetJoinStack();
		String joinSplitAlias = null;

		for (PathComponent<ObjAttribute, ObjRelationship> component : getObjEntity().resolvePath(pathExp,
				queryAssembler.getPathAliases())) {

			if (component.isAlias()) {
				joinSplitAlias = component.getName();
				for (PathComponent<ObjAttribute, ObjRelationship> aliasPart : component.getAliasedPath()) {

					ObjRelationship relationship = aliasPart.getRelationship();

					if (relationship == null) {
						throw new IllegalStateException("Non-relationship aliased path part: " + aliasPart.getName());
					}

					if (aliasPart.isLast() && component.isLast()) {
						processRelTermination(relationship, aliasPart.getJoinType(), joinSplitAlias);
					} else {
						// find and add joins ....
						for (DbRelationship dbRel : relationship.getDbRelationships()) {
							queryAssembler.dbRelationshipAdded(dbRel, aliasPart.getJoinType(), joinSplitAlias);
						}
					}
				}

				continue;
			}

			ObjRelationship relationship = component.getRelationship();
			ObjAttribute attribute = component.getAttribute();

			if (relationship != null) {

				// if this is a last relationship in the path,
				// it needs special handling
				if (component.isLast()) {
					processRelTermination(relationship, component.getJoinType(), joinSplitAlias);
				} else {
					// find and add joins ....
					for (DbRelationship dbRel : relationship.getDbRelationships()) {
						queryAssembler.dbRelationshipAdded(dbRel, component.getJoinType(), joinSplitAlias);
					}
				}
			} else {
				Iterator<CayenneMapEntry> dbPathIterator = attribute.getDbPathIterator();
				while (dbPathIterator.hasNext()) {
					Object pathPart = dbPathIterator.next();

					if (pathPart == null) {
						throw new CayenneRuntimeException("ObjAttribute has no component: %s", attribute.getName());
					} else if (pathPart instanceof DbRelationship) {
						queryAssembler.dbRelationshipAdded((DbRelationship) pathPart, JoinType.INNER, joinSplitAlias);
					} else if (pathPart instanceof DbAttribute) {
						processColumnWithQuoteSqlIdentifiers((DbAttribute) pathPart, pathExp);
					}
				}

			}
		}
	}

	protected void appendDbPath(Expression pathExp) {

		queryAssembler.resetJoinStack();
		String joinSplitAlias = null;

		for (PathComponent<DbAttribute, DbRelationship> component : getDbEntity().resolvePath(pathExp,
				queryAssembler.getPathAliases())) {

			if (component.isAlias()) {
				joinSplitAlias = component.getName();
				for (PathComponent<DbAttribute, DbRelationship> aliasPart : component.getAliasedPath()) {

					DbRelationship relationship = aliasPart.getRelationship();

					if (relationship == null) {
						throw new IllegalStateException("Non-relationship aliased path part: " + aliasPart.getName());
					}

					if (aliasPart.isLast() && component.isLast()) {
						processRelTermination(relationship, aliasPart.getJoinType(), joinSplitAlias);
					} else {
						queryAssembler.dbRelationshipAdded(relationship, component.getJoinType(), joinSplitAlias);
					}
				}

				continue;
			}

			DbRelationship relationship = component.getRelationship();

			if (relationship != null) {

				// if this is a last relationship in the path,
				// it needs special handling
				if (component.isLast()) {
					processRelTermination(relationship, component.getJoinType(), joinSplitAlias);
				} else {
					// find and add joins ....
					queryAssembler.dbRelationshipAdded(relationship, component.getJoinType(), joinSplitAlias);
				}
			} else {
				processColumnWithQuoteSqlIdentifiers(component.getAttribute(), pathExp);
			}
		}
	}

	protected void processColumn(DbAttribute dbAttr) {
		processColumnWithQuoteSqlIdentifiers(dbAttr, null);
	}

	protected void processColumnWithQuoteSqlIdentifiers(DbAttribute dbAttr, Expression pathExp) {

		String alias = (queryAssembler.supportsTableAliases()) ? queryAssembler.getCurrentAlias() : null;
		out.append(strategy.quotedIdentifier(dbAttr.getEntity(), alias, dbAttr.getName()));
	}

	/**
	 * Appends SQL code to the query buffer to handle <code>val</code> as a
	 * parameter to the PreparedStatement being built. Adds <code>val</code>
	 * into QueryAssembler parameter list.
	 * <p>
	 * If <code>val</code> is null, "NULL" is appended to the query.
	 * </p>
	 * <p>
	 * If <code>val</code> is a DataObject, its primary key value is used as a
	 * parameter. <i>Only objects with a single column primary key can be
	 * used.</i>
	 * 
	 * @param val
	 *            object that should be appended as a literal to the query. Must
	 *            be of one of "standard JDBC" types, null or a DataObject.
	 * @param attr
	 *            DbAttribute that has information on what type of parameter is
	 *            being appended.
	 */
	protected void appendLiteral(Object val, DbAttribute attr, Expression parentExpression) throws IOException {

		if (val == null) {
			out.append("NULL");
		} else if (val instanceof Persistent) {
			// TODO: see cay1796
			// This check is unlikely to happen,
			// since Expression got ObjectId from Persistent object.
			// Left for future research.
			ObjectId id = ((Persistent) val).getObjectId();

			// check if this id is acceptable to be a parameter
			if (id == null) {
				throw new CayenneRuntimeException("Can't use TRANSIENT object as a query parameter.");
			}

			if (id.isTemporary()) {
				throw new CayenneRuntimeException("Can't use NEW object as a query parameter.");
			}

			Map<String, Object> snap = id.getIdSnapshot();
			if (snap.size() != 1) {
				throw new CayenneRuntimeException("Object must have a single primary key column to serve " +
						"as a query parameter. This object has %s: %s", snap.size(), snap);
			}

			// checks have been passed, use id value
			appendLiteralDirect(snap.get(snap.keySet().iterator().next()), attr, parentExpression);
		} else if (val instanceof ObjectId) {

			ObjectId id = (ObjectId) val;

			if (id.isTemporary()) {
				throw new CayenneRuntimeException("Can't use NEW object as a query parameter.");
			}

			Map<String, Object> snap = id.getIdSnapshot();
			if (snap.size() != 1) {
				throw new CayenneRuntimeException("Object must have a single primary key column to serve " +
						"as a query parameter. This object has %s: %s", snap.size(), snap);
			}

			// checks have been passed, use id value
			appendLiteralDirect(snap.get(snap.keySet().iterator().next()), attr, parentExpression);
		} else {
			appendLiteralDirect(val, attr, parentExpression);
		}
	}

	/**
	 * Appends SQL code to the query buffer to handle <code>val</code> as a
	 * parameter to the PreparedStatement being built. Adds <code>val</code>
	 * into QueryAssembler parameter list.
	 */
	protected void appendLiteralDirect(Object val, DbAttribute attr, Expression parentExpression) throws IOException {
		out.append('?');
		queryAssembler.addToParamList(attr, val);
	}

	/**
	 * Returns database type of expression parameters or null if it can not be
	 * determined.
	 */
	protected DbAttribute paramsDbType(Expression e) {
		int len = e.getOperandCount();

		// for unary expressions, find parent binary - this is a hack mainly to
		// support
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
					PathComponent<ObjAttribute, ObjRelationship> last = getObjEntity().lastPathComponent(expression,
							queryAssembler.getPathAliases());

					// TODO: handle EmbeddableAttribute
					// if (last instanceof EmbeddableAttribute)
					// break;

					if (last.getAttribute() != null) {
						attribute = last.getAttribute().getDbAttribute();
						break;
					} else if (last.getRelationship() != null) {
						List<DbRelationship> dbPath = last.getRelationship().getDbRelationships();
						if (dbPath.size() > 0) {
							relationship = dbPath.get(dbPath.size() - 1);
							break;
						}
					}
				} else if (expression.getType() == Expression.DB_PATH) {
					PathComponent<DbAttribute, DbRelationship> last = getDbEntity().lastPathComponent(expression,
							queryAssembler.getPathAliases());
					if (last.getAttribute() != null) {
						attribute = last.getAttribute();
						break;
					} else if (last.getRelationship() != null) {
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
	 * Processes case when an OBJ_PATH expression ends with relationship. If
	 * this is a "to many" relationship, a join is added and a column expression
	 * for the target entity primary key. If this is a "to one" relationship,
	 * column expression for the source foreign key is added.
	 * 
	 * @since 3.0
	 */
	protected void processRelTermination(ObjRelationship rel, JoinType joinType, String joinSplitAlias) {

		Iterator<DbRelationship> dbRels = rel.getDbRelationships().iterator();

		// scan DbRelationships
		while (dbRels.hasNext()) {
			DbRelationship dbRel = dbRels.next();

			// if this is a last relationship in the path,
			// it needs special handling
			if (!dbRels.hasNext()) {
				processRelTermination(dbRel, joinType, joinSplitAlias);
			} else {
				// find and add joins ....
				queryAssembler.dbRelationshipAdded(dbRel, joinType, joinSplitAlias);
			}
		}
	}

	/**
	 * Handles case when a DB_NAME expression ends with relationship. If this is
	 * a "to many" relationship, a join is added and a column expression for the
	 * target entity primary key. If this is a "to one" relationship, column
	 * expression for the source foreign key is added.
	 * 
	 * @since 3.0
	 */
	protected void processRelTermination(DbRelationship rel, JoinType joinType, String joinSplitAlias) {

		if (forceJoinForRelations || rel.isToMany()) {
			// append joins
			queryAssembler.dbRelationshipAdded(rel, joinType, joinSplitAlias);
		}

		// get last DbRelationship on the list
		List<DbJoin> joins = rel.getJoins();
		if (joins.size() != 1) {
			String msg = "OBJ_PATH expressions are only supported for a single-join relationships. " +
					"This relationship has %s joins.";
			throw new CayenneRuntimeException(msg, joins.size());
		}

		DbJoin join = joins.get(0);

		DbAttribute attribute;

		if (rel.isToMany()) {
			DbEntity ent = join.getRelationship().getTargetEntity();
			Collection<DbAttribute> pk = ent.getPrimaryKeys();
			if (pk.size() != 1) {
				String msg = "DB_NAME expressions can only support targets with a single column PK. " +
						"This entity has %d columns in primary key.";
				throw new CayenneRuntimeException(msg, pk.size());
			}

			attribute = pk.iterator().next();
		} else {
			attribute = forceJoinForRelations ? join.getTarget() : join.getSource();
		}

		processColumn(attribute);
	}

	/**
	 * Force joining tables for all relations, not only for toMany
	 * @since 4.0
	 */
	protected void setForceJoinForRelations(boolean forceJoinForRelations) {
		this.forceJoinForRelations = forceJoinForRelations;
	}
}
