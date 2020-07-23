package org.apache.tinkerpop.gremlin.server.util;

import org.apache.commons.configuration.Configuration;

import java.util.Objects;

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