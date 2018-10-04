/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.core.config;


/**
 * Created by wasim on 09/11/16.
 */
public class SearchConfiguration {

    private String host;
    private String mode;
    private String user;
    private String password;
    private String manager;
    private boolean active;
    private int timeout;
    @Deprecated
    private int rows;
    private int insertBatchSize;

    private static final String DEFAULT_HOST = "localhost:8983/solr/";
    private static final String DEFAULT_MODE = "cloud";
    private static final String DEFAULT_USER = "";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_MANAGER = "";
    private static final boolean DEFAULT_ACTIVE = true;
    private static final int DEFAULT_TIMEOUT = 30000;
    private static final int DEFAULT_INSERT_BATCH_SIZE = 10000;


    public SearchConfiguration() {
        this(DEFAULT_HOST, DEFAULT_MODE, DEFAULT_USER, DEFAULT_PASSWORD, DEFAULT_MANAGER, DEFAULT_ACTIVE, DEFAULT_TIMEOUT,
                DEFAULT_INSERT_BATCH_SIZE);
    }

    @Deprecated
    public SearchConfiguration(String host, String mode, String user, String password, boolean active, int timeout, int insertBatchSize) {
        this.host = host;
        this.mode = mode;
        this.user = user;
        this.password = password;
        this.active = active;
        this.timeout = timeout;
        this.insertBatchSize = insertBatchSize;
        this.rows = insertBatchSize;
    }

    public SearchConfiguration(String host, String mode, String user, String password, String manager, boolean active, int timeout,
                               int insertBatchSize) {
        this.host = host;
        this.mode = mode;
        this.user = user;
        this.password = password;
        this.manager = manager;
        this.active = active;
        this.timeout = timeout;
        this.rows = insertBatchSize;
        this.insertBatchSize = insertBatchSize;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SearchConfiguration{");
        sb.append("host='").append(host).append('\'');
        sb.append(", mode='").append(mode).append('\'');
        sb.append(", user='").append(user).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", manager='").append(manager).append('\'');
        sb.append(", active=").append(active);
        sb.append(", timeout=").append(timeout);
        sb.append(", rows=").append(rows);
        sb.append(", insertBatchSize=").append(insertBatchSize);
        sb.append('}');
        return sb.toString();
    }

    public String getHost() {
        return host;
    }

    public SearchConfiguration setHost(String host) {
        this.host = host;
        return this;
    }

    public String getMode() {
        return mode;
    }

    public SearchConfiguration setMode(String mode) {
        this.mode = mode;
        return this;
    }

    public String getUser() {
        return user;
    }

    public SearchConfiguration setUser(String user) {
        this.user = user;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public SearchConfiguration setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getManager() {
        return manager;
    }

    public SearchConfiguration setManager(String manager) {
        this.manager = manager;
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public SearchConfiguration setActive(boolean active) {
        this.active = active;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public SearchConfiguration setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    @Deprecated
    public int getRows() {
        return rows;
    }

    @Deprecated
    public SearchConfiguration setRows(int rows) {
        this.rows = rows;
        return this;
    }

    public int getInsertBatchSize() {
        return insertBatchSize;
    }

    public SearchConfiguration setInsertBatchSize(int insertBatchSize) {
        this.insertBatchSize = insertBatchSize;
        return this;
    }
}
