package com.github.sourguice.appengine.appadmin;

public interface TaskMessageSender {
	public /*synchronized*/ void sendMessage(String message);
}
