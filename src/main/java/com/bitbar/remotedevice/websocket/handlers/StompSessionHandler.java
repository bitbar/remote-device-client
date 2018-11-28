package com.bitbar.remotedevice.websocket.handlers;

import com.bitbar.remotedevice.websocket.WebsocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class StompSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StompSessionHandler.class);

    private static final String DEVICE_SESSION_SELECTOR_FORMAT = "deviceSessionId = %d";

    private static final String SELECTOR_HEADER_NAME = "selector";

    private String connectionTopic;

    private Long deviceSessionId;

    private WebsocketManager websocketManager;

    public StompSessionHandler(WebsocketManager websocketManager, String connectionTopic, Long deviceSessionId) {
        this.connectionTopic = connectionTopic;
        this.deviceSessionId = deviceSessionId;
        this.websocketManager = websocketManager;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders headers) {
        StompHeaders subscribeHeaders = new StompHeaders();
        subscribeHeaders.setDestination(connectionTopic);
        subscribeHeaders.set(SELECTOR_HEADER_NAME, String.format(DEVICE_SESSION_SELECTOR_FORMAT, deviceSessionId));
        session.subscribe(subscribeHeaders, new DeviceSessionStompFrameHandler(websocketManager));
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
