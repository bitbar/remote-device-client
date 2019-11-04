package com.bitbar.remotedevice.android;

import com.bitbar.remotedevice.errors.WrongParameterException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ADBVersionParserTest {

    private static Collection<Object[]> parseData() {
        Object[][] data = new Object[][]{
                {"1.0.32"},
                {"1.0.41"}
        };
        return Arrays.asList(data);
    }

    private static Collection<Object[]> validateData() {
        Object[][] data = new Object[][]{
                {EMPTY,     "Parameter \"adbversion\" is wrong!"},
                {"1",       "Parameter \"adbversion\" is wrong!"},
                {"1.2",     "Parameter \"adbversion\" is wrong!"},
                {"1.2.3",   null},
                {"1.2.3.4", "Parameter \"adbversion\" is wrong!"}
        };
        return Arrays.asList(data);
    }

    @ParameterizedTest
    @MethodSource("parseData")
    void parse(String version) throws IOException{
        ADBVersionParser classUnderTest = new ADBVersionParser();
        assertThat(classUnderTest.parse(getStringFromFile(version))).isEqualTo(version);
    }

    @ParameterizedTest
    @MethodSource("validateData")
    void validate(String version, String error) {
        ADBVersionParser classUnderTest = new ADBVersionParser();
        Executable executable = () -> classUnderTest.validate(version);
        if (error == null) {
            assertDoesNotThrow(executable);
        } else {
            Throwable throwable = assertThrows(WrongParameterException.class, executable);
            assertThat(throwable.getMessage()).isEqualTo("Parameter \"adbversion\" is wrong!");
        }
    }

    private static String getStringFromFile(String version) throws IOException {
        try (InputStream inputStream = ADBVersionParserTest.class.getResourceAsStream("/adb-version-output/" + version)) {
            return IOUtils.toString(inputStream, UTF_8);
        }
    }
}
