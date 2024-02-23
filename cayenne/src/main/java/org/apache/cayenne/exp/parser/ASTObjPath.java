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

import java.io.IOException;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.util.CayenneMapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ASTObjPath extends ASTPath {

	private static final long serialVersionUID = -3574281576491705706L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ASTObjPath.class);

	public static final String OBJ_PREFIX = "obj:";

	/**
	 * Constructor used by expression parser. Do not invoke directly.
	 */
	ASTObjPath(int id) {
		super(id);
	}

	public ASTObjPath() {
		super(ExpressionParserTreeConstants.JJTOBJPATH);
	}

	public ASTObjPath(String value) {
		super(ExpressionParserTreeConstants.JJTOBJPATH);
		setPath(value);
	}

	public ASTObjPath(CayennePath path) {
		super(ExpressionParserTreeConstants.JJTOBJPATH);
		setPath(path);
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
		ASTObjPath copy = new ASTObjPath(id);
		copy.path = path;
		copy.setPathAliases(pathAliases);
		return copy;
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
		out.append(rootId);
		out.append('.');
		out.append(path.value());
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void appendAsString(Appendable out) throws IOException {
		out.append(path.value());
	}

	@Override
	public int getType() {
		return Expression.OBJ_PATH;
	}

	void injectValue(Object source, Object value) {
		if (getPath().length() == 1) {
			try {
				String firstSegment = getPath().first().value();
				if (source instanceof Persistent) {
					Persistent persistent = (Persistent) source;
					persistent.writeProperty(firstSegment, dynamicCastValue(persistent, value));
				} else {
					PropertyUtils.setProperty(source, firstSegment, value);
				}
			} catch (CayenneRuntimeException ex) {
				LOGGER.warn("Failed to inject value " + value + " on path " + getPath() + " to " + source, ex);
			}
		}
	}

	private Object dynamicCastValue(Persistent source, Object value) {
		Class<?> javaClass = getDataTypeForObject(source);
		if(javaClass == null) {
			return value;
		}

		if(javaClass.isEnum()) {
			@SuppressWarnings({"unchecked", "rawtypes"})
			Enum enumValue = Enum.valueOf((Class<Enum>) javaClass, value.toString());
			return enumValue;
		}

		return value;
	}

	private Class<?> getDataTypeForObject(Persistent source) {
		ObjectContext context = source.getObjectContext();
		ObjectId objectId = source.getObjectId();
		if(context == null || objectId == null) {
			return null;
		}

		ObjEntity entity = context.getEntityResolver().getObjEntity(objectId.getEntityName());
		CayenneMapEntry entry = evaluateEntityNode(entity);
		if(!(entry instanceof ObjAttribute)) {
			return null;
		}

		return ((ObjAttribute) entry).getJavaClass();
	}
}
