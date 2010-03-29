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
package org.apache.cayenne.maven.plugin.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;

public class PatternGroup {

    protected Collection patterns;

    static Collection parsePatterns(File patternsFile) throws MojoExecutionException {
        Collection patterns = new ArrayList();

        if (patternsFile == null || !patternsFile.isFile()) {
            return patterns;
        }

        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(patternsFile));
        }
        catch (FileNotFoundException e) {
            throw new MojoExecutionException("Error reading patterns file "
                    + patternsFile, e);
        }

        try {
            String line;
            while ((line = in.readLine()) != null) {
                patterns.add(line);
            }
        }
        catch (IOException e) {
            throw new MojoExecutionException("Error reading patterns file "
                    + patternsFile, e);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ioex) {
            }
        }

        return patterns;
    }

    public PatternGroup(Collection patterns) {
        this.patterns = patterns;
    }

    public PatternGroup(File patternsFile) throws MojoExecutionException {
        this(parsePatterns(patternsFile));
    }

    public void addPatterns(String[] patterns) {
        this.patterns.addAll(Arrays.asList(patterns));
    }

    public String[] getPatterns() {
        return (String[]) patterns.toArray(new String[patterns.size()]);
    }
    
    public int size() {
        return patterns.size();
    }
}
