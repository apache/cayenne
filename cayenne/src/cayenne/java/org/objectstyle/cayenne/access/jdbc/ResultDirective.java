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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * A custom Velocity directive to describe a ResultSet column.
 * There are the following possible invocation formats inside the template:
 * 
 * <pre>
 * #result(column_name) - e.g. #result('ARTIST_ID')
 * #result(column_name java_type) - e.g. #result('ARTIST_ID' 'String')
 * #result(column_name java_type column_alias) - e.g. #result('ARTIST_ID' 'String' 'ID')
 * </pre>
 * 
 * <p>Note that most common Java types used in JDBC can be specified without 
 * a package. This includes all numeric types, primitives, String, SQL dates, BigDecimal
 * and BigInteger.
 * </p>
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class ResultDirective extends Directive {

    private static final Map typesGuess = new HashMap();

    static {
        // init default types

        // primitives
        typesGuess.put("long", Long.class.getName());
        typesGuess.put("double", Double.class.getName());
        typesGuess.put("byte", Byte.class.getName());
        typesGuess.put("boolean", Boolean.class.getName());
        typesGuess.put("float", Float.class.getName());
        typesGuess.put("short", Short.class.getName());
        typesGuess.put("int", Integer.class.getName());

        // numeric
        typesGuess.put("Long", Long.class.getName());
        typesGuess.put("Double", Double.class.getName());
        typesGuess.put("Byte", Byte.class.getName());
        typesGuess.put("Boolean", Boolean.class.getName());
        typesGuess.put("Float", Float.class.getName());
        typesGuess.put("Short", Short.class.getName());
        typesGuess.put("Integer", Integer.class.getName());

        // other
        typesGuess.put("String", String.class.getName());
        typesGuess.put("Date", Date.class.getName());
        typesGuess.put("Time", Time.class.getName());
        typesGuess.put("Timestamp", Timestamp.class.getName());
        typesGuess.put("BigDecimal", BigDecimal.class.getName());
        typesGuess.put("BigInteger", BigInteger.class.getName());
    }

    public String getName() {
        return "result";
    }

    public int getType() {
        return LINE;
    }

    public boolean render(InternalContextAdapter context, Writer writer, Node node)
        throws
            IOException,
            ResourceNotFoundException,
            ParseErrorException,
            MethodInvocationException {

        Object column = getChild(context, node, 0);
        if (column == null) {
            throw new ParseErrorException(
                "Column name expected at line "
                    + node.getLine()
                    + ", column "
                    + node.getColumn());
        }

        Object alias = getChild(context, node, 2);

        ColumnDescriptor columnDescriptor = new ColumnDescriptor();
        columnDescriptor.setName(
            (alias != null) ? alias.toString() : column.toString());

        Object type = getChild(context, node, 1);
        if (type != null) {
            columnDescriptor.setJavaClass(guessType(type.toString()));
        }

        writer.write(column.toString());
        if (alias != null && !alias.equals(column)) {
            writer.write(" AS ");
            writer.write(alias.toString());
        }

        bindResult(context, columnDescriptor);
        return true;
    }

    protected Object getChild(InternalContextAdapter context, Node node, int i)
        throws MethodInvocationException {
        return (i >= 0 && i < node.jjtGetNumChildren())
            ? node.jjtGetChild(i).value(context)
            : null;
    }

    /**
     * Converts "short" type notation to the fully qualified class name.
     * Right now supports all major standard SQL types, including primitives.
     * All other types are expected to be fully qualified, and are not converted.
     */
    protected String guessType(String type) {
        String guessed = (String) typesGuess.get(type);
        return guessed != null ? guessed : type;
    }

    /**
     * Adds value to the list of result columns in the context.
     */
    protected void bindResult(
        InternalContextAdapter context,
        ColumnDescriptor columnDescriptor) {

        Collection resultColumns =
            (Collection) context.getInternalUserContext().get(
                SQLTemplateProcessor.RESULT_COLUMNS_LIST_KEY);

        if (resultColumns != null) {
            resultColumns.add(columnDescriptor);
        }
    }
}
