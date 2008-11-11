package org.apache.cayenne.access;

import org.apache.cayenne.unit.InheritanceCase;
import org.apache.cayenne.testdo.horizontalinherit.SubEntity1;
import org.apache.cayenne.testdo.horizontalinherit.AbstractSuperEntity;
import org.apache.cayenne.testdo.horizontalinherit.SubEntity2;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SQLTemplate;

import java.util.List;

/**
 * Tests for horizontal inheritance implementation.
 * 
 */
public class HorizontalInheritanceTest extends InheritanceCase {

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }

    public void testAbstractSuperEntity() {
        SubEntity1 subEntity1 = context.newObject(SubEntity1.class);
        subEntity1.setSuperIntAttr(666);
        subEntity1.setSuperStringAttr("stringValue");
        subEntity1.setSubEntityStringAttr("anotherStringValue");
        context.commitChanges();

        SelectQuery concreteSelect = new SelectQuery(SubEntity1.class);
        List result = context.performQuery(concreteSelect);
        assertNotNull(result);
        assertEquals(1, result.size());

        SubEntity2 subEntity2 = context.newObject(SubEntity2.class);
        subEntity2.setSuperIntAttr(666);
        subEntity2.setSuperStringAttr("stringValue");
        subEntity2.setSubEntityIntAttr(13);
        context.commitChanges();

        SelectQuery abstractSelect = new SelectQuery(AbstractSuperEntity.class);
        // this fails for now
        // List result1 = context.performQuery(abstractSelect);
        // assertNotNull(result1);
        // assertEquals(2, result1.size());
    }

    public void testUnions() {
        SubEntity1 subEntity1 = context.newObject(SubEntity1.class);
        subEntity1.setSuperIntAttr(666);
        subEntity1.setSuperStringAttr("stringValue");
        subEntity1.setSubEntityStringAttr("anotherStringValue");
        context.commitChanges();

        SQLTemplate insertSql = new SQLTemplate(
                SubEntity1.class,
                " INSERT INTO DbEntity4 VALUES (13, 'Inserted value 1', 666, 'Inserted value 2')");

        context.performQuery(insertSql);

        SQLTemplate unionSql = new SQLTemplate(
                SubEntity1.class,
                " SELECT ID_1 as ID, SUBENTITY_STRING_DB_ATTR_1 as SUBENTITY_STRING_DB_ATTR, SUPER_INT_DB_ATTR_1 as SUPER_INT_DB_ATTR, SUPER_STRING_DB_ATTR_1 as SUPER_STRING_DB_ATTR FROM DbEntity4"
                        + " UNION ALL"
                        + " SELECT * FROM DbEntity1");

        List result = context.performQuery(unionSql);

        assertNotNull(result);
        assertEquals(2, result.size());

    }
}
