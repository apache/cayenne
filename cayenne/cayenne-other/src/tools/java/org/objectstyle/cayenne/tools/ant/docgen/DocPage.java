/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.tools.ant.docgen;

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
 * Represents a TOC entry. This has a lot of tree-like search functions, but
 * I did not find a tree implementation that I thought was worth using for this. 
 * 
 * @author Cris Daniluk
 */
public class DocPage {

    private static final Pattern orderingPattern = Pattern.compile("\n?\\{excerpt(.*?)\\}");
    
    private static final Map titleMap = new HashMap();
    
    private String title = null;
    private long id;
    private String rawContent;
    private DocPage parentRef;
    
    private List children = null;
    private List ordering;
    private int depth;
    
    public static DocPage getPageByTitle(String title) {
        return (DocPage) titleMap.get(title);
    }

    public DocPage(DocPage parentRef, String title, long id, String rawContent) {
        this.parentRef = parentRef;
        this.title = title;
        this.id = id;
        this.rawContent = rawContent;
        
        titleMap.put(title, this);
        
        // Look for a page ordering...
        Matcher matcher = orderingPattern.matcher(rawContent);
        if (matcher.find()) {
            int regionStart = matcher.end() + 1;
            matcher.find();
            
            ordering = Arrays.asList(rawContent.substring(regionStart, matcher.start()).split("\n"));
            
        }
        
        if (parentRef == null) {
            depth = 1;
        } else if (ordering == null && parentRef.ordering != null) {
            ordering = parentRef.ordering;
        }
        
        children = new ArrayList();
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

    @SuppressWarnings("unchecked")
    public List getChildren() {
        // If an ordering is present, sort by it...
        if (ordering != null) {
         
            Collections.sort(children, new Comparator() {

                public int compare(Object arg0, Object arg1) {
                    // we're the only one who modified this list, so we can trust it 
                    // (and live with the consequences if we're wrong)
                    DocPage child0 = (DocPage) arg0;
                    DocPage child1 = (DocPage) arg1;
                    
                    if (child0.getTitle().equals(child1.getTitle())) {
                        return 0;
                    } else if (ordering.indexOf(child1.getTitle()) == -1) {
                        // if its not on the list, float it to the bottom
                        return 1;
                    }
                    if (ordering.indexOf(child0.getTitle()) < ordering.indexOf(child1.getTitle())) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
                
            });
        } else {
            
            // no beanutils, so do this manually...
            Collections.sort(children, new Comparator() {

                public int compare(Object arg0, Object arg1) {
                    DocPage child0 = (DocPage) arg0;
                    DocPage child1 = (DocPage) arg1;
                    return (child0.getTitle().compareTo(child1.getTitle()));
                }
                
            });
        }
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
     * Get the "module" root. This returns the next-to-top
     * element in the tree.
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
