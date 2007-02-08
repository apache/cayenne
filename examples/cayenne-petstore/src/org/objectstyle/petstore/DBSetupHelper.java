package org.objectstyle.petstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.query.QueryChain;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.petstore.domain.Account;

/**
 * Helper class to init test DB.
 * 
 * @author Andrus Adamchik
 */
class DBSetupHelper {

    static final String TEST_DATA = "/WEB-INF/petstore-dataload.sql";

    ServletContext context;
    DataDomain domain;

    DBSetupHelper(ServletContext context, DataDomain domain) {
        this.context = context;
        this.domain = domain;
    }

    void setupDatabase() throws ServletException {
        if (checkDBSetupNeeded()) {
            setupDemoSchema();

            InputStream in = context.getResourceAsStream(TEST_DATA);
            if (in == null) {
                throw new ServletException("Can't find  resource " + TEST_DATA);
            }

            setupDemoData(in);
        }
    }

    private DataNode getDataNode() {
        return domain.lookupDataNode(getDataMap());
    }

    private DataMap getDataMap() {
        return domain.getEntityResolver().lookupObjEntity(Account.class).getDataMap();
    }

    /**
     * Runs a test query to see if a schema is initialized.
     */
    private boolean checkDBSetupNeeded() {
        try {
            DataContext.createDataContext().performNonSelectingQuery("schemaCheck");
            return false;
        }
        catch (Throwable th) {
            return true;
        }
    }

    private void setupDemoSchema() throws ServletException {
        DataNode node = getDataNode();
        DbGenerator generator = new DbGenerator(node.getAdapter(), getDataMap());
        try {
            generator.runGenerator(node.getDataSource());
        }
        catch (Exception e) {
            throw new ServletException("Error generating DB schema", e);
        }
    }

    private void setupDemoData(InputStream sql) throws ServletException {

        DataMap map = getDataMap();

        // TODO: Andrus, 01/08/2006 - move code that loads SQL from file to QueryChain or
        // some other Cayenne utilities class.
        QueryChain chain = new QueryChain();
        BufferedReader reader = new BufferedReader(new InputStreamReader(sql));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0) {
                    continue;
                }

                if (line.endsWith(";")) {
                    line = line.substring(0, line.length() - 1);
                }

                chain.addQuery(new SQLTemplate(map, line));
            }
        }
        catch (IOException e) {
            throw new ServletException("Error reading " + TEST_DATA);
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {

            }
        }

        if (!chain.isEmpty()) {
            DataContext cayenneContext = DataContext.createDataContext();
            cayenneContext.performNonSelectingQuery(chain);
        }
    }
}
