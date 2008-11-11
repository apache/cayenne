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
package org.apache.cayenne.maven.plugin.confluence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A comparator of pages based on preset case-insensitive ordering of titles.
 * 
 */
class PreorderedTitleComparator implements Comparator {

    private List titles;

    PreorderedTitleComparator(List titles) {
        this.titles = new ArrayList(titles.size());
        Iterator it = titles.iterator();
        while (it.hasNext()) {
            String title = (String) it.next();
            this.titles.add(title.toUpperCase());
        }
    }

    public int compare(Object o1, Object o2) {
        DocPage child0 = (DocPage) o1;
        DocPage child1 = (DocPage) o2;

        String title0 = child0.getTitle().toUpperCase();
        String title1 = child1.getTitle().toUpperCase();

        if (title0.equals(title1)) {
            return 0;
        }
        else if (titles.indexOf(title1) == -1) {
            // if its not on the list, float it to the bottom
            return 1;
        }
        if (titles.indexOf(title0) < titles.indexOf(title1)) {
            return -1;
        }
        else {
            return 1;
        }
    }
}
