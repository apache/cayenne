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
package org.objectstyle.cayenne.modeler.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * A base class for monitoring progress of long running tasks. It can runshowing the exact
 * percentage of the task progress or in "indeterminate" mode.
 * <p>
 * <i>Warning: If the task started via "startAndWait()", caller must ensure that she is
 * not running in the Swing EventDispatchThread, otherwise an exception is thrown, as the
 * EvenDispatchThread will be blocked, preventing LongRunningTask from showing progress
 * dialog. </i>
 * </p>
 * 
 * @author Andrei Adamchik
 */
public abstract class LongRunningTask {

    private static final Logger logObj = Logger.getLogger(LongRunningTask.class);

    protected static final int DEFAULT_MS_TO_DECIDE_TO_POPUP = 500;

    protected ProgressDialog dialog;
    protected JFrame frame;
    protected String title;
    protected Timer taskPollingTimer;
    protected boolean canceled;
    protected int minValue;
    protected int maxValue;
    protected boolean finished;

    public LongRunningTask(JFrame frame, String title) {
        this.frame = frame;
        this.title = title;
    }

    /**
     * Starts current task, and blocks current thread until the task is done.
     */
    public synchronized void startAndWait() {
        // running from Event Dispatch Thread is bad, as this will block the timers...
        if (SwingUtilities.isEventDispatchThread()) {
            throw new CayenneRuntimeException(
                    "Can't block EventDispatchThread. Call 'startAndWait' from another thread.");
        }

        start();

        if (finished) {
            return;
        }

        try {
            wait();
        }
        catch (InterruptedException e) {
            setCanceled(true);
        }

        notifyAll();
    }

    /**
     * Configures the task to run in a separate thread, and immediately exits the method.
     * This method is allowed to be invoked from EventDispatchThread.
     */
    public void start() {
        // prepare...
        setCanceled(false);
        this.finished = false;

        Thread task = new Thread(new Runnable() {

            public void run() {
                internalExecute();
            }
        });

        Timer progressDisplayTimer = new Timer(
                DEFAULT_MS_TO_DECIDE_TO_POPUP,
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        showProgress();
                    }
                });

        progressDisplayTimer.setRepeats(false);

        // start
        progressDisplayTimer.start();
        task.start();
    }

    /**
     * Starts progress dialog if the task is not finished yet.
     */
    protected synchronized void showProgress() {
        logObj.debug("will show progress...");

        if (finished) {
            return;
        }

        int currentValue = getCurrentValue();

        if (!isCanceled() && currentValue < getMaxValue()) {

            logObj.debug("task still in progress, will show progress dialog...");
            this.dialog = new ProgressDialog(frame, "Progress...", title);
            this.dialog.getCancelButton().addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setCanceled(true);
                }
            });

            dialog.getProgressBar().setMinimum(getMinValue());
            dialog.getProgressBar().setMaximum(getMaxValue());
            updateProgress();

            this.taskPollingTimer = new Timer(500, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateProgress();
                }
            });

            this.taskPollingTimer.start();
            this.dialog.setVisible(true);
        }
    }

    /**
     * Updates current state of the progress dialog.
     */
    protected void updateProgress() {
        if (isCanceled()) {
            stop();
            return;
        }

        dialog.getStatusLabel().setText(getCurrentNote());

        JProgressBar progressBar = dialog.getProgressBar();
        if (!isIndeterminate()) {
            progressBar.setValue(getCurrentValue());
            progressBar.setIndeterminate(false);
        }
        else {
            progressBar.setIndeterminate(true);
        }
    }

    protected synchronized void stop() {
        if (taskPollingTimer != null) {
            taskPollingTimer.stop();
        }

        if (dialog != null) {
            dialog.dispose();
        }

        // there maybe other threads waiting on this task to finish...
        finished = true;
        notifyAll();
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean b) {
        if (b) {
            logObj.debug("task canceled");
        }

        this.canceled = b;
    }

    protected void internalExecute() {
        try {
            execute();
        }
        catch (Throwable th) {
            setCanceled(true);
            logObj.warn("task error", th);
        }
        finally {
            stop();
        }
    }

    /**
     * Runs the actual task, consulting "isCanceled()" state to make sure that the task
     * wasn't canceled.
     */
    protected abstract void execute();

    protected abstract String getCurrentNote();

    protected abstract int getCurrentValue();

    protected abstract boolean isIndeterminate();

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }
}