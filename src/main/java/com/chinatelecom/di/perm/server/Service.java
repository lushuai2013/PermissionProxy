package com.chinatelecom.di.perm.server;

public interface Service {

  void doStart();

  void doStop();

  void abort(Throwable why);

  void join() throws InterruptedException;
}
