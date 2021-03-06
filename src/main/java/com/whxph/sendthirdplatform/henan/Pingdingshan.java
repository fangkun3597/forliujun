package com.whxph.sendthirdplatform.henan;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.utils.OldXphapi;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * @author liujun
 */
@Component
public class Pingdingshan {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pingdingshan.class);

    private boolean start = false;

    private String token = "";

    private String username = "pingdingshan";

    private Set<Integer> deviceSet = new HashSet<>();

    private String address = "http://123.163.55.113:8686/pdssanitate/services/SaveYCJCService?wsdl";

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(cron = "0 0 0/12 * * ?")
    public void updateDeviceSet() {
        if (start) {
            token = OldXphapi.updateToken(restTemplate);
            deviceSet = OldXphapi.updateDevice(restTemplate, username, token);
        }
    }

    @Scheduled(cron = "0 0/16 * * * ?")
    public void update() throws InterruptedException {
        if (start) {
            for (Integer deviceId : deviceSet) {
                JSONObject data = restTemplate.getForObject(String.format("http://115.28.187.9:8005/intfa/queryData/%d", deviceId), JSONObject.class);
                assert data != null;
                JSONArray entity = data.getJSONArray("entity");
                try {
                    JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
                    Client client = dcf.createClient(address);
                    String facId = "101" + deviceId.toString().substring(2);
                    String dateTime = entity.getJSONObject(0).getString("datetime");
                    DecimalFormat decimalFormat = new DecimalFormat("0.00");
                    String content = "DevID:|:" + facId +
                            "#|#Time:|:" + dateTime +
                            "#|#HUMI:|:-1#|#TEMP:|:-1#|#PRE:|:0#|#WINDD:|:-1" +
                            "#|#WINDS:|:" + decimalFormat.format(entity.getJSONObject(0).getFloatValue("eValue")) +
                            "#|#NOISE:|:" + decimalFormat.format(entity.getJSONObject(4).getFloatValue("eValue")) +
                            "#|#PM25:|:" + decimalFormat.format(entity.getJSONObject(5).getFloatValue("eValue")) +
                            "#|#PM10:|:" + decimalFormat.format(entity.getJSONObject(6).getFloatValue("eValue")) +
                            "#|#TSP:|:0";
                    LOGGER.info("[{}]: {}", deviceId, content);
                    Object[] objects = client.invoke("saveYCJC", content);
                    LOGGER.info("{}", objects[0]);
                } catch (Exception e) {
                    LOGGER.error("接口请求异常");
                }
                Thread.sleep(500);
            }
        }
    }

    public void start() {
        this.start = true;
        token = OldXphapi.updateToken(restTemplate);
        deviceSet = OldXphapi.updateDevice(restTemplate, username, token);
    }
}
