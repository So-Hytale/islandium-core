package com.islandium.core.service.economy;

import com.islandium.core.api.economy.EconomyService;
import com.islandium.core.api.economy.Transaction;
import com.islandium.core.api.economy.TransactionType;
import com.islandium.core.IslandiumPlugin;
import com.islandium.core.database.repository.PlayerRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implémentation du service d'économie.
 */
public class EconomyServiceImpl implements EconomyService {

    private final IslandiumPlugin plugin;
    private final PlayerRepository playerRepository;

    public EconomyServiceImpl(@NotNull IslandiumPlugin plugin, @NotNull PlayerRepository playerRepository) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(@NotNull UUID playerUuid) {
        var onlinePlayerOpt = plugin.getPlayerManager().getOnlinePlayer(playerUuid);
        if (onlinePlayerOpt.isPresent()) {
            return CompletableFuture.completedFuture(onlinePlayerOpt.get().getBalance());
        }

        return playerRepository.findById(playerUuid)
                .thenApply(opt -> opt.map(data -> data.balance())
                        .orElse(getStartingBalance()));
    }

    @Override
    public CompletableFuture<Void> setBalance(@NotNull UUID playerUuid, @NotNull BigDecimal amount) {
        var onlinePlayerOpt = plugin.getPlayerManager().getOnlinePlayer(playerUuid);
        if (onlinePlayerOpt.isPresent()) {
            return onlinePlayerOpt.get().setBalance(amount);
        }

        return playerRepository.updateBalance(playerUuid, amount);
    }

    @Override
    public CompletableFuture<Void> addBalance(
            @NotNull UUID playerUuid,
            @NotNull BigDecimal amount,
            @Nullable String description
    ) {
        return getBalance(playerUuid).thenCompose(balance -> {
            BigDecimal newBalance = balance.add(amount);
            return setBalance(playerUuid, newBalance).thenCompose(v ->
                    logTransaction(null, playerUuid, amount, TransactionType.ADMIN_ADD, description)
            );
        });
    }

    @Override
    public CompletableFuture<Boolean> removeBalance(
            @NotNull UUID playerUuid,
            @NotNull BigDecimal amount,
            @Nullable String description
    ) {
        return getBalance(playerUuid).thenCompose(balance -> {
            if (balance.compareTo(amount) < 0) {
                return CompletableFuture.completedFuture(false);
            }
            BigDecimal newBalance = balance.subtract(amount);
            return setBalance(playerUuid, newBalance).thenCompose(v ->
                    logTransaction(playerUuid, null, amount, TransactionType.ADMIN_REMOVE, description)
                            .thenApply(x -> true)
            );
        });
    }

    @Override
    public CompletableFuture<Boolean> hasBalance(@NotNull UUID playerUuid, @NotNull BigDecimal amount) {
        return getBalance(playerUuid).thenApply(balance -> balance.compareTo(amount) >= 0);
    }

    @Override
    public CompletableFuture<Boolean> transfer(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull BigDecimal amount,
            @Nullable String description
    ) {
        return hasBalance(from, amount).thenCompose(hasEnough -> {
            if (!hasEnough) {
                return CompletableFuture.completedFuture(false);
            }

            return getBalance(from).thenCompose(fromBalance -> {
                BigDecimal newFromBalance = fromBalance.subtract(amount);
                return setBalance(from, newFromBalance);
            }).thenCompose(v ->
                    getBalance(to).thenCompose(toBalance -> {
                        BigDecimal newToBalance = toBalance.add(amount);
                        return setBalance(to, newToBalance);
                    })
            ).thenCompose(v ->
                    logTransaction(from, to, amount, TransactionType.TRANSFER, description)
                            .thenApply(x -> true)
            );
        });
    }

    @Override
    public CompletableFuture<List<UUID>> getTopPlayers(int limit) {
        return playerRepository.getTopByBalance(limit);
    }

    @Override
    public CompletableFuture<List<Transaction>> getTransactions(@NotNull UUID playerUuid, int limit) {
        return plugin.getDatabaseManager().getExecutor().queryList("""
            SELECT * FROM essentials_transactions
            WHERE from_uuid = ? OR to_uuid = ?
            ORDER BY created_at DESC
            LIMIT ?
        """, rs -> {
            try {
                String fromUuidStr = rs.getString("from_uuid");
                String toUuidStr = rs.getString("to_uuid");

                return new Transaction(
                        rs.getLong("id"),
                        fromUuidStr != null ? UUID.fromString(fromUuidStr) : null,
                        toUuidStr != null ? UUID.fromString(toUuidStr) : null,
                        rs.getBigDecimal("amount"),
                        TransactionType.valueOf(rs.getString("type")),
                        rs.getString("description"),
                        rs.getLong("created_at")
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, playerUuid.toString(), playerUuid.toString(), limit);
    }

    private CompletableFuture<Void> logTransaction(
            @Nullable UUID from,
            @Nullable UUID to,
            @NotNull BigDecimal amount,
            @NotNull TransactionType type,
            @Nullable String description
    ) {
        return plugin.getDatabaseManager().getExecutor().execute("""
            INSERT INTO essentials_transactions (from_uuid, to_uuid, amount, type, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """,
                from != null ? from.toString() : null,
                to != null ? to.toString() : null,
                amount,
                type.name(),
                description,
                System.currentTimeMillis()
        );
    }

    @Override
    @NotNull
    public BigDecimal getStartingBalance() {
        return plugin.getConfigManager().getMainConfig().getStartingBalance();
    }

    @Override
    @NotNull
    public String getCurrencySymbol() {
        return plugin.getConfigManager().getMainConfig().getCurrencySymbol();
    }

    @Override
    @NotNull
    public String getCurrencyName() {
        return plugin.getConfigManager().getMainConfig().getCurrencyName();
    }

    @Override
    @NotNull
    public String format(@NotNull BigDecimal amount) {
        return getCurrencySymbol() + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
