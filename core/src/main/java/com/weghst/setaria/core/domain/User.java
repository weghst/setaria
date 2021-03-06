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
package com.weghst.setaria.core.domain;

import java.io.Serializable;

/**
 * 用户实体.
 *
 * @author Kevin Zou (zouyong@shzhiduan.com)
 */
public class User implements Serializable {

    private static final long serialVersionUID = -5331182262867932385L;

    /**
     * 普通用户.
     */
    public static final String TYPE_SIMPLE = "simple";
    /**
     * 管理员用户.
     */
    public static final String TYPE_MANAGER = "manager";

    /**
     * 主键.
     */
    private int id;
    /**
     * 邮箱.
     */
    private String email;
    /**
     * 密码.
     */
    private String password;
    /**
     * 创建时间.
     */
    private long createdTime;
    /**
     * 用户是否为管理员.
     */
    private String type;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", createdTime=" + createdTime +
                ", type='" + type + '\'' +
                '}';
    }
}
