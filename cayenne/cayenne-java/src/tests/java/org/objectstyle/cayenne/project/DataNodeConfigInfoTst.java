package org.objectstyle.cayenne.project;

import java.io.File;

import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataNodeConfigInfoTst extends CayenneTestCase {
	protected DataNodeConfigInfo test; 

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        test = new DataNodeConfigInfo();
    }
    
    public void testAdapter() throws Exception {
    	test.setAdapter("abc");
    	assertEquals("abc", test.getAdapter());
    }

    public void testDomain() throws Exception {
        test.setDomain("abc");
        assertEquals("abc", test.getDomain());
    }
    
    public void testDataSource() throws Exception {
        test.setDataSource("abc");
        assertEquals("abc", test.getDataSource());
    }
    
    public void testDriverFile() throws Exception {
    	File f = new File("abc");
        test.setDriverFile(f);
        assertSame(f, test.getDriverFile());
    }
    
    public void testName() throws Exception {
        test.setName("abc");
        assertEquals("abc", test.getName());
    }
}
