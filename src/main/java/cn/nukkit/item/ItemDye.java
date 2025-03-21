package cn.nukkit.item;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.BlockColor;
import cn.nukkit.utils.DyeColor;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class ItemDye extends Item {

    public static final int WHITE = DyeColor.WHITE.getDyeData();
    public static final int ORANGE = DyeColor.ORANGE.getDyeData();
    public static final int MAGENTA = DyeColor.MAGENTA.getDyeData();
    public static final int LIGHT_BLUE = DyeColor.LIGHT_BLUE.getDyeData();
    public static final int YELLOW = DyeColor.YELLOW.getDyeData();
    public static final int LIME = DyeColor.LIME.getDyeData();
    public static final int PINK = DyeColor.PINK.getDyeData();
    public static final int GRAY = DyeColor.GRAY.getDyeData();
    public static final int LIGHT_GRAY = DyeColor.LIGHT_GRAY.getDyeData();
    public static final int CYAN = DyeColor.CYAN.getDyeData();
    public static final int PURPLE = DyeColor.PURPLE.getDyeData();
    public static final int BLUE = DyeColor.BLUE.getDyeData();
    public static final int BROWN = DyeColor.BROWN.getDyeData();
    public static final int GREEN = DyeColor.GREEN.getDyeData();
    public static final int RED = DyeColor.RED.getDyeData();
    public static final int BLACK = DyeColor.BLACK.getDyeData();

    public static final int INK_SAC = 0;
    public static final int COCOA_BEANS = 3;
    public static final int LAPIS_LAZULI = 4;
    public static final int BONE_MEAL = 15;

    public static final int BLACK_NEW = 16;
    public static final int BROWN_NEW = 17;
    public static final int BLUE_NEW = 18;
    public static final int WHITE_NEW = 19;

    public static final int GLOW_INK_SAC = 20;

    private static final String[] NAMES = new String[21];

    static {
        for (int i = 0; i < 16; i++) {
            NAMES[i] = DyeColor.getByDyeData(i).getDyeName();
        }
        NAMES[BLACK_NEW] = "Black Dye";
        NAMES[BROWN_NEW] = "Brown Dye";
        NAMES[BLUE_NEW] = "Blue Dye";
        NAMES[WHITE_NEW] = "White Dye";
        NAMES[GLOW_INK_SAC] = "Glow Ink Sac";
    }

    public ItemDye() {
        this(0, 1);
    }

    public ItemDye(Integer meta) {
        this(meta, 1);
    }

    public ItemDye(DyeColor dyeColor) {
        this(dyeColor.getDyeData(), 1);
    }

    public ItemDye(DyeColor dyeColor, int amount) {
        this(dyeColor.getDyeData(), amount);
    }

    public ItemDye(Integer meta, int amount) {
        super(DYE, meta, amount, meta >= 0 && meta <= 20 ? NAMES[meta] : UNKNOWN_STR);

        if (this.meta == DyeColor.BROWN.getDyeData()) {
            this.block = Block.get(BlockID.COCOA_BLOCK);
        }
    }

    public static BlockColor getColor(int meta) {
        return DyeColor.getByDyeData(meta).getColor();
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByDyeData(meta);
    }

    public static String getColorName(int meta) {
        return DyeColor.getByDyeData(meta).getName();
    }

    @Override
    public boolean isSupportedOn(int protocolId) {
        return this.getDamage() < 20 || protocolId >= ProtocolInfo.v1_17_0;
    }
}
