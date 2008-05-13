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

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.util.ConversionUtil;

/**
 * A custom Velocity directive to create a PreparedStatement parameter text.
 * There are the following possible invocation formats inside the template:
 * 
 * <pre>
 * #bind(value) - e.g. #bind($xyz)
 * #bind(value jdbc_type_name) - e.g. #bind($xyz 'VARCHAR'). This is the most common and useful form.
 * #bind(value jdbc_type_name, precision) - e.g. #bind($xyz 'VARCHAR' 2)</pre>
 * 
 * 
 * <p>Other examples:</p>
 * 
 * <p><strong>Binding literal parameter value:</strong></p>
 * <p><code>"WHERE SOME_COLUMN > #bind($xyz)"</code> produces 
 * <code>"WHERE SOME_COLUMN > ?"</code>
 * and also places the value of the "xyz" parameter in the context "bindings" collection.</p>
 * 
 * <p><strong>Binding ID column of a DataObject value:</strong></p>
 * <p><code>"WHERE ID_COL1 = #bind($helper.cayenneExp($xyz, 'db:ID_COL2')) 
 * AND ID_COL2 = #bind($helper.cayenneExp($xyz, 'db:ID_COL2'))"</code> 
 * produces <code>"WHERE ID_COL1 = ? AND ID_COL2 = ?"</code>
 * and also places the values of id columns of the DataObject parameter  "xyz" in the context 
 * "bindings" collection.</p>
 * 
 * @since 1.1
 * @author Andrus Adamchik
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
     * Extracts the value of the object property to render and passes
     * control to {@link #render(InternalContextAdapter, Writer, ParameterBinding)} 
     * to do the actual rendering.
     */
    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node)
        throws
            IOException,
            ResourceNotFoundException,
            ParseErrorException,
            MethodInvocationException {

        Object value = getChild(context, node, 0);
        Object type = getChild(context, node, 1);
        Object precision = getChild(context, node, 2);

        int jdbcType = TypesMapping.NOT_DEFINED;
        if (type != null) {
            jdbcType = TypesMapping.getSqlTypeByName(type.toString());
        }
        else if (value != null) {
            jdbcType = TypesMapping.getSqlTypeByJava(value.getClass());
        }

        if (jdbcType == TypesMapping.NOT_DEFINED) {
            throw new ParseErrorException(
                "Can't determine JDBC type of binding ("
                    + value
                    + ", "
                    + type
                    + ") at line "
                    + node.getLine()
                    + ", column "
                    + node.getColumn());
        }

        ParameterBinding binding =
            new ParameterBinding(value, jdbcType, ConversionUtil.toInt(precision, -1));

        render(context, writer, binding);
        return true;
    }

    protected void render(
        InternalContextAdapter context,
        Writer writer,
        ParameterBinding binding)
        throws IOException {

        bind(context, binding);
        
        if (binding.getValue() instanceof Collection) {
            Collection bindingList = (Collection) binding.getValue();
            for (Iterator bindingIter = bindingList.iterator(); bindingIter.hasNext(); ) {
                
                bindingIter.next();
                writer.write('?');
                
                if (bindingIter.hasNext()) {
                    writer.write(',');
                }
                
            }
        } else {
            writer.write('?');
        }
    }

    protected Object getChild(InternalContextAdapter context, Node node, int i)
        throws MethodInvocationException {
        return (i >= 0 && i < node.jjtGetNumChildren())
            ? node.jjtGetChild(i).value(context)
            : null;
    }

    /**
     * Adds value to the list of bindings in the context.
     */
    protected void bind(InternalContextAdapter context, ParameterBinding binding) {
        Collection bindings =
            (Collection) context.getInternalUserContext().get(
                SQLTemplateProcessor.BINDINGS_LIST_KEY);

        if (bindings != null) {
            
            if (binding.getValue() instanceof Collection) {
                Collection bindingList = (Collection) binding.getValue();
                for (Iterator bindingIter = bindingList.iterator(); bindingIter.hasNext(); ) {
                    bindings.add(new ParameterBinding(bindingIter.next(), binding.getJdbcType(), binding.getPrecision()));
                }
            } else {
                bindings.add(binding);
            }
        }
    }
}
