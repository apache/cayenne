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

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * A custom Velocity directive to describe a conditional chunk of a {@link ChainDirective chain}.
 * Usage of chunk is the following:
 * 
 * <pre>
 * #chunk()...#end - e.g. #chunk()A = 5#end
 * #chunk($paramKey)...#end - e.g. #chunk($a)A = $a#end
 * </pre>
 * <p>
 * If context contains paramKey and it's value isn't null, chunk is included in the
 * chain, and if it is not the first chunk, it is prefixed with chain join (OR/AND).
 * If context doesn't contain paramKey or it's value is null, chunk is skipped.
 * @since 1.1
 */
public class ChunkDirective extends Directive {

    @Override
    public String getName() {
        return "chunk";
    }

    @Override
    public int getType() {
        return BLOCK;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException,
            MethodInvocationException {

        // first child is an expression, second is BLOCK
        if (node.jjtGetNumChildren() > 1 && node.jjtGetChild(0).value(context) == null) {
            // skip this chunk
            return false;
        }

        // BLOCK is the last child
        Node block = node.jjtGetChild(node.jjtGetNumChildren() - 1);
        block.render(context, writer);
        return true;
    }

}
