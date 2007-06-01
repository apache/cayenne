/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.jdbc;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.log.NullLogSystem;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * Processor for SQL velocity templates. 
 * 
 * @see org.objectstyle.cayenne.query.SQLTemplate
 * @since 1.1
 * @author Andrei Adamchik
 */
class SQLTemplateProcessor {
    private static RuntimeInstance sharedRuntime;

    static final String BINDINGS_LIST_KEY = "bindings";
    static final String RESULT_COLUMNS_LIST_KEY = "resultColumns";
    static final String HELPER_KEY = "helper";

    private static final SQLTemplateRenderingUtils sharedUtils =
        new SQLTemplateRenderingUtils();

    RuntimeInstance velocityRuntime;
    SQLTemplateRenderingUtils renderingUtils;

    static {
        initVelocityRuntime();
    }

    private static void initVelocityRuntime() {
        // init static velocity engine
        sharedRuntime = new RuntimeInstance();

        // set null logger
        sharedRuntime.addProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, new NullLogSystem());
        
        sharedRuntime.addProperty(
            RuntimeConstants.RESOURCE_MANAGER_CLASS,
            SQLTemplateResourceManager.class.getName());
        sharedRuntime.addProperty("userdirective", BindDirective.class.getName());
        sharedRuntime.addProperty("userdirective", BindEqualDirective.class.getName());
        sharedRuntime.addProperty("userdirective", BindNotEqualDirective.class.getName());
        sharedRuntime.addProperty("userdirective", ResultDirective.class.getName());
        sharedRuntime.addProperty("userdirective", ChainDirective.class.getName());
        sharedRuntime.addProperty("userdirective", ChunkDirective.class.getName());
        try {
            sharedRuntime.init();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException(
                "Error setting up Velocity RuntimeInstance.",
                ex);
        }
    }

    SQLTemplateProcessor() {
        this.velocityRuntime = sharedRuntime;
        this.renderingUtils = sharedUtils;
    }

    SQLTemplateProcessor(
        RuntimeInstance velocityRuntime,
        SQLTemplateRenderingUtils renderingUtils) {
        this.velocityRuntime = velocityRuntime;
        this.renderingUtils = renderingUtils;
    }

    /**
     * Builds and returns a SQLSelectStatement based on SQL template and a set of parameters. 
     * During rendering VelocityContext exposes the following  as variables: all parameters 
     * in the map, {@link SQLTemplateRenderingUtils} as a "helper" variable and SQLStatement 
     * object as "statement" variable.
     */
    SQLSelectStatement processSelectTemplate(String template, Map parameters)
        throws Exception {

        // have to make a copy of parameter map since we are gonna modify it..
        Map internalParameters =
            (parameters != null && !parameters.isEmpty())
                ? new HashMap(parameters)
                : new HashMap(3);

        List bindings = new ArrayList();
        List results = new ArrayList();
        internalParameters.put(BINDINGS_LIST_KEY, bindings);
        internalParameters.put(RESULT_COLUMNS_LIST_KEY, results);
        internalParameters.put(HELPER_KEY, renderingUtils);

        String sql =
            buildStatement(new VelocityContext(internalParameters), template, parameters);

        ParameterBinding[] bindingsArray = new ParameterBinding[bindings.size()];
        bindings.toArray(bindingsArray);

        ColumnDescriptor[] resultsArray = new ColumnDescriptor[results.size()];
        results.toArray(resultsArray);
        return new SQLSelectStatement(sql, resultsArray, bindingsArray);
    }

    /**
     * Builds and returns a SQLStatement based on SQL template and a set of parameters. 
     * During rendering, VelocityContext exposes the following  as variables: all parameters 
     * in the map, {@link SQLTemplateRenderingUtils} as a "helper" variable and SQLStatement 
     * object as "statement" variable.
     */
    SQLStatement processTemplate(String template, Map parameters) throws Exception {
        // have to make a copy of parameter map since we are gonna modify it..
        Map internalParameters =
            (parameters != null && !parameters.isEmpty())
                ? new HashMap(parameters)
                : new HashMap(3);

        List bindings = new ArrayList();
        internalParameters.put(BINDINGS_LIST_KEY, bindings);
        internalParameters.put(HELPER_KEY, renderingUtils);

        String sql =
            buildStatement(new VelocityContext(internalParameters), template, parameters);

        ParameterBinding[] bindingsArray = new ParameterBinding[bindings.size()];
        bindings.toArray(bindingsArray);
        return new SQLStatement(sql, bindingsArray);
    }

    String buildStatement(VelocityContext context, String template, Map parameters)
        throws Exception {
        // Note: this method is a reworked version of org.apache.velocity.app.Velocity.evaluate(..)
        // cleaned up to avoid using any Velocity singletons

        StringWriter out = new StringWriter(template.length());
        SimpleNode nodeTree = null;

        try {
            nodeTree = velocityRuntime.parse(new StringReader(template), template);
        }
        catch (ParseException pex) {
            throw new ParseErrorException(pex.getMessage());
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
        }
        finally {
            ica.popCurrentTemplateName();
        }
    }
}
