package org.apache.cayenne.exp.parser;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.enum_test.Enum1;
import org.apache.cayenne.testdo.enum_test.EnumEntity;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@UseServerRuntime(CayenneProjects.ENUM_PROJECT)
public class ASTObjPathEnumIT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Test
    public void testInjectEnumByName() {
        ASTObjPath node = new ASTObjPath("enumAttribute");

        EnumEntity enumEntity = context.newObject(EnumEntity.class);
        assertNull(enumEntity.getEnumAttribute());

        node.injectValue(enumEntity, "one");
        assertEquals(Enum1.one, enumEntity.getEnumAttribute());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInjectUnknownEnumByName() {
        ASTObjPath node = new ASTObjPath("enumAttribute");

        EnumEntity enumEntity = context.newObject(EnumEntity.class);
        assertNull(enumEntity.getEnumAttribute());

        node.injectValue(enumEntity, "four");
    }
}
