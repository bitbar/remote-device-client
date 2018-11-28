package com.bitbar.remotedevice;

import com.bitbar.remotedevice.websocket.WebsocketManager;
import com.bitbar.remotedevice.websocket.listeners.APIConnectionListener;
import com.testdroid.api.model.APIConnection;
import com.testdroid.api.model.APIDeviceSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompSession;

import java.util.concurrent.ExecutionException;

public abstract class RemoteDeviceSession implements APIConnectionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDeviceSession.class);
    protected WebsocketManager websocketManager;
    protected APIDeviceSession apiDeviceSession;
    protected StompSession stompSession;
    protected boolean finished = false;
    protected boolean connected = false;

    public void start() {
        try {
            stompSession = websocketManager.startWebsocketSession(apiDeviceSession);
            websocketManager.registerAPIConnectionListener(this);
        } catch (ExecutionException e) {
            LOGGER.error("Failed connecting to device session message queue", e);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void handleAPIConnection(APIConnection connection) {
        LOGGER.debug("New connection available: {}", connection);
    }

    public boolean isFinished() {
        return finished;
    }

    public APIDeviceSession getAPIDeviceSession() {
        return apiDeviceSession;
    }

    public void stop() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        finished = true;
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }
}
