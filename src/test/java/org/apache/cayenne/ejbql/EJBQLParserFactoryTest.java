package org.apache.cayenne.ejbql;

import org.apache.cayenne.ejbql.EJBQLParser;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.ejbql.parser.SimpleNode;

import junit.framework.TestCase;

public class EJBQLParserFactoryTest extends TestCase {

	public void testGetParser() {
		EJBQLParser parser = EJBQLParserFactory.getParser();
		assertNotNull(parser);
	}
	
	public void testDefaultParser() {
		EJBQLParser parser = EJBQLParserFactory.getParser();
		Object parsedSelect = parser.parse("select a from b");
		assertNotNull(parsedSelect);
		assertTrue(parsedSelect instanceof SimpleNode);
		
		Object parsedDelete = parser.parse("delete from c");
		assertNotNull(parsedDelete);
		assertTrue(parsedDelete instanceof SimpleNode);
	}
}
