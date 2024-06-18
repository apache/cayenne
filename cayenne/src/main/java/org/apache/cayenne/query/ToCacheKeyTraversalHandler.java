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

package org.apache.cayenne.query;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.ASTScalar;

/**
 * Expression traverse handler to create cache key string out of Expression.
 * {@link Expression#appendAsString(Appendable)} where previously used for that,
 * but it can't handle custom value objects properly (see CAY-2210).
 *
 * @see ValueObjectTypeRegistry
 * @since 4.2
 */
class ToCacheKeyTraversalHandler implements TraversalHandler {

    private ValueObjectTypeRegistry registry;
    private StringBuilder out;

    ToCacheKeyTraversalHandler(ValueObjectTypeRegistry registry, StringBuilder out) {
        this.registry = registry;
        this.out = out;
    }

    @Override
    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
        out.append(',');
    }

    @Override
    public void startNode(Expression node, Expression parentNode) {
        if(node.getType() == Expression.FUNCTION_CALL) {
            out.append(((ASTFunctionCall)node).getFunctionName()).append('(');
        } else {
            out.append(node.getType()).append('(');
        }
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
        out.append(')');
    }

    @Override
    public void objectNode(Object leaf, Expression parentNode) {
        if(leaf == null) {
            out.append("null");
            return;
        }

        if(leaf instanceof ASTScalar) {
            leaf = ((ASTScalar) leaf).getValue();
        } else if(leaf instanceof Object[]) {
            for(Object value : (Object[])leaf) {
                objectNode(value, parentNode);
                out.append(',');
            }
            return;
        }

        if (leaf instanceof Persistent) {
            ObjectId id = ((Persistent) leaf).getObjectId();
            Object encode = (id != null) ? id : leaf;
            out.append(encode);
        } else if (leaf instanceof Enum<?>) {
            Enum<?> e = (Enum<?>) leaf;
            out.append("e:").append(leaf.getClass().getName()).append(':').append(e.ordinal());
        } else {
            ValueObjectType<Object, ?> valueObjectType;
            if (registry == null || (valueObjectType = registry.getValueType(leaf.getClass())) == null) {
                out.append(leaf);
            } else {
                out.append(valueObjectType.toCacheKey(leaf));
            }
        }
    }
}
