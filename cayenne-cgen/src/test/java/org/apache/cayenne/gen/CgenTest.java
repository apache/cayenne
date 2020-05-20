package org.apache.cayenne.gen;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class CgenTest {

    protected ClassGenerationAction action;
    protected CgenConfiguration cgenConfiguration;

    protected DataMap dataMap;
    protected ObjEntity objEntity;

    @Before
    public void setUp() throws Exception {
        cgenConfiguration = new CgenConfiguration(false);
        action = new ClassGenerationAction(cgenConfiguration);

        dataMap = new DataMap();
        objEntity = new ObjEntity();
    }

    @After
    public void tearDown() throws Exception {
        dataMap = null;
        objEntity = null;
    }

    @Test
    public void testSelectQuery() throws Exception {

        String param = "param";
        String qualifierString = "name = $" + param;

        DbEntity dbEntity = new DbEntity();
        ObjAttribute attribute = new ObjAttribute("name");
        attribute.setDbAttributePath("testKey");
        attribute.setType("java.lang.String");
        objEntity.addAttribute(attribute);
        objEntity.setDbEntity(dbEntity);
        objEntity.setClassName("Test");

        SelectQueryDescriptor selectQueryDescriptor = new SelectQueryDescriptor();
        Expression exp = ExpressionFactory.exp(qualifierString);
        selectQueryDescriptor.setQualifier(exp);
        selectQueryDescriptor.setName("select");
        selectQueryDescriptor.setRoot(objEntity);

        dataMap.setName("DataMapTest");
        dataMap.setDefaultPackage("test");

        Collection<QueryDescriptor> descriptors = new ArrayList<>();
        descriptors.add(selectQueryDescriptor);

        DataMapArtifact dataMapArtifact = new DataMapArtifact(dataMap, descriptors);

        execute(dataMapArtifact);    }

    @Test
    public void testSQLTemplate() throws Exception {

        DbEntity dbEntity = new DbEntity();
        objEntity.setDbEntity(dbEntity);
        objEntity.setClassName("Test");

        SQLTemplateDescriptor sqlTemplateDescriptor = new SQLTemplateDescriptor();
        sqlTemplateDescriptor.setSql("SELECT * FROM table");
        sqlTemplateDescriptor.setRoot(objEntity);
        sqlTemplateDescriptor.setName("select");
        sqlTemplateDescriptor.setRoot(objEntity);

        dataMap.setName("SQLTemplate");
        dataMap.setDefaultPackage("test");

        Collection<QueryDescriptor> descriptors = new ArrayList<>();
        descriptors.add(sqlTemplateDescriptor);

        DataMapArtifact dataMapArtifact = new DataMapArtifact(dataMap, descriptors);

        execute(dataMapArtifact);
    }

    @Test
    public void testGenClass() throws Exception {

        dataMap.setName("EntityTest");

        DbEntity dbEntity = new DbEntity();
        dbEntity.setName("EntityTest");
        objEntity.setDbEntity(dbEntity);
        objEntity.setClassName("test.EntityTest");
        objEntity.setDataMap(dataMap);

        EntityArtifact entityArtifact = new EntityArtifact(objEntity);

        execute(entityArtifact);
    }

    public void execute(Artifact artifact) throws Exception{
        cgenConfiguration.addArtifact(artifact);

        cgenConfiguration.setRelPath("src/test/resources");
        cgenConfiguration.loadEntity(objEntity);
        cgenConfiguration.setDataMap(dataMap);

        action.setUtilsFactory(new DefaultToolsUtilsFactory());
        action.execute();

        fileComparison(dataMap.getName());

        fileComparison("auto/_" + dataMap.getName());

        rmdir(new File(cgenConfiguration.getRelPath() + "/test"));
    }

    public void fileComparison(String fileName) {

        try {
            FileReader fileReader1 = new FileReader(new File("src/test/resources/templateTest/" + fileName + ".java"));
            BufferedReader reader1 = new BufferedReader(fileReader1);
            String lineFile1;
            String string1 = "";

            FileReader fileReader2 = new FileReader(new File("src/test/resources/test/" + fileName + ".java"));
            BufferedReader reader2 = new BufferedReader(fileReader2);
            String lineFile2;
            String string2 = "";

            while ((lineFile1 = reader1.readLine()) != null
                    && (lineFile2 = reader2.readLine()) != null) {
                string1 += lineFile1;
                string2 += lineFile2;
            }

            assertEquals(string1, string2);

            reader1.close();
            reader2.close();
            fileReader1.close();
            fileReader2.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rmdir(File file) {
        if (!file.exists())
            return;

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                rmdir(f);
            }
        }
        file.delete();
    }
}
