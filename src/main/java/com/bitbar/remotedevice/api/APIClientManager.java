package com.bitbar.remotedevice.api;

import com.bitbar.remotedevice.cli.CommandLineParameter;
import com.bitbar.remotedevice.errors.RequiredParameterIsEmptyException;
import com.testdroid.api.*;
import com.testdroid.api.dto.Context;
import com.testdroid.api.dto.MappingKey;
import com.testdroid.api.dto.Operand;
import com.testdroid.api.filter.FilterEntry;
import com.testdroid.api.filter.ListStringFilterEntry;
import com.testdroid.api.filter.NumberFilterEntry;
import com.testdroid.api.filter.StringFilterEntry;
import com.testdroid.api.model.*;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.bitbar.remotedevice.StaticParameters.DEVICE_SESSIONS_URI;
import static com.bitbar.remotedevice.StaticParameters.RELEASE_DS_URI;
import static com.testdroid.api.dto.MappingKey.LABEL_IDS_ARR;

public class APIClientManager {

    private static final String DEVICE_MODEL_ID_PARAM = "deviceModelId";

    private static final String INFO_URI = "/info";

    private static final String TYPE_PARAM = "type";

    private static final String TYPE_PARAM_VALUE = "REMOTE";

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

    private Optional<Long> getRemoteSessionsLabelId() throws APIException {
        Optional<Long> result = Optional.empty();
        Context<APILabelGroup> ctx = new Context<>(APILabelGroup.class);
        ctx.addFilter(new StringFilterEntry(MappingKey.NAME, Operand.EQ, "supported-frameworks"));
        List<APILabelGroup> labelGroups = apiClient.getLabelGroups(ctx).getEntity().getData();
        if (labelGroups.size() > 0) {
            Context<APIDeviceProperty> lCtx = new Context<>(APIDeviceProperty.class);
            lCtx.addFilter(new StringFilterEntry(MappingKey.NAME, Operand.EQ, "remote-session"));
            result = labelGroups.get(0).getDevicePropertiesResource(lCtx).getEntity().getData()
                    .stream().findFirst().map(APIEntity::getId);
        }
        return result;
    }

    public List<APIDevice> getSupportedDevices() throws APIException {
        Context<APIDevice> ctx = new Context<>(APIDevice.class);
        ctx.getFilters().add(new ListStringFilterEntry(MappingKey.OS_TYPE, Operand.IN,
                Arrays.asList(APIDevice.OsType.ANDROID.getDisplayName(), APIDevice.OsType.IOS.getDisplayName())));
        getRemoteSessionsLabelId().ifPresent(val -> ctx.setExtraParams(
                new HashSetValuedHashMap<>(Collections.singletonMap(LABEL_IDS_ARR, val))
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
        filters.add(new NumberFilterEntry(MappingKey.ID, Operand.EQ, id));
        APIList<APIDevice> devices = apiClient.getDevices(ctx).getEntity();
        if (devices.isEmpty()) {
            throw new APIException(String.format("Could not find device with id %d", id));
        }
        return devices.get(0);
    }

    public APIDeviceSession createDeviceSession(Long deviceModelId)
            throws APIException, RequiredParameterIsEmptyException {
        if (deviceModelId == null) {
            throw new RequiredParameterIsEmptyException(CommandLineParameter.DEVICE_MODEL_ID);
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(DEVICE_MODEL_ID_PARAM, deviceModelId);
        parameters.put(TYPE_PARAM, TYPE_PARAM_VALUE);

        APIDeviceSession deviceSession = apiClient.post(DEVICE_SESSIONS_URI, parameters, APIDeviceSession.class);
        return deviceSession;
    }

    public void releaseDeviceSession(APIDeviceSession deviceSession) throws APIException {
        apiClient.post(String.format(RELEASE_DS_URI, deviceSession.getId()), null, APIDeviceSession.class);
    }

    public String getBackendUrl() throws APIException {
        APICloudInfo info = apiClient.get(INFO_URI, APICloudInfo.class);
        return info.getCloudUrl();
    }

}
