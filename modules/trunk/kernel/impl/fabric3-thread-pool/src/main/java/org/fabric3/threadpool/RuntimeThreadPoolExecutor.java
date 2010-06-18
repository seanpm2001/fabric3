/*
* Fabric3
* Copyright (c) 2009 Metaform Systems
*
* Fabric3 is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as
* published by the Free Software Foundation, either version 3 of
* the License, or (at your option) any later version, with the
* following exception:
*
* Linking this software statically or dynamically with other
* modules is making a combined work based on this software.
* Thus, the terms and conditions of the GNU General Public
* License cover the whole combination.
*
* As a special exception, the copyright holders of this software
* give you permission to link this software with independent
* modules to produce an executable, regardless of the license
* terms of these independent modules, and to copy and distribute
* the resulting executable under terms of your choice, provided
* that you also meet, for each linked independent module, the
* terms and conditions of the license of that module. An
* independent module is a module which is not derived from or
* based on this software. If you modify this software, you may
* extend this exception to your version of the software, but
* you are not obligated to do so. If you do not wish to do so,
* delete this exception statement from your version.
*
* Fabric3 is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty
* of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the
* GNU General Public License along with Fabric3.
* If not, see <http://www.gnu.org/licenses/>.
*/
package org.fabric3.threadpool;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.osoa.sca.annotations.Destroy;
import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Init;
import org.osoa.sca.annotations.Property;
import org.osoa.sca.annotations.Service;

import org.fabric3.api.annotation.monitor.Monitor;

/**
 * Processes work using a delegate {@link ThreadPoolExecutor}. This executor records processing statistics as well as monitors for stalled threads.
 * When a stalled thread is encountered (i.e. when the processing time for a runnable has exceeded a threshold), an event is sent to the monitor.
 * <p/>
 * The default configuration uses a bounded queue to accept work. If the queue size is exceeded, work will be rejected. This allows the runtime to
 * degrade gracefully under load by pushing requests back to the client and avoid out-of-memory conditions.
 *
 * @version $Rev$ $Date$
 */
@EagerInit
@Service(interfaces = {ExecutorService.class, RuntimeThreadPoolExecutorMBean.class})
public class RuntimeThreadPoolExecutor extends AbstractExecutorService implements RuntimeThreadPoolExecutorMBean {
    private int coreSize = 20;
    private int maximumSize = 20;
    private int queueSize = 10000;
    private int stallThreshold = 600000;
    private long stallCheckPeriod = 60000;

    private ThreadPoolExecutor delegate;
    private LinkedBlockingQueue<Runnable> queue;
    private StalledThreadMonitor stalledMonitor;
    private ExecutorMonitor monitor;

    // queue of in-flight work
    private ConcurrentLinkedQueue<RunnableWrapper> inFlight = new ConcurrentLinkedQueue<RunnableWrapper>();

    // statistics
    private AtomicLong totalExecutionTime = new AtomicLong();
    private AtomicLong completedWorkCount = new AtomicLong();

    /**
     * Sets the number of threads always available to service the executor queue.
     *
     * @param size the number of threads.
     */
    @Property(required = false)
    public void setCoreSize(int size) {
        this.coreSize = size;
    }

    /**
     * Sets the maximum number of threads to service the executor queue.
     *
     * @param size the number of threads.
     */
    @Property(required = false)
    public void setMaximumSize(int size) {
        this.maximumSize = size;
    }

    /**
     * Sets the maximum number of work items that can be queued before the executor rejects additional work.
     *
     * @param size the maximum number of work items
     */
    @Property(required = false)
    public void setQueueSize(int size) {
        this.queueSize = size;
    }

    /**
     * Sets the period between checks for stalled threads.
     * @param period the period between checks for stalled threads.
     */
    @Property(required = false)
    public void setStallCheckPeriod(long period) {
        this.stallCheckPeriod = period;
    }

    /**
     * Sets the time a thread can be processing work before it is considered stalled. The default is ten minutes.
     *
     * @param stallThreshold the time a thread can be processing work before it is considered stalled
     */
    @Property(required = false)
    public void setStallThreshold(int stallThreshold) {
        this.stallThreshold = stallThreshold;
    }

