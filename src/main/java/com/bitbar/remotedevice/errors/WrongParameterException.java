package com.bitbar.remotedevice.errors;

import com.bitbar.remotedevice.cli.CommandLineParameter;

public class WrongParameterException extends Exception {

    public WrongParameterException(CommandLineParameter parameter) {
        super(String.format("Parameter \"%s\" is wrong!", parameter.getArgument()));
    }
}
