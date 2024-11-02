package com.bergerkiller.bukkit.common.internal.logic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.logging.Level;

import com.bergerkiller.mountiplex.reflection.declarations.TypeDeclaration;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.bases.ExtendedEntity;
import com.bergerkiller.bukkit.common.conversion.type.HandleConversion;
import com.bergerkiller.bukkit.common.internal.hooks.EntityTrackerEntryHook;
import com.bergerkiller.bukkit.common.internal.hooks.EntityTrackerEntryHook_1_14;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.wrappers.EntityTracker;
import com.bergerkiller.generated.net.minecraft.server.level.EntityTrackerEntryHandle;
import com.bergerkiller.generated.net.minecraft.server.level.EntityTrackerEntryStateHandle;
import com.bergerkiller.generated.net.minecraft.server.level.WorldServerHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.EntityTypesHandle;
import com.bergerkiller.mountiplex.MountiplexUtil;
import com.bergerkiller.mountiplex.reflection.ClassTemplate;
import com.bergerkiller.mountiplex.reflection.declarations.Template;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;
import com.bergerkiller.mountiplex.reflection.util.NullInstantiator;
import com.bergerkiller.mountiplex.reflection.util.asm.MPLType;

/**
 * Since Minecraft 1.14 it has become much harder to know what NMS Entity
 * Class is spawned for various Entity Types. This class tracks
 * all these in a map.
 */
class EntityTypingHandler_1_14 extends EntityTypingHandler {
    private final IdentityHashMap<Object, Class<?>> _cache = new IdentityHashMap<Object, Class<?>>();
    private final Handler _handler;
    private final Object nmsWorldHandle;

    // Initialize findEntityTypesClass which is a fallback for types we did not pre-register
    public EntityTypingHandler_1_14() {
        this._handler = Template.Class.create(Handler.class, Common.TEMPLATE_RESOLVER);
        this.nmsWorldHandle = WorldServerHandle.T.newInstanceNull();
    }

    @Override
    public void enable() {
        this._handler.forceInitialization();

        // Initialize a dummy field with the sole purpose of constructing an entity without errors
        {
            // Create WorldData instance by null-constructing it
            Object nmsWorldData;
            Class<?> worldDataServerType = CommonUtil.getClass("net.minecraft.world.level.storage.WorldDataServer");
            if (Common.evaluateMCVersion(">=", "1.16")) {
                nmsWorldData = NullInstantiator.of(worldDataServerType).create();
            } else {
                nmsWorldData = ClassTemplate.create(worldDataServerType).getConstructor().newInstance();
            }

            this._handler.initWorldServer(this.nmsWorldHandle, nmsWorldData);
        }

        // Pre-register certain classes that cause events to be fired when constructing
        registerEntityTypes("AREA_EFFECT_CLOUD", "net.minecraft.world.entity.EntityAreaEffectCloud");
        registerEntityTypes("ENDER_DRAGON", "net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon");
        registerEntityTypes("FIREBALL", "net.minecraft.world.entity.projectile.EntityLargeFireball");
        registerEntityTypes("FISHING_BOBBER", "net.minecraft.world.entity.projectile.EntityFishingHook");
        registerEntityTypes("LIGHTNING_BOLT", "net.minecraft.world.entity.EntityLightning");
        registerEntityTypes("PLAYER", "net.minecraft.server.level.EntityPlayer");
        registerEntityTypes("WITHER", "net.minecraft.world.entity.boss.wither.EntityWither"); // scoreboard things

        // Go by all static fields in the EntityTypes class and decode the generic type information
        // of the fields to figure out what type is represented. This might fail if generics are stripped
        // during compiling, but most of the time this works fine.
        for (Field f : EntityTypesHandle.T.getType().getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            TypeDeclaration typeDec = TypeDeclaration.fromType(f.getGenericType());
            if (!EntityTypesHandle.T.getType().isAssignableFrom(typeDec.type)) {
                continue;
            }
            if (typeDec.genericTypes.length != 1) {
                continue;
            }
            Object nmsEntityTypes;
            try {
                if (!Modifier.isPublic(f.getModifiers())) {
                    f.setAccessible(true);
                }
                nmsEntityTypes = f.get(null);
            } catch (Throwable t) {
                continue;
            }

            // If not already registered, register it
            if (!_cache.containsKey(nmsEntityTypes)) {
                _cache.put(nmsEntityTypes, typeDec.genericTypes[0].type);
            }
        }
    }

