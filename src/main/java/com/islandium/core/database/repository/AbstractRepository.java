package com.islandium.core.database.repository;

import com.islandium.core.database.SQLExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Classe de base pour les repositories.
 */
public abstract class AbstractRepository<T, ID> implements Repository<T, ID> {

    protected final SQLExecutor sql;

    protected AbstractRepository(@NotNull SQLExecutor sql) {
        this.sql = sql;
    }

    /**
     * @return le nom de la table
     */
    protected abstract String getTableName();
}
