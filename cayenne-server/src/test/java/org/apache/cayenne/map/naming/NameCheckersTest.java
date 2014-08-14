package org.apache.cayenne.map.naming;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.*;
import org.apache.cayenne.query.SelectQuery;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class NameCheckersTest {

    @Test
    public void testObjEntityAttributes() throws Exception {
        NameCheckers maker = NameCheckers.ObjAttribute;
        ObjEntity namingContainer = new ObjEntity();

        String baseName = maker.baseName();
        String name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName, name);
        namingContainer.addAttribute(new ObjAttribute(name));

        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName + "1", name);
        namingContainer.addAttribute(new ObjAttribute(name));

        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName + "2", name);
        namingContainer.addAttribute(new ObjAttribute(name));

        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName + "3", name);
        namingContainer.addAttribute(new ObjAttribute(name));

        maker = NameCheckers.ObjRelationship;
        baseName = maker.baseName();
        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName, name);
        namingContainer.addRelationship(new ObjRelationship(name));

        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName + "1", name);
        namingContainer.addRelationship(new ObjRelationship(name));

        maker = NameCheckers.ObjCallbackMethod;
        baseName = maker.baseName();
        name = DefaultUniqueNameGenerator.generate(maker, namingContainer);
        assertEquals(baseName, name);
        namingContainer.addRelationship(new ObjRelationship(name));
    }

    @Test
    public void testEntity () {
        DataMap map = new DataMap();

        map.addDbEntity(new DbEntity("name"));
        checkNameAndOther(map, NameCheckers.DbEntity, "name");

        map.addObjEntity(new ObjEntity("name"));
        checkNameAndOther(map, NameCheckers.ObjEntity, "name");

        map.addProcedure(new Procedure("name"));
        checkNameAndOther(map, NameCheckers.Procedure, "name");

        SelectQuery query = new SelectQuery("name");
        query.setName("name");
        map.addQuery(query);
        checkNameAndOther(map, NameCheckers.SelectQuery, "name");
    }

    @Test
    public void testProject() throws Exception {
        assertFalse(NameCheckers.DataChannelDescriptor.isNameInUse(null, null));
    }

    @Test
    public void testDbEntity() throws Exception {
        DbEntity dbEntity = new DbEntity();

        dbEntity.addRelationship(new DbRelationship("name"));
        checkNameAndOther(dbEntity, NameCheckers.DbRelationship, "name");
    }

    @Test
    public void testProcedureAttr() throws Exception {
        Procedure procedure = new Procedure();

        procedure.addCallParameter(new ProcedureParameter("name"));
        checkNameAndOther(procedure, NameCheckers.ProcedureParameter, "name");
    }

    @Test
    public void testEmbeddableAttr() throws Exception {
        Embeddable embeddable = new Embeddable();

        embeddable.addAttribute(new EmbeddableAttribute("name"));
        checkNameAndOther(embeddable, NameCheckers.EmbeddableAttribute, "name");
    }

    @Test
    public void testDatanode() throws Exception {
        DataChannelDescriptor descriptor = new DataChannelDescriptor();

        descriptor.getDataMaps().add(new DataMap("name"));
        checkNameAndOther(descriptor, NameCheckers.DataMap, "name");

        descriptor.getNodeDescriptors().add(new DataNodeDescriptor("name"));
        checkNameAndOther(descriptor, NameCheckers.DataNodeDescriptor, "name");
    }

    @Test
    public void testDataMap() throws Exception {
        DataDomain dataDomain = new DataDomain("name");

        dataDomain.addDataMap(new DataMap("name"));
        checkNameAndOther(dataDomain, NameCheckers.DataMap, "name");

        assertFalse(NameCheckers.DataMap.isNameInUse(null, "name"));
        assertFalse(NameCheckers.DataMap.isNameInUse(1, "name"));
    }

    private void checkNameAndOther(Object namingContainer, NameCheckers maker, String newName) {
        assertTrue(maker.isNameInUse(namingContainer, newName));
        assertEquals(newName + "1", DefaultUniqueNameGenerator.generate(maker,namingContainer, newName));
        assertEquals("other" + newName, DefaultUniqueNameGenerator.generate(maker,namingContainer, "other" + newName));
    }

    @Test
    public void testOverlappingAttributeAndCallbackNames() throws Exception {
        ObjEntity namingContainer = new ObjEntity();

        namingContainer.addAttribute(new ObjAttribute("myName"));
        Assert.assertEquals("getMyName1", DefaultUniqueNameGenerator.generate(NameCheckers.ObjCallbackMethod, namingContainer, "getMyName"));

        namingContainer.getCallbackMap().getPostAdd().addCallbackMethod("getSecondName");
        Assert.assertEquals("SecondName1", DefaultUniqueNameGenerator.generate(NameCheckers.ObjAttribute, namingContainer, "SecondName"));
        Assert.assertEquals("secondName1", DefaultUniqueNameGenerator.generate(NameCheckers.ObjAttribute, namingContainer, "secondName"));
        Assert.assertEquals("SecondName1", DefaultUniqueNameGenerator.generate(NameCheckers.ObjRelationship, namingContainer, "SecondName"));
        Assert.assertEquals("secondName1", DefaultUniqueNameGenerator.generate(NameCheckers.ObjRelationship, namingContainer, "secondName"));
    }

    @Test
    public void testAttributeDifferentInFirstLetterCases() throws Exception {
        ObjEntity namingContainer = new ObjEntity();

        namingContainer.addAttribute(new ObjAttribute("myName"));
        Assert.assertTrue(NameCheckers.ObjAttribute.isNameInUse(namingContainer, "myName"));
        Assert.assertFalse(NameCheckers.ObjAttribute.isNameInUse(namingContainer, "MyName"));

        namingContainer.getCallbackMap().getPostAdd().addCallbackMethod("getSecondName");
        Assert.assertEquals("SecondName1", DefaultUniqueNameGenerator.generate(NameCheckers.ObjAttribute, namingContainer, "SecondName"));
        Assert.assertEquals("secondName1", DefaultUniqueNameGenerator.generate(NameCheckers.ObjAttribute, namingContainer, "secondName"));
    }

    @Test
    public void testEmbeddable() {
        DataMap map = new DataMap();

        map.addEmbeddable(new Embeddable("name"));
        Assert.assertTrue(NameCheckers.Embeddable.isNameInUse(map, "name"));
        Assert.assertEquals("name1", DefaultUniqueNameGenerator.generate(NameCheckers.Embeddable, map, "name"));
        Assert.assertFalse(NameCheckers.Embeddable.isNameInUse(map, "other-name"));

        map.setDefaultPackage("package");
        Assert.assertFalse(NameCheckers.Embeddable.isNameInUse(map, "name"));
        Assert.assertEquals("package.name", DefaultUniqueNameGenerator.generate(NameCheckers.Embeddable, map, "name"));
        map.addEmbeddable(new Embeddable("package.name"));

        Assert.assertTrue(NameCheckers.Embeddable.isNameInUse(map, "name"));
        Assert.assertEquals("package.name1", DefaultUniqueNameGenerator.generate(NameCheckers.Embeddable, map, "name"));
        Assert.assertFalse(NameCheckers.Embeddable.isNameInUse(map, "other-name"));
    }
}