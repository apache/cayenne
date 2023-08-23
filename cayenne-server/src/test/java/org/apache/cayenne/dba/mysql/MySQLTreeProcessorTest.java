package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.sqlbuilder.BaseSqlBuilderTest;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

public class MySQLTreeProcessorTest extends BaseSqlBuilderTest {

    private Node sqlNode;

    @Before
    public void generateSql() {
        sqlNode = select(column("*"))
                .from(table("test"))
                .where(() -> {
                    Node node = new LikeNode(false, false, (char) 0);
                    node.addChild(column("column").build());
                    node.addChild(value("abc").build());
                    return node;
                })
                .build();
        assertSQL("SELECT * FROM test WHERE column LIKE 'abc'", sqlNode);
    }

    @Test
    public void testLikeCI() {
        MySQLTreeProcessor instance = MySQLTreeProcessor.getInstance(true);
        Node processed = instance.process(sqlNode);
        assertSQL("SELECT * FROM test WHERE column LIKE BINARY 'abc'", processed);
    }

    @Test
    public void testLikeCS() {
        MySQLTreeProcessor instance = MySQLTreeProcessor.getInstance(false);
        Node processed = instance.process(sqlNode);
        assertSQL("SELECT * FROM test WHERE column LIKE 'abc'", processed);
    }
}