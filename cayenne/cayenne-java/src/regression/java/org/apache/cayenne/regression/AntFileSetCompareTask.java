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


package org.apache.cayenne.regression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

/**
 * @author Mike Kienenberger
 *
 * Ant task to compare two parallel sets of files.
 * 
 * <code>failOnError</code> determines whether failure to compare is fatal.
 * <code>dir1</code> is directory containing first set of relative fileset names.
 * <code>dir2</code> is directory containing second set of relative fileset names.
 * nested <code>fileset</code> element specifies relative fileset names.
 * 
 * 
 */
public class AntFileSetCompareTask extends MatchingTask {

    private boolean failOnError = true;
    private File dir1;
    private File dir2;
    List filesets = new ArrayList();

    /**
     * Files to generate checksums for.
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
    }

    public void setDir1(File dir1) {
        this.dir1 = dir1;
    }

    public void setDir2(File dir2) {
        this.dir2 = dir2;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public void compare(InputStream one, InputStream two) throws IOException,
            BuildException {
        int count = 0;
        int oneValue = one.read();
        int twoValue = two.read();
        while ((oneValue != -1) && (twoValue != -1)) {
            if (oneValue != twoValue) {
                throw new BuildException("Contents differ at offset " + count);
            }

            ++count;
            oneValue = one.read();
            twoValue = two.read();
        }

        if (oneValue != twoValue) {
            throw new BuildException("Contents differ at offset " + count);
        }
    }

    public void execute() throws BuildException {
        if (dir1 == null) {
            throw new BuildException("dir1 must be specified");
        }
        if (dir2 == null) {
            throw new BuildException("dir2 must be specified");
        }

        for (int i = 0; i < filesets.size(); i++) {
            FileSet fileSet = (FileSet) filesets.get(i);
            DirectoryScanner directoryScanner = fileSet.getDirectoryScanner(getProject());
            String[] relativeFileNames = directoryScanner.getIncludedFiles();
            for (int j = 0; j < relativeFileNames.length; j++) {
                String baseName = relativeFileNames[j];
                File file1 = new File(dir1, baseName);

                FileInputStream inputStream1;
                try {
                    inputStream1 = new FileInputStream(file1);
                }
                catch (FileNotFoundException e) {
                    if (failOnError) {
                        throw new BuildException(file1.getAbsolutePath() + " not found.");
                    }
                    log(file1.getAbsolutePath() + " not found.");
                    continue;
                }

                File file2 = new File(dir2, baseName);
                FileInputStream inputStream2;
                try {
                    inputStream2 = new FileInputStream(file2);
                }
                catch (FileNotFoundException e) {
                    if (failOnError) {
                        throw new BuildException(file2.getAbsolutePath() + " not found.");
                    }
                    log(file2.getAbsolutePath() + " not found.");
                    continue;
                }

                try {
                    compare(inputStream1, inputStream2);
                }
                catch (BuildException e) {
                    if (failOnError) {
                        throw new BuildException(file1.getAbsolutePath() + " & " + file2.getAbsolutePath() + ": " + e.getMessage());
                    }
                    log(baseName + ": " + e.getMessage());
                    continue;
                }
                catch (IOException e) {
                    if (failOnError) {
                        throw new BuildException(e.getMessage(), e);
                    }
                    log(e.getMessage());
                    continue;
                }
            }
        }
    }
}
