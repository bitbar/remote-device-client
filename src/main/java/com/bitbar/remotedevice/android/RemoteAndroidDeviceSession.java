package com.bitbar.remotedevice.android;

import com.bitbar.remotedevice.RemoteDeviceSession;
import com.bitbar.remotedevice.websocket.WebsocketManager;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.testdroid.api.model.APIConnection;
import com.testdroid.api.model.APIDeviceSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAndroidDeviceSession extends RemoteDeviceSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAndroidDeviceSession.class);
    private static final String IDENTITY_NAME_FORMAT = "bitbar-%s";
    private static final String ADB = "ADB";
    private JSch jsch;
    private ADBPortForwarding adbPortForwarding;

    public RemoteAndroidDeviceSession(WebsocketManager websocketManager, APIDeviceSession apiDeviceSession) {
        this.websocketManager = websocketManager;
        this.apiDeviceSession = apiDeviceSession;
        jsch = new JSch();
    }

    @Override
    public void handleAPIConnection(APIConnection connection) {
        if (ADB.equals(connection.getType().toUpperCase())) {
            try {
                if (connection.getPassword() != null) {
                    jsch.addIdentity(String.format(IDENTITY_NAME_FORMAT, connection.getDeviceSessionId()),
                            connection.getPassword().getBytes(), null, null);
                }
                adbPortForwarding = new ADBPortForwarding(jsch, connection.getHost(), connection.getPort());
                adbPortForwarding.connect();
                connected = true;
            } catch (JSchException | InterruptedException e) {
                LOGGER.error("Problem occurred while trying to forward ADB port", e);
                stop();
            }
        } else {
            super.handleAPIConnection(connection);
        }
    }

    @Override
    public void stop() {
        if (adbPortForwarding != null) {
            adbPortForwarding.disconnect();
        }
        super.stop();
    }
}
