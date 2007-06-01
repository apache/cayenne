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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conf.ConfigStatus;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DataMapException;
import org.objectstyle.cayenne.map.MapLoader;
import org.xml.sax.InputSource;

/**
 * Cayenne project that consists of a single DataMap.
 * 
 * @author Andrei Adamchik
 */
public class DataMapProject extends Project {
	private static Logger logObj = Logger.getLogger(DataMapProject.class);
	
    protected DataMap map;

    /**
     * Constructor for MapProject.
     * 
     * @param projectFile
     */
    public DataMapProject(File projectFile) {
        super(projectFile);
    }

    /**
     * @since 1.1
     */
    public void upgrade() throws ProjectException {
        // upgrades not supported in this type of project
        throw new ProjectException("'DataMapProject' does not support upgrades.");
    }
    
    /**
     * Does nothing.
     */
    public void checkForUpgrades() {
        // do nothing
    }

    /**
    * Initializes internal <code>map</code> object and then calls super.
    */
    protected void postInitialize(File projectFile) {
        if (projectFile != null) {
            try {
                InputStream in = new FileInputStream(projectFile.getCanonicalFile());
                map = new MapLoader().loadDataMap(new InputSource(in));

                String fileName = resolveSymbolicName(projectFile);
                logObj.error("resolving: " + projectFile + " to " + fileName);
                String mapName =
                    (fileName != null && fileName.endsWith(DataMapFile.LOCATION_SUFFIX))
                        ? fileName.substring(0, fileName.length() - DataMapFile.LOCATION_SUFFIX.length())
                        : "UntitledMap";

                map.setName(mapName);
            } catch (IOException e) {
                throw new ProjectException("Error creating " + this.getClass().getName(), e);
            } catch (DataMapException dme) {
                throw new ProjectException("Error creating " + this.getClass().getName(), dme);
            }
        } else {
            map = (DataMap) NamedObjectFactory.createObject(DataMap.class, null);
        }

        super.postInitialize(projectFile);
    }


    /**
     * Returns a list that contains project DataMap as a single object. 
     */
    public List getChildren() {
    	List entities = new ArrayList();
    	entities.add(map);
        return entities;
    }


    /**
     * Returns appropriate ProjectFile or null if object does not require 
     * a file of its own. In case of DataMapProject, the only 
     * object that requires a file is the project itself.
     */
    public ProjectFile projectFileForObject(Object obj) {
    	if(obj == this) {
    		return new DataMapFile(this, map);
    	}
    	
        return null;
    }
    
    /**
     * Always returns empty status. Map projects do not support status tracking
     * yet.
     */
    public ConfigStatus getLoadStatus() {
        return new ConfigStatus();
    }
}
