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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Iterator;

import org.objectstyle.confluence.rpc.soap_axis.confluenceservice_v1.ConfluenceSoapService;
import org.objectstyle.confluence.rpc.soap_axis.confluenceservice_v1.ConfluenceSoapServiceProxy;

import com.atlassian.confluence.rpc.soap.beans.RemoteAttachment;
import com.atlassian.confluence.rpc.soap.beans.RemotePage;
import com.atlassian.confluence.rpc.soap.beans.RemotePageSummary;

/**
 * Generates standalone documentation for Cayenne based on Confluence content.
 * 
 * @author Cris Daniluk
 */
public class DocGenerator {

    private static final String DEFAULT_TEMPLATE = "doctemplates/default.vm";
    private String spaceKey;
    private String docBase;
    private String startPage;

    private String token;
    private ConfluenceSoapService service;

    private String username;
    private String password;

    private String template;

    private DocPageRenderer parser;

    public DocGenerator(String spaceKey, String docBase, String startPage,
            String username, String password, String template) {
        this.spaceKey = spaceKey;
        this.docBase = docBase;
        this.startPage = startPage;
        this.username = username;
        this.password = password;
        
        if (template == null) {
            this.template = DEFAULT_TEMPLATE;
        } else {
            this.template = template;
        }
    }

    public void generateDocs() throws Exception {

        login();

        createPath(docBase);

        // Build a page hierarchy first..
        DocPage page = getPage(null, startPage);

        iterateChildren(page);

        // Now render the content nodes..
        renderPage(page, docBase);

    }

    protected void iterateChildren(DocPage parent) throws Exception {

        RemotePageSummary[] children = getChildren(parent);
        for (int i = 0; i < children.length; i++) {

            DocPage child = getPage(parent, children[i].getTitle());

            parent.addChild(child);
            iterateChildren(child);

        }

    }

    protected void renderPage(DocPage page, String basePath) throws Exception {
        String currentPath = basePath + "/" + page.getTitle();

        createPath(currentPath);

        FileWriter fw = new FileWriter(currentPath + "/index.html");
        parser.render(page, fw);
        fw.close();
        
        writeAttachments(currentPath, page);

        for (Iterator childIter = page.getChildren().iterator(); childIter.hasNext();) {
            renderPage((DocPage) childIter.next(), currentPath);
        }

    }

    protected RemotePageSummary[] getChildren(DocPage page) throws Exception {
        return service.getChildren(token, page.getId());
    }

    protected void writeAttachments(String basePath, DocPage page) throws Exception {
        RemoteAttachment[] attachments = service.getAttachments(token, page.getId());

        for (int j = 0; j < attachments.length; j++) {

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(basePath + "/" + attachments[j].getFileName());

                fos.write(getAttachmentData(page, attachments[j]));
            }
            finally {
                fos.close();
            }

        }
    }

    protected byte[] getAttachmentData(DocPage page, RemoteAttachment attachment)
            throws Exception {
        return service
                .getAttachmentData(token, page.getId(), attachment.getFileName(), 0);
    }

    protected void login() throws Exception {
        service = new ConfluenceSoapServiceProxy();
        token = service.login(username, password);
        parser = new DocPageRenderer(service, token, spaceKey, template);
    }

    protected DocPage getPage(DocPage parentPage, String pageTitle) throws Exception {
        RemotePage page = service.getPage(token, spaceKey, pageTitle);
        return new DocPage(parentPage, page.getTitle(), page.getId(), page.getContent());
    }

    protected void createPath(String path) {
        new File(path).mkdirs();

    }

}
