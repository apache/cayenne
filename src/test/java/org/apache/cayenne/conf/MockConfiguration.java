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

package org.apache.cayenne.conf;

import java.io.InputStream;

import org.apache.commons.lang.NotImplementedException;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.util.ResourceLocator;

/**
 * @author Andrus Adamchik
 */
public class MockConfiguration extends Configuration {

    public MockConfiguration() {
        super();
    }

    @Override
    public boolean canInitialize() {
        return true;
    }

    @Override
    public void didInitialize() {
    }

    @Override
    protected InputStream getDomainConfiguration() {
        throw new NotImplementedException(
                "this is an in-memory mockup...'getDomainConfiguration' is not implemented.");
    }

    @Override
    protected InputStream getMapConfiguration(String name) {
        return null;
    }

    @Override
    protected ResourceLocator getResourceLocator() {
        return null;
    }

    @Override
    protected InputStream getViewConfiguration(String location) {
        return null;
    }

    @Override
    public void initialize() throws Exception {
    }
}
