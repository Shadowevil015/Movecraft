package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class IntegerProperty implements Property<Integer> {
    private final String fileKey;
    private final NamespacedKey namespacedKey;
    private final Function<CraftType, Integer> defaultProvider;

    /**
     * Construct an IntegerProperty
     * <p>Note: this constructor makes this a required property.
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     */
    public IntegerProperty(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey) {
        this.fileKey = fileKey;
        this.namespacedKey = namespacedKey;
        this.defaultProvider = null;
    }

    /**
     * Construct an IntegerProperty
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     * @param defaultProvider the provider for the default value of this property
     */
    public IntegerProperty(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey, @NotNull Function<CraftType, Integer> defaultProvider) {
        this.fileKey = fileKey;
        this.namespacedKey = namespacedKey;
        this.defaultProvider = defaultProvider;
    }

    /**
     * Load and validate the property from data
     *
     * @param data TypeData to read the property from
     * @param type CrafType to provide to defaultProvider
     * @return the value
     */
    @Nullable
    public Integer load(@NotNull TypeData data, @NotNull CraftType type) {
        try {
            return data.getInt(fileKey);
        }
        catch (TypeData.KeyNotFoundException e) {
            if(defaultProvider == null)
                throw e;

            return defaultProvider.apply(type);
        }
    }

    /**
     * Get the file key for this property
     *
     * @return the file key
     */
    @NotNull
    public String getFileKey() {
        return fileKey;
    }

    /**
     * Get the NamespacedKey for this property
     *
     * @return the NamespacedKey
     */
    @NotNull
    public NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }
}