    @Override
    public void disable() {
    }

    private void registerEntityTypes(String name, String nmsClassName) {
        String realName = Resolver.resolveFieldName(EntityTypesHandle.T.getType(), name);
        String s = name.equals(realName) ? name : (name + ":" + realName);
        try {
            java.lang.reflect.Field field = MPLType.getDeclaredField(EntityTypesHandle.T.getType(), realName);
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                throw new IllegalStateException("EntityTypes field " + s + " is not static");
            }

            Object nmsEntityTypes;
            if ((field.getModifiers() & Modifier.PUBLIC) == 0) {
                field.setAccessible(true);
                nmsEntityTypes = field.get(null);
                field.setAccessible(false);
            } else {
                nmsEntityTypes = field.get(null);
            }

            Class<?> type = CommonUtil.getClass(nmsClassName);
            if (type == null) {
                throw new IllegalStateException("EntityTypes type " + nmsClassName + " not found");
            }

            this._cache.put(nmsEntityTypes, type);
        } catch (Throwable t) {
            MountiplexUtil.LOGGER.log(Level.SEVERE, "Failed to find EntityTypes field " + s, t);
        }
    }

    @Override
    public Class<?> getClassFromEntityTypes(Object nmsEntityTypesInstance) {
        Class<?> result = this._cache.get(nmsEntityTypesInstance);
        if (result == null) {
            result = _handler.findClassFromEntityTypes(nmsEntityTypesInstance, this.nmsWorldHandle);
            this._cache.put(nmsEntityTypesInstance, result);
        }
        return result;
    }

    @Override
    public EntityTrackerEntryHandle createEntityTrackerEntry(EntityTracker entityTracker, Entity entity) {
        Object handle = _handler.createEntry(entityTracker.getRawHandle(), HandleConversion.toEntityHandle(entity));
        EntityTrackerEntryHandle entry = EntityTrackerEntryHandle.createHandle(handle);

        // Set the passengers field to the current passengers
        EntityTrackerEntryStateHandle.T.opt_passengers.set(entry.getState().getRaw(), (new ExtendedEntity<Entity>(entity)).getPassengers());

        return entry;
    }

    @Override
    public EntityTrackerEntryHook getEntityTrackerEntryHook(Object entityTrackerEntryHandle) {
        return EntityTrackerEntryHook_1_14.get(entityTrackerEntryHandle, EntityTrackerEntryHook_1_14.class);
    }

    @Override
    public Object hookEntityTrackerEntry(Object entityTrackerEntryHandle) {
        return new EntityTrackerEntryHook_1_14().hook(entityTrackerEntryHandle);
    }

    @Template.Package("net.minecraft.server.level")
    @Template.Import("net.minecraft.world.entity.EntityTypes")
    @Template.Import("net.minecraft.world.level.storage.WorldDataServer")
    @Template.Import("net.minecraft.world.entity.Entity")
    @Template.Import("net.minecraft.world.level.World")
    @Template.Import("net.minecraft.core.registries.BuiltInRegistries")
    @Template.Import("net.minecraft.resources.MinecraftKey")
    public static abstract class Handler extends Template.Class<Template.Handle> {

        /*
         * <CLASS_FROM_ENTITYTYPES>
         * public static Class<?> findClassFromEntityTypes((Object) EntityTypes entityTypes, (Object) World world) {
         *     Object entity;
         *     try {
         * #if version >= 1.21.2
         *         entity = entityTypes.create(world, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
         * #elseif version >= 1.18
         *         entity = entityTypes.create(world);
         * #else
         *         entity = entityTypes.a(world);
         * #endif
         *     } catch (Throwable t) {
         *         // Try to find some descriptive name for this entity type
         *         // This could fail if the type is not registered
         *         MinecraftKey name = (MinecraftKey) BuiltInRegistries.ENTITY_TYPE.getKey(entityTypes);
         *         if (name == null) {
         *             throw new IllegalStateException("Failed to find entity class of unregistered entity type (" +
         *                     entityTypes.getClass().getName() + ")", t);
         *         }
         *
         *         // Look the same entity type object up again by this same key
         *         // If this returns a different object, try with that instead
         *         EntityTypes entityTypesAlter = (EntityTypes) BuiltInRegistries.ENTITY_TYPE.get(name);
         *         if (entityTypes == entityTypesAlter || entityTypesAlter == null) {
         *             throw new IllegalStateException("Failed to construct entity of type " + name, t);
         *         }
         *
         *         // Try again, but this time with the one from the registry
         *         try {
         * #if version >= 1.21.2
         *             entity = entityTypesAlter.create(world, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
         * #elseif version >= 1.18
         *             entity = entityTypesAlter.create(world);
         * #else
         *             entity = entityTypesAlter.a(world);
         * #endif
         *         } catch (Throwable t2) {
         *             // Throw the original error
         *             throw new IllegalStateException("Failed to construct entity of (adjusted) type " + name, t);
         *         }
         *     }
         *
         *     if (entity == null) {
         *         return null;
         *     } else {
         *         return entity.getClass();
         *     }
         * }
         */
        @Template.Generated("%CLASS_FROM_ENTITYTYPES%")
        public abstract Class<?> findClassFromEntityTypes(Object entityTypes, Object world);

        /*
         * <INIT_WORLD>
         * public static void initWorldServer((Object) WorldServer worldserver, (Object) WorldDataServer worldData) {
         *     String dummyWorldName = "zzdummyzz";
         * 
         * // Spigot World configuration
         * #if fieldexists net.minecraft.world.level.World public final org.spigotmc.SpigotWorldConfig spigotConfig;
         *     org.spigotmc.SpigotWorldConfig spigotConfig;
         * 
         *     // While loading set verbose to false, and later restore, to avoid logging a bunch of crap about this dummy world
         *     String spigotDummyWorldConfigKey = "world-settings." + dummyWorldName;
         *     String spigotConfigVerboseKey = spigotDummyWorldConfigKey + ".verbose";
         *     boolean hadDummyWorldConfig = org.spigotmc.SpigotConfig.config.contains(spigotDummyWorldConfigKey);
         *     boolean hadVerboseConfigOption = org.spigotmc.SpigotConfig.config.contains(spigotConfigVerboseKey, true);
         *     boolean prevVerboseConfigOption = org.spigotmc.SpigotConfig.config.getBoolean(spigotConfigVerboseKey, true);
         *     org.spigotmc.SpigotConfig.config.set(spigotConfigVerboseKey, Boolean.FALSE);
         *     try {
         *         #require net.minecraft.world.level.World public final org.spigotmc.SpigotWorldConfig spigotConfig;
         *         spigotConfig = new org.spigotmc.SpigotWorldConfig(dummyWorldName);
         *         worldserver#spigotConfig = spigotConfig;
         *     } finally {
         *         // Restore
         *         if (hadVerboseConfigOption) {
         *             org.spigotmc.SpigotConfig.config.set(spigotConfigVerboseKey, Boolean.valueOf(prevVerboseConfigOption));
         *         } else {
         *             org.spigotmc.SpigotConfig.config.set(spigotConfigVerboseKey, null);
         *         }
         *         // This will almost always be the case, unless some idiot named his world exactly like that...
         *         // Don't underestimate stupidity! So check for that 1/1000000 chance.
         *         if (!hadDummyWorldConfig) {
         *             org.spigotmc.SpigotConfig.config.set(spigotDummyWorldConfigKey, null);
         *         }
         *     }
         * #endif
         * 
         * // Paper(Spigot) World configuration
         * #if fieldexists net.minecraft.world.level.World private final io.papermc.paper.configuration.WorldConfiguration paperConfig;
         *     #require net.minecraft.world.level.World private final io.papermc.paper.configuration.WorldConfiguration paperConfig;
         *     #require io.papermc.paper.configuration.WorldConfiguration WorldConfiguration create_paper_wc:<init>(org.spigotmc.SpigotWorldConfig spigotWC, net.minecraft.resources.MinecraftKey worldKey);
         *   #if version >= 1.21
         *     net.minecraft.resources.MinecraftKey worldKey = net.minecraft.resources.MinecraftKey.parse(dummyWorldName);
         *   #else
         *     net.minecraft.resources.MinecraftKey worldKey = new net.minecraft.resources.MinecraftKey(dummyWorldName);
         *   #endif
         *     io.papermc.paper.configuration.WorldConfiguration paperWorldConfig = #create_paper_wc(spigotConfig, worldKey);
         *     com.bergerkiller.bukkit.common.internal.logic.EntityTypingHandler.initConfigurationPartRecurse(paperWorldConfig);
         *
         *     worldserver#paperConfig = paperWorldConfig;
         * #elseif fieldexists net.minecraft.world.level.World public final com.destroystokyo.paper.PaperWorldConfig paperConfig;
         *     #require net.minecraft.world.level.World public final com.destroystokyo.paper.PaperWorldConfig paperConfig;
         *     com.destroystokyo.paper.PaperWorldConfig paperConfig = new com.destroystokyo.paper.PaperWorldConfig(dummyWorldName, spigotConfig);
         *     worldserver#paperConfig = paperConfig;
         * #elseif fieldexists net.minecraft.world.level.World public final com.destroystokyo.paper.PaperWorldConfig paperSpigotConfig;
         *     #require net.minecraft.world.level.World public final com.destroystokyo.paper.PaperWorldConfig paperSpigotConfig;
         *     com.destroystokyo.paper.PaperWorldConfig paperConfig = new com.destroystokyo.paper.PaperWorldConfig(dummyWorldName, spigotConfig);
         *     worldserver#paperSpigotConfig = paperConfig;
         * #endif
         * 
         * // Purpur World configuration
         * #if fieldexists net.minecraft.world.level.World public final org.purpurmc.purpur.PurpurWorldConfig purpurConfig;
         *     #require net.minecraft.world.level.World public final org.purpurmc.purpur.PurpurWorldConfig purpurConfig;
         *     org.purpurmc.purpur.PurpurWorldConfig purpurConfig;
         *   #if exists org.purpurmc.purpur.PurpurWorldConfig public PurpurWorldConfig(net.minecraft.server.level.WorldServer level, String worldName, org.bukkit.World.Environment environment);
         *     purpurConfig = new org.purpurmc.purpur.PurpurWorldConfig(worldserver, dummyWorldName, org.bukkit.World$Environment.NORMAL);
         *   #elseif exists org.purpurmc.purpur.PurpurWorldConfig public PurpurWorldConfig(String worldName, org.bukkit.World.Environment environment);
         *     purpurConfig = new org.purpurmc.purpur.PurpurWorldConfig(dummyWorldName, org.bukkit.World$Environment.NORMAL);
         *   #elseif exists org.purpurmc.purpur.PurpurWorldConfig public org.purpurmc.purpur.PurpurWorldConfig(String worldName);
         *     purpurConfig = new org.purpurmc.purpur.PurpurWorldConfig(dummyWorldName);
         *   #else
         *     purpurConfig = new org.purpurmc.purpur.PurpurWorldConfig(dummyWorldName, paperConfig, spigotConfig);
         *   #endif
         *     worldserver#purpurConfig = purpurConfig;
         * #endif
         * 
         * #if version >= 1.17
         *     // WorldDataMutable field
         *     #require net.minecraft.world.level.World public final net.minecraft.world.level.storage.WorldDataMutable levelData;
         *     worldserver#levelData = worldData;
         * 
         *     // WorldDataServer field (on some servers, it uses the WorldDataMutable field instead)
         *   #if exists net.minecraft.server.level.WorldServer public final net.minecraft.world.level.storage.WorldDataServer serverLevelData;
         *     #require net.minecraft.server.level.WorldServer public final net.minecraft.world.level.storage.WorldDataServer serverLevelData;
         *     worldserver#serverLevelData = worldData;
         *   #endif
         * #elseif version >= 1.16
         *     // WorldDataMutable field
         *     #require net.minecraft.world.level.World public final net.minecraft.world.level.storage.WorldDataMutable worldData;
         *     worldserver#worldData = worldData;
         * 
         *     // WorldDataServer field (on some servers, it uses the WorldDataMutable field instead)
         *   #if exists net.minecraft.server.level.WorldServer public final net.minecraft.world.level.storage.WorldDataServer worldDataServer;
         *     #require net.minecraft.server.level.WorldServer public final net.minecraft.world.level.storage.WorldDataServer worldDataServer;
         *     worldserver#worldDataServer = worldData;
         *   #endif
         * #else
         *     // worldProvider field
         *     int envId = org.bukkit.World.Environment.NORMAL.getId();
         *     worldserver.worldProvider = net.minecraft.world.level.dimension.DimensionManager.a(envId).getWorldProvider((World) worldserver);
         * 
         *     // worldData field
         *     #require net.minecraft.world.level.World public final net.minecraft.world.level.storage.WorldData worldData;
         *     worldserver#worldData = worldData;
         * #endif
         * 
         *     // Random field
         * #if version >= 1.19
         *     #require net.minecraft.world.level.World public final net.minecraft.util.RandomSource random;
         *     net.minecraft.util.RandomSource newRandom = net.minecraft.util.RandomSource.create();
         *     worldserver#random = newRandom;
         * #else
         *     #require net.minecraft.world.level.World public final java.util.Random random;
         *     java.util.Random newRandom = new java.util.Random();
         *     worldserver#random = newRandom;
         * #endif
         * 
         *     // server field (for enabledFeatures() call on 1.19.3+)
         *     #require net.minecraft.server.level.WorldServer private final net.minecraft.server.MinecraftServer server;
         *     worldserver#server = net.minecraft.server.MinecraftServer.getServer();
         * }
         */
        @Template.Generated("%INIT_WORLD%")
        public abstract void initWorldServer(Object world, Object worldData);

        /*
         * <CREATE_ENTRY>
         * public static Object createEntry((Object) PlayerChunkMap playerChunkMap, (Object) Entity entity) {
         * #if version >= 1.18
         *     EntityTypes entitytypes = entity.getType();
         *     int i = entitytypes.clientTrackingRange() * 16;
         * #else
         *     EntityTypes entitytypes = entity.getEntityType();
         *     int i = entitytypes.getChunkRange() * 16;
         * #endif
         *
         * #if exists org.spigotmc.TrackingRange
         *     i = org.spigotmc.TrackingRange.getEntityTrackingRange(entity, i);
         * #endif
         *
         * #if version >= 1.18
         *     int j = entitytypes.updateInterval();
         *     boolean trackDeltas = entitytypes.trackDeltas();
         * #else
         *     int j = entitytypes.getUpdateInterval();
         *     boolean trackDeltas = entitytypes.isDeltaTracking();
         * #endif
         * 
         *     return new PlayerChunkMap$EntityTracker(playerChunkMap, entity, i, j, trackDeltas);
         * }
         */
        @Template.Generated("%CREATE_ENTRY%")
        public abstract Object createEntry(Object playerChunkMap, Object entity);
    }
}
