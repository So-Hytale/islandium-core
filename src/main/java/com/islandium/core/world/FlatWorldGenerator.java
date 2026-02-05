package com.islandium.core.world;

import com.hypixel.hytale.server.core.universe.world.World;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generateur de monde plat personnalise pour Islandium.
 *
 * Structure des couches:
 * - Y = 100: Soil_Grass (surface)
 * - Y = 0 a 99: Rock_Stone (sous-sol)
 */
public class FlatWorldGenerator {

    private static final Logger LOGGER = Logger.getLogger("FlatWorldGenerator");

    // Blocs utilises pour la generation
    public static final String SURFACE_BLOCK = "Soil_Grass";
    public static final String UNDERGROUND_BLOCK = "Rock_Stone";

    // Hauteur de la surface
    public static final int SURFACE_Y = 100;

    // Y minimum (bedrock level)
    public static final int MIN_Y = 0;

    // Taille d'un chunk
    private static final int CHUNK_SIZE = 16;

    // Nombre de blocs par batch pour eviter le lag
    private static final int BLOCKS_PER_BATCH = 4096;

    /**
     * Genere le terrain plat pour une zone donnee autour du spawn.
     *
     * @param world Le monde dans lequel generer
     * @param centerX Coordonnee X du centre
     * @param centerZ Coordonnee Z du centre
     * @param radius Rayon en blocs
     * @return CompletableFuture qui se complete quand la generation est terminee
     */
    public static CompletableFuture<Integer> generateFlatTerrain(World world, int centerX, int centerZ, int radius) {
        return CompletableFuture.supplyAsync(() -> {
            int blocksPlaced = 0;

            int minX = centerX - radius;
            int maxX = centerX + radius;
            int minZ = centerZ - radius;
            int maxZ = centerZ + radius;

            LOGGER.log(Level.INFO, "Generating flat terrain from (" + minX + ", " + minZ + ") to (" + maxX + ", " + maxZ + ")");

            // Generer couche par couche pour optimiser
            for (int y = MIN_Y; y <= SURFACE_Y; y++) {
                String blockType = (y == SURFACE_Y) ? SURFACE_BLOCK : UNDERGROUND_BLOCK;

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        final int fx = x;
                        final int fy = y;
                        final int fz = z;
                        final String fBlockType = blockType;

                        // Executer sur le thread du monde
                        world.execute(() -> {
                            world.setBlock(fx, fy, fz, fBlockType);
                        });

                        blocksPlaced++;
                    }
                }
            }

            LOGGER.log(Level.INFO, "Flat terrain generation complete: " + blocksPlaced + " blocks placed");
            return blocksPlaced;
        });
    }

    /**
     * Genere le terrain plat pour un chunk specifique.
     * Utilise pour la generation a la demande.
     *
     * @param world Le monde
     * @param chunkX Coordonnee X du chunk
     * @param chunkZ Coordonnee Z du chunk
     */
    public static void generateChunk(World world, int chunkX, int chunkZ) {
        int baseX = chunkX * CHUNK_SIZE;
        int baseZ = chunkZ * CHUNK_SIZE;

        world.execute(() -> {
            // Remplir de Rock_Stone de Y=0 a Y=99
            for (int y = MIN_Y; y < SURFACE_Y; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        world.setBlock(baseX + x, y, baseZ + z, UNDERGROUND_BLOCK);
                    }
                }
            }

            // Couche de surface a Y=100 avec Soil_Grass
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    world.setBlock(baseX + x, SURFACE_Y, baseZ + z, SURFACE_BLOCK);
                }
            }
        });
    }

    /**
     * Genere le terrain plat initial autour du spawn.
     * Cette methode est appelee apres la creation d'un nouveau monde flat.
     *
     * @param world Le monde nouvellement cree
     * @param spawnRadius Rayon en chunks autour du spawn a generer
     * @return CompletableFuture avec le nombre de blocs places
     */
    public static CompletableFuture<Integer> generateInitialSpawnArea(World world, int spawnRadius) {
        return CompletableFuture.supplyAsync(() -> {
            int blocksPlaced = 0;
            int chunksGenerated = 0;

            LOGGER.log(Level.INFO, "Generating initial spawn area with radius " + spawnRadius + " chunks");

            // Generer les chunks dans un carre autour de (0, 0)
            for (int cx = -spawnRadius; cx <= spawnRadius; cx++) {
                for (int cz = -spawnRadius; cz <= spawnRadius; cz++) {
                    int baseX = cx * CHUNK_SIZE;
                    int baseZ = cz * CHUNK_SIZE;

                    // Remplir tout le chunk
                    final int fBaseX = baseX;
                    final int fBaseZ = baseZ;

                    world.execute(() -> {
                        // Rock_Stone de Y=0 a Y=99
                        for (int y = MIN_Y; y < SURFACE_Y; y++) {
                            for (int x = 0; x < CHUNK_SIZE; x++) {
                                for (int z = 0; z < CHUNK_SIZE; z++) {
                                    world.setBlock(fBaseX + x, y, fBaseZ + z, UNDERGROUND_BLOCK);
                                }
                            }
                        }

                        // Soil_Grass a Y=100
                        for (int x = 0; x < CHUNK_SIZE; x++) {
                            for (int z = 0; z < CHUNK_SIZE; z++) {
                                world.setBlock(fBaseX + x, SURFACE_Y, fBaseZ + z, SURFACE_BLOCK);
                            }
                        }
                    });

                    chunksGenerated++;
                    blocksPlaced += CHUNK_SIZE * CHUNK_SIZE * (SURFACE_Y - MIN_Y + 1);
                }
            }

            LOGGER.log(Level.INFO, "Initial spawn area generated: " + chunksGenerated + " chunks, ~" + blocksPlaced + " blocks");
            return blocksPlaced;
        });
    }

    /**
     * Configuration pour les presets de monde plat.
     */
    public static class FlatWorldPreset {
        private final String name;
        private final String surfaceBlock;
        private final String undergroundBlock;
        private final int surfaceY;

        public FlatWorldPreset(String name, String surfaceBlock, String undergroundBlock, int surfaceY) {
            this.name = name;
            this.surfaceBlock = surfaceBlock;
            this.undergroundBlock = undergroundBlock;
            this.surfaceY = surfaceY;
        }

        public String getName() { return name; }
        public String getSurfaceBlock() { return surfaceBlock; }
        public String getUndergroundBlock() { return undergroundBlock; }
        public int getSurfaceY() { return surfaceY; }

        // Presets predefinies
        public static final FlatWorldPreset GRASS = new FlatWorldPreset("grass", "Soil_Grass", "Rock_Stone", 100);
        public static final FlatWorldPreset STONE = new FlatWorldPreset("stone", "Rock_Stone", "Rock_Stone", 100);
        public static final FlatWorldPreset SAND = new FlatWorldPreset("sand", "Soil_Sand", "Rock_Sandstone", 100);
        public static final FlatWorldPreset SNOW = new FlatWorldPreset("snow", "Soil_Snow", "Rock_Stone", 100);

        public static FlatWorldPreset fromString(String preset) {
            return switch (preset.toLowerCase()) {
                case "grass" -> GRASS;
                case "stone" -> STONE;
                case "sand" -> SAND;
                case "snow" -> SNOW;
                default -> GRASS;
            };
        }
    }

    /**
     * Genere un monde plat avec un preset specifique.
     *
     * @param world Le monde
     * @param preset Le preset a utiliser
     * @param spawnRadius Rayon en chunks
     * @return CompletableFuture avec le nombre de blocs places
     */
    public static CompletableFuture<Integer> generateWithPreset(World world, FlatWorldPreset preset, int spawnRadius) {
        return CompletableFuture.supplyAsync(() -> {
            int blocksPlaced = 0;

            LOGGER.log(Level.INFO, "Generating flat world with preset: " + preset.getName());
            LOGGER.log(Level.INFO, "Surface: " + preset.getSurfaceBlock() + " at Y=" + preset.getSurfaceY());
            LOGGER.log(Level.INFO, "Underground: " + preset.getUndergroundBlock() + " from Y=0 to Y=" + (preset.getSurfaceY() - 1));

            for (int cx = -spawnRadius; cx <= spawnRadius; cx++) {
                for (int cz = -spawnRadius; cz <= spawnRadius; cz++) {
                    int baseX = cx * CHUNK_SIZE;
                    int baseZ = cz * CHUNK_SIZE;

                    final int fBaseX = baseX;
                    final int fBaseZ = baseZ;

                    world.execute(() -> {
                        // Underground blocks from Y=0 to surfaceY-1
                        for (int y = MIN_Y; y < preset.getSurfaceY(); y++) {
                            for (int x = 0; x < CHUNK_SIZE; x++) {
                                for (int z = 0; z < CHUNK_SIZE; z++) {
                                    world.setBlock(fBaseX + x, y, fBaseZ + z, preset.getUndergroundBlock());
                                }
                            }
                        }

                        // Surface block at surfaceY
                        for (int x = 0; x < CHUNK_SIZE; x++) {
                            for (int z = 0; z < CHUNK_SIZE; z++) {
                                world.setBlock(fBaseX + x, preset.getSurfaceY(), fBaseZ + z, preset.getSurfaceBlock());
                            }
                        }
                    });

                    blocksPlaced += CHUNK_SIZE * CHUNK_SIZE * (preset.getSurfaceY() - MIN_Y + 1);
                }
            }

            return blocksPlaced;
        });
    }
}
