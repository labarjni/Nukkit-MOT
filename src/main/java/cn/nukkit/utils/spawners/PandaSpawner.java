package cn.nukkit.utils.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.BaseEntity;
import cn.nukkit.entity.passive.EntityPanda;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.AbstractEntitySpawner;
import cn.nukkit.utils.SpawnerTask;
import cn.nukkit.utils.Utils;

public class PandaSpawner extends AbstractEntitySpawner {

    public PandaSpawner(SpawnerTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 3) != 1) {
            return;
        }
        final int biomeId = level.getBiomeId((int) pos.x, (int) pos.z);
        if (((biomeId == 21 || biomeId == 22) && Utils.rand(1, 10) != 1) || biomeId != 48 && biomeId != 49 && biomeId != 21 && biomeId != 22) {
            return;
        }
        if (!level.isAnimalSpawningAllowedByTime() || level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z) != Block.GRASS) {
            return;
        }
        for (int i = 0; i < Utils.rand(1, 2); i++) {
            BaseEntity entity = this.spawnTask.createEntity("Panda", pos.add(0.5, 1, 0.5));
            if (entity == null) return;
            if (Utils.rand(1, 20) == 1) {
                entity.setBaby(true);
            }
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return EntityPanda.NETWORK_ID;
    }
}