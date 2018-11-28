# Bitbar Remote Device Client

[![Language grade](https://img.shields.io/lgtm/grade/java/g/bitbar/remote-device-client.svg)](https://lgtm.com/projects/g/bitbar/remote-device-client/context:java)
[![Current Release](https://img.shields.io/github/release/bitbar/remote-device-client.svg)](https://github.com/bitbar/remote-device-client/releases)
[![License](https://img.shields.io/github/license/bitbar/remote-device-client.svg)](https://github.com/bitbar/remote-device-client/blob/master/LICENSE)

## Usage

**Run syntax**

```sh
java -jar <JAR_WITH_DEPENDENCIES_PATH> \
     -cloudurl <BACKEND_URL> \
     -apikey <APIKEY> \
     -device <DEVICE_MODEL_ID> \
     <COMMAND>
```

**Parameters**

- `cloudurl` \
  URL to Bitbar Testing backend
- `apikey` \
  api key used to authenticate with Bitbar Testing
- `device` \
  id of device model to connect

**Commands**

- `devices` \
  display list of available devices which have remote device enabled
- `connect` \
  connect to a device, requires `-device` option

**Example**

```sh
java -jar remote-device-client.jar \
     -cloudurl https://cloud.bitbar.com/cloud \
     -apikey XXXXXXX \
     -device 172 \
     connect
```


## Running With Maven

Use the `exec:java` goal and `-Dexec.args` to pass arguments.

**Example**

```sh
mvn compile exec:java "-Dexec.args=-cloudurl https://cloud.bitbar.com/cloud -apikey XXXXXXXX -device 209 connect"
```


## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
