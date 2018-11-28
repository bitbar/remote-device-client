package com.bitbar.remotedevice.errors;

import com.bitbar.remotedevice.cli.CommandLineParameter;

public class RequiredParameterIsEmptyException extends Exception {

    public RequiredParameterIsEmptyException(CommandLineParameter parameter) {
        super(String.format("Required parameter \"%s\" is empty!", parameter.getArgument()));
    }
}
