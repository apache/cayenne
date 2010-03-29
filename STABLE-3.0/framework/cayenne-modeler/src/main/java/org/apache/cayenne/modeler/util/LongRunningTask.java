/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 */
public abstract class LongRunningTask {

    private static Log logObj = LogFactory.getLog(LongRunningTask.class);

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
