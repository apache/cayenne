/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.exp.parser;

import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Generic path expression.
 * 
 * @since 1.1
 */
public abstract class ASTPath extends SimpleNode {

	private static final long serialVersionUID = -8099822503585617295L;
	
	protected CayennePath path;
	protected Map<String, String> pathAliases;

	ASTPath(int i) {
		super(i);
	}

	@Override
	public int getOperandCount() {
		return 1;
	}

	@Override
	public Object getOperand(int index) {
		if (index == 0) {
			return path;
		}

		throw new ArrayIndexOutOfBoundsException(index);
	}

	@Override
	public void setOperand(int index, Object value) {
		if (index != 0) {
			throw new ArrayIndexOutOfBoundsException(index);
		}

		setPath(value);
	}

	protected void setPath(CayennePath path) {
		this.path = path;
	}

	protected void setPath(Object path) {
		if(path instanceof CayennePath) {
			setPath((CayennePath) path);
		} else {
			this.path = (path != null)
					? CayennePath.of(path.toString())
					: CayennePath.EMPTY_PATH;
		}
	}

	public CayennePath getPath() {
		return path;
	}

	/**
	 * @since 3.0
	 */
	@Override
	public Map<String, String> getPathAliases() {
		return pathAliases != null ? pathAliases : super.getPathAliases();
	}

	/**
	 * @since 3.0
	 */
	public void setPathAliases(Map<String, String> pathAliases) {
		this.pathAliases = pathAliases;
	}

	/**
	 * Helper method to evaluate path expression with Cayenne Entity.
	 */
	protected CayenneMapEntry evaluateEntityNode(Entity<?,?,?> entity) {
		Iterator<? extends PathComponent<?, ?>> path = entity.resolvePath(this, getPathAliases()).iterator();
		PathComponent<?, ?> next = null;
		while (path.hasNext()) {
			next = path.next();
		}

		if(next == null) {
			return null;
		}

		if(next.getRelationship() != null) {
			return next.getRelationship();
		}
		return next.getAttribute();
	}

	@Override
	protected String getExpressionOperator(int index) {
		throw new UnsupportedOperationException("No operator for '" + ExpressionParserTreeConstants.jjtNodeName[id]
				+ "'");
	}

	/**
	 * @inheritDoc
	 * @since 5.0
	 */
	@Override
	public Expression exists() {
		return ExpressionFactory.exists(this);
	}

	/**
	 * @inheritDoc
	 * @since 5.0
	 */
	@Override
	public Expression notExists() {
		return ExpressionFactory.notExists(this);
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}
}
