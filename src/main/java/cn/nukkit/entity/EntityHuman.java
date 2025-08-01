package cn.nukkit.entity;

import cn.nukkit.Player;
import cn.nukkit.entity.data.IntPositionEntityData;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemShield;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.network.protocol.AddPlayerPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import cn.nukkit.utils.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class EntityHuman extends EntityHumanType {

    public static final int DATA_PLAYER_FLAG_SLEEP = 1;
    public static final int DATA_PLAYER_FLAG_DEAD = 2;

    public static final int DATA_PLAYER_FLAGS = 26;
    public static final int DATA_PLAYER_BUTTON_TEXT = 40;

    protected static PlayerInventory EMPTY_INVENTORY = new PlayerInventory(null);

    protected UUID uuid;
    protected byte[] rawUUID;

    protected Skin skin;

    @Override
    public float getWidth() {
        return 0.58f;
    }

    @Override
    public float getLength() {
        return 0.58f;
    }

    @Override
    public float getHeight() {
        if(isSwimming() || isGliding()) {
            return 0.6f;
        } else if (isShortSneaking()) {
            if (this instanceof Player player && player.protocol < ProtocolInfo.v1_20_0) {
                return 1.65f; // Return the old height
            } else {
                return 1.49f; // Enable the player to enter 1.5 Spaces when jumping while sneaking.
            }
        } else if (isCrawling()) {
            return 0.625f;
        }
        return 1.8f;
    }

    @Override
    protected double getStepHeight() {
        return 0.6f;
    }

    @Override
    public float getEyeHeight() {
        if(isSwimming() || isGliding() || isCrawling()) {
            return 0.42f;
        } else if (isShortSneaking()) {
            return 1.26f;
        }
        return 1.62f;
    }

    @Override
    protected float getBaseOffset() {
        return 1.62f;
    }

    @Override
    public int getNetworkId() {
        return -1;
    }

    public EntityHuman(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    public Skin getSkin() {
        return skin;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    public byte[] getRawUniqueId() {
        return rawUUID;
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    @Override
    protected void initEntity() {
        this.setDataFlag(DATA_PLAYER_FLAGS, DATA_PLAYER_FLAG_SLEEP, false, false);
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_GRAVITY, true, false);
        this.setDataProperty(new IntPositionEntityData(DATA_PLAYER_BED_POSITION, 0, 0, 0), false);

        if (!(this instanceof Player)) {
            if (this.namedTag.contains("NameTag")) {
                this.setNameTag(this.namedTag.getString("NameTag"));
            }

            if (this.namedTag.contains("Skin") && this.namedTag.get("Skin") instanceof CompoundTag) {
                CompoundTag skinTag = this.namedTag.getCompound("Skin");
                if (!skinTag.contains("Transparent")) {
                    skinTag.putBoolean("Transparent", false);
                }
                Skin newSkin = new Skin();
                if (skinTag.contains("ModelId")) {
                    newSkin.setSkinId(skinTag.getString("ModelId"));
                }
                if (skinTag.contains("PlayFabId")) {
                    newSkin.setPlayFabId(skinTag.getString("PlayFabId"));
                }
                if (skinTag.contains("Data")) {
                    byte[] data = skinTag.getByteArray("Data");
                    if (skinTag.contains("SkinImageWidth") && skinTag.contains("SkinImageHeight")) {
                        newSkin.setSkinData(new SerializedImage(skinTag.getInt("SkinImageWidth"), skinTag.getInt("SkinImageHeight"), data));
                    } else {
                        newSkin.setSkinData(data);
                    }
                }
                if (skinTag.contains("CapeId")) {
                    newSkin.setCapeId(skinTag.getString("CapeId"));
                }
                if (skinTag.contains("CapeData")) {
                    byte[] data = skinTag.getByteArray("CapeData");
                    if (skinTag.contains("CapeImageWidth") && skinTag.contains("CapeImageHeight")) {
                        newSkin.setCapeData(new SerializedImage(skinTag.getInt("CapeImageWidth"), skinTag.getInt("CapeImageHeight"), data));
                    } else {
                        newSkin.setCapeData(data);
                    }
                }
                if (skinTag.contains("GeometryName")) {
                    newSkin.setGeometryName(skinTag.getString("GeometryName"));
                }
                if (skinTag.contains("SkinResourcePatch")) {
                    newSkin.setSkinResourcePatch(new String(skinTag.getByteArray("SkinResourcePatch"), StandardCharsets.UTF_8));
                }
                if (skinTag.contains("GeometryData")) {
                    newSkin.setGeometryData(new String(skinTag.getByteArray("GeometryData"), StandardCharsets.UTF_8));
                }
                if (skinTag.contains("SkinAnimationData")) {
                    newSkin.setAnimationData(new String(skinTag.getByteArray("SkinAnimationData"), StandardCharsets.UTF_8));
                } else if (skinTag.contains("AnimationData")) { // backwards compatible
                    newSkin.setAnimationData(new String(skinTag.getByteArray("AnimationData"), StandardCharsets.UTF_8));
                }
                if (skinTag.contains("PremiumSkin")) {
                    newSkin.setPremium(skinTag.getBoolean("PremiumSkin"));
                }
                if (skinTag.contains("PersonaSkin")) {
                    newSkin.setPersona(skinTag.getBoolean("PersonaSkin"));
                }
                if (skinTag.contains("CapeOnClassicSkin")) {
                    newSkin.setCapeOnClassic(skinTag.getBoolean("CapeOnClassicSkin"));
                }
                if (skinTag.contains("AnimatedImageData")) {
                    for (CompoundTag animationTag : skinTag.getList("AnimatedImageData", CompoundTag.class).getAll()) {
                        newSkin.getAnimations().add(new SkinAnimation(new SerializedImage(animationTag.getInt("ImageWidth"), animationTag.getInt("ImageHeight"), animationTag.getByteArray("Image")), animationTag.getInt("Type"), animationTag.getFloat("Frames"), animationTag.getInt("AnimationExpression")));
                    }
                }
                if (skinTag.contains("ArmSize")) {
                    newSkin.setArmSize(skinTag.getString("ArmSize"));
                }
                if (skinTag.contains("SkinColor")) {
                    newSkin.setSkinColor(skinTag.getString("SkinColor"));
                }
                if (skinTag.contains("PersonaPieces")) {
                    ListTag<CompoundTag> pieces = skinTag.getList("PersonaPieces", CompoundTag.class);
                    for (CompoundTag piece : pieces.getAll()) {
                        newSkin.getPersonaPieces().add(new PersonaPiece(
                                piece.getString("PieceId"),
                                piece.getString("PieceType"),
                                piece.getString("PackId"),
                                piece.getBoolean("IsDefault"),
                                piece.getString("ProductId")
                        ));
                    }
                }
                if (skinTag.contains("PieceTintColors")) {
                    ListTag<CompoundTag> tintColors = skinTag.getList("PieceTintColors", CompoundTag.class);
                    for (CompoundTag tintColor : tintColors.getAll()) {
                        newSkin.getTintColors().add(new PersonaPieceTint(
                                tintColor.getString("PieceType"),
                                tintColor.getList("Colors", StringTag.class).getAll().stream()
                                        .map(stringTag -> stringTag.data).collect(Collectors.toList())
                        ));
                    }
                }
                if (skinTag.contains("IsTrustedSkin")) {
                    newSkin.setTrusted(skinTag.getBoolean("IsTrustedSkin"));
                }
                this.setSkin(newSkin);
            }

            this.uuid = Utils.dataToUUID(String.valueOf(this.getId()).getBytes(StandardCharsets.UTF_8), this.skin
                    .getSkinData().data, this.getNameTag().getBytes(StandardCharsets.UTF_8));
        } else {
            // HACK: Fix gravity on 1.2.11 and lower
            if (((Player) this).protocol <= 201) {
                this.setDataFlag(DATA_FLAGS, 46, true, false);
            }
        }

        super.initEntity();
    }

    @Override
    public String getName() {
        return this.getNameTag();
    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        if (skin != null) {
            CompoundTag skinTag = new CompoundTag()
                    .putByteArray("Data", this.getSkin().getSkinData().data)
                    .putInt("SkinImageWidth", this.getSkin().getSkinData().width)
                    .putInt("SkinImageHeight", this.getSkin().getSkinData().height)
                    .putString("ModelId", this.skin.getSkinId())
                    .putString("CapeId", this.getSkin().getCapeId())
                    .putByteArray("CapeData", this.getSkin().getCapeData().data)
                    .putInt("CapeImageWidth", this.getSkin().getCapeData().width)
                    .putInt("CapeImageHeight", this.getSkin().getCapeData().height)
                    .putByteArray("SkinResourcePatch", this.getSkin().getSkinResourcePatch().getBytes(StandardCharsets.UTF_8))
                    .putByteArray("GeometryData", this.skin.getGeometryData().getBytes(StandardCharsets.UTF_8))
                    .putByteArray("SkinAnimationData", this.getSkin().getAnimationData().getBytes(StandardCharsets.UTF_8))
                    .putBoolean("PremiumSkin", this.getSkin().isPremium())
                    .putBoolean("PersonaSkin", this.getSkin().isPersona())
                    .putBoolean("CapeOnClassicSkin", this.getSkin().isCapeOnClassic())
                    .putString("ArmSize", this.getSkin().getArmSize())
                    .putString("SkinColor", this.getSkin().getSkinColor())
                    .putBoolean("IsTrustedSkin", this.getSkin().isTrusted());

            List<SkinAnimation> animations = this.getSkin().getAnimations();

            if (!animations.isEmpty()) {
                ListTag<CompoundTag> animationsTag = new ListTag<>("AnimatedImageData");

                for (SkinAnimation animation : animations) {
                    animationsTag.add(new CompoundTag()
                            .putFloat("Frames", animation.frames)
                            .putInt("Type", animation.type)
                            .putInt("ImageWidth", animation.image.width)
                            .putInt("ImageHeight", animation.image.height)
                            .putInt("AnimationExpression", animation.expression)
                            .putByteArray("Image", animation.image.data));
                }

                skinTag.putList(animationsTag);
            }

            List<PersonaPiece> personaPieces = this.getSkin().getPersonaPieces();
            if (!personaPieces.isEmpty()) {
                ListTag<CompoundTag> piecesTag = new ListTag<>("PersonaPieces");
                for (PersonaPiece piece : personaPieces) {
                    piecesTag.add(new CompoundTag().putString("PieceId", piece.id)
                            .putString("PieceType", piece.type)
                            .putString("PackId", piece.packId)
                            .putBoolean("IsDefault", piece.isDefault)
                            .putString("ProductId", piece.productId));
                }
            }

            List<PersonaPieceTint> tints = this.getSkin().getTintColors();
            if (!tints.isEmpty()) {
                ListTag<CompoundTag> tintsTag = new ListTag<>("PieceTintColors");
                for (PersonaPieceTint tint : tints) {
                    ListTag<StringTag> colors = new ListTag<>("Colors");
                    colors.setAll(tint.colors.stream().map(s -> new StringTag("", s)).collect(Collectors.toList()));
                    tintsTag.add(new CompoundTag()
                            .putString("PieceType", tint.pieceType)
                            .putList(colors));
                }
            }

            if (!this.getSkin().getPlayFabId().isEmpty()) {
                skinTag.putString("PlayFabId", this.getSkin().getPlayFabId());
            }

            this.namedTag.putCompound("Skin", skinTag);
        }
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        this.level.addPlayerMovement(this, x, y, z, yaw, pitch, headYaw);
    }

    @Override
    public void spawnTo(Player player) {
        if (this != player && !this.hasSpawned.containsKey(player.getLoaderId())) {
            this.hasSpawned.put(player.getLoaderId(), player);

            if (!this.skin.isValid()) {
                throw new IllegalStateException(this.getClass().getSimpleName() + " must have a valid skin set");
            }

            if (this.isPlayer) {
                this.server.updatePlayerListData(this.uuid, this.getId(), ((Player) this).getDisplayName(), this.skin, ((Player) this).getLoginChainData().getXUID(), new Player[]{player});
            } else {
                this.server.updatePlayerListData(this.uuid, this.getId(), this.getName(), this.skin, new Player[]{player});
            }

            PlayerInventory playerInventory = Objects.requireNonNullElse(this.inventory, EMPTY_INVENTORY);

            AddPlayerPacket pk = new AddPlayerPacket();
            pk.uuid = this.uuid;
            pk.username = this.getName();
            pk.entityUniqueId = this.getId();
            pk.entityRuntimeId = this.getId();
            pk.x = (float) this.x;
            pk.y = (float) this.y;
            pk.z = (float) this.z;
            pk.speedX = (float) this.motionX;
            pk.speedY = (float) this.motionY;
            pk.speedZ = (float) this.motionZ;
            pk.yaw = (float) this.yaw;
            pk.pitch = (float) this.pitch;
            pk.item = playerInventory.getItemInHand();
            pk.metadata = this.dataProperties.clone();
            player.dataPacket(pk);

            if (this.isPlayer) {
                playerInventory.sendArmorContents(player);
            } else {
                playerInventory.sendArmorContentsIfNotAr(player);
            }
            this.offhandInventory.sendContents(player);

            if (this.riding != null) {
                SetEntityLinkPacket pkk = new SetEntityLinkPacket();
                pkk.vehicleUniqueId = this.riding.getId();
                pkk.riderUniqueId = this.getId();
                pkk.type = 1;
                pkk.immediate = 1;
                player.dataPacket(pkk);
            }

            if (!this.isPlayer) {
                this.server.removePlayerListData(this.uuid, player);
            }
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            if (inventory != null && (!(this instanceof Player) || ((Player) this).loggedIn)) {
                for (Player viewer : this.inventory.getViewers()) {
                    viewer.removeWindow(this.inventory);
                }
            }

            super.close();
        }
    }

    @Override
    protected void onBlock(Entity entity, EntityDamageEvent event, boolean animate) {
        super.onBlock(entity, event, animate);
        Item shieldOffhand = getOffhandInventory().getItem(0);
        if (shieldOffhand instanceof ItemShield) {
            getOffhandInventory().setItem(0, damageArmor(shieldOffhand, entity, event.getDamage(), true, event.getCause()));
        } else {
            Item shield = getInventory().getItemInHand();
            if (shield instanceof ItemShield) {
                getInventory().setItemInHand(damageArmor(shield, entity, event.getDamage(), true, event.getCause()));
            }
        }
    }
}
