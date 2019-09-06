package com.bitbar.remotedevice.websocket;

import static com.bitbar.remotedevice.StaticParameters.WEBSOCKET_URI;

class WebsocketUrlBuilder {

    private static final String PROTOCOL_URL_PREFIX_REGEX = "^(http[s]?://)";

    private static final String PROTOCOL_SECURE_PREFIX = "https://";

    private static final String WEBSOCKET_URL_TEMPLATE = "%s://%s%s";

    static String buildWebsocketUrl(String cloudBEUrl) {
        String websocketProtocolPrefix = cloudBEUrl.startsWith(PROTOCOL_SECURE_PREFIX) ? "wss" : "ws";
        return String.format(WEBSOCKET_URL_TEMPLATE,
                websocketProtocolPrefix,
                cloudBEUrl.replaceFirst(PROTOCOL_URL_PREFIX_REGEX, ""),
                WEBSOCKET_URI);
    }

}
