package com.chinatelecom.di.perm.rest;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.chinatelecom.di.perm.common.Constant;
import com.chinatelecom.di.perm.linux.UserGroupInfo;
import com.chinatelecom.di.perm.server.HdfsProxyService;

@Controller
@RequestMapping("/v1/hdfs")
public class HdfsController {

  @Autowired
  private ServletContext servletContext;

  private HdfsProxyService  hdfsService;

  @PostConstruct
  public void init() {
    hdfsService = (HdfsProxyService) servletContext.getAttribute(Constant.CONTEXT_HDFS_PROXY_SERVICE_NAME);
    if (hdfsService == null) {
      throw new IllegalArgumentException("Can't get " + Constant.CONTEXT_HDFS_PROXY_SERVICE_NAME
          + " from servlet context.");
    }
  }

  @ResponseBody
  @RequestMapping("/user/info/{u_name}")
  public Msg findUser(@PathVariable("u_name") String name) {
    Msg msg = null;
    try {
      UserGroupInfo ugi = new UserGroupInfo();
      String[] groups = hdfsService.findUser(name);
      ugi.setUser(name);
      ugi.setGroups(groups);
      msg = ugi;
    } catch (IOException e) {
      ErrorMsg err = new ErrorMsg();
      err.setCode(ErrorMsg.ErrCode.IOE.val());
      err.setErrMsg(e.getMessage());
      msg = err;
    }
    return msg;
  }

  @ResponseBody
  @RequestMapping(
    value = "/user/group/append/{u_name}",
    method = { RequestMethod.GET, RequestMethod.POST })
  public Msg appendGroup(@PathVariable("u_name") String name, @RequestParam("groups") String gs) {
    String[] groups = gs.split(",");

    for (String g : groups) {
      if (g.length() == 0) {
        ErrorMsg err = new ErrorMsg();
        err.setCode(ErrorMsg.ErrCode.INVALID_PARAM.val());
        err.setErrMsg("Group can't be empty");
        return err;
      }
    }

    Msg msg = null;

    try {
      hdfsService.appendGroup(name, groups);
      msg = new SuccessMsg();
    } catch (IOException e) {
      ErrorMsg err = new ErrorMsg();
      err.setCode(ErrorMsg.ErrCode.IOE.val());
      err.setErrMsg(e.getMessage());
      msg = err;
    }

    return msg;
  }

  @ResponseBody
  @RequestMapping("/user/add/{u_name}")
  public Msg addUser(@PathVariable("u_name") String name) {
    Msg msg = null;
    try {
      hdfsService.addUser(name);
      msg = new SuccessMsg();
    } catch (IOException e) {
      ErrorMsg err = new ErrorMsg();
      err.setCode(ErrorMsg.ErrCode.IOE.val());
      err.setErrMsg(e.getMessage());
      msg = err;
    }

    return msg;
  }

  @ResponseBody
  @RequestMapping(
    value = "/group/add/{g_name}",
    method = { RequestMethod.GET, RequestMethod.POST })
  public Msg addGroup(@PathVariable("g_name") String name) {
    Msg msg = null;

    try {
      hdfsService.addGroup(name);
      msg = new SuccessMsg();
    } catch (IOException e) {
      ErrorMsg err = new ErrorMsg();
      err.setCode(ErrorMsg.ErrCode.IOE.val());
      err.setErrMsg(e.getMessage());
      msg = err;
    }

    return msg;
  }

}

