package com.bitbar.remotedevice;

import com.bitbar.remotedevice.android.PortForwardingParameters;
import com.bitbar.remotedevice.android.RemoteAndroidDeviceSession;
import com.bitbar.remotedevice.api.APIClientManager;
import com.bitbar.remotedevice.cli.CommandLineParameter;
import com.bitbar.remotedevice.cli.RemoteDeviceClientCommandLineInterface;
import com.bitbar.remotedevice.errors.PortNotFreeException;
import com.bitbar.remotedevice.errors.RequiredParameterIsEmptyException;
import com.bitbar.remotedevice.ios.RemoteIOSDeviceSession;
import com.bitbar.remotedevice.websocket.WebsocketManager;
import com.testdroid.api.APIException;
import com.testdroid.api.model.APIDevice;
import com.testdroid.api.model.APIDeviceSession;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class RemoteDeviceClientMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDeviceClientMain.class);

    private static final String JAR_NAME = "bitbar-remote-client.jar";

    private static final String CONNECT_COMMAND = "CONNECT";

    private static final String DEVICES_COMMAND = "DEVICES";

    private APIClientManager apiClientManager;

    private WebsocketManager websocketManager;

    private static Map<Long, RemoteDeviceSession> remoteDeviceSessions = new HashMap<>();

    private static boolean interactive;

    private static Thread keyboardInputThread;

    private static Object keyboardLock = new Object();

    private RemoteDeviceClientMain(CommandLine commandLine) throws RequiredParameterIsEmptyException, APIException {
        String cloudUrl = commandLine.getOptionValue(CommandLineParameter.CLOUD_URI.getArgument());
        String apiKey = commandLine.getOptionValue(CommandLineParameter.API_KEY.getArgument());
        apiClientManager = new APIClientManager(cloudUrl, apiKey);
        websocketManager = new WebsocketManager(apiClientManager.getBackendUrl(), apiKey);
    }

    public static void main(String[] args) {
        try {
            CommandLine cl = RemoteDeviceClientCommandLineInterface.parseArguments(args, 0);
            if (cl.getArgs().length == 0) {
                usage();
                System.exit(1);
            }

            RemoteDeviceClientMain client = new RemoteDeviceClientMain(cl);
            String command = cl.getArgs()[0].toUpperCase();

            switch (command) {
                case CONNECT_COMMAND:
                    interactive = true;
                    client.connect(cl);
                    break;
                case DEVICES_COMMAND:
                    client.devices();
                    break;
                default:
                    LOGGER.error(String.format("Unknown command: %s", command));
                    System.exit(1);
            }
        } catch (ParseException | RequiredParameterIsEmptyException e) {
            LOGGER.error(e.getMessage());
            usage();
        } catch (APIException e) {
            LOGGER.error("Problem occurred with API call to cloud", e);
        }
    }

    private static void usage() {
        HelpFormatter formatter = new HelpFormatter();
        String[] commands = {
                "devices - print devices available for remote connection",
                "connect - connect to device, requires -device parameter"
        };
        String header = "\nCommands:\n" + String.join("\n", commands) + "\n\nOptions:\n";

        String footer = String.format("\nExample:\njava -jar %s " + // TODO: add commands
                        "-cloudurl https://cloud.bitbar.com/cloud " +
                        "-apikey YOUR_API_KEY " +
                        "-device 12 " +
                        "connect",
                JAR_NAME
        );
        formatter.printHelp("java -jar bitbar-remote-client.jar [options] <command>",
                header, RemoteDeviceClientCommandLineInterface.getOptions(), footer, false);
    }

    private void devices() {
        try {
            for (APIDevice device : apiClientManager.getSupportedDevices()) {
                String state = "";
                if (device.isLocked()) {
                    state = "busy";
                } else if (!device.isOnline()) {
                    state = "offline";
                }
                LOGGER.info(String.format("%5d %-40s %s", device.getId(), device.getDisplayName(),
                        state));
            }
        } catch (APIException e) {
            e.printStackTrace();
        }
    }

    private void connect(CommandLine commandLine)
            throws RequiredParameterIsEmptyException, APIException {
        Long deviceModelId;
        try {
            deviceModelId = Long.parseLong(commandLine.getOptionValue(CommandLineParameter.DEVICE_MODEL_ID.getArgument()));
        } catch (NumberFormatException e) {
            throw new RequiredParameterIsEmptyException(CommandLineParameter.DEVICE_MODEL_ID);
        }

        APIDevice device = apiClientManager.getDevice(deviceModelId);
        if (device.isLocked()) {
            LOGGER.info("{}: \"{}\" is busy", device.getId(), device.getDisplayName());
            System.exit(1);
        } else if (!device.isOnline()) {
            LOGGER.info("{}: \"{}\" is offline", device.getId(), device.getDisplayName());
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::finishDeviceSessions));

        if (device.getOsType() == APIDevice.OsType.ANDROID) {
            try {
                checkPortFree(PortForwardingParameters.LOCAL_PORT);
                APIDeviceSession apiSession = apiClientManager.createDeviceSession(deviceModelId);
                remoteDeviceSessions.put(apiSession.getId(),
                        new RemoteAndroidDeviceSession(websocketManager, apiSession));
            } catch (PortNotFreeException e) {
                LOGGER.error(e.getMessage());
            }
        } else if (device.getOsType() == APIDevice.OsType.IOS) {
            APIDeviceSession apiSession = apiClientManager.createDeviceSession(deviceModelId);
            remoteDeviceSessions.put(apiSession.getId(),
                    new RemoteIOSDeviceSession(websocketManager, apiSession));
        } else {
            LOGGER.info(String.format("Remote device connections do not support %s devices", device.getOsType().name()));
            System.exit(1);
        }

        for (RemoteDeviceSession session : remoteDeviceSessions.values()) {
            LOGGER.info("Device session {} for \"{}\" created - waiting for device to be ready to connect.",
                    session.getAPIDeviceSession().getId(),
                    session.getAPIDeviceSession().getDevice().getDisplayName());
            session.start();
        }
        try {
            waitForSessionsToFinish();
        } catch (InterruptedException ignore) {
        }
    }

    private void checkPortFree(int port) throws PortNotFreeException {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"),
                    port));
        } catch (Exception e) {
            throw new PortNotFreeException();
        }
    }

    private static void printStatusLine() {
        if (interactive) {
            final String commandLine = "%s device(s) connected. Type \"quit\" to stop session and restore to local devices.";
            int connectedDevicesCount = 0;
            for (RemoteDeviceSession session : remoteDeviceSessions.values()) {
                if (session.isConnected()) {
                    connectedDevicesCount++;
                }
            }
            LOGGER.info(String.format(commandLine, connectedDevicesCount));
        }
    }

    public static void startCommandLine() {
        synchronized (keyboardLock) {
            if (interactive && keyboardInputThread == null) {
                keyboardInputThread = new Thread(() -> {
                    boolean running = true;
                    Scanner keyboard = new Scanner(System.in);
                    while (running) {
                        String command = keyboard.nextLine();
                        switch (command) {
                            case "q":
                            case "quit":
                                System.exit(0);
                            default:
                                printStatusLine();
                        }
                    }
                });
                keyboardInputThread.start();
            }
        }
    }

    public static void stopCommandLine() {
        synchronized (keyboardLock) {
            if (keyboardInputThread != null) {
                try {
                    keyboardInputThread.interrupt();
                } catch (Exception ignore) {
                } finally {
                    keyboardInputThread = null;
                }
            }
        }
    }

    private void waitForSessionsToFinish() throws InterruptedException {
        if (remoteDeviceSessions.size() > 0) {

            boolean finished = false;
            int lastConnectedCount = 0;
            while (!finished) {
                finished = true;
                int connectedCount = 0;
                for (RemoteDeviceSession session : remoteDeviceSessions.values()) {
                    if (session.isConnected()) {
                        connectedCount++;
                    }
                    if (!session.isFinished()) {
                        finished = false;
                    }
                }
                if (connectedCount != lastConnectedCount) {
                    printStatusLine();
                    // Start the keyboard input only after connections are up
                    // For example iOS connection may ask for sudo password which we should not eat away!
                    if (connectedCount > 0 && keyboardInputThread == null) {
                        startCommandLine();
                    }
                }
                lastConnectedCount = connectedCount;

                Thread.sleep(2000);
            }
        }
        System.exit(0);
    }

    private void finishDeviceSessions() {
        for (RemoteDeviceSession remoteSession : remoteDeviceSessions.values()) {
            APIDeviceSession apiDeviceSession = remoteSession.getAPIDeviceSession();
            LOGGER.info("Releasing device session {}", apiDeviceSession.getId());
            try {
                apiClientManager.releaseDeviceSession(apiDeviceSession);
            } catch (APIException e) {
                LOGGER.error("Problem occurred with API call to cloud", e);
            }
            remoteSession.stop();
        }
    }

}
