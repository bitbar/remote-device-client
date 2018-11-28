package com.bitbar.remotedevice.errors;

public class PortNotFreeException extends Exception {

    public PortNotFreeException() {
        super("Default port used by ADB (5037) is not free. Do you have local ADB instance running? " +
                "You can stop it using command \"adb kill-server\"");
    }
}
