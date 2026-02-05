package com.islandium.core.config;

import com.islandium.core.api.util.ColorUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.Message;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration des messages (messages.yml).
 */
public class MessagesConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private Map<String, String> messages;

    public MessagesConfig(@NotNull Path path) {
        this.path = path;
    }

    @SuppressWarnings("unchecked")
    public void load() throws IOException {
        if (Files.exists(path)) {
            String content = Files.readString(path);
            this.messages = GSON.fromJson(content, Map.class);
        } else {
            this.messages = createDefault();
            save();
        }
    }

    public void save() throws IOException {
        Files.writeString(path, GSON.toJson(messages));
    }

    private Map<String, String> createDefault() {
        Map<String, String> msgs = new HashMap<>();

        // Prefix
        msgs.put("prefix", "&8[&6ISLANDIUM&8] &f");

        // General
        msgs.put("no-permission", "&cTu n'as pas la permission!");
        msgs.put("player-only", "&cCette commande est réservée aux joueurs!");
        msgs.put("player-not-found", "&cJoueur &e{player}&c introuvable!");
        msgs.put("player-offline", "&cLe joueur &e{player}&c n'est pas connecté!");
        msgs.put("invalid-number", "&c{value} n'est pas un nombre valide!");

        // Home
        msgs.put("home.set", "&aHome &e{name}&a défini!");
        msgs.put("home.deleted", "&cHome &e{name}&c supprimé.");
        msgs.put("home.teleported", "&aTéléportation vers &e{name}&a...");
        msgs.put("home.not-found", "&cHome &e{name}&c introuvable.");
        msgs.put("home.list", "&6Tes homes: &e{homes}");
        msgs.put("home.list-empty", "&cTu n'as aucun home.");
        msgs.put("home.limit-reached", "&cTu as atteint la limite de homes! ({current}/{max})");

        // Warp
        msgs.put("warp.set", "&aWarp &e{name}&a créé!");
        msgs.put("warp.deleted", "&cWarp &e{name}&c supprimé.");
        msgs.put("warp.teleported", "&aTéléportation vers &e{name}&a...");
        msgs.put("warp.not-found", "&cWarp &e{name}&c introuvable.");
        msgs.put("warp.list", "&6Warps: &e{warps}");
        msgs.put("warp.list-empty", "&cAucun warp disponible.");
        msgs.put("warp.already-exists", "&cLe warp &e{name}&c existe déjà!");

        // Spawn
        msgs.put("spawn.set", "&aSpawn défini à &e{x}&a, &e{y}&a, &e{z}&a (&e{world}&a)!");
        msgs.put("spawn.teleported", "&aTéléportation au spawn...");
        msgs.put("spawn.not-set", "&cLe spawn n'est pas défini!");
        msgs.put("spawn.error", "&cErreur lors de la définition du spawn.");

        // Back
        msgs.put("back.no-location", "&cAucune position précédente enregistrée!");
        msgs.put("back.teleporting", "&aTéléportation à ta position précédente...");

        // Teleport (additional)
        msgs.put("teleport.already-pending", "&cUne téléportation est déjà en cours!");

        // Welcome
        msgs.put("welcome.new-player", "&6&l[+] &eBienvenue à &6{player}&e sur le serveur!");

        // TPA
        msgs.put("tpa.sent", "&aRequête TPA envoyée à &e{player}&a.");
        msgs.put("tpa.received", "&e{player}&a veut se téléporter à toi. &e/tpaccept &aou &e/tpdeny");
        msgs.put("tpa.here-received", "&e{player}&a veut que tu te téléportes à lui. &e/tpaccept &aou &e/tpdeny");
        msgs.put("tpa.accepted", "&aRequête TPA acceptée!");
        msgs.put("tpa.accepted-target", "&e{player}&a a accepté ta requête TPA!");
        msgs.put("tpa.denied", "&cRequête TPA refusée.");
        msgs.put("tpa.denied-target", "&e{player}&c a refusé ta requête TPA.");
        msgs.put("tpa.expired", "&cLa requête TPA a expiré.");
        msgs.put("tpa.no-pending", "&cAucune requête TPA en attente.");
        msgs.put("tpa.already-pending", "&cTu as déjà une requête en attente vers &e{player}&c!");
        msgs.put("tpa.self", "&cTu ne peux pas envoyer une requête TPA à toi-même!");

        // Teleport
        msgs.put("teleport.warmup", "&aTéléportation dans &e{seconds}&a secondes. Ne bouge pas!");
        msgs.put("teleport.cancelled", "&cTéléportation annulée!");
        msgs.put("teleport.cooldown", "&cTu dois attendre &e{seconds}&c secondes avant de te téléporter!");
        msgs.put("teleport.success", "&aTéléportation réussie!");
        msgs.put("teleport.to-player", "&aTéléportation vers &e{player}&a...");

        // Admin
        msgs.put("fly.enabled", "&aMode vol activé!");
        msgs.put("fly.disabled", "&cMode vol désactivé.");
        msgs.put("fly.enabled-other", "&aMode vol activé pour &e{player}&a!");
        msgs.put("fly.disabled-other", "&cMode vol désactivé pour &e{player}&c.");

        msgs.put("vanish.enabled", "&aVanish activé! Tu es invisible.");
        msgs.put("vanish.disabled", "&cVanish désactivé. Tu es visible.");

        msgs.put("god.enabled", "&aMode Dieu activé! Tu es invincible.");
        msgs.put("god.disabled", "&cMode Dieu désactivé.");

        msgs.put("heal.healed", "&aTu as été soigné!");
        msgs.put("heal.healed-other", "&e{player}&a a été soigné!");

        msgs.put("feed.fed", "&aTu as été nourri!");
        msgs.put("feed.fed-other", "&e{player}&a a été nourri!");

        msgs.put("gamemode.changed", "&aMode de jeu changé en &e{mode}&a!");
        msgs.put("gamemode.changed-other", "&aMode de jeu de &e{player}&a changé en &e{mode}&a!");

        // Moderation
        msgs.put("ban.banned", "&cTu as été banni! Raison: &e{reason}");
        msgs.put("ban.temp-banned", "&cTu as été banni temporairement! Durée: &e{duration}&c. Raison: &e{reason}");
        msgs.put("ban.success", "&e{player}&c a été banni!");
        msgs.put("ban.already-banned", "&e{player}&c est déjà banni!");
        msgs.put("unban.success", "&e{player}&a a été débanni!");
        msgs.put("unban.not-banned", "&e{player}&c n'est pas banni.");

        msgs.put("mute.muted", "&cTu as été muté! Raison: &e{reason}");
        msgs.put("mute.temp-muted", "&cTu as été muté temporairement! Durée: &e{duration}&c. Raison: &e{reason}");
        msgs.put("mute.success", "&e{player}&c a été muté!");
        msgs.put("mute.already-muted", "&e{player}&c est déjà muté!");
        msgs.put("mute.cannot-chat", "&cTu es muté et ne peux pas parler!");
        msgs.put("unmute.success", "&e{player}&a a été démuté!");
        msgs.put("unmute.not-muted", "&e{player}&c n'est pas muté.");

        msgs.put("kick.kicked", "&cTu as été expulsé! Raison: &e{reason}");
        msgs.put("kick.success", "&e{player}&c a été expulsé!");

        // Economy
        msgs.put("balance.self", "&6Ton solde: &e{balance}");
        msgs.put("balance.other", "&6Solde de &e{player}&6: &e{balance}");
        msgs.put("pay.success", "&aTu as envoyé &e{amount}&a à &e{player}&a!");
        msgs.put("pay.received", "&aTu as reçu &e{amount}&a de &e{player}&a!");
        msgs.put("pay.not-enough", "&cTu n'as pas assez d'argent! (Solde: &e{balance}&c)");
        msgs.put("pay.self", "&cTu ne peux pas t'envoyer de l'argent!");
        msgs.put("pay.invalid-amount", "&cMontant invalide!");

        // Kit
        msgs.put("kit.received", "&aKit &e{kit}&a reçu!");
        msgs.put("kit.not-found", "&cKit &e{kit}&c introuvable.");
        msgs.put("kit.cooldown", "&cTu dois attendre &e{time}&c avant de réutiliser ce kit!");
        msgs.put("kit.list", "&6Kits disponibles: &e{kits}");
        msgs.put("kit.list-empty", "&cAucun kit disponible.");

        // AFK
        msgs.put("afk.now-afk", "&e{player}&7 est maintenant AFK.");
        msgs.put("afk.no-longer-afk", "&e{player}&7 n'est plus AFK.");

        // Messages
        msgs.put("msg.format-sent", "&7[Moi -> &e{player}&7] &f{message}");
        msgs.put("msg.format-received", "&7[&e{player}&7 -> Moi] &f{message}");
        msgs.put("msg.no-reply", "&cPersonne à qui répondre!");

        // Seen
        msgs.put("seen.online", "&aLe joueur &e{player}&a est en ligne sur &e{server}&a.");
        msgs.put("seen.offline", "&cLe joueur &e{player}&c était en ligne pour la dernière fois &e{last-seen}&c.");

        // Up
        msgs.put("up.success", "&aTu as été téléporté de &e{height}&a bloc(s) vers le haut!");
        msgs.put("up.failed", "&cImpossible de te téléporter vers le haut.");

        // TP
        msgs.put("tp.teleported-pos", "&aTéléporté aux coordonnées &e{x}&a, &e{y}&a, &e{z}&a!");
        msgs.put("tp.failed", "&cÉchec de la téléportation.");

        // Server
        msgs.put("server.current", "&6Tu es actuellement sur: &e{server}");
        msgs.put("server.connecting", "&aConnexion au serveur &e{server}&a...");
        msgs.put("server.already-connected", "&cTu es déjà sur le serveur &e{server}&c!");
        msgs.put("server.not-found", "&cServeur &e{server}&c introuvable.");
        msgs.put("server.no-servers", "&cAucun serveur configuré.");
        msgs.put("server.list-header", "&6Serveurs disponibles:");
        msgs.put("server.list-entry", "  &7- &e{name} &7(&f{display}&7)");
        msgs.put("server.list-entry-current", "  &7- &a{name} &7(&f{display}&7) &a← actuel");

        return msgs;
    }

    /**
     * Récupère un message.
     *
     * @param key la clé du message
     * @return le message ou la clé si non trouvé
     */
    @NotNull
    public String get(@NotNull String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * Récupère un message formaté avec des placeholders.
     *
     * @param key la clé du message
     * @param replacements les remplacements (clé, valeur, clé, valeur, ...)
     * @return le message formaté
     */
    @NotNull
    public String get(@NotNull String key, Object... replacements) {
        String message = get(key);

        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = "{" + replacements[i] + "}";
            String value = String.valueOf(replacements[i + 1]);
            message = message.replace(placeholder, value);
        }

        return message;
    }

    /**
     * Récupère un message avec le préfixe.
     */
    @NotNull
    public String getPrefixed(@NotNull String key) {
        return get("prefix") + get(key);
    }

    /**
     * Récupère un message formaté avec le préfixe.
     */
    @NotNull
    public String getPrefixed(@NotNull String key, Object... replacements) {
        return get("prefix") + get(key, replacements);
    }

    // === Méthodes Hytale Message ===

    /**
     * Récupère un message Hytale coloré.
     *
     * @param key la clé du message
     * @return le Message Hytale avec couleurs
     */
    @NotNull
    public Message getMessage(@NotNull String key) {
        return ColorUtil.parse(get(key));
    }

    /**
     * Récupère un message Hytale coloré avec des placeholders.
     *
     * @param key la clé du message
     * @param replacements les remplacements (clé, valeur, clé, valeur, ...)
     * @return le Message Hytale avec couleurs
     */
    @NotNull
    public Message getMessage(@NotNull String key, Object... replacements) {
        return ColorUtil.parse(get(key, replacements));
    }

    /**
     * Récupère un message Hytale coloré avec le préfixe.
     *
     * @param key la clé du message
     * @return le Message Hytale avec préfixe et couleurs
     */
    @NotNull
    public Message getMessagePrefixed(@NotNull String key) {
        return ColorUtil.parse(getPrefixed(key));
    }

    /**
     * Récupère un message Hytale coloré avec le préfixe et des placeholders.
     *
     * @param key la clé du message
     * @param replacements les remplacements (clé, valeur, clé, valeur, ...)
     * @return le Message Hytale avec préfixe et couleurs
     */
    @NotNull
    public Message getMessagePrefixed(@NotNull String key, Object... replacements) {
        return ColorUtil.parse(getPrefixed(key, replacements));
    }
}
