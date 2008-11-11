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
 */
public class DocGenerator {
	private static final String DEFAULT_TEMPLATE = "doctemplates/default.vm";

	private static final String ENDPOINT_SUFFIX = "/rpc/soap-axis/confluenceservice-v1";

	private String baseUrl;

	private String spaceKey;

	private String docBase;

	private String startPage;

	private String token;

	private ConfluenceSoapService service;

	private String username;

	private String password;

	private String template;

	private DocPageRenderer parser;

	public DocGenerator(String baseUrl, String spaceKey, String docBase,
			String startPage, String username, String password, String template) {

		ConfluenceSoapServiceProxy service = new ConfluenceSoapServiceProxy();

		// derive service URL from base URL
		if (baseUrl != null) {
			if (baseUrl.endsWith("/")) {
				baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
			}

			String endpoint = baseUrl + ENDPOINT_SUFFIX;
			service.setEndpoint(endpoint);
		}
		// service base URL from service default URL
		else if (service.getEndpoint().endsWith(ENDPOINT_SUFFIX)) {
			String endpoint = service.getEndpoint();
			baseUrl = endpoint.substring(0, endpoint.length()
					- ENDPOINT_SUFFIX.length());
		} else {
			throw new IllegalArgumentException(
					"Null base url and invalid service URL");
		}

		this.baseUrl = baseUrl;
		this.service = service;
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

	/**
	 * Main worker method for documentation generation from Wiki.
	 */
	public void generateDocs() throws Exception {

		login();
		createPath(docBase);

		// Build a page hierarchy first..
		DocPage root = getPage(null, startPage);

		iterateChildren(root);

		renderPage(root, docBase);
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

		for (Iterator childIter = page.getChildren().iterator(); childIter
				.hasNext();) {
			renderPage((DocPage) childIter.next(), currentPath);
		}
	}

	protected RemotePageSummary[] getChildren(DocPage page) throws Exception {
		return service.getChildren(token, page.getId());
	}

	protected void writeAttachments(String basePath, DocPage page)
			throws Exception {
		RemoteAttachment[] attachments = service.getAttachments(token, page
				.getId());

		for (int j = 0; j < attachments.length; j++) {

			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(basePath + "/"
						+ attachments[j].getFileName());

				fos.write(getAttachmentData(page, attachments[j]));
			} finally {
				fos.close();
			}

		}
	}

	protected byte[] getAttachmentData(DocPage page, RemoteAttachment attachment)
			throws Exception {
		return service.getAttachmentData(token, page.getId(), attachment
				.getFileName(), 0);
	}

	protected void login() throws Exception {
		token = service.login(username, password);
		parser = new DocPageRenderer(service, baseUrl, token, spaceKey,
				template);
	}

	protected DocPage getPage(DocPage parentPage, String pageTitle)
			throws Exception {
		RemotePage page = service.getPage(token, spaceKey, pageTitle);
		return new DocPage(parentPage, page.getTitle(), page.getId(), page
				.getContent());
	}

	protected void createPath(String path) {
		new File(path).mkdirs();
	}

	public String getBaseUrl() {
		return baseUrl;
	}
}
