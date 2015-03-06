package de.jexp.jequel.transform;

import de.jexp.jequel.expression.Expressions;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.tables.TEST_TABLES;
import junit.framework.TestCase;

public class LineTransformerTest extends TestCase {
    private LineTransformer transformer;

    public void testTransformSelect() {
        assertEquals("Select( )", transformer.transformKeywords("SELECT "));
        assertEquals("(subSelect( )", transformer.transformKeywords("(select "));
        assertEquals("Select( a ).from( b ).where( c ).orderBy( d)", transformer.transformKeywords("select a from b where c order by d"));
    }

    public void testCleanUp() {
        assertEquals("", transformer.cleanUp("query.append("));
        assertEquals("", transformer.cleanUp(").append("));
        assertEquals("", transformer.cleanUp(").append(\""));
        assertEquals("", transformer.cleanUp("sql.append(\""));
        assertEquals("", transformer.cleanUp("\");"));
        assertEquals("", transformer.cleanUp(");"));
        assertEquals("abcdef", transformer.cleanUp("query.append(abc).append(\"def\");"));
    }

    public void testIgnore() {
        assertTrue(transformer.ignoreLine("if (a==1) {"));
    }

    public void testUnaryOperations() {
        assertEquals("abc sum(def", transformer.transformUnaryOperations("abc SUM(def"));
    }

    public void testBinaryOperations() {
        assertEquals("a .eq( b", transformer.transformBinaryOperations("a = b"));
    }

    public void testWhiteSpace() {
        assertEquals("test(abc", transformer.removeWhiteSpace("test( abc"));
        assertEquals("a b c d", transformer.removeWhiteSpace("a\n\tb  c d"));
    }

    public void testGetImports() {
        assertEquals(
                staticImport(Expressions.class) +
                staticImport(Sql.class) +
                staticImport(transformer.getSchemaClass()), transformer.getImports());
    }

    private String staticImport(final Class<?> type) {
        return "import static " + type.getName() + ".*;\n";
    }

    public void testUpcaseIdentfiers() {
        assertEquals("from abc where a.oid = def.a and ABC.OID = DEF.NAME ", transformer.upcaseIdentifiers("from abc where a.oid = def.a and abc.oid = def.name "));
    }

    public void testUpcaseIdentfiersSchema() {
        assertEquals("from ARTICLE_COLOR, ARTICLE where ARTICLE_COLOR.OID = ARTICLE.OID and a.OID = b.OID ",
                transformer.upcaseIdentifiersForSchema("from article_color, article where article_color.oid = article.oid and a.oid = b.oid "));
    }

    public void testAll() {
        String sql = "query.append(\"select a,b from c,d where\n\n c.a = b.a\t and c.a in(1,2,3)\n or d.d is not null   order by a\");";
        sql = transformer.transformLine(sql);
        assertEquals("Select( a,b ).from( c,d ).where( c.a .eq( b.a ).and( c.a .in(1,2,3)).or( d.d .isNot( null ).orderBy( a)", sql);
    }

    protected void setUp() throws Exception {
        super.setUp();
        transformer = new LineTransformer(TEST_TABLES.class,true);
    }
}
