package com.bitbar.remotedevice.android;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.bitbar.remotedevice.android.PortForwardingParameters.*;

public class ADBPortForwarding {

    private static final Logger LOGGER = LoggerFactory.getLogger(ADBPortForwarding.class);

    private Session daemonSession;

    ADBPortForwarding(JSch jsch, String daemonHost, Integer daemonPort) throws JSchException {
        daemonSession = jsch.getSession(USER, daemonHost, daemonPort);
        daemonSession.setConfig("PreferredAuthentications", "publickey");
        daemonSession.setConfig(CONFIG_KEY_STRICTHOSTKEY, "no");
        daemonSession.setPortForwardingL(LOCAL_PORT, REMOTE_ADB_HOST, REMOTE_FORWARDED_PORT);
    }

    public void connect() throws JSchException {
        daemonSession.connect();
        daemonSession.openChannel(CONNECTION_CHANNEL);
        if (daemonSession.isConnected()) {
            LOGGER.info("ADB forwarding has started");
        } else {
            LOGGER.error("Unknown problem occurred while trying to forward ADB port");
        }
    }

    public void disconnect() {
        if (daemonSession != null && daemonSession.isConnected()) {
            LOGGER.info("Stopping ADB forwarding");
            try {
                daemonSession.disconnect();
            } catch (Exception ignore) {
            }
        }
    }

}
