package com.bergerkiller.reflection.net.minecraft.server;

import org.bukkit.World;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.common.conversion.DuplexConversion;
import com.bergerkiller.bukkit.common.wrappers.ChatText;
import com.bergerkiller.bukkit.common.wrappers.DataWatcher;
import com.bergerkiller.generated.net.minecraft.core.BlockPositionHandle;
import com.bergerkiller.generated.net.minecraft.util.RandomSourceHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.EntityHandle;
import com.bergerkiller.mountiplex.reflection.ClassTemplate;
import com.bergerkiller.mountiplex.reflection.FieldAccessor;
import com.bergerkiller.mountiplex.reflection.MethodAccessor;
import com.bergerkiller.mountiplex.reflection.SafeDirectField;
import com.bergerkiller.mountiplex.reflection.TranslatorFieldAccessor;

@Deprecated
public class NMSEntity {
    public static final ClassTemplate<?> T = ClassTemplate.create(EntityHandle.T.getType())
    		.addImport("org.bukkit.craftbukkit.entity.CraftEntity");

    /* ================================================================================================================ */
    /* ================================================== FIELDS ====================================================== */
    /* ================================================================================================================ */

    public static final FieldAccessor<Entity> bukkitEntity  = EntityHandle.T.bukkitEntityField.toFieldAccessor();
    public static final TranslatorFieldAccessor<Entity> vehicleField = EntityHandle.T.vehicle.raw.toFieldAccessor().translate(DuplexConversion.entity);
    public static final TranslatorFieldAccessor<World>  world = new SafeDirectField<Object>() {
        @Override
        public Object get(Object instance) {
            return EntityHandle.T.getWorld.raw.invoke(instance);
        }

        @Override
        public boolean set(Object instance, Object value) {
            EntityHandle.T.setWorld.raw.invoke(instance, value);
            return true;
        }
    }.translate(DuplexConversion.world);
    public static final FieldAccessor<Double> lastX = EntityHandle.T.lastX.toFieldAccessor();
    public static final FieldAccessor<Double> lastY = EntityHandle.T.lastY.toFieldAccessor();
    public static final FieldAccessor<Double> lastZ = EntityHandle.T.lastZ.toFieldAccessor();
    public static final FieldAccessor<Float>  yaw   = EntityHandle.T.yaw.toFieldAccessor();
    public static final FieldAccessor<Float>  pitch = EntityHandle.T.pitch.toFieldAccessor();
    public static final FieldAccessor<Float>   lastYaw     = EntityHandle.T.lastYaw.toFieldAccessor();
    public static final FieldAccessor<Float>   lastPitch   = EntityHandle.T.lastPitch.toFieldAccessor();
    public static final FieldAccessor<Object>  boundingBox = EntityHandle.T.boundingBoxField.raw.toFieldAccessor();
    public static final FieldAccessor<Boolean> onGround    = EntityHandle.T.onGround.toFieldAccessor();
    public static final FieldAccessor<Boolean> velocityChanged = EntityHandle.T.velocityChanged.toFieldAccessor();
    public static final FieldAccessor<Float> fallDistance = EntityHandle.T.fallDistance.toFieldAccessor();

    @Deprecated
    public static final FieldAccessor<Float> stepCounter = new SafeDirectField<Float>() {
        @Override
        public Float get(Object instance) {
            return EntityHandle.createHandle(instance).getStepCounter();
        }
        @Override
        public boolean set(Object instance, Float value) {
            EntityHandle.createHandle(instance).setStepCounter(value.floatValue());
            return true;
        }
    };

    public static final FieldAccessor<Boolean> noclip = EntityHandle.T.noclip.toFieldAccessor();
    public static final FieldAccessor<RandomSourceHandle>  random = EntityHandle.T.random.toFieldAccessor();
    public static final TranslatorFieldAccessor<DataWatcher> datawatcher = EntityHandle.T.datawatcherField.toFieldAccessor();

