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
import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.ASTDirective;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * A custom Velocity directive to conditionally join a number of {@link ChunkDirective chunks}.
 * Usage of chain is the following:
 * 
 * <pre>
 * #chain(operator) - e.g. #chain(' AND ')
 * #chain(operator prefix) - e.g. #chain(' AND ' 'WHERE ')</pre>
 * 
 * <p><code>operator</code> (e.g. AND, OR, etc.) is used to join chunks that are included
 * in a chain. <code>prefix</code> is inserted if a chain contains at least one chunk.
 * </p>
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class ChainDirective extends Directive {

    public String getName() {
        return "chain";
    }

    public int getType() {
        return BLOCK;
    }

    public boolean render(InternalContextAdapter context, Writer writer, Node node)
        throws
            IOException,
            ResourceNotFoundException,
            ParseErrorException,
            MethodInvocationException {

        int size = node.jjtGetNumChildren();
        if (size == 0) {
            return true;
        }

        // BLOCK is the last child
        Node block = node.jjtGetChild(node.jjtGetNumChildren() - 1);
        String join = (size > 1) ? (String) node.jjtGetChild(0).value(context) : "";
        String prefix = (size > 2) ? (String) node.jjtGetChild(1).value(context) : "";

        // if there is a conditional prefix, use a separate buffer ofr children
        StringWriter childWriter = new StringWriter(30);

        int len = block.jjtGetNumChildren();
        int includedChunks = 0;
        for (int i = 0; i < len; i++) {
            Node child = block.jjtGetChild(i);

            // if this is a "chunk", evaluate its expression and prepend join if included...
            if (child instanceof ASTDirective
                && "chunk".equals(((ASTDirective) child).getDirectiveName())) {

                if (child.jjtGetNumChildren() < 2
                    || child.jjtGetChild(0).evaluate(context)) {

                    if (includedChunks > 0) {
                        childWriter.write(join);
                    }

                    includedChunks++;
                }
            }

            child.render(context, childWriter);
        }

        if (includedChunks > 0) {
            childWriter.flush();
            writer.write(prefix);
            writer.write(childWriter.toString());
        }

        return true;
    }
}
