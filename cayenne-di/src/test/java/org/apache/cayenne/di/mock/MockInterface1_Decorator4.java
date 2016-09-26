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
package org.apache.cayenne.di.mock;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;

public class MockInterface1_Decorator4 implements MockInterface1 {

    private Provider<MockInterface1> delegate;

    public MockInterface1_Decorator4(@Inject Provider<MockInterface1> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return "[4" + delegate.get().getName() + "4]";
    }
}