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

package org.objectstyle.cayenne.regression;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

import org.apache.log4j.Level;
import org.objectstyle.ashwood.dbutil.RandomSchema;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.FileConfiguration;

/**
 * Main configures and runs regression tests defined in RandomDomainBuilder and
 * DataModificationRobot. It is responsible for performance metering as well.
 *
 * @author Andriy Shapochka
 */

public class Main {
    protected Preferences prefs;

    public static void main(String[] args) {
        QueryLogger.setLoggingLevel(Level.ALL);
        QueryLogger.setLoggingLevel(null);
        System.out.println(
            "max memory, MB: "
                + Runtime.getRuntime().maxMemory() / (1024 * 1024));
        System.out.println(
            "total memory, MB: "
                + Runtime.getRuntime().totalMemory() / (1024 * 1024));
        System.out.println(
            "free memory, MB: "
                + Runtime.getRuntime().freeMemory() / (1024 * 1024));

        Preferences prefs;
        try {
            prefs = new Preferences(args);
        } catch (Exception ex) {
            System.out.println("Fatal Error: " + ex.getMessage());
            System.exit(1);
            return;
        }

        if (new Main(prefs).execute()) {
            System.exit(1);
        }

        System.exit(0);
    }

    public Main(Preferences prefs) {
        this.prefs = prefs;
    }

    protected DataDomain createDomain() throws Exception {
        ClassLoader loader = new DOStubClassLoader();
        Configuration.bootstrapSharedConfiguration(loader.loadClass("Table"));
        Configuration conf = new FileConfiguration(prefs.getCayenneProject());
        return conf.getDomain();
    }

    protected RandomDomainBuilder createDomainBuilder(
        DataDomain domain,
        Random randomizer) {

        RandomDomainBuilder domainBuilder = new RandomDomainBuilder(domain);
        RandomSchema rndSchema = domainBuilder.getRandomSchema();
        rndSchema.setSchemaName(prefs.getSchema());
        rndSchema.setTableCount(prefs.getTableCount());
        rndSchema.setMaxReferencesPerTable(prefs.getMaxReferencesPerTable());
        rndSchema.setMaxForeignKeysPerTable(prefs.getMaxForeignKeysPerTable());
        rndSchema.setLoopCount(prefs.getLoopCount());
        rndSchema.setMaxLoopsPerTable(prefs.getMaxLoopsPerTable());
        rndSchema.setRandomizer(randomizer);
        return domainBuilder;
    }

    public boolean execute() {
        boolean hasFailures = false;
        FileWriter fileOut = null;
        PrintWriter out = null;
        PrintWriter console = new PrintWriter(System.out, true);
        try {
            if (prefs.getOutFile() != null
                && !prefs.getOutFile().isDirectory()) {
                fileOut = new FileWriter(prefs.getOutFile());
                out = new PrintWriter(fileOut);
            }
            printHeader(console);
            if (out != null)
                printHeader(out);
            printPrefs(console, prefs);
            if (out != null)
                printPrefs(out, prefs);

            DataDomain domain = createDomain();
            Random randomizer = new Random(prefs.getSeed());
            RandomDomainBuilder domainBuilder =
                createDomainBuilder(domain, randomizer);

            File workDir = prefs.getWorkDirectory();
            String dirPrefix = prefs.getSchemaDirPrefix() + "-";
            for (int i = 1; i <= prefs.getSchemaCount(); i++) {
                try {
                    File schemaDir =
                        new File(
                            workDir,
                            dirPrefix + System.currentTimeMillis());
                    schemaDir.mkdirs();
                    printSchemaStart(console, i, schemaDir);
                    if (out != null)
                        printSchemaStart(out, i, schemaDir);
                    domainBuilder.generate(schemaDir);
                    DataContext ctxt = domain.createDataContext();
                    for (int j = 1; j <= prefs.getCommitsPerSchema(); j++) {
                        printCommitStart(console, j);
                        if (out != null)
                            printCommitStart(out, j);
                        long freeMem = Runtime.getRuntime().freeMemory();
                        console.println(
                            "free memory before gc, MB: "
                                + freeMem / (1024 * 1024));
                        do System.gc();
                        while (freeMem > Runtime.getRuntime().freeMemory());
                        freeMem = Runtime.getRuntime().freeMemory();
                        console.println(
                            "free memory after gc, MB: "
                                + freeMem / (1024 * 1024));
                        if (freeMem / (1024 * 1024) < 5) {
                            console.println("Out of memory!");
                            return true;
                        }
                        DataModificationRobot robot =
                            new DataModificationRobot(
                                ctxt,
                                randomizer,
                                prefs.getNewObjectPerTableCount(),
                                prefs.getDeleteObjectPerTableCount());
                        robot.generate();
                        try {
                            long start = System.currentTimeMillis();
                            ctxt.commitChanges(null);
                            long end = System.currentTimeMillis();
                            printCommitSuccess(console, j, end - start);
                            if (out != null)
                                printCommitSuccess(out, j, end - start);
                        } catch (Exception ex) {
                            hasFailures = true;
                            printCommitFailure(console, j, ex);
                            if (out != null)
                                printCommitFailure(out, j, ex);
                        }
                    }
                    printSchemaSuccess(console, i);
                    if (out != null)
                        printSchemaSuccess(out, i);
                } catch (Exception ex) {
                    hasFailures = true;
                    printSchemaFailure(console, i, ex);
                    if (out != null)
                        printSchemaFailure(out, i, ex);
                } finally {
                    domainBuilder.drop();
                }
            }
        } catch (Exception ex) {
            hasFailures = true;
            printFailure(console, ex);
            if (out != null)
                printFailure(out, ex);
        } finally {
            printFooter(console);
            if (out != null)
                printFooter(out);
            console.flush();
            try {
                out.close();
            } catch (Exception ex) {
            }
            try {
                fileOut.close();
            } catch (Exception ex) {
            }
        }

        return hasFailures;
    }

    protected void printHeader(PrintWriter out) {
        out.println("Test starting!");
    }

    protected void printFooter(PrintWriter out) {
        out.println("Test finished!");
        out.println("Good-bye.");
    }

    protected void printPrefs(PrintWriter out, Preferences prefs) {
        out.println("Loaded preferences - ");
        prefs.print(out);
    }

    protected void printSchemaStart(
        PrintWriter out,
        int schemaIndex,
        File schemaDir) {
        out.println();
        out.println("Schema " + schemaIndex + " generating.");
        out.println("schema recording in " + schemaDir);
    }

    protected void printCommitStart(PrintWriter out, int commitIndex) {
        out.println("  Commit " + commitIndex + " starting.");
    }

    protected void printCommitSuccess(
        PrintWriter out,
        int commitIndex,
        long ms) {
        out.println(
            "  Commit " + commitIndex + " succeeded. Time=" + ms + " ms");
    }

    protected void printCommitFailure(
        PrintWriter out,
        int commitIndex,
        Exception e) {
        out.println("  Commit " + commitIndex + " failed.");
        e.printStackTrace(out);
        out.println();
    }

    protected void printSchemaSuccess(PrintWriter out, int schemaIndex) {
        out.println("Schema " + schemaIndex + " succeeded.");
    }

    protected void printSchemaFailure(
        PrintWriter out,
        int schemaIndex,
        Exception e) {
        out.println("Schema " + schemaIndex + " failed.");
        e.printStackTrace(out);
        out.println();
    }

    protected void printFailure(PrintWriter out, Exception e) {
        out.println("Fatal Error: ");
        e.printStackTrace(out);
        out.println();
    }
}