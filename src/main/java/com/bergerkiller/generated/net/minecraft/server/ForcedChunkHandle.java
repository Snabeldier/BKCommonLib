package com.bergerkiller.generated.net.minecraft.server;

import com.bergerkiller.mountiplex.reflection.util.StaticInitHelper;
import com.bergerkiller.mountiplex.reflection.declarations.Template;

/**
 * Instance wrapper handle for type <b>net.minecraft.server.ForcedChunk</b>.
 * To access members without creating a handle type, use the static {@link #T} member.
 * New handles can be created from raw instances using {@link #createHandle(Object)}.
 */
@Template.Optional
public abstract class ForcedChunkHandle extends Template.Handle {
    /** @See {@link ForcedChunkClass} */
    public static final ForcedChunkClass T = new ForcedChunkClass();
    static final StaticInitHelper _init_helper = new StaticInitHelper(ForcedChunkHandle.class, "net.minecraft.server.ForcedChunk", com.bergerkiller.bukkit.common.Common.TEMPLATE_RESOLVER);

    /* ============================================================================== */

    public static ForcedChunkHandle createHandle(Object handleInstance) {
        return T.createHandle(handleInstance);
    }

    /* ============================================================================== */

    /**
     * Stores class members for <b>net.minecraft.server.ForcedChunk</b>.
     * Methods, fields, and constructors can be used without using Handle Objects.
     */
    public static final class ForcedChunkClass extends Template.Class<ForcedChunkHandle> {
    }

}

