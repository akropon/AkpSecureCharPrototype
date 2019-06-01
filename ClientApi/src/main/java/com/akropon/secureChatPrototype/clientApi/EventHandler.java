package com.akropon.secureChatPrototype.clientApi;

import java.net.ConnectException;

public interface EventHandler {

    void fireSuccessfullyConnectedToServer();

    void fireFailedConnectionToServer(Exception e);

    void fireConnectionClosed();

    void fireClosingFailed(InterruptedException e);

    void fireMessageReceived(String msg);
}
