package com.islandium.core.server;

import org.jetbrains.annotations.NotNull;

/**
 * Donnees d'un serveur stocke en BDD.
 */
public class ServerData {

    private final String name;
    private String host;
    private int port;
    private String displayName;
    private final long createdAt;

    public ServerData(@NotNull String name, @NotNull String host, int port, @NotNull String displayName, long createdAt) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getHost() {
        return host;
    }

    public void setHost(@NotNull String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
