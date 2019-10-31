package com.bitbar.remotedevice.android;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ADBVersionParser {

    private static final Pattern ADB_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");

    private static final String UNKNOWN_VERSION = "UNKNOWN";

    public String parse(String input) {
        Matcher matcher = ADB_VERSION_PATTERN.matcher(input);
        return matcher.find() ? matcher.group(1) : UNKNOWN_VERSION;
    }
}
