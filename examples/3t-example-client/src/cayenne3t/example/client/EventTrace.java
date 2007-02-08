package cayenne3t.example.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectstyle.cayenne.DataChannelListener;
import org.objectstyle.cayenne.graph.GraphEvent;

public class EventTrace implements DataChannelListener {

	Log logger;

	GraphChangeTrace graphChangeTrace;

	public EventTrace(String logLabel) {
		this.logger = LogFactory.getLog(logLabel);
		this.graphChangeTrace = new GraphChangeTrace(logLabel
				+ " [graph change] ");
	}

	public void graphChanged(GraphEvent event) {
		logger.info("*** Graph Changed");
		processGraphDiff(event);
	}

	public void graphFlushed(GraphEvent event) {
		logger.info("*** Graph Committed:" + event.getSource());
		processGraphDiff(event);
	}

	public void graphRolledback(GraphEvent event) {
		logger.info("*** Graph Rolledback");
		processGraphDiff(event);
	}

	void processGraphDiff(GraphEvent e) {
		if (e.getDiff() != null) {
			e.getDiff().apply(graphChangeTrace);
		}
	}
}
