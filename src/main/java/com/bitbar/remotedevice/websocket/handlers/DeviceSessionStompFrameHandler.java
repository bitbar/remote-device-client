package com.bitbar.remotedevice.websocket.handlers;

import com.bitbar.remotedevice.websocket.WebsocketManager;
import com.testdroid.api.model.APIConnection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;

class DeviceSessionStompFrameHandler implements org.springframework.messaging.simp.stomp.StompFrameHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceSessionStompFrameHandler.class);

    private static final String PAYLOAD_TYPE_HEADER = "payloadType";

    private WebsocketManager websocketManager;

    DeviceSessionStompFrameHandler(WebsocketManager websocketManager) {
        this.websocketManager = websocketManager;
    }

    public Type getPayloadType(StompHeaders headers) {
        String className = headers.get(PAYLOAD_TYPE_HEADER).get(0);
        if (className == null) {
            return null;
        }
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Error while parsing websocket message - unknown payload class {}", className);
        }
        return clazz;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {

        if (payload instanceof APIConnection) {
            APIConnection connection = (APIConnection) payload;
            LOGGER.debug("Received message: APIConnection(urlSchema: {}, host: {}, path: {}, port: {}, " +
                            "contains password: {})", connection.getUrlSchema(), connection.getHost(),
                    connection.getPath(), connection.getPort(), StringUtils.isNotBlank(connection.getPassword()));
            websocketManager.notify(connection);
        } else {
            LOGGER.debug("Unhandled message payload class - {}", payload.getClass().getName());
        }
    }


}
