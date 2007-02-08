package cayenne3t.example.client;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.objectstyle.cayenne.CayenneContext;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.query.NamedQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryChain;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.remote.ClientChannel;
import org.objectstyle.cayenne.remote.ClientConnection;
import org.objectstyle.cayenne.remote.hessian.HessianConnection;
import org.objectstyle.cayenne.util.EventUtil;

import cayenne3t.example.hr.CDepartment;
import cayenne3t.example.hr.CPerson;

/**
 * @author Andrus Adamchik
 */
public class Main {

	EventTrace channelTrace;
	ObjectContext context;

	public static void main(String[] args) {
		try {
			new Main().execute();
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}

	Main() {
		// init Log4J .. isn't strictly needed as Cayenne Client uses
		// commons-logging, but
		// helpful when run from Eclipse.
		BasicConfigurator.configure();

		// obtain object context
		ClientConnection connection = new HessianConnection(
				"http://localhost:8080/3t-example/cayenne", null, null,
				"shared-session");

		// enable channel events
		DataChannel channel = new ClientChannel(connection, true);

		// trace ALL context local and remote events ... must keep references to
		// listeners
		// to avoid deallocation
		this.channelTrace = new EventTrace("channel-event");
		EventUtil.listenForChannelEvents(channel.getEventManager(),
				channelTrace);

		// enable graph events
		context = new CayenneContext(channel, true, true);
	}

	void execute() {
		Log logger = LogFactory.getLog("3t-client-demo");
		logger.info("*** 1. Setup data: ");

		// batch a few queries in a single call...
		Query[] deletes = new Query[2];
		deletes[0] = new NamedQuery("DeletePerson");
		deletes[1] = new NamedQuery("DeleteDepartment");

		QueryResponse deleteResponse = context
				.performGenericQuery(new QueryChain(deletes));

		for (deleteResponse.reset(); deleteResponse.next();) {
			if (!deleteResponse.isList()) {
				int[] counts = deleteResponse.currentUpdateCount();
				for (int i = 0; i < counts.length; i++) {
					logger.info("   deleted = " + counts[i]);
				}
			}
		}

		// do a single named insert query...
		Query insert = new NamedQuery("CreateData");
		QueryResponse insertResponse = context.performGenericQuery(insert);
		for (insertResponse.reset(); insertResponse.next();) {
			if (!insertResponse.isList()) {
				int[] counts = insertResponse.currentUpdateCount();
				for (int i = 0; i < counts.length; i++) {
					logger.info("   inserted = " + counts[i]);
				}
			}
		}

		logger.info("=======================================\n\n ");
		logger.info("*** 2. Select: ");
		List results = context
				.performQuery(new NamedQuery("DepartmentWithName"));
		logger.info("   select results: " + results);

		CDepartment department = (CDepartment) results.get(0);
		department.setDescription(department.getDescription() + "_");

		logger.info("=======================================\n\n ");
		logger.info("*** 3. Commit modified: ");
		context.commitChanges();
		logger.info("   department: " + department);

		logger.info("=======================================\n\n ");
		logger.info("*** 4. Commit New Object: ");
		CPerson person = (CPerson) context.newObject(CPerson.class);
		person.setBaseSalary(new Double(23000.00));
		person.setDateHired(new Date());
		person.setFullName("Test Person");

		context.commitChanges();

		logger.info("   person id: " + person.getObjectId());
		logger.info("   person: " + person);

		logger.info("=======================================\n\n ");
		logger.info("*** 5. Setup relationship: ");

		person.setDepartment(department);

		CPerson anotherPerson = (CPerson) context.newObject(CPerson.class);
		anotherPerson.setBaseSalary(new Double(88000.00));
		anotherPerson.setDateHired(new Date());
		anotherPerson.setFullName("Another Test Person");
		department.addToEmployees(anotherPerson);

		context.commitChanges();

		CPerson yetAnotherPerson = (CPerson) context.newObject(CPerson.class);
		yetAnotherPerson.setBaseSalary(new Double(1000000.00));
		yetAnotherPerson.setDateHired(new Date());
		yetAnotherPerson.setFullName("Yet Another Test Person");
		department.addToEmployees(yetAnotherPerson);

		context.commitChanges();

		logger.info("=======================================\n\n ");
		logger.info("*** 6. Delete relationship: ");

		department.removeFromEmployees(anotherPerson);
		yetAnotherPerson.setDepartment(null);
		context.commitChanges();
		logger.info(" employees: " + department.getEmployees());

		logger.info("=======================================\n\n ");
		logger.info("*** 7. Rollback: ");

		department.setName("xyz");
		CPerson rolledbackPerson = (CPerson) context.newObject(CPerson.class);
		department.addToEmployees(rolledbackPerson);

		context.rollbackChanges();
		logger.info("***  " + department);
		logger.info("***  " + rolledbackPerson);

		logger.info("=======================================\n\n ");
		logger.info("*** 8. Arbitrary Select: ");

		// select using entity with qualifier
		SelectQuery select1 = new SelectQuery("Person", Expression
				.fromString("fullName like '%Another%'"));
		List matches1 = context.performQuery(select1);
		logger.info("results: " + matches1);

		// same select using client class name
		SelectQuery select2 = new SelectQuery(CPerson.class, Expression
				.fromString("fullName like '%Another%'"));
		List matches2 = context.performQuery(select2);
		logger.info("results: " + matches2);

		logger.info("=======================================\n\n ");
		logger.info("*** 9. Select with Prefetches: ");
		SelectQuery select3 = new SelectQuery(CPerson.class, Expression
				.fromString("fullName like '%Tes%'"));
		select3.addPrefetch("department");
		List matches3 = context.performQuery(select3);
		Iterator it3 = matches3.iterator();
		while (it3.hasNext()) {
			CPerson p = (CPerson) it3.next();
			logger.info("*** result: " + p + ", prefetched: "
					+ p.getDepartment());
		}
		
		logger.info("=======================================\n\n ");
		logger.info("*** 10. Paginated Select: ");
		SelectQuery select4 = new SelectQuery(CPerson.class);
		select4.setPageSize(2);
		
		List matches4 = context.performQuery(select4);
		Iterator it4 = matches4.iterator();
		while (it4.hasNext()) {
			CPerson p = (CPerson) it4.next();
			logger.info("*** result: " + p);
		}
	}
}