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
package org.apache.cayenne.modeler.graph;

import java.io.PrintWriter;

import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectFile;
import org.apache.cayenne.util.XMLEncoder;

/**
 * File, representing graph(s) of a domain
 */
public class GraphFile extends ProjectFile {
    public static final String LOCATION_SUFFIX = ".graph.xml";
    
    GraphMap graphMap;
    
    public GraphFile(Project project, GraphMap graphMap) {
        super(project, null);
        this.graphMap = graphMap;
    }
    
    /**
     * @see ProjectFile#getObject()
     */
    @Override
    public Object getObject() {
        return graphMap;
    }
    
    /**
     * @see ProjectFile#getObjectName()
     */
    @Override
    public String getObjectName() {
        return graphMap.getDomain().getName();
    }
    
    @Override
    public String getLocationSuffix() {
        return LOCATION_SUFFIX;
    }
    
    /**
     * @see org.apache.cayenne.project.ProjectFile#canHandle(Object)
     */
    @Override
    public boolean canHandle(Object obj) {
        return obj instanceof GraphMap && ((GraphMap) obj).size() > 0;
    }
    
    @Override
    public void save(PrintWriter out) throws Exception {
        out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        graphMap.encodeAsXML(new XMLEncoder(out, "\t"));
    }
}
