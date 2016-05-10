package com.chinatelecom.di.perm.linux;

import com.chinatelecom.di.perm.rest.SuccessMsg;


public class UserGroupInfo extends SuccessMsg {
  private String[] groups;
  private String user;

  public String[] getGroups() {
    return groups;
  }
  public String getUser() {
    return user;
  }
  public void setGroups(String ...groups) {
    this.groups = groups;
  }
  public void setUser(String user) {
    this.user = user;
  }
}
