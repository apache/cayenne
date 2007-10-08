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

import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogSystem;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.gen.ClassGeneratorResourceLoader;
import org.objectstyle.confluence.rpc.soap_axis.confluenceservice_v1.ConfluenceSoapService;

/**
 * Extracts embedded links from Confluence documentation and converts them to local fs references
 * 
 * @author Cris Daniluk
 */
public class DocPageRenderer extends TestCase {
    
    private static final String URL_PREFIX = "/confluence";
    private static final String CONFLUENCE_URL = "http://www.objectstyle.org/confluence";

    /**
     * Only attachments within the page are supported right now. This could easily be
     * adjusted to find attachments in external documents if necessary.
     */
    private static final Pattern attachmentPattern = Pattern.compile("(href|src)=\"" + URL_PREFIX + "/download/attachments/(.*?)/(.*?)\"");
    
    /**
     * When browsing the local filesystem, browsers like %20 (hex encoded) instead of + (legacy HTTP 0.9) for
     * spaces. 
     */
    private static final Pattern spaceEncoderPattern = Pattern.compile("href=\"(?!http://).*?\\+.*?\"");
    
    
    /**
     * Not all images are supported - only the ones referenced by current docs.
     */
    private static final Pattern confluenceImagePattern = Pattern.compile("src=\"" + URL_PREFIX + "/images/icons/(.*?)\"");
    
    /**
     * Take any confluence links to non-doc content and add the url
     */
    private Pattern confluenceLinkPattern = Pattern.compile("href=\"(" + URL_PREFIX + "/display/.*?)\"");
    
    private Pattern embeddedLinkPattern;
    
    private ConfluenceSoapService service;
    private String token;
    private String spaceKey;
    
    private VelocityContext velCtxt;
    private Template pageTemplate;
    
    public DocPageRenderer(ConfluenceSoapService service, String token, String spaceKey, String template) throws Exception {
        
        // Note that these regexps have a fairly narrow capture - since the HTML is machine-generated,
        // we're kind of assuming it is well-formed
        embeddedLinkPattern = Pattern.compile("href=\"" + URL_PREFIX + "/display/" + spaceKey + "/(.*?)\"");
        
        this.service = service;
        this.token = token;
        this.spaceKey = spaceKey;
        
        velCtxt = new VelocityContext();
        
        initializeClassTemplate(template);
    }
    
    
    private void initializeClassTemplate(String template) throws Exception {
        VelocityEngine velocityEngine = new VelocityEngine();
        try {

            // use ClasspathResourceLoader for velocity templates lookup
            // if Cayenne URL is not null, load resource from this URL
            Properties props = new Properties();

            // null logger that will prevent velocity.log from being generated
            props.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogSystem.class
                    .getName());

            props.put("resource.loader", "cayenne");

            props.put("cayenne.resource.loader.class", ClassGeneratorResourceLoader.class
                    .getName());
            
            velocityEngine.init(props);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Can't initialize Velocity", ex);
        }
        
        pageTemplate = velocityEngine.getTemplate(template);
    }
    
    public void render(DocPage page, Writer out) throws Exception {

        // Add the TOC, unless this is the top-level page
        StringBuffer toc = new StringBuffer();
        if (page.getParentRef() != null) {
            toc.append("<div id=\"cayenne_toc\">\n");

            DocPage root = page.getRoot();

            iterateChildren(toc, page, root);
            toc.append("</div>\n");
        }
        
        // Figure out the level of nesting for relative links
        String basePath = "";
        for (int i = 1; i <= page.getDepth(); i++) {
            basePath += "../"; 
        }
        
        String renderedContent = service.renderContent(token, spaceKey, page.getId(), page.getRawContent(), new HashMap(
                Collections.singletonMap("style", "clean")));
        
        // Replace cross-doc links
        Matcher linkMatcher = embeddedLinkPattern.matcher(renderedContent);
        StringBuffer replacementBuffer = new StringBuffer();
        while (linkMatcher.find()) {
            DocPage destPage = DocPage.getPageByTitle(linkMatcher.group(1).replace('+', ' '));
            
            // If we don't understand the link, just leave it alone to be safe
            if (destPage == null) {
                continue;
            }
            linkMatcher.appendReplacement(replacementBuffer, "href=\"" + basePath + destPage.getLinkPath() + "/index.html\"");
        }
        linkMatcher.appendTail(replacementBuffer);
        
        renderedContent = replacementBuffer.toString();
        
        //renderedContent = embeddedLinkPattern.matcher(renderedContent).replaceAll("href=\"$1/index.html\"");
        
        // Replace attachment links
        renderedContent = attachmentPattern.matcher(renderedContent).replaceAll("$1=\"$3\"");
        
        // Convert confluence images to relative links
        renderedContent = confluenceImagePattern.matcher(renderedContent).replaceAll("src=\"" + basePath + "images/$1\"");

        // Replace wiki links
        renderedContent = confluenceLinkPattern.matcher(renderedContent).replaceAll("href=\"" + CONFLUENCE_URL + "$1\"");
        
        // Convert local links with + to %20 to make browsers happy (wtf?)
        Matcher matcher = spaceEncoderPattern.matcher(renderedContent);
        
        replacementBuffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(replacementBuffer, matcher.group(0).replace("+", "%20"));
        }
        matcher.appendTail(replacementBuffer);
        
        renderedContent = replacementBuffer.toString();
        
        
        velCtxt.put("page", page);
        velCtxt.put("basePath", basePath);
        velCtxt.put("pageContent", toc.toString() + renderedContent);

        pageTemplate.merge(velCtxt, out);
        
        
    }

    private void iterateChildren(StringBuffer toc, DocPage currentPage, DocPage basePage) {
        toc.append("<ul>\n");
        for (Iterator baseIter = basePage.getChildren().iterator(); baseIter.hasNext(); ) {
            
            DocPage child = (DocPage) baseIter.next();
            
            toc.append("<li>").append("<a href=\"");
            for (int i = 1; i <= currentPage.getDepth(); i++) {
                toc.append("../"); 
            }
            toc.append(child.getLinkPath()).append("/index.html\">");
            toc.append(child.getTitle()).append("</a>");
            if (child.hasDescendent(currentPage)) {
                // render children
                iterateChildren(toc, currentPage, child);
            }
            
            toc.append("</li>\n");
        }

        toc.append("</ul>\n");
    }

}
