package cn.nukkit.item;

import cn.nukkit.Player;

import java.util.*;
import java.util.function.Predicate;

public final class CreativeItemPermissions {

    private static final String BASE_PERMISSION = "nukkit.creativeitem.";

    private static final Map<String, Predicate<Item>> PERMISSION_GROUPS = new LinkedHashMap<>();

    public static void registerPermissionGroup(String permissionSuffix, Predicate<Item> condition) {
        PERMISSION_GROUPS.put(permissionSuffix, condition);
    }

    public static boolean hasPermission(Item item, Player player) {
        if (player == null) return true;

        for (Map.Entry<String, Predicate<Item>> entry : PERMISSION_GROUPS.entrySet()) {
            if (entry.getValue().test(item)) {
                String groupPerm = BASE_PERMISSION + entry.getKey();
                if (!player.hasPermission(groupPerm)) {
                    return false;
                }
            }
        }

        return true;
    }
}