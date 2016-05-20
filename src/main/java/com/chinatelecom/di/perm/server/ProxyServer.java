package com.chinatelecom.di.perm.server;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.chinatelecom.di.perm.common.PropertiesHelper;
import com.chinatelecom.di.perm.common.PropertiesHelper.PropertiesConfig;
import com.chinatelecom.di.perm.linux.ShellUtils.ShellResult;

public class ProxyServer extends Thread implements Service, HdfsProxyService {
  private static final Log LOG = LogFactory.getLog(ProxyServer.class);

  private PropertiesConfig conf;
  private HttpServer httpServer;
  private HdfsUserManager hdfsUserMgr;
  private boolean running = false;

  protected ProxyServer() throws Exception {
    Properties prop = PropertiesHelper.load("perm-proxy.properties");
    conf = PropertiesHelper.createConfig(prop);
    httpServer = new HttpServer(conf);
    hdfsUserMgr = new HdfsUserManager(conf);
    setupREST();
  }


  private void setupREST() {
    WebApp rest = new WebApp("PermissionProxy", "WEB-INF/permission-proxy-web.xml", this);
    httpServer.addWebApp(rest);
  }


  private void startServices() throws Exception {
    httpServer.start();
  }

  private void stopServices() throws Exception {
    if (httpServer != null) httpServer.stop();
  }

  @Override
  public void run() {
    while(running) {
      // TODO
      // Do something else instead of joining.
      try {
        httpServer.join();
      } catch (InterruptedException ignore) {
      }
    }
  }

  @Override
  public void doStart() {
    LOG.info("Start ProxyServer...");
    running = true;
    try {
      startServices();
    } catch (Exception e) {
      running = false;
      LOG.fatal("Failed to start ProxyServer.", e);
      doStop();
      return ;
    }
    this.start();
    Runtime.getRuntime().addShutdownHook(new ShutdownHook());
  }

  @Override
  public void doStop() {
    LOG.info("Start to stop ProxyServer...");
    running = false;
    try {
      stopServices();
    } catch (Exception e) {
      LOG.error("Failed to stop ProxyServer.", e);
    }
    this.interrupt();
  }

  @Override
  public void abort(Throwable why) {
    LOG.fatal("Abort ProxyServer , because '" + why.getMessage() + "'", why);
    doStop();
  }

  @Override
  public String[] findUser(String user) throws IOException {
    List<ShellResult> shellRess = hdfsUserMgr.findUser(user);

    // TODO
    // We need to deal with inconsistent results
    for (ShellResult res : shellRess) {
      if (res.getExitCode() != ShellResult.SUCCESS_CODE) {
        throw new IOException("An exceptional exit code, ShellResult=" + res);
      }
      return res.getStdOut().split(" ");
    }

    // Impossibility
    return null;
  }

  @Override
  public boolean appendGroup(String user, String... groups) throws IOException {
    List<ShellResult> shellRess = hdfsUserMgr.appendGroup(user, groups);
    boolean success = true;
    // TODO
    // We need to deal with inconsistent results
    for (ShellResult res : shellRess) {
      if (res.getExitCode() != ShellResult.SUCCESS_CODE) {
        throw new IOException("An exceptional exit code, ShellResult=" + res);
      }
      success &= (res.getExitCode() == 0);
    }

    return success;
  }

  @Override
  public boolean delUserGroup(String user, String... groups) throws IOException {
    List<ShellResult> shellRess = hdfsUserMgr.delUserGroup(user, groups);
    boolean success = true;
    // TODO
    // We need to deal with inconsistent results
    for (ShellResult res : shellRess) {
      if (res.getExitCode() != ShellResult.SUCCESS_CODE) {
        throw new IOException("An exceptional exit code, ShellResult=" + res);
      }
      success &= (res.getExitCode() == 0);
    }

    return success;
  }

  @Override
  public boolean addGroup(String group) throws IOException {
    List<ShellResult> shellRess = hdfsUserMgr.addGroup(group);
    boolean success = true;
    // TODO
    // We need to deal with inconsistent results
    for (ShellResult res : shellRess) {
      if (res.getExitCode() != ShellResult.SUCCESS_CODE) {
        throw new IOException("An exceptional exit code, ShellResult=" + res);
      }
      success &= (res.getExitCode() == 0);
    }

    return success;
  }

  @Override
  public boolean delGroup(String group) throws IOException {
    List<ShellResult> shellRess = hdfsUserMgr.delGroup(group);
    boolean success = true;
    // TODO
    // We need to deal with inconsistent results
    for (ShellResult res : shellRess) {
      if (res.getExitCode() != ShellResult.SUCCESS_CODE) {
        throw new IOException("An exceptional exit code, ShellResult=" + res);
      }
      success &= (res.getExitCode() == 0);
    }

    return success;
  }


  @Override
  public boolean delUser(String user) throws IOException {
    List<ShellResult> shellRess = hdfsUserMgr.delUser(user);
    boolean success = true;
    // TODO
    // We need to deal with inconsistent results
    for (ShellResult res : shellRess) {
      if (res.getExitCode() != ShellResult.SUCCESS_CODE) {
        throw new IOException("An exceptional exit code, ShellResult=" + res);
      }
      success &= (res.getExitCode() == 0);
    }

    return success;
  }

  @Override
  public boolean addUser(String user) throws IOException {
    List<ShellResult> shellRess = hdfsUserMgr.addUser(user);
    boolean success = true;
    // TODO
    // We need to deal with inconsistent results
    for (ShellResult res : shellRess) {
      if (res.getExitCode() != ShellResult.SUCCESS_CODE) {
        throw new IOException("An exceptional exit code, ShellResult=" + res);
      }
      success &= (res.getExitCode() == 0);
    }

    return success;
  }

  public static void main(String[] args) {
    ProxyServer proxy = null;
    try {
      proxy = new ProxyServer();
      proxy.doStart();
    } catch (Throwable e) {
      if (proxy != null) proxy.abort(e);
      e.printStackTrace();
    }
  }

  private class ShutdownHook extends Thread {

    @Override
    public void run() {
      LOG.info("ProxyServer ShutdownHook START ");
      ProxyServer.this.doStop();
      try {
        ProxyServer.this.join();
      } catch (InterruptedException e) {
        LOG.error("ShutdownHook was interrupted.", e);
      }
      LOG.info("ProxyServer ShutdownHook FINISHED.");
    }

  }

}

