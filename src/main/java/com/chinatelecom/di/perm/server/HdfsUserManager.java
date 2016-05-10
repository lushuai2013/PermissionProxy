package com.chinatelecom.di.perm.server;

import java.io.File;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.chinatelecom.di.perm.common.Constant;
import com.chinatelecom.di.perm.common.PropertiesHelper;
import com.chinatelecom.di.perm.linux.ShellUtils;
import com.chinatelecom.di.perm.linux.ShellUtils.ShellResult;

/**
 * Delegate shell operation.
 *
 */
public class HdfsUserManager {
  private static final Log LOG = LogFactory.getLog(HdfsUserManager.class);

  private String[] namenodeHosts;
  private String scriptPath;

  public HdfsUserManager(PropertiesHelper.PropertiesConfig conf) throws Exception {
    String address = conf.get(Constant.NAME_NODE_ADDRESS_KEY, "");
    namenodeHosts = address.split(",");

    if (namenodeHosts == null || namenodeHosts.length == 0) {
      throw new Exception("No config params " + Constant.NAME_NODE_ADDRESS_KEY);
    }

    for (int i = 0; i < namenodeHosts.length; i++) {
      try {
        Inet4Address.getAllByName(namenodeHosts[i]);
      } catch (UnknownHostException e) {
        LOG.fatal("Can't resolve " + namenodeHosts, e);
        throw new Exception(e);
      }
    }

    String home = System.getProperty(Constant.PERMISSION_PROXY_HOME);
    if (home == null) {
      // May be in test mode, try to load UserManager.sh
      URL url = HdfsUserManager.class.getClassLoader().getResource(Constant.USER_MANAGER_SHELL);
      if (url == null)
        throw new Exception("Miss " + Constant.PERMISSION_PROXY_HOME + " in system properties.");
      scriptPath = url.getPath();
    } else {
      scriptPath = home + "/bin/" + Constant.USER_MANAGER_SHELL;
    }

    File f = new File(scriptPath);
    if (!f.exists()) {
      throw new NoSuchFileException(scriptPath + " doesn't exist.");
    }

    f.setExecutable(true);
    f.setReadable(true);
  }

  public String[] getNameNodeHosts() {
    return Arrays.copyOf(namenodeHosts, namenodeHosts.length);
  }

  public List<ShellResult> addUser(String user) {
    List<ShellResult> retVal = new ArrayList<ShellResult>(namenodeHosts.length);

    for (int i = 0 ; i < namenodeHosts.length; i++) {
      String cmd = scriptPath + " addUser " + namenodeHosts[i] + " " + user;
      retVal.add(ShellUtils.runCommand(cmd).setHost(namenodeHosts[i]));
    }

    return retVal;
  }

  public List<ShellResult> addGroup(String user, String ...groups) {
    List<ShellResult> retVal = new ArrayList<ShellResult>(namenodeHosts.length);

    String Gargs = null;
    for (String g : groups) {
      if (Gargs == null) Gargs = g;
      else Gargs += "," + g;
    }

    for (int i = 0 ; i < namenodeHosts.length; i++) {
      String cmd = scriptPath + " addGroup " + namenodeHosts[i] + " " + user + " " + Gargs;
      retVal.add(ShellUtils.runCommand(cmd).setHost(namenodeHosts[i]));
    }

    return retVal;
  }

  /**
   * Find group information for this user.
   * Group information within ShellResult.getStdOut() and each group separate by a space.
   * <br/>
   * Such as 'group1 group2 group3'
   * @param user
   * @return
   *
   *
   */
  public List<ShellResult> findUser(String user) {
    List<ShellResult> retVal = new ArrayList<ShellResult>(namenodeHosts.length);

    for (int i = 0 ; i < namenodeHosts.length; i++) {
      String cmd = scriptPath + " findUser " + namenodeHosts[i] + " " + user;
      ShellResult res = ShellUtils.runCommand(cmd).setHost(namenodeHosts[i]);
      if (res.getStdOut() != null && res.getStdOut().length() > 0 && !extractValidStdout(res)) {
        LOG.warn("Can't get valid output, ShellResult=" + res);
      }
      retVal.add(res);
    }

    return retVal;
  }

  private boolean extractValidStdout(ShellResult res) {
    if (res.getStdOut() == null) {
      LOG.debug("Want to extract stdout but null, ShellResult=" + res);
      return true;
    }

    String out = res.getStdOut();
    int s = out.indexOf("$<");
    int e = out.lastIndexOf(">$");
    if (s == -1 || e == -1) {
      if (res.getExitCode() == 0) {
        res.setExitCode(ShellUtils.ExceptionCode.INVALID_OUT.val());
      }
      return false;
    }
    String usefulOut = out.substring(s + 2, e);
    if (out.length() != usefulOut.length() + 4) {
      LOG.warn("There are some unuseful contents in stdout, ShellResult=" + res);
    }
    res.setStdOut(usefulOut);
    return true;
  }
}
