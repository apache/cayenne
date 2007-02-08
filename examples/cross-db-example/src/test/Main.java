package test;

import java.util.Iterator;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.query.SelectQuery;

import test.db1.Department;
import test.db2.Person;

public class Main {

    DataContext context;

    public static void main(String[] args) {
        Main m = new Main();
        try {
            m.initSchema("map1");
            m.initSchema("map2");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        m.createDepartmentWithPeople();

        m.invalidateAll();
        m.readToManyRelationship();

        m.invalidateAll();
        m.readToOneRelationship();
    }

    Main() {
        this.context = DataContext.createDataContext();
    }

    /**
     * Helper method that initializes schema on startup. As we are using in-memory HSQLDB,
     * we have to do it every time.
     */
    void initSchema(String mapName) throws Exception {
        DataMap map = context.getEntityResolver().getDataMap(mapName);
        DataNode node = context.getParentDataDomain().lookupDataNode(map);

        // last domain parameter is needed to avoid bogus cross-db FK generation
        DbGenerator g = new DbGenerator(node.getAdapter(), map, null, context
                .getParentDataDomain());

        g.runGenerator(node.getDataSource());
    }

    /**
     * Helper method that invalidates all cached objects to make sure the next operation
     * starts fresh from the database.
     */
    void invalidateAll() {
        context.invalidateObjects(context.getGraphManager().registeredNodes());
    }

    void createDepartmentWithPeople() {
        Department itDepartment = (Department) context
                .createAndRegisterNewObject(Department.class);
        itDepartment.setName("IT Department");

        Person joe = (Person) context.createAndRegisterNewObject(Person.class);
        joe.setFullName("Joe Doe");
        joe.setDepartment(itDepartment);

        // we can insert objects from different DBs in the same commit.
        context.commitChanges();
    }

    void readToManyRelationship() {
        Expression qualifier = ExpressionFactory.matchExp(
                Department.NAME_PROPERTY,
                "IT Department");
        SelectQuery q = new SelectQuery(Department.class, qualifier);
        Department itDepartment = (Department) context.performQuery(q).get(0);

        // reading people will result in an internal query from a different DB:
        Iterator it = itDepartment.getPeople().iterator();
        while (it.hasNext()) {
            Person p = (Person) it.next();
            System.out.println(p.getFullName() + " works in " + itDepartment.getName());
        }
    }

    void readToOneRelationship() {
        Expression qualifier = ExpressionFactory.matchExp(
                Person.FULL_NAME_PROPERTY,
                "Joe Doe");
        SelectQuery q = new SelectQuery(Person.class, qualifier);
        Person joe = (Person) context.performQuery(q).get(0);

        // reading department data will result in an internal query from a different DB:
        System.out.println(joe.getFullName()
                + " works in "
                + joe.getDepartment().getName());
    }
}
