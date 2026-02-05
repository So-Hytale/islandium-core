package com.islandium.core.wiki;

import com.islandium.core.IslandiumPlugin;
import com.islandium.core.wiki.model.WikiCategory;
import com.islandium.core.wiki.model.WikiDrop;
import com.islandium.core.wiki.model.WikiEntity;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDrop;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;

import java.util.*;
import java.util.logging.Level;

/**
 * Loads wiki data from Hytale vanilla assets.
 * Scans ItemDropList assets to find all droppable items and their sources.
 * Falls back to default data if no assets are found.
 */
public class WikiDataLoader {

    private final IslandiumPlugin plugin;
    private final List<WikiEntity> entities = new ArrayList<>();
    private final Map<String, List<WikiDrop>> dropListCache = new HashMap<>();

    public WikiDataLoader(IslandiumPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all wiki data from Hytale assets.
     */
    public List<WikiEntity> loadAll() {
        entities.clear();
        dropListCache.clear();

        // Try to load from Hytale vanilla assets
        loadFromItemDropLists();

        // If no entities were loaded, use default data
        if (entities.isEmpty()) {
            plugin.getLogger().at(Level.INFO).log("WikiDataLoader: No assets found, loading default entity data...");
            loadDefaultEntities();
        }

        plugin.getLogger().at(Level.INFO).log("WikiDataLoader: Loaded " + entities.size() + " entities with drops");
        return new ArrayList<>(entities);
    }

    /**
     * Load entities and drops from ItemDropList assets.
     * This scans all registered ItemDropLists in the game.
     */
    private void loadFromItemDropLists() {
        try {
            var assetMap = ItemDropList.getAssetMap();
            if (assetMap == null) {
                plugin.getLogger().at(Level.INFO).log("WikiDataLoader: ItemDropList asset map not available yet");
                return;
            }

            Map<String, ItemDropList> dropLists = assetMap.getAssetMap();
            if (dropLists == null || dropLists.isEmpty()) {
                plugin.getLogger().at(Level.INFO).log("WikiDataLoader: No ItemDropLists found in assets");
                return;
            }

            plugin.getLogger().at(Level.INFO).log("WikiDataLoader: Found " + dropLists.size() + " ItemDropLists to process");

            // Process each drop list and create wiki entities
            for (Map.Entry<String, ItemDropList> entry : dropLists.entrySet()) {
                String dropListId = entry.getKey();
                ItemDropList dropList = entry.getValue();

                if (dropList == null) continue;

                // Parse the drop list ID to extract entity info
                WikiEntity entity = createEntityFromDropList(dropListId, dropList);
                if (entity != null && entity.hasDrops()) {
                    entities.add(entity);
                }
            }

            // Sort entities by category then name
            entities.sort((a, b) -> {
                int catCompare = a.getCategory().compareTo(b.getCategory());
                if (catCompare != 0) return catCompare;
                return a.getDisplayName().compareTo(b.getDisplayName());
            });

        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("WikiDataLoader: Error loading from ItemDropLists: " + e.getMessage());
        }
    }

    /**
     * Load default/example entities for demonstration and testing.
     * These are shown when no asset data is available.
     */
    private void loadDefaultEntities() {
        // ======== HOSTILE MOBS ========
        entities.add(WikiEntity.builder()
                .id("hytale:trork")
                .displayName("Trork")
                .category(WikiCategory.HOSTILE)
                .description("Une creature hostile des forets de Hytale.")
                .health(30)
                .attackDamage(5)
                .addDrop(new WikiDrop.Builder().itemId("hytale:trork_hide").itemName("Peau de Trork").quantity(1, 2).dropChance(0.80).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:trork_tusk").itemName("Defense de Trork").quantity(0, 1).dropChance(0.25).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:raw_meat").itemName("Viande crue").quantity(1, 3).dropChance(0.90).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:kweebec_warrior")
                .displayName("Guerrier Kweebec")
                .category(WikiCategory.HOSTILE)
                .description("Un petit guerrier des bois hostile aux intrus.")
                .health(20)
                .attackDamage(4)
                .addDrop(new WikiDrop.Builder().itemId("hytale:kweebec_spear").itemName("Lance Kweebec").quantity(1, 1).dropChance(0.15).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:wooden_stick").itemName("Baton de bois").quantity(1, 2).dropChance(0.60).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:skeleton")
                .displayName("Squelette")
                .category(WikiCategory.HOSTILE)
                .description("Un archer squelettique qui tire des fleches.")
                .health(20)
                .attackDamage(4)
                .addDrop(new WikiDrop.Builder().itemId("hytale:bone").itemName("Os").quantity(0, 2).dropChance(0.85).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:arrow").itemName("Fleche").quantity(0, 2).dropChance(0.85).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:bow").itemName("Arc").quantity(1, 1).dropChance(0.085).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:zombie")
                .displayName("Zombie")
                .category(WikiCategory.HOSTILE)
                .description("Un mort-vivant qui attaque les joueurs a vue.")
                .health(20)
                .attackDamage(3)
                .addDrop(new WikiDrop.Builder().itemId("hytale:rotten_flesh").itemName("Chair pourrie").quantity(0, 2).dropChance(0.85).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:iron_ingot").itemName("Lingot de fer").quantity(1, 1).dropChance(0.025).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:spider")
                .displayName("Araignee")
                .category(WikiCategory.HOSTILE)
                .description("Une araignee geante qui grimpe aux murs.")
                .health(16)
                .attackDamage(2)
                .addDrop(new WikiDrop.Builder().itemId("hytale:string").itemName("Ficelle").quantity(0, 2).dropChance(0.80).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:spider_eye").itemName("Oeil d'araignee").quantity(0, 1).dropChance(0.33).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:sand_scorpion")
                .displayName("Scorpion des sables")
                .category(WikiCategory.HOSTILE)
                .description("Un scorpion geant du desert.")
                .health(25)
                .attackDamage(6)
                .addDrop(new WikiDrop.Builder().itemId("hytale:scorpion_chitin").itemName("Chitine de scorpion").quantity(1, 3).dropChance(0.70).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:scorpion_stinger").itemName("Dard de scorpion").quantity(0, 1).dropChance(0.20).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:venom_sac").itemName("Sac de venin").quantity(0, 1).dropChance(0.10).build())
                .build());

        // ======== PASSIVE MOBS ========
        entities.add(WikiEntity.builder()
                .id("hytale:sheep")
                .displayName("Mouton")
                .category(WikiCategory.PASSIVE)
                .description("Un ovin paisible qui fournit de la laine.")
                .health(8)
                .addDrop(new WikiDrop.Builder().itemId("hytale:wool").itemName("Laine").quantity(1, 1).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:mutton").itemName("Mouton cru").quantity(1, 2).dropChance(1.0).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:cow")
                .displayName("Vache")
                .category(WikiCategory.PASSIVE)
                .description("Un bovin qui donne du lait et du cuir.")
                .health(10)
                .addDrop(new WikiDrop.Builder().itemId("hytale:leather").itemName("Cuir").quantity(0, 2).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:beef").itemName("Boeuf cru").quantity(1, 3).dropChance(1.0).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:chicken")
                .displayName("Poule")
                .category(WikiCategory.PASSIVE)
                .description("Une volaille qui pond des oeufs.")
                .health(4)
                .addDrop(new WikiDrop.Builder().itemId("hytale:feather").itemName("Plume").quantity(0, 2).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:chicken_meat").itemName("Poulet cru").quantity(1, 1).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:egg").itemName("Oeuf").quantity(1, 1).dropChance(0.05).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:pig")
                .displayName("Cochon")
                .category(WikiCategory.PASSIVE)
                .description("Un animal domestique paisible.")
                .health(10)
                .addDrop(new WikiDrop.Builder().itemId("hytale:porkchop").itemName("Cote de porc").quantity(1, 3).dropChance(1.0).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:deer")
                .displayName("Cerf")
                .category(WikiCategory.PASSIVE)
                .description("Un cervin elegant des forets.")
                .health(12)
                .addDrop(new WikiDrop.Builder().itemId("hytale:venison").itemName("Venaison").quantity(1, 2).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:leather").itemName("Cuir").quantity(0, 2).dropChance(0.80).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:antler").itemName("Bois de cerf").quantity(0, 2).dropChance(0.30).build())
                .build());

        // ======== NEUTRAL MOBS ========
        entities.add(WikiEntity.builder()
                .id("hytale:wolf")
                .displayName("Loup")
                .category(WikiCategory.NEUTRAL)
                .description("Un loup sauvage qui peut etre apprivoise.")
                .health(12)
                .attackDamage(4)
                .addDrop(new WikiDrop.Builder().itemId("hytale:wolf_pelt").itemName("Peau de loup").quantity(0, 1).dropChance(0.50).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:bear")
                .displayName("Ours")
                .category(WikiCategory.NEUTRAL)
                .description("Un ours imposant, dangereux si provoque.")
                .health(30)
                .attackDamage(8)
                .addDrop(new WikiDrop.Builder().itemId("hytale:bear_pelt").itemName("Fourrure d'ours").quantity(1, 1).dropChance(0.80).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:raw_meat").itemName("Viande crue").quantity(2, 4).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:bear_claw").itemName("Griffe d'ours").quantity(0, 2).dropChance(0.25).build())
                .build());

        // ======== BOSS MOBS ========
        entities.add(WikiEntity.builder()
                .id("hytale:void_dragon")
                .displayName("Dragon du Vide")
                .category(WikiCategory.BOSS)
                .description("Un dragon colossal gardien des dimensions.")
                .health(500)
                .attackDamage(25)
                .addDrop(new WikiDrop.Builder().itemId("hytale:dragon_scale").itemName("Ecaille de dragon").quantity(5, 10).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:dragon_heart").itemName("Coeur de dragon").quantity(1, 1).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:void_essence").itemName("Essence du vide").quantity(3, 5).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:legendary_weapon").itemName("Arme legendaire").quantity(1, 1).dropChance(0.05).build())
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:forest_guardian")
                .displayName("Gardien de la foret")
                .category(WikiCategory.BOSS)
                .description("Un esprit ancestral protecteur de la nature.")
                .health(300)
                .attackDamage(15)
                .addDrop(new WikiDrop.Builder().itemId("hytale:ancient_wood").itemName("Bois ancien").quantity(10, 20).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:forest_heart").itemName("Coeur de la foret").quantity(1, 1).dropChance(1.0).build())
                .addDrop(new WikiDrop.Builder().itemId("hytale:nature_essence").itemName("Essence de nature").quantity(2, 4).dropChance(1.0).build())
                .build());

        // ======== NPC ========
        entities.add(WikiEntity.builder()
                .id("hytale:merchant")
                .displayName("Marchand")
                .category(WikiCategory.NPC)
                .description("Un marchand ambulant qui vend des objets varies.")
                .health(20)
                .build());

        entities.add(WikiEntity.builder()
                .id("hytale:blacksmith")
                .displayName("Forgeron")
                .category(WikiCategory.NPC)
                .description("Un artisan qui fabrique des armes et armures.")
                .health(25)
                .build());

        // Sort by category then name
        entities.sort((a, b) -> {
            int catCompare = a.getCategory().compareTo(b.getCategory());
            if (catCompare != 0) return catCompare;
            return a.getDisplayName().compareTo(b.getDisplayName());
        });
    }

    /**
     * Create a WikiEntity from an ItemDropList.
     */
    private WikiEntity createEntityFromDropList(String dropListId, ItemDropList dropList) {
        try {
            ItemDropContainer container = dropList.getContainer();
            if (container == null) return null;

            List<ItemDrop> allDrops = container.getAllDrops(new ArrayList<>());
            if (allDrops == null || allDrops.isEmpty()) return null;

            // Parse entity info from drop list ID
            String entityId = parseEntityIdFromDropList(dropListId);
            String displayName = formatDisplayName(entityId);
            WikiCategory category = guessCategory(dropListId, entityId);

            WikiEntity.Builder builder = WikiEntity.builder()
                    .id(entityId.isEmpty() ? dropListId : entityId)
                    .displayName(displayName.isEmpty() ? formatDisplayName(dropListId) : displayName)
                    .category(category)
                    .description("Source: " + dropListId);

            // Convert ItemDrops to WikiDrops
            for (ItemDrop drop : allDrops) {
                WikiDrop wikiDrop = convertToWikiDrop(drop);
                if (wikiDrop != null) {
                    builder.addDrop(wikiDrop);
                }
            }

            return builder.build();
        } catch (Exception e) {
            plugin.getLogger().at(Level.FINE).log("WikiDataLoader: Could not process droplist " + dropListId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Convert a Hytale ItemDrop to a WikiDrop.
     */
    private WikiDrop convertToWikiDrop(ItemDrop drop) {
        if (drop == null || drop.getItemId() == null) return null;

        String itemId = drop.getItemId();
        String itemName = getItemDisplayName(itemId);

        return new WikiDrop.Builder()
                .itemId(itemId)
                .itemName(itemName)
                .quantity(drop.getQuantityMin(), drop.getQuantityMax())
                .dropChance(1.0)
                .build();
    }

    /**
     * Get display name for an item from its ID.
     */
    private String getItemDisplayName(String itemId) {
        try {
            Item item = Item.getAssetMap().getAsset(itemId);
            if (item != null) {
                return formatDisplayName(itemId);
            }
        } catch (Exception e) {
            // Ignore, use formatted ID
        }
        return formatDisplayName(itemId);
    }

    /**
     * Parse entity ID from a drop list ID.
     */
    private String parseEntityIdFromDropList(String dropListId) {
        if (dropListId == null) return "";

        String id = dropListId;
        if (id.contains(":")) {
            id = id.substring(id.indexOf(":") + 1);
        }

        String[] suffixes = {"_drops", "_loot", "_death", "_kill", "_harvest", "_drop"};
        for (String suffix : suffixes) {
            if (id.toLowerCase().endsWith(suffix)) {
                id = id.substring(0, id.length() - suffix.length());
                break;
            }
        }

        String[] prefixes = {"entity_", "mob_", "npc_", "creature_", "animal_"};
        for (String prefix : prefixes) {
            if (id.toLowerCase().startsWith(prefix)) {
                id = id.substring(prefix.length());
                break;
            }
        }

        return id;
    }

    /**
     * Format an ID into a display name.
     */
    private String formatDisplayName(String id) {
        if (id == null || id.isEmpty()) return "Unknown";

        if (id.contains(":")) {
            id = id.substring(id.indexOf(":") + 1);
        }

        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Guess the category based on drop list ID and entity ID.
     */
    private WikiCategory guessCategory(String dropListId, String entityId) {
        String combined = (dropListId + " " + entityId).toLowerCase();

        if (combined.contains("boss") || combined.contains("dragon") || combined.contains("wither") || combined.contains("guardian")) {
            return WikiCategory.BOSS;
        }
        if (combined.contains("npc") || combined.contains("villager") || combined.contains("merchant") || combined.contains("trader")) {
            return WikiCategory.NPC;
        }
        if (combined.contains("passive") || combined.contains("animal") ||
                combined.contains("pig") || combined.contains("cow") || combined.contains("sheep") ||
                combined.contains("chicken") || combined.contains("rabbit") || combined.contains("fish") || combined.contains("deer")) {
            return WikiCategory.PASSIVE;
        }
        if (combined.contains("neutral") || combined.contains("wolf") || combined.contains("bear") ||
                combined.contains("golem") || combined.contains("enderman")) {
            return WikiCategory.NEUTRAL;
        }
        if (combined.contains("hostile") || combined.contains("mob") || combined.contains("monster") ||
                combined.contains("zombie") || combined.contains("skeleton") || combined.contains("spider") ||
                combined.contains("creeper") || combined.contains("slime") || combined.contains("trork") || combined.contains("scorpion")) {
            return WikiCategory.HOSTILE;
        }

        return WikiCategory.UNKNOWN;
    }

    /**
     * Get cached drops for a drop list ID.
     */
    public List<WikiDrop> getDropsForList(String dropListId) {
        return dropListCache.getOrDefault(dropListId, Collections.emptyList());
    }

    /**
     * Get all loaded entities.
     */
    public List<WikiEntity> getEntities() {
        return new ArrayList<>(entities);
    }

    /**
     * Reload all data.
     */
    public void reload() {
        loadAll();
    }
}
