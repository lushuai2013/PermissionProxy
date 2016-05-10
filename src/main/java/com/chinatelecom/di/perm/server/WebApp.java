package com.chinatelecom.di.perm.server;

import java.net.URL;

import org.eclipse.jetty.webapp.WebAppContext;

import com.chinatelecom.di.perm.common.Constant;

public class WebApp extends WebAppContext {

  public WebApp(String name, String pathOfwebXML, HdfsProxyService hdfsService) {
    this.setContextPath("/" + name);
    this.setDisplayName(name);
    URL XMLurl = ClassLoader.getSystemClassLoader().getResource(pathOfwebXML);
    if (XMLurl == null || pathOfwebXML.length() == 0) {
      throw new IllegalArgumentException("Can't find web.xml in " + pathOfwebXML);
    }
    this.setDescriptor(XMLurl.getPath());
    URL base = ClassLoader.getSystemClassLoader().getResource("");
    this.setResourceBase(base.getPath());
    this.getServletContext().setAttribute(Constant.CONTEXT_HDFS_PROXY_SERVICE_NAME, hdfsService);
  }


  @Override
  public String toString() {
    return "{name:" + this.getDisplayName() + ", contextPath:" + this.getContextPath()
        + ", resourceBase:" + this.getResourceBase() + "}";
  }
}
