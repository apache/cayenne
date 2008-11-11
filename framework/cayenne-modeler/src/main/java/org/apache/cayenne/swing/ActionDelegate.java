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

package org.apache.cayenne.swing;

import java.util.Collections;

/**
 * An implementation of BindingDelegate that invokes a no-argument context action on every
 * model update.
 * 
 */
public class ActionDelegate implements BindingDelegate {

    protected BindingExpression expression;

    public ActionDelegate(String expression) {
        this.expression = new BindingExpression(expression);
    }

    public void modelUpdated(ObjectBinding binding, Object oldValue, Object newValue) {
        // TODO: might add new and old value as variables...
        expression.getValue(binding.getContext(), Collections.EMPTY_MAP);
    }
}
