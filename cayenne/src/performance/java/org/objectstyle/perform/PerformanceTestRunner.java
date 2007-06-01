/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.perform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Andrei Adamchik
 */
public class PerformanceTestRunner {
	protected ResultRenderer renderer;
	protected Map resultCache = new HashMap();

	/**
	 * Constructor for PerformanceTestRunner.
	 */
	public PerformanceTestRunner(ResultRenderer renderer) {
		super();
		this.renderer = renderer;
	}

	public void runSuite(PerformanceTestSuite suite) {
		resultCache.clear();
		
		Iterator it = suite.getPairs().iterator();
		while (it.hasNext()) {
			PerformanceTestPair pair = (PerformanceTestPair) it.next();

			TestResult mainResult = runTest(pair.getMainTest());
			TestResult refResult = runTest(pair.getReferenceTest());

			renderer.addResult(pair, new PairResult(mainResult, refResult));
		}
	}

    /** 
     * Runs performance test. This implementation will cache test
     * results, so that the same test case is never run more than once.
     * This will allow to speed up testing by reusing reference test results.
     */
	public TestResult runTest(PerformanceTest test) {
		if (test == null) {
			return null;
		}

        // use cached result when possible
		TestResult result = (TestResult)resultCache.get(test.getClass());
		if(result != null) {
			return result;
		}
		
		result = new TestResult();

		try {
			test.prepare();
			long start = System.currentTimeMillis();
            test.runTest();
			long end = System.currentTimeMillis();
			result.setMs(end - start);

		} catch (Exception ex) {
			result.setTestEx(ex);
		} finally {
			try {
				test.cleanup();
			} catch (Exception ex) {
				result.setCleanupEx(ex);
			}
		}

        // cache result
        resultCache.put(test.getClass(), result);
        
		return result;
	}
}