    public static final DataWatcher.Key<Byte> DATA_FLAGS = EntityHandle.DATA_FLAGS;
    public static final DataWatcher.Key<Integer> DATA_AIR_TICKS = EntityHandle.DATA_AIR_TICKS;
    public static final DataWatcher.Key<ChatText> DATA_CUSTOM_NAME = EntityHandle.DATA_CUSTOM_NAME;
    public static final DataWatcher.Key<Boolean> DATA_CUSTOM_NAME_VISIBLE = EntityHandle.DATA_CUSTOM_NAME_VISIBLE;
    public static final DataWatcher.Key<Boolean> DATA_SILENT = EntityHandle.DATA_SILENT;
    public static final DataWatcher.Key<Boolean> DATA_NO_GRAVITY = EntityHandle.DATA_NO_GRAVITY;

    public static final int DATA_FLAG_ON_FIRE = EntityHandle.DATA_FLAG_ON_FIRE;
    public static final int DATA_FLAG_SNEAKING = EntityHandle.DATA_FLAG_SNEAKING;
    public static final int DATA_FLAG_UNKNOWN1 = EntityHandle.DATA_FLAG_UNKNOWN1;
    public static final int DATA_FLAG_SPRINTING = EntityHandle.DATA_FLAG_SPRINTING;
    public static final int DATA_FLAG_UNKNOWN2 = EntityHandle.DATA_FLAG_UNKNOWN2;
    public static final int DATA_FLAG_INVISIBLE = EntityHandle.DATA_FLAG_INVISIBLE;
    public static final int DATA_FLAG_GLOWING = EntityHandle.DATA_FLAG_GLOWING;
    public static final int DATA_FLAG_FLYING = EntityHandle.DATA_FLAG_FLYING;

    public static final FieldAccessor<Boolean> positionChanged = EntityHandle.T.positionChanged.toFieldAccessor();
    public static final FieldAccessor<Integer> portalCooldown = EntityHandle.T.portalCooldown.toFieldAccessor();

    /* Used in the move() function; unknown function. */
    public static final FieldAccessor<double[]> move_SomeArray = EntityHandle.T.move_SomeArray.toFieldAccessor();
    public static final FieldAccessor<Long> move_SomeState = EntityHandle.T.move_SomeState.toFieldAccessor();

    /* ================================================================================================================ */
    /* ================================================== METHODS ===================================================== */
    /* ================================================================================================================ */

    /*
     # protected void ##METHODNAME##(BlockPosition blockposition, Block block) {
     *     SoundEffectType soundeffecttype = block.getStepSound();
     *     if (this.world.getType(blockposition.up()).getBlock() == Blocks.SNOW_LAYER) {
     *         soundeffecttype = Blocks.SNOW_LAYER.getStepSound();
     *         this.a(soundeffecttype.d(), soundeffecttype.a() * 0.15F, soundeffecttype.b());
     *     } else if (!block.getBlockData().getMaterial().isLiquid()) {
     *         this.a(soundeffecttype.d(), soundeffecttype.a() * 0.15F, soundeffecttype.b());
     *     }
     * }
     */
    private static final MethodAccessor<Void> playStepSound = EntityHandle.T.playStepSound.raw.toMethodAccessor();

    /*
     # protected void ##METHODNAME##(float f, float f1) {
     *     // CraftBukkit start - yaw was sometimes set to NaN, so we need to set it back to 0
     *     if (Float.isNaN(f)) {
     *         f = 0;
     *     }
     *     if (f == Float.POSITIVE_INFINITY || f == Float.NEGATIVE_INFINITY) {
     *         if (this instanceof EntityPlayer) {
     *             this.world.getServer().getLogger().warning(this.getName() + " was caught trying to crash the server with an invalid yaw");
     *             ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite yaw (Hacking?)"); //Spigot "Nope" -> Descriptive reason
     *         }
     *         f = 0;
     *     }
     *     ...
     * }
     */
    private static final MethodAccessor<Void> setRotation = EntityHandle.T.setRotation.toMethodAccessor();

    /*
     # protected void ##METHODNAME##(float i) { // CraftBukkit - int -> float
     *     if (!this.fireProof) {
     *         this.damageEntity(DamageSource.FIRE, (float) i);
     *     }
     * }
     */
    public static final MethodAccessor<Void> burn = EntityHandle.T.burn.toMethodAccessor();

