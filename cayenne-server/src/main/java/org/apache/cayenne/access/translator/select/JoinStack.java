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

import java.util.List;
import java.util.function.Function;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjEntity;

/**
 * Encapsulates join reuse/split logic used in SelectQuery processing. All
 * expression path's that exist in the query (in the qualifier, etc.) are
 * processed to produce a combined join tree.
 * 
 * @since 3.0
 */
public class JoinStack {

	protected JoinTreeNode rootNode;
	protected JoinTreeNode topNode;
	private QuotingStrategy quotingStrategy;

	private int aliasCounter;

	/**
	 * Helper class to process DbEntity qualifiers
	 */
	private QualifierTranslator qualifierTranslator;

	protected JoinStack(DbAdapter dbAdapter, QueryAssembler assembler) {
		this.rootNode = new JoinTreeNode(this);
		this.rootNode.setTargetTableAlias(newAlias());

		this.quotingStrategy = dbAdapter.getQuotingStrategy();
		this.qualifierTranslator = dbAdapter.getQualifierTranslator(assembler);

		resetStack();
	}

	String getCurrentAlias() {
		return topNode.getTargetTableAlias();
	}

	/**
	 * Returns the number of configured joins.
	 */
	protected int size() {
		// do not count root as a join
		return rootNode.size() - 1;
	}

	void appendRootWithQuoteSqlIdentifiers(StringBuilder out, DbEntity rootEntity) {

		out.append(quotingStrategy.quotedFullyQualifiedName(rootEntity));
		out.append(' ');
		out.append(quotingStrategy.quotedIdentifier(rootEntity, rootNode.getTargetTableAlias()));
	}

	/**
	 * Appends all configured joins to the provided output object.
	 */
	protected void appendJoins(StringBuilder out) {

		// skip root, recursively append its children
		for (JoinTreeNode child : rootNode.getChildren()) {
			appendJoinSubtree(out, child);
		}
	}

	protected void appendJoinSubtree(StringBuilder out, JoinTreeNode node) {

		DbRelationship relationship = node.getRelationship();

		DbEntity targetEntity = relationship.getTargetEntity();
		String srcAlias = node.getSourceTableAlias();
		String targetAlias = node.getTargetTableAlias();

		switch (node.getJoinType()) {
		case INNER:
			out.append(" JOIN");
			break;
		case LEFT_OUTER:
			out.append(" LEFT JOIN");
			break;
		default:
			throw new IllegalArgumentException("Unsupported join type: " + node.getJoinType());
		}

		out.append(' ');
		out.append(quotingStrategy.quotedFullyQualifiedName(targetEntity));

		out.append(' ');
		out.append(quotingStrategy.quotedIdentifier(targetEntity, targetAlias));
		out.append(" ON (");

		List<DbJoin> joins = relationship.getJoins();
		int len = joins.size();
		for (int i = 0; i < len; i++) {
			DbJoin join = joins.get(i);
			if (i > 0) {
				out.append(" AND ");
			}

			out.append(quotingStrategy.quotedIdentifier(relationship.getSourceEntity(), srcAlias, join.getSourceName()));
			out.append(" = ");
			out.append(quotingStrategy.quotedIdentifier(targetEntity, targetAlias, join.getTargetName()));
		}

		/*
		 * Attaching root Db entity's qualifier
		 */
		Expression dbQualifier = targetEntity.getQualifier();
		if (dbQualifier != null) {
			dbQualifier = dbQualifier.transform(new JoinedDbEntityQualifierTransformer(node));

			if (len > 0) {
				out.append(" AND ");
			}
			qualifierTranslator.setOut(out);
			qualifierTranslator.doAppendPart(dbQualifier);
		}

		out.append(')');

		for (JoinTreeNode child : node.getChildren()) {
			appendJoinSubtree(out, child);
		}
	}

	/**
	 * Append join information to the qualifier - the part after "WHERE".
	 */
	protected void appendQualifier(StringBuilder out, boolean firstQualifierElement) {
		// nothing as standard join is performed before "WHERE"
	}

	/**
	 * Pops the stack all the way to the root node.
	 */
	void resetStack() {
		topNode = rootNode;
	}

	/**
	 * Finds or creates a JoinTreeNode for the given arguments and sets it as
	 * the next current join.
	 */
	void pushJoin(DbRelationship relationship, JoinType joinType, String alias) {
		topNode = topNode.findOrCreateChild(relationship, joinType, alias);
	}

	protected String newAlias() {
		return "t" + aliasCounter++;
	}

	/**
	 * Class to translate *joined* DB Entity qualifiers annotation to *current*
	 * Obj-entity qualifiers annotation This is done by changing all Obj-paths
	 * to concatenated Db-paths to root entity and rejecting all original
	 * Db-paths
	 */
	class JoinedDbEntityQualifierTransformer implements Function<Object, Object> {

		StringBuilder pathToRoot;

		JoinedDbEntityQualifierTransformer(JoinTreeNode node) {
			pathToRoot = new StringBuilder();
			while (node != null && node.getRelationship() != null) {
				String relName = node.getRelationship().getName();

				/*
				 * We must be in the same join as 'node', otherwise incorrect
				 * join statement like JOIN t1 ... ON (t0.id=t1.id AND
				 * t2.qualifier=0) could be generated
				 */
				if (node.getJoinType() == JoinType.LEFT_OUTER) {
					relName += Entity.OUTER_JOIN_INDICATOR;
				}
				relName += ObjEntity.PATH_SEPARATOR;

				pathToRoot.insert(0, relName);
				node = node.getParent();
			}
		}

		public Object apply(Object input) {
			if (input instanceof ASTPath) {
				return new ASTDbPath(pathToRoot.toString() + ((ASTPath) input).getPath());
			}
			return input;
		}
	}
}
