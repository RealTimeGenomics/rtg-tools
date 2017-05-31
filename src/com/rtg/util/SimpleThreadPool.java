/*
 * Copyright (c) 2014. Real Time Genomics Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rtg.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.IOUtils;

/**
 * A simple of way of using threads.
 * Can specify the number of threads, schedule runnable tasks to be executed as threads
 * become available and wait until all scheduled tasks have been completed. <p>
 *
 * Normal usage is to add lots of jobs to the pool by calling
 * <code>execute</code> many times, and up to <code>mMaxThreads</code> of those jobs
 * will execute in parallel.
 * Call <code>terminate</code> to wait until all the tasks have finished.
 * You can also call <code>code shutdown</code> (before using <code>terminate</code>
 * to wait for any remaining jobs to finish) if you want the pool to
 * close early without running all the jobs.<p>
 *
 * If any of the jobs throw an uncaught exception,
 * <code>ProgramState.setAbort()</code> is called, and the exception
 * will be stored and later re-thrown when terminate returns. Normally
 * this is after all other jobs have finished executing, but if they
 * are calling <code>ProgramState.checkAbort()</code> regularly, they
 * will abort early.
 *
 */
public final class SimpleThreadPool {

  private static final int NOT_DONE_SLEEP_TIME = 500;
  private final int mMaxThreads;

  private final Queue<Runnable> mJobs = new LinkedList<>();
  private final List<WorkerThread> mThreads = new ArrayList<>();
  private final String mThreadPoolName;
  private long mTotalJobs;
  private long mTotalJobsFinished = 0;
  private boolean mBasicProgress = false;

  private boolean mQueueDone = false;

  private volatile boolean mProcessJobs = true;
  private volatile boolean mBusy;
  private final Thread mQueueThread;

  private class QueueThread extends Thread {
    private final String mSubName;
    private final boolean mLogLifecycleEvents;

    QueueThread(final String name, final String subname, boolean logJobLifeCycle) {
      super(name);
      mSubName = subname;
      mLogLifecycleEvents = logJobLifeCycle;
    }

    @Override
    public void run() {
      try {
        Diagnostic.developerLog(mSubName + ": Started");
        while (mProcessJobs) {
          boolean localBusy = false;
          synchronized (mJobs) {
            for (final WorkerThread t : mThreads) {
              if (!t.hasJob()) {
                if (!mJobs.isEmpty()) {
                  t.enqueueJob(mJobs.remove());
                  localBusy = true;
                  if (mLogLifecycleEvents) {
                    Diagnostic.developerLog(mSubName + ": New Job Started by thread: " + t.getName() + " - " + mJobs.size() + " Jobs Left Queued");
                  }
                }
              } else {
                localBusy = true;
              }
            }
            while (!mJobs.isEmpty() && mThreads.size() < mMaxThreads) {
              final WorkerThread t = new WorkerThread(mSubName + "-" + mThreads.size(), mJobs);
              mThreads.add(t);
              if (mLogLifecycleEvents) {
                Diagnostic.developerLog(mSubName + ": Worker Thread Created - " + t.getName() + " - " + mThreads.size() + "/" + mMaxThreads + " Threads");
              }
              t.enqueueJob(mJobs.remove());
              t.start();
              localBusy = true;
              if (mLogLifecycleEvents) {
                Diagnostic.developerLog(mSubName + ": New Job Started by thread: " + t.getName() + " - " + mJobs.size() + " Jobs Left Queued");
              }
            }
            mBusy = localBusy;
            mJobs.notifyAll();
            try {
              if (mProcessJobs) {
                mJobs.wait(NOT_DONE_SLEEP_TIME);
              }
            } catch (final InterruptedException e) {
              //dont care
            }
          }
        }
      } catch (final Throwable t) {
        mThrown = t;
        mProcessJobs = false;
        ProgramState.setAbort();
      } finally {
        for (final WorkerThread t : mThreads) {
          t.die();
        }
        mBusy = false;
        synchronized (mJobs) {
          mJobs.clear();
        }
        Diagnostic.developerLog(mSubName + ": Finished");
        synchronized (this) {
          mQueueDone = true;
          notifyAll();
        }
      }
    }
  }

  private Throwable mThrown = null;

