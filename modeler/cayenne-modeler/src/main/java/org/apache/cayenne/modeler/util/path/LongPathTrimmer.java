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

public class LongPathTrimmer implements PathTrimmer {

    private static final String PATH_SEPARATOR = File.separator;
    private static final int PATH_LENGTH_THRESHOLD = 8;

    private String[] splitPath(String path) {
        return path.split(PATH_SEPARATOR);
    }

    private String joinPath(String[] components) {
        if(components.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(components[0]);
        for(int i=1; i<components.length; i++) {
            sb.append(PATH_SEPARATOR);
            sb.append(components[i]);
        }
        return sb.toString();
    }

    @Override
    public String trim(String path) {
        String[] components = splitPath(path);
        if(components.length <= PATH_LENGTH_THRESHOLD + 1) {
            return path;
        }
        String[] strippedComponents = new String[PATH_LENGTH_THRESHOLD + 1];
        int half = PATH_LENGTH_THRESHOLD / 2;
        int end = components.length - half;
        int idx = 0;
        boolean trimAdded = false;
        for(int i=0; i<components.length; i++) {
            if(i < half || i >= end) {
                strippedComponents[idx++] = components[i];
            } else if(!trimAdded) {
                strippedComponents[idx++] = "...";
                trimAdded = true;
            }
        }
        return joinPath(strippedComponents);
    }
}
