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

import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogSystem;
import org.objectstyle.confluence.rpc.soap_axis.confluenceservice_v1.ConfluenceSoapService;

/**
 * Extracts embedded links from Confluence documentation and converts them to local fs
 * references
 * 
 */
public class DocPageRenderer {

    private static final String URL_PREFIX = "/confluence";

    /**
     * Only attachments within the page are supported right now. This could easily be
     * adjusted to find attachments in external documents if necessary.
     */
    private static final Pattern attachmentPattern = Pattern.compile("(href|src)=\""
            + URL_PREFIX
            + "/download/attachments/(.*?)/(.*?)\"");

    /**
     * When browsing the local filesystem, browsers like %20 (hex encoded) instead of +
     * (legacy HTTP 0.9) for spaces.
     */
    private static final Pattern spaceEncoderPattern = Pattern
            .compile("href=\"(?!http://).*?\\+.*?\"");

    /**
     * Not all images are supported - only the ones referenced by current docs.
     */
    private static final Pattern confluenceImagePattern = Pattern.compile("src=\""
            + URL_PREFIX
            + "/images/icons/(.*?)\"");

    /**
     * Take any confluence links to non-doc content and add the url
     */
    private Pattern confluenceLinkPattern = Pattern.compile("href=\"("
            + URL_PREFIX
            + "/display/.*?)\"");

    private Pattern embeddedLinkPattern;

    private ConfluenceSoapService service;

    private String token;

    private String spaceKey;

    private String baseUrl;

    private VelocityContext velCtxt;

    private Template pageTemplate;

    public DocPageRenderer(ConfluenceSoapService service, String baseUrl, String token,
            String spaceKey, String template) throws Exception {

        // Note that these regexps have a fairly narrow capture - since the HTML
        // is
        // machine-generated,
        // we're kind of assuming it is well-formed
        embeddedLinkPattern = Pattern.compile("href=\""
                + URL_PREFIX
                + "/display/"
                + spaceKey
                + "/(.*?)\"");

        this.service = service;
        this.baseUrl = baseUrl;
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

            props.put(
                    "cayenne.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

            velocityEngine.init(props);
        }
        catch (Exception ex) {
            throw new RuntimeException("Can't initialize Velocity", ex);
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

        String renderedContent = null;
        try {
            renderedContent = service.renderContent(token, spaceKey, page.getId(), page
                    .getRawContent(), new HashMap(Collections.singletonMap(
                    "style",
                    "clean")));
        }
        catch (Throwable t) {
            // could have hit a DOS prevention bit so
            // sleep for 250ms and try again
            Thread.sleep(250);
            renderedContent = service.renderContent(token, spaceKey, page.getId(), page
                    .getRawContent(), new HashMap(Collections.singletonMap(
                    "style",
                    "clean")));
        }
        // Replace cross-doc links
        Matcher linkMatcher = embeddedLinkPattern.matcher(renderedContent);
        StringBuffer replacementBuffer = new StringBuffer();
        while (linkMatcher.find()) {
            DocPage destPage = DocPage.getPageByTitle(linkMatcher.group(1).replace(
                    '+',
                    ' '));

            // If we don't understand the link, just leave it alone to be safe
            if (destPage == null) {
                continue;
            }
            linkMatcher.appendReplacement(replacementBuffer, "href=\""
                    + basePath
                    + destPage.getLinkPath()
                    + "/index.html\"");
        }
        linkMatcher.appendTail(replacementBuffer);

        renderedContent = replacementBuffer.toString();

        // renderedContent =
        // embeddedLinkPattern.matcher(renderedContent).replaceAll("href=\"$1/index.html\"");

        // Replace attachment links
        renderedContent = attachmentPattern.matcher(renderedContent).replaceAll(
                "$1=\"$3\"");

        // Convert confluence images to relative links
        renderedContent = confluenceImagePattern.matcher(renderedContent).replaceAll(
                "src=\"" + basePath + "images/$1\"");

        // Replace wiki links
        renderedContent = confluenceLinkPattern.matcher(renderedContent).replaceAll(
                "href=\"" + baseUrl + "$1\"");

        // Convert local links with + to %20 to make browsers happy (wtf?)
        Matcher matcher = spaceEncoderPattern.matcher(renderedContent);

        replacementBuffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(replacementBuffer, matcher.group(0).replaceAll(
                    "\\+",
                    "%20"));
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
        for (Iterator baseIter = basePage.getChildren().iterator(); baseIter.hasNext();) {

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
