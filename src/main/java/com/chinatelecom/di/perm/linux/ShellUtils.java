package com.chinatelecom.di.perm.linux;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ShellUtils {
  private static final Log LOG = LogFactory.getLog(ShellUtils.class);

  public static ShellResult runCommand(String cmd) {
    ShellResult retVal = null;

    try {
      retVal = exec(cmd);
    } catch (IOException e) {
      LOG.error("Failed to execute the shell command '" + cmd + "'", e);

      String errOut =
          e.getClass().getCanonicalName()
              + (e.getMessage() != null ? "[" + e.getMessage() + "]" : "");
      if (e.getCause() != null) {
        errOut +=
            ", cause=>" + e.getCause().getClass().getCanonicalName()
                + (e.getCause().getMessage() != null ? "[" + e.getCause().getMessage() + "]" : "");
      }

      retVal = new ShellResult(ExceptionCode.IOERROR.val(), "", errOut);
    }

    LOG.info("Command '" + cmd + "' has finished, ShellResult=" + retVal);

    return retVal;
  }

  private static ShellResult exec(String cmd) throws IOException {
    LOG.debug("Start to execute '" + cmd + "'");
    Process shell = Runtime.getRuntime().exec(cmd);
    ProcessMonitor monitor = ProcessMonitor.get();
    monitor.monitor(cmd, shell, 10 * 1000);
    boolean isTimeout;
    try {
      shell.waitFor();
      LOG.debug("Finish execution '" + cmd + "'");
      isTimeout = monitor.isTimeout();
    } catch (InterruptedException e) {
      LOG.warn("Wait for the shell command '" + cmd
          + "' to complete, but this thread be interrupted.");
      return new ShellResult(ExceptionCode.INTERRUPTED.val(), "", e.getClass().getCanonicalName());
    } finally {
      ProcessMonitor.put(monitor);
    }

    int exitCode = shell.exitValue();

    if (isTimeout && exitCode !=0) {
      return new ShellResult(ExceptionCode.TIMEOUT.val(), "", "Timeout when ran command " + cmd);
    }

    Scanner scan = new Scanner(shell.getInputStream(), "UTF-8");
    scan.useDelimiter("\\A");
    String stdOut = scan.hasNext() ? scan.next() : "";
    scan.close();

    String errOut = "";

    if (exitCode != 0) {
      scan = new Scanner(shell.getErrorStream(), "UTF-8");
      scan.useDelimiter("\\A");
      errOut = scan.hasNext() ? scan.next() : "";
      scan.close();
    }

    shell.destroy();

    return new ShellResult(exitCode, stdOut, errOut);
  }

  private static class ProcessMonitor implements Runnable {

    private Process process;
    private long timeout;  //Milliseconds
    private String cmd;
    private boolean killed;

    private static final int limit = 10;
    private static final LinkedBlockingQueue<ProcessMonitor> boundedPool =
        new LinkedBlockingQueue<ProcessMonitor>(limit);
    private static final Executor executor = Executors.newCachedThreadPool();

    private ProcessMonitor() {
    }

    public static ProcessMonitor get() {
      ProcessMonitor monitor = boundedPool.poll();
      if (monitor == null) {
        monitor = new ProcessMonitor();
      }
      return monitor;
    }

    public static void put(ProcessMonitor monitor) {
      if (monitor == null) return;
      monitor.
      boundedPool.offer(monitor);
    }

    public void monitor(String cmd, Process process, long timeout) {
      this.process = process;
      this.timeout = timeout;
      this.cmd = cmd;
      killed = false;
      executor.execute(this);
    }

    @Override
    public void run() {
      while(true) {
        if (!isRunning()) return;
        long s = System.currentTimeMillis();
        while (System.currentTimeMillis() - s <= timeout) {
          try { Thread.sleep(500); } catch (InterruptedException ignore) { }
          if (isRunning()) continue;
          return; // Process finished.
        }
        process.destroy();
        killed = true;
        LOG.warn("Command '" + cmd + "' timeout, exceed " + timeout + " milliseconds.");
      }
    }

    public boolean isTimeout() {
      return killed;
    }

    private boolean isRunning() {
      try {
        process.exitValue();
      } catch (IllegalThreadStateException alreadyRun) {
        return true;
      }
      return false;
    }
  }

  public enum ExceptionCode {
    IOERROR(-1), INTERRUPTED(-2), INVALID_OUT(-3), TIMEOUT(-4);

    private final int val;

    private ExceptionCode(int value) {
      val = value;
    }

    public int val() {
      return val;
    }
  }

  public static class ShellResult {
    public static final int SUCCESS_CODE = 0;

    private int exitCode;
    private String stdOut;
    private String errOut;
    private String host;

    public ShellResult(int exitCode, String stdOut, String errOut) {
      this.exitCode = exitCode;
      this.stdOut = stdOut;
      this.errOut = errOut;
    }

    public String getHost() {
      return host;
    }

    public ShellResult setHost(String host) {
      this.host = host;
      return this;
    }

    public int getExitCode() {
      return exitCode;
    }
    public String getStdOut() {
      return stdOut;
    }
    public String getErrOut() {
      return errOut;
    }

    public void setExitCode(int exitCode) {
      this.exitCode = exitCode;
    }

    public void setStdOut(String stdOut) {
      this.stdOut = stdOut;
    }

    public void setErrOut(String errOut) {
      this.errOut = errOut;
    }

    @Override
    public String toString() {
      return "[host:" + host + ", exitCode:" + exitCode + ", stdout:'" + stdOut + "', errout:'" + errOut + "']";
    }
  }
}
