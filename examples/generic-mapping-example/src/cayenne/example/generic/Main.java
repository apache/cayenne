package cayenne.example.generic;

import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * An example launcher. Uses in-memory HSQLDB for all database operations.
 * 
 * @author Andrus Adamchik
 */
public class Main {

    DataContext context;
    DataMap orMapping;

    public static void main(String[] args) {

        Main example = new Main();

        // dynamically change mapping to add more attributes
        example.extendMapping();

        // create HSQLDB schema

        // in a real application a smarter approach to schema update may be needed, that
        // only loads new changes instead of rebuilding the full DB.
        example.generateDBSchema();

        // create/save/query some objects
        example.workWithGenericObjects();
    }

    Main() {
        context = DataContext.createDataContext();
        orMapping = context.getEntityResolver().getDataMap("generic-example-map");
    }

    /**
     * Dynamically add two new attributes to the "message" entity - "topic" and
     * "category".
     */
    void extendMapping() {
        DbEntity messageTable = orMapping.getDbEntity("message");

        DbAttribute topicColumn = new DbAttribute("topic", Types.VARCHAR, messageTable);
        topicColumn.setMaxLength(200);
        messageTable.addAttribute(topicColumn);

        DbAttribute categoryColumn = new DbAttribute(
                "category",
                Types.VARCHAR,
                messageTable);
        categoryColumn.setMaxLength(200);
        messageTable.addAttribute(categoryColumn);

        ObjEntity messageEntity = orMapping.getObjEntity("Message");

        ObjAttribute topicProperty = new ObjAttribute(
                "topic",
                String.class.getName(),
                messageEntity);
        topicProperty.setDbAttribute(topicColumn);
        messageEntity.addAttribute(topicProperty);

        ObjAttribute categoryProperty = new ObjAttribute("category", String.class
                .getName(), messageEntity);
        categoryProperty.setDbAttribute(categoryColumn);
        messageEntity.addAttribute(categoryProperty);
    }

    /**
     * Creates a DataMap on the fly, generating DB schema from it.
     */
    void generateDBSchema() {
        DataNode db = context.getParentDataDomain().lookupDataNode(orMapping);
        DbGenerator generator = new DbGenerator(db.getAdapter(), orMapping);

        try {
            generator.runGenerator(db.getDataSource());
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error generating schema", e);
        }
    }

    void workWithGenericObjects() {

        // 1. create and save a new author and a few messages....
        DataObject author = context.createAndRegisterNewObject("Person");
        author.writeProperty("firstName", "Joe");
        author.writeProperty("lastName", "Doe");

        DataObject firstPost = context.createAndRegisterNewObject("Message");
        firstPost.writeProperty("subject", "First POST!!");
        firstPost.writeProperty("text", "... read the subject ....");

        // just like with "normal" DataObjects, this call will set reverse relationship as
        // well
        firstPost.writeProperty("author", author);

        // now set post attributes that where generated dynamically on startup
        firstPost.writeProperty("topic", "off-topic");
        firstPost.writeProperty("category", "technical discussion");

        DataObject anotherPost = context.createAndRegisterNewObject("Message");
        anotherPost.writeProperty("subject", "Post On Topic");
        anotherPost.writeProperty("text", "bla-bla-bla");
        anotherPost.writeProperty("author", author);

        context.commitChanges();

        // 2. Query database. Note that entity name (not Class) must be used as query
        // root.

        Expression e = ExpressionFactory.likeIgnoreCaseExp("subject", "%first%");
        SelectQuery q = new SelectQuery("Message", e);
        List spam = context.performQuery(q);

        Iterator it = spam.iterator();
        while (it.hasNext()) {
            DataObject spamMessage = (DataObject) it.next();
            System.out.println("*** Got fp spam: "
                    + spamMessage.readProperty("subject")
                    + " by "
                    + spamMessage.readNestedProperty("author.lastName"));
        }
    }
}
