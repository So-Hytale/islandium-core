package com.islandium.core.api.location;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Représente une location sur un serveur (cross-server).
 */
public record ServerLocation(
        @NotNull String server,
        @NotNull String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    /**
     * Crée une ServerLocation.
     */
    public static ServerLocation of(
            @NotNull String server,
            @NotNull String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        return new ServerLocation(server, world, x, y, z, yaw, pitch);
    }

    /**
     * Crée une ServerLocation sans rotation.
     */
    public static ServerLocation of(
            @NotNull String server,
            @NotNull String world,
            double x,
            double y,
            double z
    ) {
        return new ServerLocation(server, world, x, y, z, 0f, 0f);
    }

    /**
     * @return le serveur
     */
    public String getServer() {
        return server;
    }

    /**
     * @return le monde
     */
    public String getWorld() {
        return world;
    }

    /**
     * @return la coordonnée X
     */
    public double getX() {
        return x;
    }

    /**
     * @return la coordonnée Y
     */
    public double getY() {
        return y;
    }

    /**
     * @return la coordonnée Z
     */
    public double getZ() {
        return z;
    }

    /**
     * @return le yaw (rotation horizontale)
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * @return le pitch (rotation verticale)
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Sérialise la location en string.
     */
    public String serialize() {
        return server + ":" + world + ":" + x + ":" + y + ":" + z + ":" + yaw + ":" + pitch;
    }

    /**
     * Désérialise une location depuis un string.
     */
    @Nullable
    public static ServerLocation deserialize(@NotNull String str) {
        try {
            String[] parts = str.split(":");
            if (parts.length < 5) return null;

            String server = parts[0];
            String world = parts[1];
            double x = Double.parseDouble(parts[2]);
            double y = Double.parseDouble(parts[3]);
            double z = Double.parseDouble(parts[4]);
            float yaw = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            float pitch = parts.length > 6 ? Float.parseFloat(parts[6]) : 0f;

            return new ServerLocation(server, world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "ServerLocation{" +
                "server='" + server + '\'' +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }
}
