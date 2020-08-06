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

package org.apache.cayenne.modeler.util.path;

import java.io.File;

public class DefaultResourceTrimmer implements PathTrimmer {

    private static String DEFAULT_RESOURCE_PATH;
    private static String DEFAULT_TEST_RESOURCE_PATH;
    static {
        String separator = File.separator;
        DEFAULT_RESOURCE_PATH = "src" + separator + "main" + separator + "resources";
        DEFAULT_TEST_RESOURCE_PATH = "src" + separator + "test" + separator + "resources";
    }

    @Override
    public String trim(String path) {
        path = path.replace(DEFAULT_TEST_RESOURCE_PATH, "..test..");
        path = path.replace(DEFAULT_RESOURCE_PATH, "..main..");
        return path;
    }
}
