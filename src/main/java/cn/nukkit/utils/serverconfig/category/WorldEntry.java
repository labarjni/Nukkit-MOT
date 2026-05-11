package cn.nukkit.utils.serverconfig.category;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;

/**
 * World entry configuration record (Java 17+).
 * Provides immutable data container with automatic equals, hashCode, and toString.
 */
public record WorldEntry(
    @Comment("World generator type (normal, flat, nether, the_end, void)")
    String generator,
    
    @Comment("World seed (0 = random)")
    long seed,
    
    @Comment("Generator settings (e.g. flat layer definition)")
    @CustomKey("generator-settings")
    String generatorSettings
) {
    // Canonical constructor with defaults
    public WorldEntry {
        if (generator == null || generator.isBlank()) {
            generator = "normal";
        }
        if (generatorSettings == null) {
            generatorSettings = "";
        }
    }
    
    // Convenience constructor with defaults
    public WorldEntry() {
        this("normal", 0L, "");
    }
    
    // Builder-style withers for immutable updates
    public WorldEntry withGenerator(String generator) {
        return new WorldEntry(generator != null ? generator : "normal", this.seed, this.generatorSettings);
    }
    
    public WorldEntry withSeed(long seed) {
        return new WorldEntry(this.generator, seed, this.generatorSettings);
    }
    
    public WorldEntry withGeneratorSettings(String generatorSettings) {
        return new WorldEntry(this.generator, this.seed, generatorSettings != null ? generatorSettings : "");
    }
}
