package com.chinatelecom.di.perm.server;

import org.junit.Before;
import org.junit.Test;

public class ProxyServerTest {



  private ProxyServer server ;

  @Before
  public void setup() throws Exception {
    server = new ProxyServer();
  }

  @Test(timeout=60000)
  public void testStartStop() throws Exception {
    server.doStart();
    Thread.sleep(20 * 1000);
    server.doStop();
  }

}
