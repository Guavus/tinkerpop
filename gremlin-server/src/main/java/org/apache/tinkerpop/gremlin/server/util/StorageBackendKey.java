/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.server.util;

import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.apache.tinkerpop.gremlin.server.auth.Authenticator;
import org.apache.tinkerpop.gremlin.server.channel.NioChannelizer;
import org.apache.tinkerpop.gremlin.server.channel.WebSocketChannelizer;

import java.util.Objects;

/**
 * Key class for storage backend
 *
 * @author shekhar.bansal
 */
public class StorageBackendKey {

    private static final String STORAGE_BACKEND_KEY = "storage.backend";

    private static final String STORAGE_HOSTNAME_KEY = "storage.hostname";
    private static final String STORAGE_DIRECTORY_KEY = "storage.directory";

    private static final String STORAGE_HBASE_TABLE_KEY = "storage.hbase.table";

    private static final String STORAGE_CASSANDRA_KEYSPACE_KEY = "storage.cassandra.keyspace";
    private static final String STORAGE_CQL_KEYSPACE_KEY = "storage.cql.keyspace";

    private String backend_type;

    private String storage_path;

    private String storage_instance;

    public StorageBackendKey(Configuration configuration) {
        backend_type = configuration.getString(STORAGE_BACKEND_KEY);

        if(backend_type.equals("hbase")
                || backend_type.equals("cql")
                || backend_type.equals("cassandrathrift")
                || backend_type.equals("cassandra")
                || backend_type.equals("embeddedcassandra")) {
            storage_path = configuration.getString(STORAGE_HOSTNAME_KEY);
        } else if (backend_type.equals("berkeleyje")) {
            storage_path = configuration.getString(STORAGE_DIRECTORY_KEY);
        }

        if (backend_type.equals("hbase")) {
            storage_instance = configuration.getString(STORAGE_HBASE_TABLE_KEY);
        } else if (backend_type.equals("cql")) {
            storage_instance = configuration.getString(STORAGE_CQL_KEYSPACE_KEY);
        } else if (backend_type.equals("cassandra")
                || backend_type.equals("cassandrathrift")) {
            storage_instance = configuration.getString(STORAGE_CASSANDRA_KEYSPACE_KEY);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageBackendKey that = (StorageBackendKey) o;
        return Objects.equals(backend_type, that.backend_type) &&
                Objects.equals(storage_path, that.storage_path) &&
                Objects.equals(storage_instance, that.storage_instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backend_type, storage_path, storage_instance);
    }

    @Override
    public String toString() {
        return "StorageBackendKey{" +
                "backend_type='" + backend_type + '\'' +
                ", storage_path='" + storage_path + '\'' +
                ", storage_instance='" + storage_instance + '\'' +
                '}';
    }
}