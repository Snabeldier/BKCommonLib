package com.bergerkiller.bukkit.common.server;

import java.util.Map;

import com.bergerkiller.mountiplex.reflection.declarations.Template;
import com.bergerkiller.mountiplex.reflection.resolver.ClassNameResolver;
import com.bergerkiller.mountiplex.reflection.resolver.FieldNameResolver;
import com.bergerkiller.mountiplex.reflection.resolver.MethodNameResolver;

/**
 * Mohist is a PaperSpigot + Forge implementation
 */
public class MohistServer extends PaperSpigotServer implements FieldNameResolver, MethodNameResolver, ClassNameResolver {
    private RemapUtilsClass remapUtils = null;

    @Override
    public boolean init() {
        if (!super.init()) {
            return false;
        }

        // Check this is actually a Mohist server, we expect this Class to exist
        try {
            Class.forName("red.mohist.Mohist");
        } catch (Throwable t) {
            return false;
        }

        // Make sure RemapUtils exists, this initializes the RemapUtilsHandle using the above initialized declaration
        remapUtils = Template.Class.create(RemapUtilsClass.class);
        if (!remapUtils.isAvailable()) {
            return false;
        }

        // Force initialization to avoid late catastrophic failing
        remapUtils.forceInitialization();

        return true;
    }

    @Override
    public String getServerName() {
        return "Mohist";
    }

    @Override
    public String resolveClassPath(String path) {
        // Replaces path with proper net.minecraft.server.v1_1_1 path
        path = super.resolveClassPath(path);

        // Ask Mohist what the actual class name is on Forge
        path = remapUtils.mapClassName(path);

        return path;
    }

    @Override
    public String resolveMethodName(Class<?> type, String methodName, Class<?>[] params) {
        return remapUtils.inverseMapMethodName(type, methodName, params);
    }

    @Override
    public String resolveFieldName(Class<?> type, String fieldName) {
        return remapUtils.inverseMapFieldName(type, fieldName);
    }

    @Override
    public String resolveClassName(Class<?> clazz) {
        return remapUtils.inverseMapClassName(clazz);
    }

    @Override
    public void addVariables(Map<String, String> variables) {
        super.addVariables(variables);
        variables.put("forge", "mohist");
    }

    @Template.Optional
    @Template.InstanceType("red.mohist.bukkit.nms.utils.RemapUtils")
    public static abstract class RemapUtilsClass extends Template.Class<Template.Handle> {
        @Template.Generated("public static String mapClassName(String className) {\r\n" + 
                            "    if (className.startsWith(\"net.minecraft.server.\")) {\r\n" + 
                            "        red.mohist.bukkit.nms.model.ClassMapping mapping;\r\n" + 
                            "        mapping = (red.mohist.bukkit.nms.model.ClassMapping) RemapUtils.jarMapping.byNMSName.get(className);\r\n" + 
                            "        if (mapping != null) {\r\n" + 
                            "            return mapping.getMcpName();\r\n" + 
                            "        } else {\r\n" + 
                            "            // Mohist BUGFIX!!!\r\n" + 
                            "            // If we do not do this, it will suffer a NPE in the PluginClassLoader\r\n" + 
                            "            return \"missing.type.\" + className;\r\n" + 
                            "        }\r\n" + 
                            "    }\r\n" + 
                            "    return className;\r\n" + 
                            "}")
        public abstract String mapClassName(String className);

        @Template.Generated("public static String inverseMapClassName(Class<?> type) {\n" +
                            "    // Because we generate this, the remapper of Mohist is unable to wrap it\n" +
                            "    // As a result, getName() is now correct! Yay!\n" +
                            "    return type.getName();\n" +
                            "}")
        public abstract String inverseMapClassName(Class<?> type);

        @Template.Generated("public static transient String inverseMapMethodName(Class<?> type, String name, Class<?>[] parameterTypes)")
        public abstract String inverseMapMethodName(Class<?> type, String name, Class<?>[] parameterTypes);

        @Template.Generated("public static String inverseMapFieldName(Class<?> type, String fieldName)")
        public abstract String inverseMapFieldName(Class<?> type, String fieldName);
    }

}