  private void updateProgress() {
    if (mBasicProgress) {
      synchronized (mJobs) {
        ++mTotalJobsFinished;
        final String message = mThreadPoolName + ": " + mTotalJobsFinished + "/" + mTotalJobs + " Jobs Finished";
        Diagnostic.progress(message);
        Diagnostic.developerLog(message);
      }
    }
  }

  /**
   * Use this class to wrap runnables and to collect any exceptions
   * coming out of the <code>run()</code> call.  Only one of these is
   * remembered and reported after termination. This is roughly
   * equivalent to an uncaught exception handler.
   */
  private class RunProxy implements Runnable {

    RunProxy(final IORunnable run) {
      mRun = run;
    }

    private final IORunnable mRun;

    @Override
    public void run() {
      try {
        mRun.run();
        updateProgress();
      } catch (final Throwable e) {
        if (!(e instanceof ProgramState.SlimAbortException)) {
          synchronized (mJobs) {
            mJobs.clear();
            synchronized (SimpleThreadPool.this) {
              if (mThrown == null) {
                mThrown = e;
              }
              ProgramState.setAbort();
              for (final WorkerThread t : mThreads) {
                t.interrupt();
              }
            }
          }
        }
      }
    }

  }

  /**
   * Constructor for a thread pool.
   * Basic progress output enabled by default.
   * @param numberThreads maximum number of threads that will be used.
   * @param subname textual label to use in threads.
   * @param logLifecycleEvents logs thread life cycle events
   */
  public SimpleThreadPool(final int numberThreads, final String subname, boolean logLifecycleEvents) {
    mThreadPoolName = subname;
    assert numberThreads > 0;
    mMaxThreads = numberThreads;
    mQueueThread = new QueueThread("SimpleThreadPool-" + mThreadPoolName + "-Queue", subname, logLifecycleEvents);
    mQueueThread.setDaemon(true);
    mQueueThread.start();
    Diagnostic.developerLog(mThreadPoolName + ": Starting SimpleThreadPool with maximum " + numberThreads + " threads");
  }

  /**
   * Enable the basic progress output with the total number of jobs that this thread pool will process.
   * @param totalJobs the total number of jobs this thread pool will be processing.
   */
  public void enableBasicProgress(long totalJobs) {
    mTotalJobs = totalJobs;
    mBasicProgress = true;
    Diagnostic.progress(mThreadPoolName + ": Starting " + mTotalJobs + " Jobs");
  }

  /**
   * Stops the thread pool
   */
  private void shutdown() {
    synchronized (mJobs) {
      mProcessJobs = false;
      mJobs.notifyAll();
    }
  }

  /**
   * Enqueue job to run in pool
   * @param run job to run
   * @return true if the job was added, false if it was not added due to pool being in an error state.
   */
  public boolean execute(final IORunnable run) {
    synchronized (mJobs) {
      if (mProcessJobs) { // QueueThread is still able to despool jobs
        if (!ProgramState.isAbort()) { // And pool not in error state
          mJobs.add(new RunProxy(run)); // OK to add
          mJobs.notifyAll();
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Blocks until all tasks have completed execution after a shutdown request,
   * or the current thread is interrupted, whichever happens first.
   *
   * @throws IOException if the sub-jobs died with an IOException.
   */
  public void terminate() throws IOException {
    try {
      try {
        synchronized (mJobs) {
          while (mBusy || !mJobs.isEmpty()) {
            mJobs.wait(NOT_DONE_SLEEP_TIME);
          }
        }
      } catch (final InterruptedException e) {
        // The Runnables may not get to see this before it finishes
        // (they may not even be looking), but it is better than
        // nothing.
        ProgramState.setAbort();
      } finally {
        shutdown(); // Ensure the queueing thread closes things down.
      }
      rethrow();
    } finally {
      ProgramState.clearAbort();
      try {
        synchronized (mQueueThread) {
          //waiting for queuethread to complete
          while (!mQueueDone) {
            mQueueThread.wait(NOT_DONE_SLEEP_TIME);
          }
        }
      } catch (final InterruptedException e) {
        //just exit
      }
    }
  }

  void rethrow() throws IOException {
    if (mThrown != null) {
      IOUtils.rethrow(mThrown);
    }
  }
}
