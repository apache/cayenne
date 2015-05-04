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

package org.apache.cayenne.access.jdbc;

/**
 * Describes PreparedStatement parameter binding.
 * 
 * @since 4.0
 */
public class SQLParameterBinding {

	protected int jdbcType;
	protected int scale;
	protected Object value;

	public SQLParameterBinding(Object value, int jdbcType, int scale) {
		this.value = value;
		this.jdbcType = jdbcType;
		this.scale = scale;
	}

	public int getJdbcType() {
		return jdbcType;
	}

	public int getScale() {
		return scale;
	}

	public Object getValue() {
		return value;
	}

	public void setJdbcType(int i) {
		jdbcType = i;
	}

	public void setScale(int i) {
		scale = i;
	}

	public void setValue(Object object) {
		value = object;
	}
}
