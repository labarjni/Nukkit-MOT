package cn.nukkit.block;

import cn.nukkit.Server;
import cn.nukkit.event.block.BlockGrowEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemSeedsMelon;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.BlockFace.Plane;

import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.Utils;

import cn.nukkit.block.custom.properties.BlockProperties;
import cn.nukkit.block.custom.properties.EnumBlockProperty;
import cn.nukkit.block.custom.properties.IntBlockProperty;
import cn.nukkit.block.properties.BlockPropertiesHelper;

/**
 * Created by Pub4Game on 15.01.2016.
 */
public class BlockStemMelon extends BlockCrops implements Faceable, BlockPropertiesHelper {

    private static final IntBlockProperty GROWTH = new IntBlockProperty("growth", false, 7, 0);

    private static final EnumBlockProperty<BlockFace> ATTACHED_SIDE = new EnumBlockProperty<>("attached_side", false, BlockFace.class);

    private static final BlockProperties PROPERTIES = new BlockProperties(GROWTH, ATTACHED_SIDE);

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
    public BlockFace getBlockFace() {
        return BlockFace.fromIndex(this.getDamage() - 0x08);
    }

    public void setBlockFace(BlockFace face) {
        int i = switch (face.getName()) {
            case "south" -> 24;
            case "west" -> 32;
            case "north" -> 16;
            case "east" -> 40;
            default -> 0;
        };

        this.setDamage((0x08 + face.getIndex()) - i);
    }

    @Override
    public BlockProperties getBlockProperties() {
        return PROPERTIES;
    }

    @Override
    public String getIdentifier() {
        return "minecraft:melon_stem";
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

                    this.setPropertyValue(GROWTH, this.getPropertyValue(GROWTH) + 1);

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

                            System.out.println(this.getDamage());
                            setPropertyValue(ATTACHED_SIDE, sideFace);
                            System.out.println(this.getDamage());
                            System.out.println(this.getPropertyValue(ATTACHED_SIDE));
                            this.setBlockFace(sideFace);
                            System.out.println(this.getPropertyValue(ATTACHED_SIDE));
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
}