/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.event;

import org.apache.cayenne.di.BeforeScopeEnd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A default implementation of {@link EventManager}.
 * 
 * @since 3.1
 */
public class DefaultEventManager implements EventManager {

    private static final int DEFAULT_DISPATCH_THREAD_COUNT = 5;

    // keeps weak references to subjects
    protected final Map<EventSubject, DispatchQueue> subjects;
    protected final List<Dispatch> eventQueue;
    protected final boolean singleThread;
    protected final List<DispatchThread> dispatchThreads;

    protected volatile boolean stopped;

    /**
     * Creates a multithreaded EventManager using default thread count.
     */
    public DefaultEventManager() {
        this(DEFAULT_DISPATCH_THREAD_COUNT);
    }

    /**
     * Creates an EventManager starting the specified number of threads for multithreaded
     * dispatching. To create a single-threaded EventManager, use thread count of zero or
     * less.
     */
    public DefaultEventManager(int dispatchThreadCount) {
        this.subjects = Collections.synchronizedMap(new WeakHashMap<>());
        this.eventQueue = Collections.synchronizedList(new LinkedList<>());
        this.singleThread = dispatchThreadCount <= 0;

        if (!singleThread) {
            dispatchThreads = new ArrayList<>(dispatchThreadCount);

            String prefix = "cayenne-event-";

            // start dispatch threads
            for (int i = 0; i < dispatchThreadCount; i++) {
                DispatchThread thread = new DispatchThread(prefix + i);
                dispatchThreads.add(thread);
                thread.start();
            }
        } else {
            dispatchThreads = Collections.emptyList();
        }
    }

    /**
     * Returns true if the EventManager was stopped via {@link #shutdown()} method.
     * 
     * @since 3.1
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * Returns true if this EventManager is single-threaded. If so it will throw an
     * exception on any attempt to register an unblocking listener or dispatch a
     * non-blocking event.
     * 
     * @since 1.2
     */
    public boolean isSingleThreaded() {
        return singleThread;
    }

    /**
     * Stops event threads. After the EventManager is stopped, it can not be restarted and
     * should be discarded.
     * 
     * @since 3.0
     */
    @BeforeScopeEnd
    public void shutdown() {

        if (!stopped) {

            this.stopped = true;

            for (DispatchThread thread : dispatchThreads) {
                thread.interrupt();
            }

            dispatchThreads.clear();
        }
    }

    @Override
    public <L, E extends EventObject> void addListener(
            L listener,
            Class<E> eventClass,
            EventHandler<? super L, ? super E> handler,
            EventSubject subject) {
        addListener(listener, eventClass, handler, subject, null, true);
    }

    @Override
    public <L, E extends EventObject> void addNonBlockingListener(
            L listener,
            Class<E> eventClass,
            EventHandler<? super L, ? super E> handler,
            EventSubject subject) {

        if (singleThread) {
            throw new IllegalStateException("DefaultEventManager is configured to be single-threaded.");
        }

        addListener(listener, eventClass, handler, subject, null, false);
    }

    @Override
    public <L, E extends EventObject> void addListener(
            L listener,
            Class<E> eventClass,
            EventHandler<? super L, ? super E> handler,
            EventSubject subject,
            Object sender) {
        addListener(listener, eventClass, handler, subject, sender, true);
    }

    @Override
    public <L, E extends EventObject> void addNonBlockingListener(
            L listener,
            Class<E> eventClass,
            EventHandler<? super L, ? super E> handler,
            EventSubject subject,
            Object sender) {

        if (singleThread) {
            throw new IllegalStateException("DefaultEventManager is configured to be single-threaded.");
        }

        addListener(listener, eventClass, handler, subject, sender, false);
    }

    protected <L, E extends EventObject> void addListener(
            L listener,
            Class<E> eventClass,
            EventHandler<? super L, ? super E> handler,
            EventSubject subject,
            Object sender,
            boolean blocking) {

        if (listener == null) {
            throw new IllegalArgumentException("Listener must not be null.");
        }

        if (eventClass == null) {
            throw new IllegalArgumentException("Event class must not be null.");
        }

        if (handler == null) {
            throw new IllegalArgumentException("Handler must not be null.");
        }

        if (subject == null) {
            throw new IllegalArgumentException("Subject must not be null.");
        }

        ListenerRegistration<L, E> registration = new ListenerRegistration<>(listener, eventClass, handler, blocking);
        dispatchQueueForSubject(subject, true).addRegistration(registration, sender);
    }

    /**
     * Unregister the specified listener from all event subjects handled by this manager
     * instance.
     * 
     * @param listener the object to be unregistered
     * @return <code>true</code> if <code>listener</code> could be removed for any
     *         existing subjects, else returns <code>false</code>.
     */
    public boolean removeListener(Object listener) {
        if (listener == null) {
            return false;
        }

        boolean didRemove = false;

        synchronized (subjects) {
            if (!subjects.isEmpty()) {
                for (EventSubject subject : subjects.keySet()) {
                    didRemove |= this.removeListener(listener, subject);
                }
            }
        }

        return didRemove;
    }

    /**
     * Removes all listeners for a given subject.
     */
    public boolean removeAllListeners(EventSubject subject) {
        if (subject != null) {
            synchronized (subjects) {
                return subjects.remove(subject) != null;
            }
        }

        return false;
    }

