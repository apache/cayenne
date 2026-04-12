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

package org.apache.cayenne.swing;

/**
 * A builder for component bindings that delegates the creation of the binding to the
 * underlying factory, and itself configures a number of binding parameters.
 * 
 */
public class BindingBuilder {

    protected BindingFactory factory;
    protected BindingDelegate delegate;
    protected Object context;

    /**
     * Constructs BindingBuilder with a BindingFactory and a root model object (or
     * context) of the binding.
     */
    public BindingBuilder(BindingFactory factory, Object context) {
        this.factory = factory;
        this.context = context;
    }

    public BindingDelegate getDelegate() {
        return delegate;
    }

    /**
     * Sets BindingDelegate that will be assigned to all bindings created via this
     * BindingBuilder.
     */
    public void setDelegate(BindingDelegate delegate) {
        this.delegate = delegate;
    }

    public Object getContext() {
        return context;
    }

    /**
     * Sets the context object that will be used by all bindings created via this
     * BindingBuilder. Context is a root of the domain model for the given binding.
     */
    public void setContext(Object context) {
        this.context = context;
    }

    public BindingFactory getFactory() {
        return factory;
    }


    protected ObjectBinding initBinding(ObjectBinding binding, BindingDelegate delegate) {
        binding.setDelegate(delegate);
        binding.setContext(context);
        return binding;
    }
}
