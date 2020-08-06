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

package org.apache.cayenne.exp.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.0
 */
public abstract class EvaluatedNode extends SimpleNode {

    protected EvaluatedNode(int i) {
        super(i);
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        int requiredLen = getRequiredChildrenCount();
        if (len < requiredLen) {
            return null;
        }

        if(requiredLen == 0) {
            return evaluateSubNode(null, null);
        }

        final Object[] evaluatedChildren = new Object[len];
        for(int i=0; i<len; i++) {
            evaluatedChildren[i] = evaluateChild(i, o);
        }

        Object firstChild = evaluatedChildren[0];

        // convert Map, keep Map keys
        if(firstChild instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> child = (Map<Object, Object>) firstChild;
            Map<Object, Object> result = new HashMap<>(child.size());
            for(Map.Entry<Object, Object> entry : child.entrySet()) {
                result.put(entry.getKey(), evaluateSubNode(entry.getValue(), evaluatedChildren));
            }
            return result;
        }

        // convert collection
        if (firstChild instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> child = (Collection<Object>) firstChild;
            Collection<Object> result = new ArrayList<>(child.size());
            for(Object c : child) {
                result.add(evaluateSubNode(c, evaluatedChildren));
            }
            return result;
        }

        // convert scalar
        return evaluateSubNode(firstChild, evaluatedChildren);
    }

    abstract protected int getRequiredChildrenCount();

    abstract protected Object evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception;

}
