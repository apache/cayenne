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

package org.apache.cayenne.velocity;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.ParameterBinding;
import org.apache.cayenne.access.jdbc.SQLStatement;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;

/**
 * Processor for SQL velocity templates.
 * 
 * @see org.apache.cayenne.query.SQLTemplate
 * @since 4.0
 */
public class VelocitySQLTemplateProcessor implements SQLTemplateProcessor {

	static final String BINDINGS_LIST_KEY = "bindings";
	static final String RESULT_COLUMNS_LIST_KEY = "resultColumns";
	static final String HELPER_KEY = "helper";

	protected RuntimeInstance velocityRuntime;
	protected SQLTemplateRenderingUtils renderingUtils;

	public VelocitySQLTemplateProcessor() {
		this.renderingUtils = new SQLTemplateRenderingUtils();
		this.velocityRuntime = new RuntimeInstance();

		// set null logger
		velocityRuntime.addProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, new NullLogChute());

		velocityRuntime
				.addProperty(RuntimeConstants.RESOURCE_MANAGER_CLASS, SQLTemplateResourceManager.class.getName());
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
		Map<String, Object> internalParameters = (parameters != null && !parameters.isEmpty()) ? new HashMap<String, Object>(
				parameters) : new HashMap<String, Object>(5);

		List<ParameterBinding> bindings = new ArrayList<ParameterBinding>();
		List<ColumnDescriptor> results = new ArrayList<ColumnDescriptor>();
		internalParameters.put(BINDINGS_LIST_KEY, bindings);
		internalParameters.put(RESULT_COLUMNS_LIST_KEY, results);
		internalParameters.put(HELPER_KEY, renderingUtils);

		String sql;
		try {
			sql = buildStatement(new VelocityContext(internalParameters), template);
		} catch (Exception e) {
			throw new CayenneRuntimeException("Error processing Velocity template", e);
		}

		ParameterBinding[] bindingsArray = new ParameterBinding[bindings.size()];
		bindings.toArray(bindingsArray);

		ColumnDescriptor[] resultsArray = new ColumnDescriptor[results.size()];
		results.toArray(resultsArray);

		return new SQLStatement(sql, resultsArray, bindingsArray);
	}

	String buildStatement(VelocityContext context, String template) throws Exception {
		// Note: this method is a reworked version of
		// org.apache.velocity.app.Velocity.evaluate(..)
		// cleaned up to avoid using any Velocity singletons

		StringWriter out = new StringWriter(template.length());
		SimpleNode nodeTree = null;

		try {
			nodeTree = velocityRuntime.parse(new StringReader(template), template);
		} catch (ParseException pex) {
			throw new CayenneRuntimeException("Error parsing template '" + template + "' : " + pex.getMessage());
		}

		if (nodeTree == null) {
			throw new CayenneRuntimeException("Error parsing template " + template);
		}

		// ... not sure what InternalContextAdapter is for...
		InternalContextAdapterImpl ica = new InternalContextAdapterImpl(context);
		ica.pushCurrentTemplateName(template);

		try {
			nodeTree.init(ica, velocityRuntime);
			nodeTree.render(ica, out);
			return out.toString();
		} finally {
			ica.popCurrentTemplateName();
		}
	}
}
