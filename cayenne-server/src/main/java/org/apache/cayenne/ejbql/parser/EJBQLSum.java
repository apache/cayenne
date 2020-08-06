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
package org.apache.cayenne.ejbql.parser;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * @since 3.0
 */
public class EJBQLSum extends EJBQLAggregateColumn {

	// per JPA spec, 4.8.4, SUM type mapping rules are a bit convoluted. Mapping
	// them here...

	private static final long serialVersionUID = 2256495371122671530L;
	static final Map<String, String> typeMap;

	static {
		typeMap = new HashMap<>();
		typeMap.put(Integer.class.getName(), Long.class.getName());
		typeMap.put(Short.class.getName(), Long.class.getName());
		typeMap.put(Float.class.getName(), Double.class.getName());
	}

	public EJBQLSum(int id) {
		super(id);
	}

	@Override
	protected boolean visitNode(EJBQLExpressionVisitor visitor) {
		return visitor.visitSum(this);
	}

	@Override
	public String getFunction() {
		return "SUM";
	}

	@Override
	public String getJavaType(String pathType) {

		if (pathType == null) {
			return "java.lang.Long";
		}

		// type map only contains mappings that are different from the attribute
		// path, so
		// if no mapping exists, return the argument passed to this method.
		String mappedType = typeMap.get(pathType);
		return mappedType != null ? mappedType : pathType;
	}
}
