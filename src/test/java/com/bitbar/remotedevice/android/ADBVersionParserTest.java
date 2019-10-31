package com.bitbar.remotedevice.android;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class ADBVersionParserTest {

    private static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"1.0.32"},
                {"1.0.41"}
        };
        return Arrays.asList(data);
    }

    @ParameterizedTest
    @MethodSource("data")
    void parse(String version) throws IOException{
        ADBVersionParser classUnderTest = new ADBVersionParser();
        assertThat(classUnderTest.parse(getStringFromFile(version))).isEqualTo(version);
    }

    private static String getStringFromFile(String version) throws IOException {
        try (InputStream inputStream = ADBVersionParserTest.class.getResourceAsStream("/adb-version-output/" + version)) {
            return IOUtils.toString(inputStream, UTF_8);
        }
    }
}
