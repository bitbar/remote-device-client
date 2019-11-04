package com.bitbar.remotedevice.cli;

public enum CommandLineParameter {
    API_KEY("apikey", true, "API key for cloud user authentication"),
    ADB_VERSION("adbversion", true, "requested version of adb in format x.y.x"),
    DEVICE_MODEL_ID("device", true, "device id to connect"),
    CLOUD_URI("cloudurl", true, "address of cloud service");

    private String argument;

    private boolean hasValue;

    private String description;

    CommandLineParameter(String argument, boolean hasValue, String description) {
        this.argument = argument;
        this.hasValue = hasValue;
        this.description = description;
    }

    public String getArgument() {
        return argument;
    }

    public boolean getHasValue() {
        return hasValue;
    }

    public String getDescription() {
        return description;
    }

}
