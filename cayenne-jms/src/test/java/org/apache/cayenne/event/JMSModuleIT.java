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

package org.apache.cayenne.event;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;

/**
 * @since 4.0
 */
public class JMSModuleIT {

    @Test
    public void testModule() {

        Module configModule = new Module() {
            public void configure(Binder binder) {
                ServerModule.contributeProperties(binder).put(Constants.SERVER_DOMAIN_NAME_PROPERTY, "test");
            }
        };

        Injector injector = DIBootstrap.createInjector(new ServerModule(), new JMSModule(), configModule);

        EventBridge bridge = injector.getInstance(EventBridge.class);
        assertThat(bridge, instanceOf(JMSBridge.class));

        EventBridge bridge2 = injector.getInstance(EventBridge.class);
        assertThat(bridge2, instanceOf(JMSBridge.class));
        assertNotSame(bridge, bridge2);
    }

}
