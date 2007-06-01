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

import java.io.PrintWriter;

import org.objectstyle.cayenne.conf.ConfigSaver;
import org.objectstyle.cayenne.conf.ConfigSaverDelegate;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.RuntimeSaveDelegate;

/**
 * ApplicationProjectFile is a ProjectFile abstraction of the 
 * main project file in a Cayenne project. Right now Cayenne 
 * projects can not be renamed, so all the name tracking functionality 
 * is pretty much noop.
 * 
 * @author Andrei Adamchik
 */
public class ApplicationProjectFile extends ProjectFile {
    protected ConfigSaverDelegate saveDelegate;

	private String objectName = null;

    private ApplicationProjectFile() {
    	super();
    }

	/**
	 * Constructor for default ApplicationProjectFile.
	 */
	public ApplicationProjectFile(Project project) {
		this(project, Configuration.DEFAULT_DOMAIN_FILE);
	}

	/**
	 * Constructor for ApplicationProjectFile with an existing file.
	 */
	public ApplicationProjectFile(Project project, String fileName) {
		super(project, fileName);
		this.objectName = fileName.substring(0, fileName.lastIndexOf(this.getLocationSuffix()));
	}

    /**
     * Returns suffix to append to object name when 
     * creating a file name. Default implementation 
     * returns empty string.
     */
    public String getLocationSuffix() {
        return ".xml";
    }

    /**
     * Returns a project.
     */
    public Object getObject() {
        return getProject();
    }

    /**
     * @see org.objectstyle.cayenne.project.ProjectFile#getObjectName()
     */
    public String getObjectName() {
        return this.objectName;
    }

    public void save(PrintWriter out) throws Exception {
        ConfigSaverDelegate localDelegate =
            (saveDelegate != null)
                ? saveDelegate
                : new RuntimeSaveDelegate(((ApplicationProject) projectObj).getConfiguration());
        new ConfigSaver(localDelegate).storeDomains(out);
    }

    public boolean canHandle(Object obj) {
        return obj instanceof ApplicationProject;
    }
    
    /**
     * Returns the saveDelegate.
     * @return ConfigSaverDelegate
     */
    public ConfigSaverDelegate getSaveDelegate() {
        return saveDelegate;
    }

    /**
     * Sets the saveDelegate.
     * @param saveDelegate The saveDelegate to set
     */
    public void setSaveDelegate(ConfigSaverDelegate saveDelegate) {
        this.saveDelegate = saveDelegate;
    }
}
