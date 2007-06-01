package org.objectstyle.perform;

/**
 * @author Andrei Adamchik
 */
public class PairResult {
	protected TestResult mainResult;
	protected TestResult refResult;

	/**
	 * Constructor for PairResult.
	 */
	public PairResult(TestResult mainResult, TestResult refResult) {
		super();
		this.mainResult = mainResult;
		this.refResult = refResult;
	}

	public TestResult getMainResult() {
		return mainResult;
	}

	public TestResult getRefResult() {
		return refResult;
	}

	public double compareSpeed() {
		if (refResult == null
			|| refResult.getMs() <= 0
			|| mainResult.getMs() <= 0) {
			return -1;
		}
		
		return ((double)mainResult.getMs()) / ((double)refResult.getMs());
	}
}
