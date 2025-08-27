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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.SQLStatement;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.template.SQLTemplateRenderingUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.parser.node.StandardParserDefaultVisitor;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processor for SQL velocity templates.
 * 
 * @see org.apache.cayenne.query.SQLTemplate
 * @since 4.0
 */
public class VelocitySQLTemplateProcessor implements SQLTemplateProcessor {

	private static final class PositionalParamMapper extends StandardParserDefaultVisitor {

		private int i;
		private List<Object> positionalParams;
		private Map<String, Object> params;

		PositionalParamMapper(List<Object> positionalParams, Map<String, Object> params) {
			this.positionalParams = positionalParams;
			this.params = params;
		}

		@Override
		public Object visit(ASTReference node, Object data) {

			// strip off leading "$"
			String paramName = node.getFirstToken().image.substring(1);

			// only consider the first instance of each named parameter
			if (!params.containsKey(paramName)) {

				if (i >= positionalParams.size()) {
					throw new ExpressionException("Too few parameters to bind template: " + positionalParams.size());
				}

				params.put(paramName, positionalParams.get(i));
				i++;
			}

			return data;
		}

		void onFinish() {
			if (i < positionalParams.size()) {
				throw new ExpressionException("Too many parameters to bind template. Expected: " + i + ", actual: "
						+ positionalParams.size());
			}
		}
	}

	static final String BINDINGS_LIST_KEY = "bindings";
	static final String RESULT_COLUMNS_LIST_KEY = "resultColumns";
	static final String HELPER_KEY = "helper";

	protected RuntimeInstance velocityRuntime;
	protected SQLTemplateRenderingUtils renderingUtils;

	public VelocitySQLTemplateProcessor() {
		this.renderingUtils = new SQLTemplateRenderingUtils();
		this.velocityRuntime = new RuntimeInstance();

		velocityRuntime.addProperty("userdirective", BindDirective.class.getName());
		velocityRuntime.addProperty("userdirective", BindEqualDirective.class.getName());
		velocityRuntime.addProperty("userdirective", BindNotEqualDirective.class.getName());
		velocityRuntime.addProperty("userdirective", BindObjectEqualDirective.class.getName());
		velocityRuntime.addProperty("userdirective", BindObjectNotEqualDirective.class.getName());
		velocityRuntime.addProperty("userdirective", ResultDirective.class.getName());
		velocityRuntime.addProperty("userdirective", ChainDirective.class.getName());
		velocityRuntime.addProperty("userdirective", ChunkDirective.class.getName());
		try {
			velocityRuntime.init();
		} catch (Exception ex) {
			throw new CayenneRuntimeException("Error setting up Velocity RuntimeInstance.", ex);
		}

	}

	/**
	 * Builds and returns a SQLStatement based on SQL template and a set of
	 * parameters. During rendering, VelocityContext exposes the following as
	 * variables: all parameters in the map, {@link SQLTemplateRenderingUtils}
	 * as a "helper" variable and SQLStatement object as "statement" variable.
	 */
	@Override
	public SQLStatement processTemplate(String template, Map<String, ?> parameters) {
		// have to make a copy of parameter map since we are gonna modify it..
		Map<String, Object> internalParameters = (parameters != null && !parameters.isEmpty()) ? new HashMap<>(
				parameters) : new HashMap<String, Object>(5);

		SimpleNode parsedTemplate = parse(template);
		return processTemplate(template, parsedTemplate, internalParameters);
	}

	@Override
	public SQLStatement processTemplate(String template, List<Object> positionalParameters) {

		SimpleNode parsedTemplate = parse(template);

		Map<String, Object> internalParameters = new HashMap<>();

		PositionalParamMapper visitor = new PositionalParamMapper(positionalParameters, internalParameters);
		parsedTemplate.jjtAccept(visitor, null);
		visitor.onFinish();

		return processTemplate(template, parsedTemplate, internalParameters);
	}

	SQLStatement processTemplate(String template, SimpleNode parsedTemplate, Map<String, Object> parameters) {
		List<ParameterBinding> bindings = new ArrayList<>();
		List<ColumnDescriptor> results = new ArrayList<>();
		parameters.put(BINDINGS_LIST_KEY, bindings);
		parameters.put(RESULT_COLUMNS_LIST_KEY, results);
		parameters.put(HELPER_KEY, renderingUtils);

		String sql;
		try {
			sql = buildStatement(new VelocityContext(parameters), template, parsedTemplate);
		} catch (Exception e) {
			throw new CayenneRuntimeException("Error processing Velocity template", e);
		}

		ParameterBinding[] bindingsArray = new ParameterBinding[bindings.size()];
		bindings.toArray(bindingsArray);

		ColumnDescriptor[] resultsArray = new ColumnDescriptor[results.size()];
		results.toArray(resultsArray);

		return new SQLStatement(sql, resultsArray, bindingsArray);
	}

	String buildStatement(VelocityContext context, String template, SimpleNode parsedTemplate) throws Exception {

		// ... not sure what InternalContextAdapter is for...
		InternalContextAdapterImpl ica = new InternalContextAdapterImpl(context);
		ica.pushCurrentTemplateName(template);

		StringWriter out = new StringWriter(template.length());
		try {
			parsedTemplate.init(ica, velocityRuntime);
			parsedTemplate.render(ica, out);
			return out.toString();
		} finally {
			ica.popCurrentTemplateName();
		}
	}

	private SimpleNode parse(String template) {

		SimpleNode nodeTree;
		try {
			nodeTree = velocityRuntime.parse(new StringReader(template), new Template());
		} catch (ParseException pex) {
			throw new CayenneRuntimeException("Error parsing template '%s' : %s", template, pex.getMessage());
		}

		if (nodeTree == null) {
			throw new CayenneRuntimeException("Error parsing template %s", template);
		}

		return nodeTree;
	}
}
