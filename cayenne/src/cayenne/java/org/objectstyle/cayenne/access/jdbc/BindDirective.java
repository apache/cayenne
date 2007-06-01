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

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.util.ConversionUtil;

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
 * @author Andrei Adamchik
 */
public class BindDirective extends Directive {

    public String getName() {
        return "bind";
    }

    public int getType() {
        return LINE;
    }

    /**
     * Extracts the value of the object property to render and passes
     * control to {@link #render(InternalContextAdapter, Writer, ParameterBinding)} 
     * to do the actual rendering.
     */
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
        writer.write('?');
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
            bindings.add(binding);
        }
    }
}
