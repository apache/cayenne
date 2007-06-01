/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.project;

import java.io.File;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;

/**
 * Stores information necessary to reconfigure nodes of existing projects.
 *  
 * @author Andrei Adamchik
 */
public class DataNodeConfigInfo {
    protected String name;
    protected String domain;
    protected String adapter;
    protected String dataSource;
    protected File driverFile;

    /**
     * Searches for the DataNode described by this DataNodeConfigInfo in the
     * provided configuration object. Throws ProjectException if there is no
     * matching DataNode.
     */
    public DataNode findDataNode(Configuration config)
        throws ProjectException {
        DataDomain domainObj = null;

        // domain name is either explicit, or use default domain
        if (domain != null) {
            domainObj = config.getDomain(domain);

            if (domainObj == null) {
                throw new ProjectException("Can't find domain named " + domain);
            }
        } else {
            try {
                domainObj = config.getDomain();
            } catch (Exception ex) {
                throw new ProjectException("Project has no default domain.", ex);
            }

            if (domainObj == null) {
                throw new ProjectException("Project has no domains configured.");
            }
        }

        DataNode node = domainObj.getNode(name);
        if (node == null) {
            throw new ProjectException(
                "Domain "
                    + domainObj.getName()
                    + " has no node named '"
                    + name
                    + "'.");
        }
        return node;
    }

    /**
     * Returns the adapter.
     * @return String
     */
    public String getAdapter() {
        return adapter;
    }

    /**
     * Returns the dataSource.
     * @return String
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * Returns the domain.
     * @return String
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Returns the driverFile.
     * @return File
     */
    public File getDriverFile() {
        return driverFile;
    }

    /**
     * Returns the name.
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the adapter.
     * @param adapter The adapter to set
     */
    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    /**
     * Sets the dataSource.
     * @param dataSource The dataSource to set
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Sets the domain.
     * @param domain The domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Sets the driverFile.
     * @param driverFile The driverFile to set
     */
    public void setDriverFile(File driverFile) {
        this.driverFile = driverFile;
    }

    /**
     * Sets the name.
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

}
