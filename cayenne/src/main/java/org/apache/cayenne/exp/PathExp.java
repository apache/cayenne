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

package org.apache.cayenne.exp;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.Entity;

/**
 * Generic path expression.
 * 
 * @since 5.0
 */
public abstract sealed class PathExp extends Expression permits DbIdPathExp, DbPathExp, ObjPathExp {
	protected CayennePath path;
	protected Map<String, String> pathAliases;

	/**
	 * Creates a path node from the single operand: a {@link CayennePath}, or any other object whose String form is
	 * parsed as a path. A null operand results in an empty path. No operands result in a bare node with no path.
	 */
	protected PathExp(Object... operands) {
		if (operands == null || operands.length == 0) {
			return;
		}
		if (operands.length > 1) {
			throw new IllegalArgumentException("A path takes a single operand, got " + operands.length);
		}
		setPath(operands[0]);
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
	 * Returns the path with {@link #getPathAliases() aliases} expanded, so that each segment is a name of an attribute
	 * or a relationship. This is the form that {@link Entity#resolvePath(CayennePath)} expects.
	 */
	public CayennePath getExpandedPath() {
		return path.expandAliases(getPathAliases());
	}

	@Override
	public Map<String, String> getPathAliases() {
		return pathAliases != null ? pathAliases : super.getPathAliases();
	}

	public void setPathAliases(Map<String, String> pathAliases) {
		this.pathAliases = pathAliases;
	}

	/**
	 * Helper method to evaluate path expression with Cayenne Entity.
	 */
	protected Object evaluateEntityNode(Entity<?,?,?> entity) {
		List<Object> components = entity.resolvePath(getExpandedPath());
		return components.isEmpty() ? null : components.getLast();
	}

	@Override
	protected String getExpressionOperator(int index) {
		throw new UnsupportedOperationException("No operator for '" + expName()
				+ "'");
	}

	@Override
	public Expression exists() {
		return ExpressionFactory.exists(this);
	}

	@Override
	public Expression notExists() {
		return ExpressionFactory.notExists(this);
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	protected boolean parenthesizeAsOperand() {
	    return false;
	}
}
