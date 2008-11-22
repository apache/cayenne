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

package org.apache.cayenne.project;

import java.io.File;

/**
 * CayenneUserDir represents a directory where all Cayenne-related information 
 * is stored on the user machine. This is normally a <code>$HOME/.cayenne</code>
 * directory.
 * 
 */
public class CayenneUserDir {

    protected static CayenneUserDir sharedInstance;

    public static final String CAYENNE_DIR = ".cayenne";
    
    /**
     * A property name for the property that allows to define an alternative
     * location of Cayenne User Directory (instead of default "$HOME/.cayenne").
     *  
     * @since 1.1
     */
    public static final String ALT_USER_DIR_PROPERTY = "cayenne.userdir";

    protected File cayenneUserDir;

    public static CayenneUserDir getInstance() {
    	if(sharedInstance == null) {
    		sharedInstance = new CayenneUserDir();
    	}
    	return sharedInstance;
    }
    
    
    /**
     * Constructor for CayenneUserDir.
     */
    protected CayenneUserDir() {
        super();

        File tmpDir = null;
        String dirName = System.getProperty(ALT_USER_DIR_PROPERTY);

        if (dirName != null) {
            tmpDir = new File(dirName);
        }
        else {
            File homeDir = new File(System.getProperty("user.home"));
            tmpDir = new File(homeDir, CAYENNE_DIR);
        }

        if (tmpDir.exists() && !tmpDir.isDirectory()) {
            tmpDir = null;
        }
        else if (tmpDir.exists() && !tmpDir.canRead()) {
            tmpDir = null;
        }
        else if (!tmpDir.exists()) {
            tmpDir.mkdirs();
            if (!tmpDir.exists()) {
                tmpDir = null;
            }
        }

        cayenneUserDir = tmpDir;
    }

    /**
     * Returns a directory object where all user Cayenne-related configuration is stored.
     * May return null if the directory is not accessible for whatever reason.
     */
    public File getDirectory() {
        return cayenneUserDir;
    }
    
    /**
     * Return false if the directory is not accessible for
     * any reason at least for reading.
     */    
    public boolean canRead() {
    	return cayenneUserDir != null;
    }
    
    /**
     * Return false if the directory is not accessible for
     * any reason at least for reading.
     */    
    public boolean canWrite() {
    	return cayenneUserDir != null && cayenneUserDir.canWrite();
    }
    
    public File resolveFile(String name) {
    	return new File(cayenneUserDir, name);
    }
}
