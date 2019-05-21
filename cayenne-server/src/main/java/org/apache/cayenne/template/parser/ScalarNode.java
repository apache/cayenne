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

package org.apache.cayenne.template.parser;

import org.apache.cayenne.template.Context;

/**
 * @since 4.1
 */
public class ScalarNode<V> extends SimpleNode implements ExpressionNode {

    V value;

    public ScalarNode(int i) {
        super(i);
    }

    public void setValue(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    @Override
    public void evaluate(Context context) {
        if(value != null) {
            context.getBuilder().append(value.toString());
        }
    }

    @Override
    public String evaluateAsString(Context context) {
        return value.toString();
    }

    @Override
    public Object evaluateAsObject(Context context) {
        return value;
    }

    @Override
    public long evaluateAsLong(Context context) {
        throw new UnsupportedOperationException("Can't convert " + value + " value to long");
    }

    @Override
    public double evaluateAsDouble(Context context) {
        throw new UnsupportedOperationException("Can't convert " + value + " value to double");
    }

    @Override
    public boolean evaluateAsBoolean(Context context) {
        throw new UnsupportedOperationException("Can't convert " + value + " value to boolean");
    }
}
