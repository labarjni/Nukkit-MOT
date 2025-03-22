package cn.nukkit.block;

import cn.nukkit.Server;
import cn.nukkit.block.custom.properties.BlockProperties;
import cn.nukkit.block.custom.properties.IntBlockProperty;
import cn.nukkit.block.properties.BlockPropertiesHelper;
import cn.nukkit.block.properties.VanillaProperties;
import cn.nukkit.event.block.BlockGrowEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemSeedsMelon;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.BlockFace.Plane;
import cn.nukkit.utils.Utils;

/**
 * Created by Pub4Game on 15.01.2016.
 */
public class BlockStemMelon extends BlockCrops implements BlockPropertiesHelper {

    private static final IntBlockProperty GROWTH = new IntBlockProperty("growth", false, 7, 0);

    private static final BlockProperties PROPERTIES = new BlockProperties(GROWTH);

    public BlockStemMelon() {
        this(0);
    }

    public BlockStemMelon(int meta) {
        super(meta);
    }

    @Override
    public int getId() {
        return MELON_STEM;
    }

    @Override
    public String getName() {
        return "Melon Stem";
    }

    @Override
    public BlockProperties getBlockProperties() {
        return PROPERTIES;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().getId() != FARMLAND) {
                this.getLevel().useBreakOn(this);
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (Utils.rand(1, 2) == 1) {
                if (this.getPropertyValue(GROWTH) < 7) {
                    setPropertyValue(GROWTH, this.getPropertyValue(GROWTH) + 1);

                    Block block = this.clone();
                    BlockGrowEvent ev = new BlockGrowEvent(this, block);
                    Server.getInstance().getPluginManager().callEvent(ev);
                    if (!ev.isCancelled()) {
                        this.getLevel().setBlock(this, ev.getNewState(), true, true);
                    }
                    return Level.BLOCK_UPDATE_RANDOM;
                } else {
                    for (BlockFace face : Plane.HORIZONTAL) {
                        Block b = this.getSide(face);
                        if (b.getId() == MELON_BLOCK) {
                            return Level.BLOCK_UPDATE_RANDOM;
                        }
                    }

                    BlockFace sideFace = Plane.HORIZONTAL.random(Utils.nukkitRandom);
                    Block side = this.getSide(sideFace);
                    Block d = side.down();
                    if (side.getId() == AIR && (d.getId() == FARMLAND || d.getId() == GRASS || d.getId() == DIRT)) {
                        BlockGrowEvent ev = new BlockGrowEvent(side, Block.get(MELON_BLOCK));
                        Server.getInstance().getPluginManager().callEvent(ev);
                        if (!ev.isCancelled()) {
                            this.getLevel().setBlock(side, ev.getNewState(), true, true);

                            this.setDamage(0x08 + sideFace.getIndex()); // Attached Melon Stem
                            this.getLevel().setBlock(this, this, true, true);
                        }
                    }
                }
            }
            return Level.BLOCK_UPDATE_RANDOM;
        }
        return 0;
    }

    @Override
    public Item toItem() {
        return new ItemSeedsMelon();
    }

    @Override
    public Item[] getDrops(Item item) {
        if (this.getDamage() < 4) return Item.EMPTY_ARRAY;
        return new Item[]{
                new ItemSeedsMelon(0, Utils.rand(0, 48) >> 4)
        };
    }

    @Override
    public String getIdentifier() {
        return "";
    }
}
