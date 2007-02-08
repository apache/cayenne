package cayenne3t.example.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectstyle.cayenne.graph.GraphChangeHandler;

/**
 * This is an example of a GraphEventListener that logs received events.
 */
public class GraphChangeTrace implements GraphChangeHandler {

    Log logger;

    public GraphChangeTrace(String logLabel) {
        this.logger = LogFactory.getLog(logLabel);
    }
    
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        logger.info("Arc created: " + arcId);
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        logger.info("Arc deleted: " + arcId);
    }

    public void graphCommitted() {
        logger.info("Graph committed");
    }

    public void graphRolledback() {
        logger.info("Graph rolled back");
    }

    public void nodeCreated(Object nodeId) {
        logger.info("Node created: " + nodeId);
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        logger.info("Node id changed from " + nodeId + " to " + newId);
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        logger.info("Node property changed: " + property);
    }

    public void nodeRemoved(Object nodeId) {
        logger.info("Node removed: " + nodeId);
    }
}
