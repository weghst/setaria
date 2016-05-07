/**
 * Copyright (C) 2016 The Weghst Inc. <kevinz@weghst.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weghst.setaria.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.weghst.setaria.client.util.ZkPathUtils;

/**
 * 分布式配置实现, 该实现依赖于 {@code ZooKeeper}.
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
public class DistributedSetariaConfig extends AbstractSetariaConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedSetariaConfig.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    {
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static final int DEFAULT_SESSION_TIMEOUT = 60 * 1000;
    private static final String DEFAULT_BASE_PATH = "/setaria";
    private static final String URL_SEGMENT = "/url";
    private static final String APP_PARENT_SEGMENT = "/apps";
    private static final String CLIENT_NODE_PREFIX = "/client-";

    /**
     * ZooKeeper 连接字符串.
     */
    public static final String CONFIG_ZOOKEEPER_CONNECT_STRING = "setaria.config.zookeeper.connectString";
    /**
     * ZooKeeper 会话超时时间(ms), 默认为 60s.
     */
    public static final String CONFIG_ZOOKEEPER_SESSION_TIMEOUT = "setaria.config.zookeeper.sessionTimeout";
    /**
     * ZooKeeper 中配置根目录, 默认为 /setaria.
     */
    public static final String CONFIG_BASE_PATH = "setaria.config.zookeeper.basePath";
    /**
     * 配置应用名称.
     */
    public static final String CONFIG_APP = "setaria.config.app";
    /**
     * 配置应用环境.
     */
    public static final String CONFIG_ENV = "setaria.config.env";

    private ZooKeeper zooKeeper;
    private ConfigProvider configProvider;

    private String app;
    private String env;
    private String urlNodePath;
    private String appNodePath;
    private String clientNodePath;

    public DistributedSetariaConfig(Map<String, String> configParameters) {
        super(configParameters);

        String connectString = getConfigParameter(CONFIG_ZOOKEEPER_CONNECT_STRING);
        String basePath = configParameters.get(CONFIG_BASE_PATH);
        if (basePath == null || basePath.isEmpty()) {
            basePath = DEFAULT_BASE_PATH;
        }
        app = getConfigParameter(CONFIG_APP);
        env = getConfigParameter(CONFIG_ENV);

        appNodePath = ZkPathUtils.join(basePath, APP_PARENT_SEGMENT, app + "-" + env);
        urlNodePath = ZkPathUtils.join(basePath, URL_SEGMENT);
        LOG.debug("分布式配置参数 connectString: {}, path: {}", connectString, appNodePath);

        int sessionTimeout = DEFAULT_SESSION_TIMEOUT;
        if (configParameters.containsKey(CONFIG_ZOOKEEPER_SESSION_TIMEOUT)) {
            sessionTimeout = Integer.parseInt(configParameters.get(CONFIG_ZOOKEEPER_SESSION_TIMEOUT));
        }

        try {
            zooKeeper = new ZooKeeper(connectString, sessionTimeout, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (appNodePath.equals(event.getPath()) && event.getType() == Event.EventType.NodeDataChanged) {
                        refresh();
                    }
                }
            });
        } catch (IOException e) {
            LOG.error("连接 ZooKeeper 服务器错误 ->> {}", connectString, e);
            throw new SetariaConfigException(e);
        }
    }

    @Override
    protected void doInit() {
        LOG.debug("初始化配置");

        refresh0();

        LOG.debug("初始化配置成功");
    }

    @Override
    protected void doRefresh() {
        LOG.debug("刷新配置");

        refresh0();

        LOG.debug("刷新配置成功");
    }

    @Override
    protected void doDestroy() {
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ConfigProvider getConfigProvider() {
        return configProvider;
    }

    private String getUrl() {
        Stat stat = new Stat();
        try {
            byte[] bytes = zooKeeper.getData(urlNodePath, false, stat);
            String url = new String(bytes);

            LOG.debug("配置服务器地址[{}], STAT: {}", url, stat);
            return url;
        } catch (Exception e) {
            LOG.error("获取配置 URL 错误", e);
            throw new SetariaConfigException(e);
        }
    }

    private void refresh0() {
        Stat stat = new Stat();
        try {
            byte[] bytes = zooKeeper.getData(appNodePath, true, stat);
            loadConfigs();
        } catch (Exception e) {
            throw new SetariaConfigException(e);
        }
    }

    private void refreshZkClientInfo() {
        ClientInfo clientInfo = new ClientInfo();
        InetAddress host;
        try {
            host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }

        clientInfo.setHost(host.toString());
        clientInfo.setLastPullTime(System.currentTimeMillis() / 1000);

        byte[] bytes;
        try {
            bytes = OBJECT_MAPPER.writeValueAsBytes(clientInfo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }

        try {
            if (clientNodePath == null) {
                clientNodePath = zooKeeper.create(ZkPathUtils.join(appNodePath, CLIENT_NODE_PREFIX), bytes,
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            } else {
                Stat stat = new Stat();
                zooKeeper.getData(clientNodePath, false, stat);
                zooKeeper.setData(clientNodePath, bytes, stat.getVersion());
            }
        } catch (Exception e) {
            LOG.error("向 ZooKeeper 提交客户端信息错误 ->> {}", clientInfo, e);
            throw new SetariaConfigException(e);
        }
    }

    private void loadConfigs() {
        String url = getUrl();
        LOG.debug("读取 [{}] 配置", url);

        HttpUrl httpUrl = HttpUrl.parse(url).newBuilder().addPathSegment(app).addPathSegment(env).build();
        Request request = new Request.Builder().url(httpUrl).get().build();
        ConfigBean[] configBeans;
        try {
            OkHttpClient httpClient = new OkHttpClient();
            Response response = httpClient.newCall(request).execute();
            configBeans = OBJECT_MAPPER.readValue(response.body().byteStream(), ConfigBean[].class);
        } catch (IOException e) {
            throw new SetariaConfigException("加载 [" + url + "] 配置失败", e);
        }

        // -----------------------------------------------------
        Properties properties = new Properties();
        for (ConfigBean configBean : configBeans) {
            properties.setProperty(configBean.getKey(), configBean.getValue());
            LOG.debug("配置项 ->> [{}: {}]", configBean.getKey(), configBean.getValue());
        }

        //
        //
        //
        configProvider = new DefaultConfigProvider(properties);
        LOG.debug("设置 ConfigProvider 成功");

        refreshZkClientInfo();
    }
}
