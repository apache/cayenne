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

import java.io.IOException;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.reflect.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 5.0
 */
public final class ObjPathExp extends PathExp {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjPathExp.class);

	public static final String OBJ_PREFIX = "obj:";

	public ObjPathExp(Object... operands) {
		super(operands);
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		return (o instanceof Persistent)
				? ((Persistent) o).readNestedProperty(path)
				: (o instanceof Entity)
					? evaluateEntityNode((Entity<?,?,?>) o)
					: PropertyUtils.getProperty(o, path);
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		ObjPathExp copy = new ObjPathExp();
		copy.path = path;
		copy.setPathAliases(pathAliases);
		return copy;
	}

	@Override
	public void appendAsString(Appendable out) throws IOException {
		out.append(path.value());
	}

}
