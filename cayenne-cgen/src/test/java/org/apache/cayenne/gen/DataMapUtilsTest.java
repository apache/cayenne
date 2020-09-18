package org.apache.cayenne.gen;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class DataMapUtilsTest {

    protected DataMapUtils dataMapUtils = null;
    protected ObjEntity objEntity = null;

    @Before
    public void setUp() {
        dataMapUtils = new DataMapUtils();
        objEntity = new ObjEntity();
    }

    @After
    public void tearDown() {
        dataMapUtils = null;
        objEntity = null;
    }

    @Test
    public void testGetParameterNamesWithFilledQueriesMap() {

        String param = "param";
        String qualifierString = "name = $" + param;

        SelectQueryDescriptor selectQueryDescriptor = new SelectQueryDescriptor();

        Set<String> result = new LinkedHashSet<>();
        assertEquals(result, dataMapUtils.getParameterNames(selectQueryDescriptor));

        Expression exp = ExpressionFactory.exp(qualifierString);
        selectQueryDescriptor.setQualifier(exp);
        selectQueryDescriptor.setName("name");

        Map<String, String> map = new HashMap<>();
        map.put(param, "java.lang.String");

        dataMapUtils.queriesMap.put("name", map);
        Collection collection = dataMapUtils.getParameterNames(selectQueryDescriptor);

        result.add(param);

        assertEquals(collection, result);
    }

    @Test
    public void testGetParameterNamesWithEmptyQueriesMap() {

        DbEntity dbEntity = new DbEntity("test");
        ObjAttribute attribute = new ObjAttribute("name");
        attribute.setDbAttributePath("testKey");
        attribute.setType("java.lang.String");
        objEntity.addAttribute(attribute);
        objEntity.setName("test");
        objEntity.setDbEntity(dbEntity);

        String param = "param";
        String qualifierString = "name = $" + param;

        SelectQueryDescriptor selectQueryDescriptor = new SelectQueryDescriptor();
        Expression exp = ExpressionFactory.exp(qualifierString);
        selectQueryDescriptor.setQualifier(exp);
        selectQueryDescriptor.setName("name");
        selectQueryDescriptor.setRoot(objEntity);

        Collection collection = dataMapUtils.getParameterNames(selectQueryDescriptor);

        Map<String, Map<String, String>> queriesMap = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put(param, "java.lang.String");
        queriesMap.put("name", map);

        assertEquals(dataMapUtils.queriesMap, queriesMap);

        Set<String> result = new LinkedHashSet<>();
        result.add(param);

        assertEquals(collection, result);
    }
}
