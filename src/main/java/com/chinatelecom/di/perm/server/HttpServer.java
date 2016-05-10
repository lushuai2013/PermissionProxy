package com.chinatelecom.di.perm.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.chinatelecom.di.perm.common.Constant;
import com.chinatelecom.di.perm.common.PropertiesHelper.PropertiesConfig;

public class HttpServer {
  private static final Log LOG = LogFactory.getLog(HttpServer.class);

  private PropertiesConfig conf;
  private Server server;
  private int port = 8080;
  private int timeout = 60 * 1000;
  private int numOfSelector = 2;
  private int backlog = 32;
  private int bufSize = 1024 * 32; //64k
  private int maxHandler = 32;

  public HttpServer(PropertiesConfig conf) {
    this.conf = conf;
    setup();
    initialize();
  }

  private void setup() {
    port = conf.get(Constant.HTTP_PORT_KEY, port);
    timeout = conf.get(Constant.HTTP_TIMEOUT_KEY, timeout);
    numOfSelector = conf.get(Constant.HTTP_CHANNEL_SELECTOR_KEY, numOfSelector);
    backlog = conf.get(Constant.HTTP_SOCKET_BACKLOG, backlog);
    bufSize = conf.get(Constant.HTTP_BUFER_SIZE_KEY, bufSize);
    maxHandler = conf.get(Constant.HTTP_MAX_HANDLER_KEY, maxHandler);
  }

  private void initialize() {
    SelectChannelConnector con = new SelectChannelConnector();
    con.setHost("0.0.0.0");
    con.setPort(port);
    con.setMaxIdleTime(timeout);
    con.setAcceptors(numOfSelector);
    con.setAcceptQueueSize(backlog);
    con.setMaxBuffers((int) Math.max(Runtime.getRuntime().maxMemory() / bufSize / 5, 1024));
    con.setRequestBufferSize(bufSize / 2);
    con.setResponseBufferSize(bufSize);
    con.setUseDirectBuffers(false);

    QueuedThreadPool pool = new QueuedThreadPool();
    pool.setMaxThreads(maxHandler);
    pool.setMinThreads(Math.min(maxHandler / 4, 2));

    server = new Server(port);
    server.setConnectors(new Connector[]{con});
    server.setThreadPool(pool);
  }

  public void addWebApp(WebApp app) {
    server.setHandler(app);
    LOG.info("Add a WebApp" + app);
  }

  public void start() throws Exception {
    server.start();
    LOG.info("Start HttpServer at " + server.getConnectors()[0].getHost() + ":"
        + server.getConnectors()[0].getPort());
  }

  public void stop() throws Exception {
    server.stop();
  }

  public void join() throws InterruptedException {
    server.join();
  }
}
