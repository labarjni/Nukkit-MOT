package cn.nukkit.block;

import cn.nukkit.item.Item;
import cn.nukkit.utils.BlockColor;

public class BlockDoorCherry extends BlockDoorWood {

    public BlockDoorCherry() {
        this(0);
    }

    public BlockDoorCherry(int meta) {
        super(meta);
    }

    @Override
    public String getName() {
        return "Cherry Door Block";
    }

    @Override
    public int getId() {
        return CHERRY_DOOR_BLOCK;
    }

    @Override
    public Item toItem() {
        return Item.fromString(Item.CHERRY_DOOR);
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.WHITE_TERRACOTA_BLOCK_COLOR;
    }
}
