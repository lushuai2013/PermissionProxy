package com.chinatelecom.di.perm.server;

import java.io.IOException;

public interface HdfsProxyService {

  String[] findUser(String user) throws IOException;
  boolean addGroup(String user, String ...groups) throws IOException;
  boolean addUser(String user) throws IOException;
}
