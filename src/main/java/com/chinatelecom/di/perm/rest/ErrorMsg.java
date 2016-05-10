package com.chinatelecom.di.perm.rest;

public class ErrorMsg implements Msg {

  private int code;
  private String errMsg;


  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getErrMsg() {
    return errMsg;
  }

  public void setErrMsg(String errMsg) {
    this.errMsg = errMsg;
  }

  public enum ErrCode {
    IOE(1), INVALID_PARAM(2);

    private int val;

    private ErrCode(int val) {
       this.val = val;
    }

    public int val() {
      return val;
    }
  }


}
