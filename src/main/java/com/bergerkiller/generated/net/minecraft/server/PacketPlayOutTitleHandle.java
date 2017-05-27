package com.bergerkiller.generated.net.minecraft.server;

import com.bergerkiller.mountiplex.reflection.declarations.Template;
import com.bergerkiller.mountiplex.reflection.util.StaticInitHelper;

public class PacketPlayOutTitleHandle extends Template.Handle {
    public static final PacketPlayOutTitleClass T = new PacketPlayOutTitleClass();
    static final StaticInitHelper _init_helper = new StaticInitHelper(PacketPlayOutTitleHandle.class, "net.minecraft.server.PacketPlayOutTitle");


    /* ============================================================================== */

    public static PacketPlayOutTitleHandle createHandle(Object handleInstance) {
        if (handleInstance == null) return null;
        PacketPlayOutTitleHandle handle = new PacketPlayOutTitleHandle();
        handle.instance = handleInstance;
        return handle;
    }

    /* ============================================================================== */

    public static final class PacketPlayOutTitleClass extends Template.Class<PacketPlayOutTitleHandle> {
    }

    public static class EnumTitleActionHandle extends Template.Handle {
        public static final EnumTitleActionClass T = new EnumTitleActionClass();
        static final StaticInitHelper _init_helper = new StaticInitHelper(EnumTitleActionHandle.class, "net.minecraft.server.PacketPlayOutTitle.EnumTitleAction");

        public static final EnumTitleActionHandle TITLE = T.TITLE.getSafe();
        public static final EnumTitleActionHandle SUBTITLE = T.SUBTITLE.getSafe();
        public static final EnumTitleActionHandle ACTIONBAR = T.ACTIONBAR.getSafe();
        public static final EnumTitleActionHandle TIMES = T.TIMES.getSafe();
        public static final EnumTitleActionHandle CLEAR = T.CLEAR.getSafe();
        public static final EnumTitleActionHandle RESET = T.RESET.getSafe();

        /* ============================================================================== */

        public static EnumTitleActionHandle createHandle(Object handleInstance) {
            if (handleInstance == null) return null;
            EnumTitleActionHandle handle = new EnumTitleActionHandle();
            handle.instance = handleInstance;
            return handle;
        }

        /* ============================================================================== */

        public static final class EnumTitleActionClass extends Template.Class<EnumTitleActionHandle> {
            public final Template.EnumConstant.Converted<EnumTitleActionHandle> TITLE = new Template.EnumConstant.Converted<EnumTitleActionHandle>();
            public final Template.EnumConstant.Converted<EnumTitleActionHandle> SUBTITLE = new Template.EnumConstant.Converted<EnumTitleActionHandle>();
            public final Template.EnumConstant.Converted<EnumTitleActionHandle> ACTIONBAR = new Template.EnumConstant.Converted<EnumTitleActionHandle>();
            public final Template.EnumConstant.Converted<EnumTitleActionHandle> TIMES = new Template.EnumConstant.Converted<EnumTitleActionHandle>();
            public final Template.EnumConstant.Converted<EnumTitleActionHandle> CLEAR = new Template.EnumConstant.Converted<EnumTitleActionHandle>();
            public final Template.EnumConstant.Converted<EnumTitleActionHandle> RESET = new Template.EnumConstant.Converted<EnumTitleActionHandle>();

        }
    }
}
