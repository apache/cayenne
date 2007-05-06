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

package org.objectstyle.cayenne.regression;

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