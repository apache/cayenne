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

package org.apache.cayenne.tools.ant.docgen;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Wraps DocGenerator in an ant task
 * 
 * @see org.objectstyle.cayenne.tools.ant.docgen.DocGenerator
 * @author Cris Daniluk
 */
public class DocGenTask extends Task {

    private String baseUrl;
    private String spaceKey;
    private String docBase;
    private String startPage;

    private String username;
    private String password;

    private String template;

    public void execute() {
        log("Exporting space '" + spaceKey + "' to " + docBase);

        try {
            DocGenerator generator = new DocGenerator(
                    baseUrl,
                    spaceKey,
                    docBase,
                    startPage,
                    username,
                    password,
                    template);

            log("Confluence base URL '" + generator.getBaseUrl() + "'");
            generator.generateDocs();
        }
        catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public void setSpaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
    }

    public String getStartPage() {
        return startPage;
    }

    public void setStartPage(String startPage) {
        this.startPage = startPage;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Sets a root URL of a confluence instance. SOAP service URL is derived from it
     * internally.
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
