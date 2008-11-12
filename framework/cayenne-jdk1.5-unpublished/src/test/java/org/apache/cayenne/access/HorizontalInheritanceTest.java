package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.unit.InheritanceCase;
import org.apache.cayenne.testdo.horizontalinherit.SubEntity1;
import org.apache.cayenne.testdo.horizontalinherit.AbstractSuperEntity;
import org.apache.cayenne.testdo.horizontalinherit.SubEntity2;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SQLTemplate;

import java.util.List;

/**
 * Tests for horizontal inheritance implementation.
 * 
 */
public class HorizontalInheritanceTest extends InheritanceCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testAbstractSuperEntity() {
        ObjectContext context = createDataContext();
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

    public void testDatabaseUnionCapabilities() {

        QueryChain inserts = new QueryChain();
        inserts
                .addQuery(new SQLTemplate(
                        SubEntity1.class,
                        "INSERT INTO INHERITANCE_SUB_ENTITY1 "
                                + "(ID, SUBENTITY_STRING_DB_ATTR, SUPER_INT_DB_ATTR, SUPER_STRING_DB_ATTR) "
                                + "VALUES (1, 'V11', 1, 'V21')"));

        inserts
                .addQuery(new SQLTemplate(
                        SubEntity1.class,
                        "INSERT INTO INHERITANCE_SUB_ENTITY2 "
                                + "(ID, OVERRIDDEN_STRING_DB_ATTR, SUBENTITY_INT_DB_ATTR, SUBENTITY_INT_DB_ATTR) "
                                + "VALUES (1, 'VX11', 101, '201')"));

        createDataContext().performGenericQuery(inserts);

        SQLTemplate unionSql = new SQLTemplate(
                SubEntity1.class,
                "SELECT ID, SUBENTITY_STRING_DB_ATTR, SUPER_STRING_DB_ATTR, SUPER_INT_DB_ATTR, NULL, 'INHERITANCE_SUB_ENTITY1'"
                        + " FROM INHERITANCE_SUB_ENTITY1"
                        + " UNION ALL"
                        + " SELECT ID, OVERRIDDEN_STRING_DB_ATTR, NULL, SUBENTITY_INT_DB_ATTR, SUBENTITY_INT_DB_ATTR, 'INHERITANCE_SUB_ENTITY2'"
                        + " FROM INHERITANCE_SUB_ENTITY2");

        unionSql.setFetchingDataRows(true);
        assertEquals(2, createDataContext().performQuery(unionSql).size());
    }
}
