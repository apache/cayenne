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
package org.objectstyle.cayenne.perform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.sql.DataSource;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.PoolDataSource;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.MapLoader;
import org.objectstyle.perform.PerformanceTest;
import org.objectstyle.perform.PerformanceTestRunner;
import org.objectstyle.perform.PerformanceTestSuite;
import org.objectstyle.perform.ResultRenderer;

/** Runs performance tests. */
public class PerformMain {
    public static DataDomain sharedDomain;

    public static void main(String[] args) {
        Configuration.configureCommonLogging();
        prepareDomain();

        if (args.length == 0) {

            // dry run
            new PerformanceTestRunner(new ResultRenderer()).runSuite(prepareDryRun());

            // real tests
            ResultRenderer renderer = new ResultRenderer();
            new PerformanceTestRunner(renderer).runSuite(prepareTests());
            renderer.showResults();
        } else {
            new PerformanceTestRunner(null).runTest(prepareTest(args[0]));
        }
    }

    public static PerformanceTest prepareTest(String testClass) {
        try {
            return PerformanceTestSuite.testForClass(testClass);
        } catch (Exception ex) {
            throw new RuntimeException("Error", ex);
        }
    }

    public static PerformanceTestSuite prepareDryRun() {
        PerformanceTestSuite dryRunSuite = new PerformanceTestSuite();

        dryRunSuite.addTestPair(
            "Dry Run",
            "Dry Run",
            "org.objectstyle.cayenne.perform.test.SelectTest",
            "org.objectstyle.cayenne.perform.test.SelectReadOnlyTest");
        return dryRunSuite;
    }

    public static PerformanceTestSuite prepareTests() {
        PerformanceTestSuite suite = new PerformanceTestSuite();

        suite.addTestPair(
            "Insert",
            "Inserting "
                + CayennePerformanceTest.objCount
                + " records, Cayenne vs. JDBC.",
            "org.objectstyle.cayenne.perform.test.InsertTest",
            "org.objectstyle.cayenne.perform.test.InsertRefTest");

        suite.addTestPair(
            "Select",
            "Select "
                + CayennePerformanceTest.objCount
                + " records, Cayenne objects vs. JDBC.",
            "org.objectstyle.cayenne.perform.test.SelectTest",
            "org.objectstyle.cayenne.perform.test.SelectRefTest");

       suite.addTestPair(
            "Select",
            "Select "
                + CayennePerformanceTest.objCount
                + " records, Cayenne objects vs. Cayenne read-only objects.",
            "org.objectstyle.cayenne.perform.test.SelectTest",
            "org.objectstyle.cayenne.perform.test.SelectReadOnlyTest");
            
            
        suite.addTestPair(
            "Select",
            "Select "
                + CayennePerformanceTest.objCount
                + " records, Cayenne data rows vs. Cayenne objects.",
            "org.objectstyle.cayenne.perform.test.SelectDataRowsTest",
            "org.objectstyle.cayenne.perform.test.SelectTest");

        suite.addTestPair(
            "Select",
            "Select "
                + CayennePerformanceTest.objCount
                + " records, Cayenne objects vs. Cayenne objects (iterated list - size 50).",
            "org.objectstyle.cayenne.perform.test.SelectTest",
            "org.objectstyle.cayenne.perform.test.SelectIteratedTest");

        suite.addTestPair(
            "Select Small Lists",
            "Select one record over and over again, JDBC Prep. Statement. vs. JDBC Statement",
            "org.objectstyle.cayenne.perform.test.PreparedSmallSelectTest",
            "org.objectstyle.cayenne.perform.test.SmallSelectTest");

        suite.addTestPair(
            "Select Small Lists",
            "Select one record over and over again, Cayenne vs. JDBC Statement (being reopened in every query)",
            "org.objectstyle.cayenne.perform.test.CayenneSmallSelectTest",
            "org.objectstyle.cayenne.perform.test.ReopenedSmallSelectTest");

        suite.addTestPair(
            "Select Small Lists",
            "Select one record over and over again, Cayenne SelectQuery vs. Cayenne SQLSelectQuery",
            "org.objectstyle.cayenne.perform.test.DataRowsSmallSelectTest",
            "org.objectstyle.cayenne.perform.test.SQLSmallSelectTest");

        return suite;
    }

    public static void prepareDomain() {
        try {
            DataSourceInfo dsi = new ConnectionSetup().buildConnectionInfo();

            PoolDataSource poolDS =
                new PoolDataSource(dsi.getJdbcDriver(), dsi.getDataSourceUrl());

            DataSource ds =
                new PoolManager(
                    poolDS,
                    dsi.getMinConnections(),
                    dsi.getMaxConnections(),
                    dsi.getUserName(),
                    dsi.getPassword());

            // map
            DataMap map = new MapLoader().loadDataMap("test-resources/testmap.map.xml");

            // node
            Class adapterClass = DataNode.DEFAULT_ADAPTER_CLASS;

            if (dsi.getAdapterClassName() != null) {
                adapterClass = Class.forName(dsi.getAdapterClassName());
            }
 
            DbAdapter adapter = (DbAdapter) adapterClass.newInstance();
			DataNode node = adapter.createDataNode("node");
			node.setDataSource(ds);
            node.addDataMap(map);

            // generate pk's
            Iterator dataMaps = node.getDataMaps().iterator();
			while (dataMaps.hasNext()) {
				Collection ents = ((DataMap)dataMaps.next()).getDbEntities();
                node.getAdapter().getPkGenerator().createAutoPk(node, new ArrayList(ents));
            }

            // domain
            sharedDomain = new DataDomain("Shared Domain");
            sharedDomain.addNode(node);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}