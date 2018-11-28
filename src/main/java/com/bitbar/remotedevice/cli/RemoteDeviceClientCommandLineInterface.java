package com.bitbar.remotedevice.cli;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class RemoteDeviceClientCommandLineInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDeviceClientCommandLineInterface.class);

    private static final Options options = new Options();

    static {
        for (CommandLineParameter p : CommandLineParameter.values()) {
            options.addOption(p.getArgument(), p.getHasValue(), p.getDescription());
        }
    }

    public static CommandLine parseArguments(String[] args, int startFrom) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args.length > startFrom ?
                    Arrays.copyOfRange(args, startFrom, args.length) : new String[0]);
    }

    public static Options getOptions() {
        return options;
    }
}
