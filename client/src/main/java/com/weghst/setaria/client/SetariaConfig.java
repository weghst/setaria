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

/**
 * 配置管理客户端接口定义.
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
public interface SetariaConfig {

    /**
     * 初始化配置信息.
     */
    void init();

    /**
     * 刷新配置信息.
     */
    void refresh();

    /**
     * 销毁配置信息.
     */
    void destroy();

    /**
     * 返回配置提供者接口.
     *
     * @return {@link ConfigProvider}
     */
    ConfigProvider getConfigProvider();

    /**
     * 添加配置监听器.
     *
     * @param listener 监听器
     */
    void addListener(SetariaConfigListener listener);

    /**
     * 删除配置监听器. 返回删除成功或失败.
     *
     * @param listener 监听器
     * @return true/false
     */
    boolean removeListener(SetariaConfigListener listener);
}