    public int getStallThreshold() {
        return stallThreshold;
    }

    public int getActiveCount() {
        return delegate.getActiveCount();
    }

    public int getMaximumPoolSize() {
        return delegate.getMaximumPoolSize();
    }

    public void setMaximumPoolSize(int size) {
        delegate.setMaximumPoolSize(size);
    }

    public int getCorePoolSize() {
        return delegate.getCorePoolSize();
    }

    public void setCorePoolSize(int size) {
        delegate.setCorePoolSize(size);
    }

    public int getLargestPoolSize() {
        return delegate.getLargestPoolSize();
    }

    public int getRemainingCapacity() {
        return queue.remainingCapacity();
    }

    public long getTotalExecutionTime() {
        return totalExecutionTime.get();
    }

    public long getCompletedWorkCount() {
        return completedWorkCount.get();
    }

    public double getMeanExecutionTime() {
        long count = completedWorkCount.get();
        long totalTime = totalExecutionTime.get();
        return count == 0 ? 0 : totalTime / count;
    }

    public long getLongestRunning() {
        RunnableWrapper runnable = inFlight.peek();
        if (runnable == null) {
            return -1;
        }
        return System.currentTimeMillis() - runnable.start;
    }

    public RuntimeThreadPoolExecutor(@Monitor ExecutorMonitor monitor) {
        this.monitor = monitor;
    }

    @Init
    public void init() {
        if (queueSize > 0) {
            // create a bounded queue to accept work
            queue = new LinkedBlockingQueue<Runnable>(queueSize);
        } else {
            // create an unbounded queue to accept work
            queue = new LinkedBlockingQueue<Runnable>();
        }
        RuntimeThreadFactory factory = new RuntimeThreadFactory();
        delegate = new ThreadPoolExecutor(coreSize, maximumSize, Long.MAX_VALUE, TimeUnit.SECONDS, queue, factory);
        stalledMonitor = new StalledThreadMonitor();
        delegate.execute(stalledMonitor);
    }

    @Destroy
    public void stop() {
        stalledMonitor.stop();
        delegate.shutdown();
    }

    public void execute(Runnable runnable) {
        Runnable wrapper = new RunnableWrapper(runnable);
        delegate.execute(wrapper);
    }

    public void shutdown() {
        delegate.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    /**
     * Wraps submitted work to record processing statistics.
     */
    private class RunnableWrapper implements Runnable {
        private Runnable delegate;
        private Thread currentThread;

        private long start = -1;

        private RunnableWrapper(Runnable delegate) {
            this.delegate = delegate;
        }

        public void run() {
            try {
                currentThread = Thread.currentThread();
                inFlight.add(this);
                start = System.currentTimeMillis();
                delegate.run();
                long elapsed = System.currentTimeMillis() - start;
                totalExecutionTime.addAndGet(elapsed);
                completedWorkCount.incrementAndGet();
            } finally {
                currentThread = null;
                inFlight.remove(this);
            }
        }
    }

    /**
     * Periodically scans in-flight work for threads that have exceeded a processing time threshold.
     */
    private class StalledThreadMonitor implements Runnable {
        private AtomicBoolean active = new AtomicBoolean(true);

        public void run() {
            while (active.get()) {
                try {
                    Thread.sleep(stallCheckPeriod);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    continue;
                }
                // iterator never throws ConcurrentModificationException and can therefore be used to safely traverse the in-flight work queue
                for (RunnableWrapper runnable : inFlight) {
                    long elapsed = System.currentTimeMillis() - runnable.start;
                    if (elapsed >= stallThreshold) {
                        Thread thread = runnable.currentThread;
                        if (thread != null) {
                            StackTraceElement[] trace = thread.getStackTrace();
                            StringBuilder builder = new StringBuilder();
                            for (StackTraceElement element : trace) {
                                builder.append("\tat ").append(element).append("\n");
                            }
                            monitor.stalledThread(thread.getName(), elapsed, builder.toString());
                        }
                    }
                }
            }
        }

        public void stop() {
            active.set(false);
        }
    }

}