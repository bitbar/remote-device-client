package com.bitbar.remotedevice.ios;

import com.bitbar.remotedevice.RemoteDeviceClientMain;
import com.testdroid.api.model.APIConnection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

public class UsbmuxdSocketForwarding implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsbmuxdSocketForwarding.class);

    private final static String USBMUXD_PATH = "/var/run/usbmuxd";
    private final static String USBMUXD_BACKUP_PATH = "/var/run/usbmuxd.backup";

    private static final String IDENTITY_NAME_FORMAT = "bitbar-%s";
    static final String USER = "testdroid";
    static final Integer DEFAULT_SSH_PORT = 22;

    private String sshHost;
    private String sshUser;
    private Integer sshPort = DEFAULT_SSH_PORT;
    private String privateKeyPath;

    Process sshProcess;
    Thread sshOutputThread;

    APIConnection connection;

    private Thread thread;
    private boolean running;
    private boolean connected;

    public UsbmuxdSocketForwarding(APIConnection connection) {
        this.connection = connection;
        running = false;
    }

    public void start() {
        if (thread == null) {
            running = true;
            thread = new Thread(this);
            thread.start();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void run() {
        File privateKeyFile = null;
        try {
            privateKeyFile = File.createTempFile(String.format(IDENTITY_NAME_FORMAT, connection.getDeviceSessionId()), ".key");
            privateKeyPath = privateKeyFile.getAbsolutePath();
            privateKeyFile.deleteOnExit();
            FileUtils.writeByteArrayToFile(privateKeyFile, connection.getPassword().getBytes());
            Files.setPosixFilePermissions(privateKeyFile.toPath(), PosixFilePermissions.fromString("rw-------"));
            sshHost = connection.getHost();
            sshPort = connection.getPort();
            sshUser = USER;
            startUsbmuxdForwarding();
        } catch (IOException e) {
            LOGGER.error("Failed establishing secure connection", e);
        } finally {
            try {
                if (privateKeyFile != null) {
                    privateKeyFile.delete();
                }
            } catch (Exception ignore) {
            }
        }
    }

    public void startUsbmuxdForwarding() {
        LOGGER.info("Backing up usbmuxd unix socket file");
        try {
            // Required to catch Ctrl-C
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Stopping usbmuxd ssh forward"));

            // Display the help only when sudo password is not recently given and remembered
            Process adminCheckProcess = getAdminCheckCommand().start();
            adminCheckProcess.waitFor();
            if (adminCheckProcess.exitValue() > 0) {
                LOGGER.error("Sudo privileges are required to create the forward.");
            }

            Process socketBackupProcess = getSocketBackupCommand().start();
            StreamUtils.copy(socketBackupProcess.getInputStream(), System.out);
            if (socketBackupProcess.isAlive()) {
                socketBackupProcess.destroy();
            }

            // Wait for SSH to be listening
            LOGGER.info("Connecting...");
            long timeoutMillis = System.currentTimeMillis() + 30000;

            while (System.currentTimeMillis() < timeoutMillis && !isPortOpen(sshHost, sshPort)) {
                Thread.sleep(1000);
            }

            sshProcess = getSshCommand().start();

            final InputStream sshInputStream = sshProcess.getInputStream();
            sshOutputThread = new Thread(() -> {
                try {
                    StreamUtils.copy(sshInputStream, System.out);
                } catch (IOException ignore) {
                }
            });
            sshOutputThread.start();

            try {
                Thread.sleep(2000);
            } catch (Exception ignore) {
            }

            new ProcessBuilder().command("/usr/bin/sudo",
                    "/bin/chmod",
                    "a+rw",
                    USBMUXD_PATH).redirectErrorStream(true).start();

            // this hopefully signals something that iTunes/XCode and fellows will notice and do a refresh
            StreamUtils.copy(getStopUsbmuxdCommand().start().getInputStream(), System.out);

            LOGGER.info("Connected.\n");
            LOGGER.info("Remote devices:");

            Process listProcess = getListCommand().start();
            StreamUtils.copy(listProcess.getInputStream(), System.out);

            connected = true;

            // TODO: when ssh process dies, should we check if the device session is still fine and reconnect?
            while (sshProcess.isAlive()) {
                Thread.sleep(1000);
            }
        } catch (IOException ex) {
            LOGGER.error("Exception while handling usbmuxd ssh forward", ex);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted.");
        } finally {
            stop();
        }
    }

    public synchronized void stop() {
        try {
            if (sshProcess != null && sshProcess.isAlive()) {
                sshProcess.destroy();
            }
            if (sshOutputThread != null && sshOutputThread.isAlive()) {
                sshOutputThread.interrupt();
            }

            if (new File(USBMUXD_BACKUP_PATH).exists()) {
                LOGGER.info("Restoring usbmuxd unix socket file");
                RemoteDeviceClientMain.stopCommandLine(); // grab input for possible sudo password prompt
                Process socketRestoreProcess = getSocketRestoreCommand().start();
                StreamUtils.copy(socketRestoreProcess.getInputStream(), System.out);
                if (socketRestoreProcess.isAlive()) {
                    socketRestoreProcess.destroy();
                }
                RemoteDeviceClientMain.startCommandLine();

                StreamUtils.copy(getStartUsbmuxdCommand().start().getInputStream(), System.out);
                try {
                    Thread.sleep(5000); // TODO: how to detect usbmuxd is back listening? it takes a few seconds
                    LOGGER.info("Local devices after usbmuxd restore:");
                    Process listProcess = getListCommand().start();
                    StreamUtils.copy(listProcess.getInputStream(), System.out);
                    if (listProcess.isAlive()) {
                        listProcess.destroy();
                    }
                } catch (InterruptedException ignore) { // we are stopping anyways
                }
            }

        } catch (IOException ex) {
            LOGGER.info("Failed restoring usbmuxd socket to local daemon", ex);
        }
        connected = false;
        running = false;
    }


    private ProcessBuilder getAdminCheckCommand() {
        return new ProcessBuilder().command("/usr/bin/sudo", "-n", "id", "-u").redirectErrorStream(true);
    }

    private ProcessBuilder getListCommand() {
        // TODO: look in path
        return new ProcessBuilder()
                .command("/usr/local/bin/idevice_id", "-l")
                .redirectErrorStream(true); //merge input and error streams
    }

    private ProcessBuilder getSocketBackupCommand() {
        return new ProcessBuilder()
                .command("/usr/bin/sudo", "/bin/mv", "-v", USBMUXD_PATH, USBMUXD_BACKUP_PATH)
                .redirectErrorStream(true);
    }

    private ProcessBuilder getSocketRestoreCommand() {
        return new ProcessBuilder()
                .command("/usr/bin/sudo", "/bin/mv", "-v", USBMUXD_BACKUP_PATH, USBMUXD_PATH)
                .redirectErrorStream(true);
    }

    private ProcessBuilder getStopUsbmuxdCommand() {
        File serviceBinary = new File("/usr/sbin/service");
        if (serviceBinary.canExecute()) {
            return new ProcessBuilder().command("/usr/bin/sudo", "/usr/sbin/service", "usbmuxd", "stop").redirectErrorStream(true);
        } else { // launchctl always respawns on OS X
            return new ProcessBuilder().command("/usr/bin/sudo", "/usr/bin/killall", "usbmuxd").redirectErrorStream(true);
        }
    }

    private ProcessBuilder getStartUsbmuxdCommand() {
        File serviceBinary = new File("/usr/sbin/service");
        if (serviceBinary.canExecute()) {
            return new ProcessBuilder().command("/usr/bin/sudo", "/usr/sbin/service", "usbmuxd", "restart").redirectErrorStream(true);
        } else { // launchctl always respawns on OS X
            return new ProcessBuilder().command("/usr/bin/sudo", "/usr/bin/killall", "usbmuxd").redirectErrorStream(true);
        }
    }

    private ProcessBuilder getSshCommand() {
        return new ProcessBuilder().command(
                "/usr/bin/sudo",
                "/usr/bin/ssh",
                "-q",
                "-T", // do not allocate pseudo terminal
                "-N", // do not execute command
                "-L", String.format("%s:%s", USBMUXD_PATH, USBMUXD_PATH),
                "-i", privateKeyPath,
                "-o", "StrictHostKeyChecking=no",
                "-o", "UserKnownHostsFile=/dev/null",
                "-o", "PasswordAuthentication=no",
                "-o", "ServerAliveInterval=5",
                "-o", "ServerAliveCountMax=12",
                "-p", sshPort.toString(),
                String.format("%s@%s", sshUser, sshHost))
                .redirectErrorStream(true);
    }

    private boolean isPortOpen(String host, int port) {
        Socket s = null;
        InputStream in = null;
        try {
            s = new Socket(host, port);
            in = s.getInputStream();
            byte[] buf = new byte[3];
            IOUtils.read(in, buf, 0, 3);
            final String header = new String(buf).toUpperCase();
            if ("SSH".equals(header)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(s);
        }
    }

}