    /**
     * Unregister the specified listener for the events about the given subject.
     * 
     * @param listener the object to be unregistered
     * @param subject the subject from which the listener is to be unregistered
     * @return <code>true</code> if <code>listener</code> could be removed for the given
     *         subject, else returns <code>false</code>.
     */
    public boolean removeListener(Object listener, EventSubject subject) {
        return this.removeListener(listener, subject, null);
    }

    /**
     * Unregister the specified listener for the events about the given subject and the
     * given sender.
     * 
     * @param listener the object to be unregistered
     * @param subject the subject from which the listener is to be unregistered
     * @param sender the object whose events the listener was interested in;
     *            <code>null</code> means 'any sender'.
     * @return <code>true</code> if <code>listener</code> could be removed for the given
     *         subject, else returns <code>false</code>.
     */
    public boolean removeListener(Object listener, EventSubject subject, Object sender) {
        if (listener == null || subject == null) {
            return false;
        }

        DispatchQueue subjectQueue = dispatchQueueForSubject(subject, false);
        if (subjectQueue == null) {
            return false;
        }

        return subjectQueue.removeRegistrations(listener, sender);
    }

    /**
     * Sends an event to all registered objects about a particular subject. Event is sent
     * synchronously, so the sender thread is blocked until all the listeners finish
     * processing the event.
     * 
     * @param event the event to be posted to the observers
     * @param subject the subject about which observers will be notified
     * @throws IllegalArgumentException if event or subject are null
     */
    public void postEvent(EventObject event, EventSubject subject) {
        dispatchEvent(new Dispatch(event, subject));
    }

    /**
     * Sends an event to all registered objects about a particular subject. Event is
     * queued by EventManager, releasing the sender thread, and is later dispatched in a
     * separate thread.
     * 
     * @param event the event to be posted to the observers
     * @param subject the subject about which observers will be notified
     * @throws IllegalArgumentException if event or subject are null
     * @since 1.1
     */
    public void postNonBlockingEvent(EventObject event, EventSubject subject) {
        if (singleThread) {
            throw new IllegalStateException("EventManager is configured to be single-threaded.");
        }

        // add dispatch to the queue and return
        synchronized (eventQueue) {
            eventQueue.add(new Dispatch(event, subject));
            eventQueue.notifyAll();
        }
    }

    private void dispatchEvent(Dispatch dispatch) {
        DispatchQueue dispatchQueue = dispatchQueueForSubject(dispatch.subject, false);
        if (dispatchQueue != null) {
            dispatchQueue.dispatchEvent(dispatch);
        }
    }

    // returns a subject's mapping from senders to registered listener invocations
    private DispatchQueue dispatchQueueForSubject(EventSubject subject, boolean create) {
        synchronized (subjects) {
            DispatchQueue listenersStore = subjects.get(subject);
            if (create && listenersStore == null) {
                listenersStore = new DispatchQueue();
                subjects.put(subject, listenersStore);
            }
            return listenersStore;
        }
    }

    // represents a posted event
    class Dispatch {

        EventObject event;
        EventSubject subject;

        Dispatch(EventObject event, EventSubject subject) {
            this.event = event;
            this.subject = subject;
        }

        Object getSender() {
            return event.getSource();
        }

        void fire() {
            DefaultEventManager.this.dispatchEvent(Dispatch.this);
        }

        // returns false if the registration is stale and should be discarded
        boolean fire(ListenerRegistration<?, ?> registration) {
            if (registration.isBlocking()) {
                return registration.fire(event);
            }

            // do minimal checks first...
            if (registration.getListener() == null) {
                return false;
            }

            // inject single listener dispatch into the queue
            synchronized (eventQueue) {
                eventQueue.add(new ListenerDispatch(event, subject, registration));
                eventQueue.notifyAll();
            }

            return true;
        }
    }

    // represents a posted event that should be sent to a single known listener
    class ListenerDispatch extends Dispatch {

        ListenerRegistration<?, ?> target;

        ListenerDispatch(EventObject event, EventSubject subject, ListenerRegistration<?, ?> target) {
            super(event, subject);
            this.target = target;
        }

        @Override
        void fire() {
            // there is no way to kill the registration if it is stale...
            // so don't check for status
            target.fire(event);
        }
    }

    final class DispatchThread extends Thread {

        DispatchThread(String name) {
            super(name);
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!stopped) {

                // get event from the queue, if the queue is empty, just wait
                Dispatch dispatch = null;

                synchronized (DefaultEventManager.this.eventQueue) {
                    if (DefaultEventManager.this.eventQueue.size() > 0) {
                        dispatch = DefaultEventManager.this.eventQueue.remove(0);
                    } else {
                        try {
                            // wake up occasionally to check whether EM has been stopped
                            DefaultEventManager.this.eventQueue.wait(3 * 60 * 1000);
                        } catch (InterruptedException e) {
                            // ignore interrupts...
                        }
                    }
                }

                // dispatch outside of synchronized block
                if (!stopped && dispatch != null) {
                    // this try/catch is needed to prevent DispatchThread
                    // from dying on dispatch errors
                    try {
                        dispatch.fire();
                    } catch (Throwable th) {
                        // ignoring exception
                    }
                }
            }
        }
    }
}
