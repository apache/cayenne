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
import org.apache.cayenne.map.ProcedureParameter;

/**
 * Describes a PreparedStatement parameter binding mapped to a CallableStatment.
 * 
 * @since 4.0
 */
public class ProcedureParameterBinding {
	public ProcedureParameterBinding(ProcedureParameter param) {
		this.param = param;
	}

	private final ProcedureParameter param;
	private Object value;
	private int statementPosition;
	private ExtendedType extendedType;

	public ProcedureParameter getParam() {
		return param;
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

	public ExtendedType getExtendedType() {
		return extendedType;
	}

	public void setExtendedType(ExtendedType extendedType) {
		this.extendedType = extendedType;
	}
}
