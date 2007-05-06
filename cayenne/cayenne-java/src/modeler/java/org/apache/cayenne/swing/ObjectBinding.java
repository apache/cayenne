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

import java.awt.Component;

/**
 * Defines API of a binding sitting between a Swing widget and domain model, synchronizing
 * the values between the two. Parent part of the binding is called "context"as it is used
 * as a context of binding expressions. Child of the binding is a bound component that is
 * being synchronized with the context.
 * 
 * @author Andrei Adamchik
 */
public interface ObjectBinding {

    Component getView();

    Object getContext();

    void setContext(Object object);

    void updateView();

    BindingDelegate getDelegate();

    void setDelegate(BindingDelegate delegate);
}
