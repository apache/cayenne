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
package org.apache.cayenne.access.translator;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.DbAttribute;

/**
 * Describes a single PreparedStatement parameter binding. A binding carries immutable metadata
 * (the optional source {@link DbAttribute}, the JDBC type and scale) established at creation time,
 * plus a mutable per-iteration state (value, statement position, {@link ExtendedType}) updated via
 * {@link #include(int, Object, ExtendedType)} / {@link #exclude()} as batch rows are processed.
 *
 * @since 4.0
 */
public class ParameterBinding {

	private static final int EXCLUDED_POSITION = -1;

	private final DbAttribute attribute;
	private final int jdbcType;
	private final int scale;

	private Object value;
	private int statementPosition;
	private ExtendedType<?> extendedType;

	/**
	 * @since 5.0
	 */
	public ParameterBinding(int jdbcType, int scale) {
		this(jdbcType, scale, null);
	}

	/**
	 * @since 5.0
	 */
	public ParameterBinding(int jdbcType, int scale, DbAttribute attribute) {
		this.attribute = attribute;
		this.jdbcType = jdbcType;
		this.scale = scale;
		this.statementPosition = EXCLUDED_POSITION;
	}

	public Object getValue() {
		return value;
	}

	public int getStatementPosition() {
		return statementPosition;
	}

	public boolean isExcluded() {
		return statementPosition == EXCLUDED_POSITION;
	}

	public ExtendedType getExtendedType() {
		return extendedType;
	}

	/**
	 * Marks the binding object as excluded for the current iteration.
	 */
	public void exclude() {
		this.statementPosition = EXCLUDED_POSITION;
		this.value = null;
		this.extendedType = null;
	}

	/**
	 * Sets the value, statement position and {@link ExtendedType} of the binding, thus "including" it in the current
	 * iteration. Returns this binding for chaining.
	 */
	public ParameterBinding include(int statementPosition, Object value, ExtendedType<?> extendedType) {
		this.statementPosition = statementPosition;
		this.value = value;
		this.extendedType = extendedType;
		return this;
	}

	public int getJdbcType() {
		return jdbcType;
	}

	public int getScale() {
		return scale;
	}

	/**
	 * @since 5.0
	 */
	public DbAttribute getAttribute() {
		return attribute;
	}
}
