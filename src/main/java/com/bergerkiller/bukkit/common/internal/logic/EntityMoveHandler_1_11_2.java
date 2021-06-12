package com.bergerkiller.bukkit.common.internal.logic;

import java.util.List;

import com.bergerkiller.generated.net.minecraft.server.WorldHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.EntityHandle;
import com.bergerkiller.generated.net.minecraft.world.phys.AxisAlignedBBHandle;

/**
 * Logic for MC 1.11.2 - 1.12.2
 */
public class EntityMoveHandler_1_11_2 extends EntityMoveHandler_1_8 {

    @Override
    protected boolean world_getBlockCubes(EntityHandle entity, AxisAlignedBBHandle movedBounds, List<AxisAlignedBBHandle> cubes) {
        return WorldHandle.T.getBlockCollisions.invoke(entity.getWorld().getRaw(), entity, movedBounds, false, cubes);
    }

}
