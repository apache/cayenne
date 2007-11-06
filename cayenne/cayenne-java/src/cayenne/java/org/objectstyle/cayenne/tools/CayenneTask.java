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
package org.objectstyle.cayenne.tools;

import org.apache.log4j.BasicConfigurator;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.objectstyle.cayenne.conf.Configuration;

/**
 * A superclass of Cayenne Ant tasks. Performs some common setup
 * 
 * @author Andrei Adamchik, Kevin Menard
 * @since 1.2
 */
public abstract class CayenneTask extends Task {

	protected Path classpath;
	
	/**
     * Sets the classpath used by the task.
     *
     * @param path The classpath to set.
     */
    public void setClasspath(Path path) {
        createClasspath().append(path);
    }

    /**
     * Sets the classpath reference used by the task.
     *
     * @param reference The classpath reference to set.
     */
    public void setClasspathRef(Reference reference) {
        createClasspath().setRefid(reference);
    }

    /**
     * Convenience method for creating a classpath instance to be used for the task.
     *
     * @return The new classpath.
     */
    private Path createClasspath() {
        if (null == classpath) {
            classpath = new Path(getProject());
        }

        return classpath.createPath();
    }

    /**
     * Sets up logging to be in line with the Ant logging system. It should be called by
     * subclasses from the "execute" method.
     */
    protected void configureLogging() {
        Configuration.setLoggingConfigured(true);

        // reset is needed since when multiple Cayenne tasks are loaded via Antlib each
        // one adds its own appender..

        // TODO: this is a really a bad solution ... each task would have to reset shared
        // resource whenever its execution starts...
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(new AntAppender(this));
    }
}