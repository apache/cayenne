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

import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.conf.FileConfiguration;
import org.objectstyle.cayenne.conf.RuntimeLoadDelegate;
import org.objectstyle.cayenne.util.ResourceLocator;

/**
 * Subclass of FileConfiguration used in the project model.
 *
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ProjectConfiguration extends FileConfiguration {

    /**
     * Override parent implementation to ignore loading failures.
     * @see FileConfiguration#FileConfiguration(File)
     */
    public ProjectConfiguration(File projectFile) {
        super(projectFile);

        // ignore loading failures
        this.setIgnoringLoadFailures(true);

        // configure deterministic file opening rules
        ResourceLocator locator = this.getResourceLocator();
        locator.setSkipAbsolutePath(false);
        locator.setSkipClasspath(true);
        locator.setSkipCurrentDirectory(true);
        locator.setSkipHomeDirectory(true);

        // install custom loader
        setLoaderDelegate(new ProjectLoader());
    }

    /**
     * Override parent implementation to prevent loading of
     * nonexisting files.
     * @see FileConfiguration#canInitialize()
     */
    public boolean canInitialize() {
        return (super.canInitialize() && this.getProjectFile().isFile());
    }

    /**
     * Override parent implementation to allow for null files.
     * @see FileConfiguration#setProjectFile(File)
     */
    protected void setProjectFile(File projectFile) {
        if ((projectFile != null) && (projectFile.exists())) {
            super.setProjectFile(projectFile);
        }
        else {
            super.projectFile = projectFile;
            this.setDomainConfigurationName(Configuration.DEFAULT_DOMAIN_FILE);
        }
    }

    /**
     * Returns a DataSource factory for projects.
     * 
     * @see org.objectstyle.cayenne.project.ProjectDataSourceFactory
     */
    public DataSourceFactory getDataSourceFactory() {
        try {
            return new ProjectDataSourceFactory(this.getProjectDirectory());
        }
        catch (Exception e) {
            throw new ProjectException("Error creating DataSourceFactory.", e);
        }
    }

    final class ProjectLoader extends RuntimeLoadDelegate {

        public ProjectLoader() {
            super(
                ProjectConfiguration.this,
                ProjectConfiguration.this.getLoadStatus(),
                ProjectConfiguration.getLoggingLevel());
        }

        public void shouldLoadDataDomain(String domainName) {
            super.shouldLoadDataDomain(domainName);

            try {
                // disable class indexing 
                findDomain(domainName).getEntityResolver().setIndexedByClass(false);
            }
            catch (Exception ex) {
                throw new ConfigurationException("Domain is not loaded: " + domainName);
            }
        }
    }
}
