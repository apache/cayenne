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
package org.apache.cayenne.itest.pojo;

import java.lang.instrument.ClassFileTransformer;

import junit.framework.TestCase;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.enhancer.CayenneEnhancerVisitorFactory;
import org.apache.cayenne.enhancer.Enhancer;
import org.apache.cayenne.instrument.InstrumentUtil;
import org.apache.cayenne.itest.ItestDBUtils;

public class PojoTestCase extends TestCase {

    static {
        ItestSetup.initInstance();
        InstrumentUtil.getInstrumentation().addTransformer(initEnhancer());
    }

    protected static ClassFileTransformer initEnhancer() {
        DataChannel channel = ItestSetup.getInstance().getDataDomain();
        return new Enhancer(
                new CayenneEnhancerVisitorFactory(channel.getEntityResolver()));
    }

    protected ItestDBUtils getDbHelper() {
        return ItestSetup.getInstance().getDbHelper();
    }
}
