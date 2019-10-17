package com.bitbar.remotedevice.websocket;

import com.bitbar.remotedevice.errors.RequiredParameterIsEmptyException;
import com.bitbar.remotedevice.websocket.handlers.StompSessionHandler;
import com.bitbar.remotedevice.websocket.listeners.APIConnectionListener;
import com.testdroid.api.model.APIConnection;
import com.testdroid.api.model.APIDeviceSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.bitbar.remotedevice.StaticParameters.WEBSOCKET_CONNECTIONS_TOPIC;
import static com.bitbar.remotedevice.cli.CommandLineParameter.API_KEY;
import static com.bitbar.remotedevice.cli.CommandLineParameter.CLOUD_URI;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class WebsocketManager {

    private static final long[] HEARTBEAT_SETTING = {5000, 5000};

    private List<APIConnectionListener> apiConnectionListeners;

    private String websocketUrl;

    private String apiKey;

    private TaskScheduler heartbeatScheduler;

    public WebsocketManager(String cloudBEUrl, String apiKey) throws RequiredParameterIsEmptyException {
        if (StringUtils.isBlank(cloudBEUrl)) {
            throw new RequiredParameterIsEmptyException(CLOUD_URI);
        } else if (StringUtils.isBlank(apiKey)) {
            throw new RequiredParameterIsEmptyException(API_KEY);
        }
        apiConnectionListeners = new LinkedList<>();
        heartbeatScheduler = createHeartbeatScheduler();
        this.apiKey = apiKey;
        websocketUrl = WebsocketUrlBuilder.buildWebsocketUrl(cloudBEUrl);
    }

    private TaskScheduler createHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.initialize();
        return scheduler;
    }

    public StompSession startWebsocketSession(APIDeviceSession deviceSession)
            throws ExecutionException, InterruptedException {
        WebSocketStompClient stompClient = createStompClient();

        StompSessionHandler sessionHandler = new StompSessionHandler(this, WEBSOCKET_CONNECTIONS_TOPIC,
                deviceSession.getId());

        WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
        webSocketHttpHeaders.add(AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((apiKey + ":").getBytes()));
        return stompClient.connect(websocketUrl, webSocketHttpHeaders, sessionHandler).get();
    }

    private WebSocketStompClient createStompClient() {
        WebSocketClient wsClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(wsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.setDefaultHeartbeat(HEARTBEAT_SETTING);
        stompClient.setTaskScheduler(heartbeatScheduler);
        return stompClient;
    }

    public void registerAPIConnectionListener(APIConnectionListener listener) {
        apiConnectionListeners.add(listener);
    }

    public void notify(APIConnection connection) {
        for (APIConnectionListener listener : apiConnectionListeners) {
            listener.handleAPIConnection(connection);
        }
    }

}
