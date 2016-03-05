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
package org.apache.cayenne.access.translator;

import org.apache.cayenne.access.types.ExtendedType;

/**
 * Describes a PreparedStatement parameter generic binding.
 * 
 * @since 4.0
 */
public class Binding {

	static final int EXCLUDED_POSITION = -1;

	private Object value;
	private int statementPosition;
	private ExtendedType extendedType;
	private Integer type;
	private int scale;

	public Binding(ExtendedType extendedType) {
		this.statementPosition = EXCLUDED_POSITION;
		this.extendedType = extendedType;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public int getStatementPosition() {
		return statementPosition;
	}

	public void setStatementPosition(int statementPosition) {
		this.statementPosition = statementPosition;
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
	}

	/**
	 * Sets the value of the binding and initializes statement position var,
	 * thus "including" this binding in the current iteration.
	 */
	public void include(int statementPosition, Object value) {
		this.statementPosition = statementPosition;
		this.value = value;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}
}
