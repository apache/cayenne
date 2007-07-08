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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.TestCase;

public class DocPageTest extends TestCase {

    public void testCreateChildOrdering1() {

        String ex = "{excerpt}\n"
                + "[c4]\n"
                + "[c2]\n"
                + "[c3]\n"
                + "[c1]\n"
                + "{excerpt}";

        DocPage parent = new DocPage("parent");

        Comparator ordering = parent.createChildOrdering(ex);
        List children = createChildren();
        Collections.sort(children, ordering);

        assertEquals("c4", ((DocPage) children.get(0)).getTitle());
        assertEquals("c2", ((DocPage) children.get(1)).getTitle());
        assertEquals("c3", ((DocPage) children.get(2)).getTitle());
        assertEquals("c1", ((DocPage) children.get(3)).getTitle());
    }

    public void testCreateChildOrdering2() {

        String ex = "{excerpt}\n"
                + "# [c4]\n"
                + "# [c2]\n"
                + "# [c3]\n"
                + "# [c1]\n"
                + "{excerpt}";

        DocPage parent = new DocPage("parent");

        Comparator ordering = parent.createChildOrdering(ex);
        List children = createChildren();
        Collections.sort(children, ordering);

        assertEquals("c4", ((DocPage) children.get(0)).getTitle());
        assertEquals("c2", ((DocPage) children.get(1)).getTitle());
        assertEquals("c3", ((DocPage) children.get(2)).getTitle());
        assertEquals("c1", ((DocPage) children.get(3)).getTitle());
    }

    public void testCreateChildOrdering3() {

        String ex = "{excerpt}\n"
                + "[X|c4]\n"
                + "[M|c2]\n"
                + "[Z a|c3]\n"
                + "[c1]\n"
                + "{excerpt}";

        DocPage parent = new DocPage("parent");

        Comparator ordering = parent.createChildOrdering(ex);
        List children = createChildren();
        Collections.sort(children, ordering);

        assertEquals("c4", ((DocPage) children.get(0)).getTitle());
        assertEquals("c2", ((DocPage) children.get(1)).getTitle());
        assertEquals("c3", ((DocPage) children.get(2)).getTitle());
        assertEquals("c1", ((DocPage) children.get(3)).getTitle());
    }

    public void testCreateChildOrdering4() {

        String ex = "{excerpt}\n"
                + "[c4 yy]\n"
                + "[c2 ww]\n"
                + "[c3 aa]\n"
                + "[c1 xx]\n"
                + "{excerpt}";

        DocPage parent = new DocPage("parent");

        Comparator ordering = parent.createChildOrdering(ex);
        List children = new ArrayList(4);
        children.add(new DocPage("c1 xx"));
        children.add(new DocPage("c2 ww"));
        children.add(new DocPage("c3 aa"));
        children.add(new DocPage("c4 yy"));

        Collections.sort(children, ordering);

        assertEquals("c4 yy", ((DocPage) children.get(0)).getTitle());
        assertEquals("c2 ww", ((DocPage) children.get(1)).getTitle());
        assertEquals("c3 aa", ((DocPage) children.get(2)).getTitle());
        assertEquals("c1 xx", ((DocPage) children.get(3)).getTitle());
    }
    
    public void testCreateChildOrdering5() {

        String ex = "{excerpt}\n"
                + "# [c4]\n"
                + "# [C2]\n"
                + "# [c3]\n"
                + "# [c1]\n"
                + "{excerpt}";

        DocPage parent = new DocPage("parent");

        Comparator ordering = parent.createChildOrdering(ex);
        List children = createChildren();
        Collections.sort(children, ordering);

        assertEquals("c4", ((DocPage) children.get(0)).getTitle());
        assertEquals("c2", ((DocPage) children.get(1)).getTitle());
        assertEquals("c3", ((DocPage) children.get(2)).getTitle());
        assertEquals("c1", ((DocPage) children.get(3)).getTitle());
    }

    private List createChildren() {
        List children = new ArrayList(4);
        children.add(new DocPage("c1"));
        children.add(new DocPage("c2"));
        children.add(new DocPage("c3"));
        children.add(new DocPage("c4"));
        return children;
    }
}
