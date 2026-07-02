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

package org.apache.cayenne.velocity;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.util.ConversionUtil;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * A custom Velocity directive to create a PreparedStatement parameter text.
 * There are the following possible invocation formats inside the template:
 * 
 * <pre>
 * #bind(value) - e.g. #bind($xyz)
 * #bind(value jdbc_type_name) - e.g. #bind($xyz 'VARCHAR'). This is the most common and useful form.
 * #bind(value jdbc_type_name, scale) - e.g. #bind($xyz 'VARCHAR' 2)
 * </pre>
 * <p>
 * Other examples:
 * </p>
 * <p>
 * <strong>Binding literal parameter value:</strong>
 * </p>
 * <p>
 * <code>"WHERE SOME_COLUMN &gt; #bind($xyz)"</code> produces
 * <code>"WHERE SOME_COLUMN &gt; ?"</code> and also places the value of the
 * "xyz" parameter in the context "bindings" collection.
 * </p>
 * <p>
 * <strong>Binding ID column of a Persistent value:</strong>
 * </p>
 * <p>
 * <code>"WHERE ID_COL1 = #bind($helper.cayenneExp($xyz, 'db:ID_COL2')) 
 * AND ID_COL2 = #bind($helper.cayenneExp($xyz, 'db:ID_COL2'))"</code> produces
 * <code>"WHERE ID_COL1 = ? AND ID_COL2 = ?"</code> and also places the values
 * of id columns of the Persistent parameter "xyz" in the context "bindings"
 * collection.
 * </p>
 * 
 * @since 1.1
 */
public class BindDirective extends Directive {

	@Override
	public String getName() {
		return "bind";
	}

	@Override
	public int getType() {
		return LINE;
	}

	/**
	 * Extracts the value of the object property to render and passes control to
	 * {@link #render(InternalContextAdapter, Writer, PSParameter)} to do
	 * the actual rendering.
	 */
	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException,
			ResourceNotFoundException, ParseErrorException, MethodInvocationException {

		Object value = getChild(context, node, 0);
		Object type = getChild(context, node, 1);
		int scale = ConversionUtil.toInt(getChild(context, node, 2), -1);
		String typeString = type != null ? type.toString() : null;

		if (value instanceof Collection) {
			Iterator<?> it = ((Collection) value).iterator();
			while (it.hasNext()) {
				render(context, writer, node, it.next(), typeString, scale);

				if (it.hasNext()) {
					writer.write(',');
				}
			}
		} else {
			render(context, writer, node, value, typeString, scale);
		}

		return true;
	}

	/**
	 * @since 3.0
	 */
	protected void render(InternalContextAdapter context, Writer writer, Node node, Object value, String typeString,
			int scale) throws IOException, ParseErrorException {

		int jdbcType;
		if (typeString != null) {
			jdbcType = TypesMapping.getSqlTypeByName(typeString);
		} else if (value != null) {
			jdbcType = TypesMapping.getSqlTypeByJava(value.getClass());
		} else {
			// value is null, set JDBC type to NULL
			jdbcType = TypesMapping.getSqlTypeByName(TypesMapping.SQL_NULL);
		}

		if (jdbcType == TypesMapping.NOT_DEFINED) {
			throw new ParseErrorException("Can't determine JDBC type of binding (" + value + ", " + typeString
					+ ") at line " + node.getLine() + ", column " + node.getColumn());
		}

		render(context, writer, preferredBindingType(context, jdbcType), scale, value);
	}

	/**
	 * Resolves the JDBC type the target adapter prefers for binding.
	 *
	 * @since 5.0
	 */
	protected int preferredBindingType(InternalContextAdapter context, int jdbcType) {
		return adapter(context).preferredBindingType(jdbcType);
	}

	private static DbAdapter adapter(InternalContextAdapter context) {
		return (DbAdapter) context.getInternalUserContext().get(VelocitySQLTemplateTranslator.ADAPTER_KEY);
	}

	protected void render(InternalContextAdapter context, Writer writer, int jdbcType, int scale, Object value)
			throws IOException {

		bind(context, jdbcType, scale, value);
		writer.write('?');
	}

	protected Object getChild(InternalContextAdapter context, Node node, int i) throws MethodInvocationException {
		return (i >= 0 && i < node.jjtGetNumChildren()) ? node.jjtGetChild(i).value(context) : null;
	}

	/**
	 * Adds value to the list of bindings in the context.
	 */
	protected void bind(InternalContextAdapter context, int jdbcType, int scale, Object value) {

		@SuppressWarnings("unchecked")
		Collection<PSParameter> bindings = (Collection<PSParameter>)
				context.getInternalUserContext().get(VelocitySQLTemplateTranslator.BINDINGS_LIST_KEY);

		if (bindings != null) {
			// a binding's statement position is its 1-based ordinal among the bound parameters; the
			// ExtendedType is resolved from the value via the adapter
			bindings.add(new PSParameter(value, bindings.size() + 1, jdbcType, scale, extendedType(context, value), null
			));
		}
	}

	private ExtendedType<?> extendedType(InternalContextAdapter context, Object value) {
		ExtendedTypeMap extendedTypes = adapter(context).getExtendedTypes();
		return value != null
				? extendedTypes.getRegisteredType(value.getClass())
				: extendedTypes.getDefaultType();
	}
}
