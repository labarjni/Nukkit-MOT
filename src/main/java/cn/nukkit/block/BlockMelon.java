package cn.nukkit.block;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemMelon;
import cn.nukkit.item.ItemTool;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.BlockColor;
import cn.nukkit.utils.Utils;

import cn.nukkit.block.custom.properties.BlockProperties;
import cn.nukkit.block.custom.properties.EnumBlockProperty;
import cn.nukkit.block.properties.BlockPropertiesHelper;

/**
 * Created on 2015/12/11 by Pub4Game.
 * Package cn.nukkit.block in project Nukkit .
 */

public class BlockMelon extends BlockSolid implements BlockPropertiesHelper {

    private static final EnumBlockProperty<BlockFace> ATTACHED_SIDE = new EnumBlockProperty<>("attached_side", false, BlockFace.class);

    private static final BlockProperties PROPERTIES = new BlockProperties(ATTACHED_SIDE);

    @Override
    public int getId() {
        return MELON_BLOCK;
    }

    @Override
    public String getName() {
        return "Melon Block";
    }

    @Override
    public double getHardness() {
        return 1;
    }

    @Override
    public double getResistance() {
        return 5;
    }

    @Override
    public BlockProperties getBlockProperties() {
        return PROPERTIES;
    }

    @Override
    public String getIdentifier() {
        return "minecraft:melon_block";
    }

    @Override
    public Item[] getDrops(Item item) {
        if (item.hasEnchantment(Enchantment.ID_SILK_TOUCH)) {
            return new Item[]{this.toItem()};
        }

        int count = 3 + Utils.random.nextInt(5);

        Enchantment fortune = item.getEnchantment(Enchantment.ID_FORTUNE_DIGGING);
        if (fortune != null && fortune.getLevel() >= 1) {
            count += Utils.random.nextInt(fortune.getLevel() + 1);
        }

        return new Item[]{
                new ItemMelon(0, Math.min(9, count))
        };
    }

    @Override
    public boolean onBreak(Item item) {
        return super.onBreak(item);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.LIME_BLOCK_COLOR;
    }
    
    @Override
    public boolean canSilkTouch() {
        return true;
    }

    @Override
    public boolean breaksWhenMoved() {
        return true;
    }

    @Override
    public boolean sticksToPiston() {
        return false;
    }
}
