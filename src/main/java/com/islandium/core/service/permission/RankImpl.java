package com.islandium.core.service.permission;

import com.islandium.core.api.permission.Rank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation de Rank.
 */
public class RankImpl implements Rank {

    private int id;
    private final String name;
    private String displayName;
    private String prefix;
    private String color;
    private int priority;
    private Integer parentId;
    private Rank parent;
    private boolean isDefault;
    private final long createdAt;
    private Set<String> directPermissions;

    public RankImpl(
            int id,
            @NotNull String name,
            @NotNull String displayName,
            @Nullable String prefix,
            @Nullable String color,
            int priority,
            @Nullable Integer parentId,
            boolean isDefault,
            long createdAt
    ) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.prefix = prefix;
        this.color = color != null ? color : "#FFFFFF";
        this.priority = priority;
        this.parentId = parentId;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.directPermissions = new HashSet<>();
    }

    /**
     * Constructeur pour creation de nouveau rank.
     */
    public RankImpl(
            @NotNull String name,
            @NotNull String displayName,
            @Nullable String prefix,
            @Nullable String color,
            int priority
    ) {
        this(0, name, displayName, prefix, color, priority, null, false, System.currentTimeMillis());
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
    }

    @Override
    @Nullable
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(@Nullable String prefix) {
        this.prefix = prefix;
    }

    @Override
    @NotNull
    public String getColor() {
        return color;
    }

    public void setColor(@NotNull String color) {
        this.color = color;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    @Nullable
    public Rank getParent() {
        return parent;
    }

    public void setParent(@Nullable Rank parent) {
        this.parent = parent;
        this.parentId = parent != null ? parent.getId() : null;
    }

    @Override
    @Nullable
    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(@Nullable Integer parentId) {
        this.parentId = parentId;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    @NotNull
    public Set<String> getDirectPermissions() {
        return new HashSet<>(directPermissions);
    }

    public void setDirectPermissions(@NotNull Set<String> permissions) {
        this.directPermissions = new HashSet<>(permissions);
    }

    public void addDirectPermission(@NotNull String permission) {
        this.directPermissions.add(permission);
    }

    public void removeDirectPermission(@NotNull String permission) {
        this.directPermissions.remove(permission);
    }

    @Override
    @NotNull
    public Set<String> getAllPermissions() {
        Set<String> all = new HashSet<>(directPermissions);
        if (parent != null) {
            all.addAll(parent.getAllPermissions());
        }
        return all;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        // Verifier la permission exacte
        if (directPermissions.contains(permission)) {
            return true;
        }

        // Verifier le wildcard global
        if (directPermissions.contains("*")) {
            return true;
        }

        // Verifier les wildcards partiels (ex: essentials.* pour essentials.home)
        String[] parts = permission.split("\\.");
        StringBuilder wildcard = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                wildcard.append(".");
            }
            wildcard.append(parts[i]);
            if (directPermissions.contains(wildcard + ".*")) {
                return true;
            }
        }

        // Verifier le parent
        if (parent != null) {
            return parent.hasPermission(permission);
        }

        return false;
    }

    @Override
    public String toString() {
        return "RankImpl{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", priority=" + priority +
                ", parentId=" + parentId +
                ", isDefault=" + isDefault +
                ", permissions=" + directPermissions.size() +
                '}';
    }
}
