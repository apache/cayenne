package org.apache.cayenne.map;

import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class ObjEntityTest {

    @Test
    public void testEncodeAsXML() {
        ObjEntity entity = new ObjEntity("X");
        entity.setClassName("org.example.Xc");

        // intentionally randomize attribute order .. must be saved in alphabetical order by name
        entity.addAttribute(new ObjAttribute("a2", "java.lang.String", entity));
        entity.addAttribute(new ObjAttribute("a1", "int", entity));
        entity.addAttribute(new ObjAttribute("a3", "long", entity));

        // relationships are saved outside the entity, so should be ignored in this test
        entity.addRelationship(new ObjRelationship("r1"));

        StringWriter out = new StringWriter();
        XMLEncoder encoder = new XMLEncoder(new PrintWriter(out));
        entity.encodeAsXML(encoder, new EncoderDummyVisitor());

        String ls = System.lineSeparator();

        assertEquals("<obj-entity name=\"X\" className=\"org.example.Xc\">" + ls +
                "<obj-attribute name=\"a1\" type=\"int\"/>" + ls +
                "<obj-attribute name=\"a2\" type=\"java.lang.String\"/>" + ls +
                "<obj-attribute name=\"a3\" type=\"long\"/>" + ls +
                "</obj-entity>" + ls, out.toString());
    }

    private class EncoderDummyVisitor extends BaseConfigurationNodeVisitor<Object> {
        @Override
        public Object visitObjEntity(ObjEntity entity) {
            return null;
        }

        @Override
        public Object visitObjAttribute(ObjAttribute attribute) {
            return null;
        }

        @Override
        public Object visitObjRelationship(ObjRelationship relationship) {
            return null;
        }
    }
}
