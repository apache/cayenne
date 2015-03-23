package de.jexp.jequel.expression;

import de.jexp.jequel.Sql92Format;
import de.jexp.jequel.expression.types.INTEGER;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

public class TableTest {

    public static final Sql92Format VISITOR = new Sql92Format();


    private final MY_TABLE MY_TABLE = new MY_TABLE();

    public static class MY_TABLE extends Table<MY_TABLE> {
        public INTEGER id = integer().primaryKey();
        public Column<String> name = character(150);

        {
            initFields();
        }
    }

    @Test
    public void testNames() {
        assertEquals("MY_TABLE", MY_TABLE.accept(VISITOR));
        assertEquals("MY_TABLE.id", MY_TABLE.id.accept(VISITOR));
        assertEquals("MY_TABLE.name", MY_TABLE.name.accept(VISITOR));
    }

    @Test
    public void testColumnAlias() {
        assertEquals("MY_TABLE.name", MY_TABLE.name.accept(VISITOR));
        assertEquals("MY_TABLE.name as firstName", MY_TABLE.name.as("firstName").accept(VISITOR));
        assertEquals("alias should not change original column",
                "MY_TABLE.name", MY_TABLE.name.accept(VISITOR));
    }

    @Test
    public void testTableAlias() {
        MY_TABLE T1 = MY_TABLE.as("T1");
        assertNotEquals(T1, MY_TABLE);

        assertNotSame(T1.id, MY_TABLE.id);
        assertEquals(T1.id, MY_TABLE.id);

        assertNotSame(T1.name, MY_TABLE.name);
        assertEquals(T1.name, MY_TABLE.name);

        assertEquals("MY_TABLE as T1", T1.accept(VISITOR));
        assertEquals("T1.id", T1.id.accept(VISITOR));
        assertEquals("T1.name", T1.name.accept(VISITOR));

        assertEquals("MY_TABLE", MY_TABLE.accept(VISITOR));
        assertEquals("MY_TABLE.id", MY_TABLE.id.accept(VISITOR));
        assertEquals("MY_TABLE.name", MY_TABLE.name.accept(VISITOR));
    }
}