    /*
     * void move(...) {
     *     ...
     #     this.a(this.##METHODNAME##(), f1, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
     * }
     */
    public static final MethodAccessor<Object> getSwimSound = EntityHandle.T.getSwimSound.raw.toMethodAccessor();

    /*
     * void move(...) {
     *     ...
     *     this.##METHODNAME##(this.aa(), f1, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
     * }
     * 
     # public void ##METHODNAME##(SoundEffect soundeffect, float f, float f1) {
     *     if (!this.isSilent()) {
     *         this.world.a((EntityHuman) null, this.locX, this.locY, this.locZ, soundeffect, this.bC(), f, f1);
     *     }
     * }
     */
    public static final MethodAccessor<Void> makeSound = EntityHandle.T.makeSound.raw.toMethodAccessor();

    /*
     # public boolean ###METHODNAME###() {
     *     if (this.world.a(this.boundingBox.grow(0.0D, -0.4000000059604645D, 0.0D).shrink(0.001D, 0.001D, 0.001D), Material.WATER, this)) {
     *         if (!this.inWater && !this.justCreated) {
     *             ...
     *         }
     *     }
     * }
     */
    private static final MethodAccessor<Boolean> isInWaterUpdate   = EntityHandle.T.isInWaterUpdate.toMethodAccessor();
    
    /*
     # public boolean ###METHODNAME###() {
     *     return this.inWater;
     * }
     */
    private static final MethodAccessor<Boolean> isInWaterNoUpdate = EntityHandle.T.isInWater.toMethodAccessor();

    /*
     * public void move(double d0, double d1, double d2) {
     *     ....
     *     {
     *         if (bl.getType() != org.bukkit.Material.AIR) {
     *             VehicleBlockCollisionEvent event = new VehicleBlockCollisionEvent(vehicle, bl);
     *             world.getServer().getPluginManager().callEvent(event);
     *         }
     *     }
     *     
     *     // CraftBukkit end
     *
     #     if (this.###METHODNAME###() && (!this.onGround || !this.isSneaking() || !(this instanceof EntityHuman)) && !this.isPassenger()) {
     *         double d22 = this.locX - d4;
     *         double d23 = this.locY - d5;
     *         ...
     *     }
     * }
     */
    private static final MethodAccessor<Boolean> hasMovementSound = EntityHandle.T.hasMovementSound.toMethodAccessor();

    /*
     # protected void ##METHODNAME##(double d0, boolean flag, IBlockData iblockdata, BlockPosition blockposition) {
     *     if (flag) {
     *         if (this.fallDistance > 0.0F) {
     *             iblockdata.getBlock().fallOn(this.world, blockposition, this, this.fallDistance);
     *         }
     *
     *         this.fallDistance = 0.0F;
     *     } else if (d0 < 0.0D) {
     *         this.fallDistance = ((float)(this.fallDistance - d0));
     *     }
     * }
     */
    public static final MethodAccessor<Void> doFallUpdate = EntityHandle.T.updateFalling.raw.toMethodAccessor();

    /*
     * public double ##METHODNAME##(double d0, double d1, double d2) {
     *     double d3 = this.locX - d0;
     *     double d4 = this.locY - d1;
     *     double d5 = this.locZ - d2;
     *
     *     return MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
     * }
     */
    @Deprecated
    public static final MethodAccessor<Double> calculateDistance = EntityHandle.T.calculateDistanceSquared.toMethodAccessor();

    public static final MethodAccessor<Object> getBoundingBox = EntityHandle.T.getBoundingBox.raw.toMethodAccessor();

    public static boolean isInWater(Object entityHandle, boolean update) {
        return update ? isInWaterUpdate.invoke(entityHandle) : isInWaterNoUpdate.invoke(entityHandle);
    }

    public static void playStepSound(Object entityHandle, int x, int y, int z, Object blockStepped) {
        if (blockStepped != null) {
            playStepSound.invoke(entityHandle, BlockPositionHandle.createNew(x, y, z).getRaw(), blockStepped);
        }
    }

    public static boolean hasMovementSound(Object entityHandle) {
    	return hasMovementSound.invoke(entityHandle);
    }

    public static void setRotation(Object entityHandle, float yaw, float pitch) {
        setRotation.invoke(entityHandle, yaw, pitch);
    }

}
