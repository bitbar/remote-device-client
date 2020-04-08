package com.bitbar.remotedevice.api;

import com.bitbar.remotedevice.cli.CommandLineParameter;
import com.bitbar.remotedevice.errors.RequiredParameterIsEmptyException;
import com.testdroid.api.*;
import com.testdroid.api.dto.Context;
import com.testdroid.api.dto.MappingKey;
import com.testdroid.api.filter.FilterEntry;
import com.testdroid.api.model.APICloudInfo;
import com.testdroid.api.model.APIDevice;
import com.testdroid.api.model.APIDeviceSession;
import com.testdroid.api.model.APIDeviceSessionConfig;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.bitbar.remotedevice.StaticParameters.DEVICE_SESSIONS_URI;
import static com.testdroid.api.dto.MappingKey.*;
import static com.testdroid.api.dto.Operand.EQ;
import static com.testdroid.api.dto.Operand.IN;
import static com.testdroid.api.model.APIDevice.OsType.ANDROID;
import static com.testdroid.api.model.APIDevice.OsType.IOS;
import static com.testdroid.api.model.APIDeviceSession.Type.REMOTE;

public class APIClientManager {

    private static final String INFO_URI = "/info";

    private APIClient apiClient;

    public APIClientManager(String cloudUrl, String apiKey) throws RequiredParameterIsEmptyException {
        apiClient = createAPIClient(cloudUrl, apiKey);
    }

    private APIClient createAPIClient(String cloudUrl, String apiKey) throws RequiredParameterIsEmptyException {
        if (StringUtils.isBlank(cloudUrl)) {
            throw new RequiredParameterIsEmptyException(CommandLineParameter.CLOUD_URI);
        } else if (StringUtils.isBlank(apiKey)) {
            throw new RequiredParameterIsEmptyException(CommandLineParameter.API_KEY);
        }
        return new APIKeyClient(cloudUrl, apiKey);
    }

    public List<APIDevice> getSupportedDevices() throws APIException {
        Context<APIDevice> ctx = new Context<>(APIDevice.class);
        ctx.getFilters().add(new FilterEntry(OS_TYPE, IN,
                Arrays.asList(ANDROID.getDisplayName(), IOS.getDisplayName())));
        apiClient.findDevicePropertyInLabelGroup("supported-frameworks", "remote-session").
                ifPresent(val -> ctx.setExtraParams(
                        new HashSetValuedHashMap<>(Collections.singletonMap(LABEL_IDS_ARR, val.getId()))
                ));
        ctx.setLimit(0);
        List<APISort.SortItem> sortItems = new ArrayList<>();
        sortItems.add(new APISort.SortItem(MappingKey.DISPLAY_NAME, APISort.Type.ASC));
        ctx.setSort(new APISort(sortItems));
        return apiClient.getDevices(ctx).getEntity().getData();
    }

    public APIDevice getDevice(Long id) throws APIException {
        Context<APIDevice> ctx = new Context<>(APIDevice.class);
        List<FilterEntry> filters = ctx.getFilters();
        filters.add(new FilterEntry(ID, EQ, id));
        APIList<APIDevice> devices = apiClient.getDevices(ctx).getEntity();
        if (devices.isEmpty()) {
            throw new APIException(String.format("Could not find device with id %d", id));
        }
        return devices.get(0);
    }

    public APIDeviceSession createDeviceSession(Long deviceModelId, String adbVersion)
            throws APIException, RequiredParameterIsEmptyException {
        if (deviceModelId == null) {
            throw new RequiredParameterIsEmptyException(CommandLineParameter.DEVICE_MODEL_ID);
        }
        APIDeviceSessionConfig config = new APIDeviceSessionConfig();
        config.setDeviceModelId(deviceModelId);
        config.setType(REMOTE);
        Optional.ofNullable(adbVersion).ifPresent(config::setAdbVersion);
        return apiClient.post(DEVICE_SESSIONS_URI, config, APIDeviceSession.class);
    }

    public void releaseDeviceSession(APIDeviceSession deviceSession) throws APIException {
        deviceSession.release();
    }

    public String getBackendUrl() throws APIException {
        APICloudInfo info = apiClient.get(INFO_URI, APICloudInfo.class);
        return info.getCloudUrl();
    }

}
