package com.bitbar.remotedevice.ios;

import com.bitbar.remotedevice.RemoteDeviceSession;
import com.bitbar.remotedevice.websocket.WebsocketManager;
import com.testdroid.api.model.APIConnection;
import com.testdroid.api.model.APIDeviceSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteIOSDeviceSession extends RemoteDeviceSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteIOSDeviceSession.class);

    static final String USBMUXD = "USBMUXD";

    UsbmuxdSocketForwarding usbmuxdSocketForwarding;

    public RemoteIOSDeviceSession(WebsocketManager websocketManager, APIDeviceSession apiDeviceSession) {
        this.websocketManager = websocketManager;
        this.apiDeviceSession = apiDeviceSession;
    }

    @Override
    public void handleAPIConnection(APIConnection connection) {
        if (USBMUXD.equals(connection.getType().toUpperCase())) {
            if (connection.getPassword() != null) {
                usbmuxdSocketForwarding = new UsbmuxdSocketForwarding(connection);
                usbmuxdSocketForwarding.start();
            } else {
                LOGGER.error("Server did not provide private key for connection. Aborting.");
                finished = true;
            }
        } else {
            super.handleAPIConnection(connection);
        }
    }

    @Override
    public synchronized void stop() {
        if (usbmuxdSocketForwarding != null) {
            usbmuxdSocketForwarding.stop();
        }
        super.stop();
    }

    @Override
    public boolean isFinished() {
        if (usbmuxdSocketForwarding == null) {
            return false;
        } else if (usbmuxdSocketForwarding.isRunning()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isConnected() {
        if (usbmuxdSocketForwarding != null) {
            return usbmuxdSocketForwarding.isConnected();
        } else {
            return false;
        }
    }
}
