package cayenne3t.example.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This is a sample filter that allows to intercept and customize requests sent
 * to Cayenne RemoteService. This implementation simply dumps request and
 * response information.
 * 
 * @author andrus
 */
public class RequestFilter implements Filter {

	static final String LOG4J_CONFIG = "log4j-config";

	final Logger logger = Logger.getLogger(getClass());

	public void init(FilterConfig config) throws ServletException {

		// setup logging..
		String logFile = config.getInitParameter(LOG4J_CONFIG);
		if (logFile != null) {

			try {
				URL loggingURL = config.getServletContext()
						.getResource(logFile);

				if (loggingURL != null) {
					PropertyConfigurator.configure(loggingURL);
				} else {
					config.getServletContext().log(
							"Failed to set up logging - can't find Log4J configuration '"
									+ logFile + "', ignoring.");
				}
			} catch (MalformedURLException ex) {
				config.getServletContext().log(
						"Error setting up logging, ignoring: "
								+ ex.getMessage());
			}
		}
	}

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		logger.debug("[Request] start....");
		dumpRequest((HttpServletRequest) request);
		
		long t0 = System.currentTimeMillis();
		

		chain.doFilter(request, response);
		
		long t1 = System.currentTimeMillis();

		logger.debug("[Request] end ... " + (t1 - t0) + " ms.");
	}

	private void dumpRequest(HttpServletRequest request) {

		logger.debug("[Request] URL: " + request.getRequestURL());

		Cookie[] cookies = request.getCookies();
		if (cookies != null && cookies.length > 0) {
			for (int i = 0; i < cookies.length; i++) {
				logger.debug("[Request] Cookie: " + cookies[i].getName() + ":" + cookies[i].getValue());
			}
		} else {
			logger.debug("[Request] No cookies");
		}
	}
}
