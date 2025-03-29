package cn.nukkit.block;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemCarrot;
import cn.nukkit.utils.Utils;

import cn.nukkit.block.custom.properties.IntBlockProperty;

/**
 * @author Nukkit Project Team
 */
public class BlockCarrot extends BlockCrops {

    protected static final IntBlockProperty GROWTH = new IntBlockProperty("growth", false, 7, 0);

    public BlockCarrot(int meta) {
        super(meta);
    }

    public BlockCarrot() {
        this(0);
    }

    @Override
    public String getName() {
        return "Carrot Block";
    }

    @Override
    public int getId() {
        return CARROT_BLOCK;
    }

    @Override
    public String getIdentifier() {
        return "minecraft:carrot_block";
    }

    @Override
    public Item[] getDrops(Item item) {
        if (this.getPropertyValue(GROWTH) >= 7) {
            return new Item[]{
                    new ItemCarrot(0, Utils.rand(1, 5))
            };
        }
        return new Item[]{
                new ItemCarrot()
        };
    }

    @Override
    public Item toItem() {
        return new ItemCarrot();
    }
}
