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

package org.apache.cayenne.access.translator.select;

/**
 * @since 4.2
 */
class PathComponents {

    private final String path;
    private String parent;
    private String[] pathComponents;

    PathComponents(String path) {
        this.path = path;
        initPath();
    }

    int size() {
        return pathComponents.length;
    }

    String getLast() {
        return pathComponents[pathComponents.length - 1];
    }

    String getParent() {
        return parent;
    }

    String[] getAll() {
        return pathComponents;
    }

    String getPath() {
        return path;
    }

    private void initPath() {
        int count = 1;
        int last = 0;

        // quick scan to check for path separator in path
        for(int i=0; i<path.length(); i++) {
            if(path.charAt(i) == '.') {
                count++;
                last = i;
            }
        }

        // fast path, simple path
        if(count == 1) {
            pathComponents = new String[]{path};
            parent = "";
            return;
        }

        // two parts path, can be fast too
        pathComponents = new String[count];
        parent = path.substring(0, last);
        if(count == 2) {
            pathComponents[0] = path.substring(0, last);
            pathComponents[1] = path.substring(last + 1);
            return;
        }

        // additional full scan
        last = 0;
        int idx = 0;
        for(int i=0; i<path.length(); i++) {
            if(path.charAt(i) == '.') {
                pathComponents[idx++] = path.substring(last, i);
                last = i + 1;
            }
        }
        pathComponents[idx] = path.substring(last);
    }
}
