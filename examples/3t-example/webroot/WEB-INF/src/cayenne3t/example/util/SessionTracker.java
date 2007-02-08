package cayenne3t.example.util;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.log4j.Logger;

/**
 * Tracks session allocation/deallocation.
 * 
 * @author andrus
 */
public class SessionTracker implements HttpSessionListener {

	final Logger logger = Logger.getLogger(getClass());

	public void sessionCreated(HttpSessionEvent e) {
		logger.info("**** HTTP Session created: " + e.getSession().getId());
	}

	public void sessionDestroyed(HttpSessionEvent e) {
		logger.info("**** HTTP Session destroyed: " + e.getSession().getId());
	}
}
