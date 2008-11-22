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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a TOC entry. This has a lot of tree-like search functions, but I did not
 * find a tree implementation that I thought was worth using for this.
 * 
 */
public class DocPage {

    private static final Pattern TOC_BLOCK = Pattern.compile("\n?\\{excerpt(.*?)\\}");
    private static final Pattern TOC_LINE = Pattern.compile("\\[([^|]*\\|)?(.+)\\]");

    private static final Map titleMap = new HashMap();

    private String title;

    private long id;
    private String rawContent;
    private DocPage parentRef;
    private List children;
    private Comparator ordering;
    private int depth;

    public static DocPage getPageByTitle(String title) {
        return (DocPage) titleMap.get(title);
    }

    DocPage(String title) {
        this.title = title;
    }

    public DocPage(DocPage parentRef, String title, long id, String rawContent) {
        this.parentRef = parentRef;
        this.title = title;
        this.id = id;
        this.rawContent = rawContent;
        this.children = new ArrayList();
        this.ordering = createChildOrdering(rawContent);

        titleMap.put(title, this);

        if (parentRef == null) {
            depth = 1;
        }
    }

    /**
     * Infers the order of children based on the content "excerpt" tags.
     */
    Comparator createChildOrdering(String rawContent) {
        Matcher matcher = TOC_BLOCK.matcher(rawContent);
        if (matcher.find()) {
            int regionStart = matcher.end() + 1;
            matcher.find();

            List lines = Arrays.asList(rawContent
                    .substring(regionStart, matcher.start())
                    .split("\n"));

            List titles = new ArrayList(lines.size());
            Iterator it = lines.iterator();
            while (it.hasNext()) {
                Matcher lineMatcher = TOC_LINE.matcher((String) it.next());
                if (lineMatcher.find()) {
                    titles.add(lineMatcher.group(2));
                }
            }

            return new PreorderedTitleComparator(titles);
        }
        else {
            return new AlphabeticalTitleComparator();
        }
    }

    public void addChild(DocPage child) {
        child.depth = depth + 1;
        children.add(child);
    }

    public String getTitle() {
        return title;
    }

    public int getDepth() {
        return depth;
    }

    public List getChildren() {
        Collections.sort(children, ordering);
        return Collections.unmodifiableList(children);
    }

    public long getId() {
        return id;
    }

    public String getRawContent() {
        return rawContent;
    }

    public DocPage getParentRef() {
        return parentRef;
    }

    public DocPage findPageId(long searchId) {

        return findChild(this, searchId);
    }

    public boolean hasDescendent(DocPage page) {
        if (findChild(this, page.getId()) != null) {
            return true;
        }
        return false;
    }

    /**
     * Get the "module" root. This returns the next-to-top element in the tree.
     */
    public DocPage getRoot() {
        DocPage base = this;
        while (base.parentRef != null && base.parentRef.parentRef != null) {
            base = base.parentRef;
        }
        return base;
    }

    private DocPage findChild(DocPage page, long searchId) {

        if (page.getId() == searchId) {
            return page;
        }
        Iterator pageIter = page.getChildren().iterator();
        while (pageIter.hasNext()) {
            DocPage match = findChild((DocPage) pageIter.next(), searchId);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    public String getLinkPath() {
        return buildLinkPath(this);
    }

    private String buildLinkPath(DocPage page) {
        if (page.getParentRef() == null) {
            return page.getTitle();
        }
        return buildLinkPath(page.getParentRef()) + "/" + page.getTitle();
    }
}
