package com.bitbar.remotedevice.android;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.bitbar.remotedevice.android.PortForwardingParameters.*;

public class ADBPortForwarding {

    private static final Logger LOGGER = LoggerFactory.getLogger(ADBPortForwarding.class);

    private static final String CONNECTION_RESET_MESSAGE = "SocketException: Connection reset";

    private static final long CONNECTION_RETRY_DELAY = 1000;

    private static final int MAX_CONNECTION_TRIES = 10;

    private Session daemonSession;

    ADBPortForwarding(JSch jsch, String daemonHost, Integer daemonPort) throws JSchException {
        daemonSession = jsch.getSession(USER, daemonHost, daemonPort);
        daemonSession.setConfig("PreferredAuthentications", "publickey");
        daemonSession.setConfig(CONFIG_KEY_STRICTHOSTKEY, "no");
        daemonSession.setPortForwardingL(LOCAL_PORT, REMOTE_ADB_HOST, REMOTE_FORWARDED_PORT);
    }

    public void connect() throws JSchException, InterruptedException {
        int tries = 1;
        while (!daemonSession.isConnected()) {
            try {
                daemonSession.connect();
            } catch (JSchException e) {
                if (tries < MAX_CONNECTION_TRIES && shouldBeRetried(e)) {
                    Thread.sleep(CONNECTION_RETRY_DELAY);
                    tries++;
                } else {
                    throw e;
                }
            }
        }
        LOGGER.debug("SSH tunnel connected after {} tries", tries);
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

    private boolean shouldBeRetried(JSchException exception) {
        return exception.getMessage().contains(CONNECTION_RESET_MESSAGE);
    }
}
