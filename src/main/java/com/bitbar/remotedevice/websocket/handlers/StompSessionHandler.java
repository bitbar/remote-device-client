package com.bitbar.remotedevice.websocket.handlers;

import com.bitbar.remotedevice.websocket.WebsocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class StompSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StompSessionHandler.class);

    private String connectionTopic;

    private WebsocketManager websocketManager;

    public StompSessionHandler(WebsocketManager websocketManager, String connectionTopicFormat, Long deviceSessionId) {
        this.connectionTopic = String.format(connectionTopicFormat, deviceSessionId);
        this.websocketManager = websocketManager;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders headers) {
        session.subscribe(connectionTopic, new DeviceSessionStompFrameHandler(websocketManager));
    }

    /**
     * Gets exceptions, which by specification are sent by plaintext
     */
    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        LOGGER.debug("Got message frame on main websocket, possible error: {}", payload);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        LOGGER.debug("Exception while waiting for connection details", exception);
    }
}
