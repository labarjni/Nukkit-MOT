package cn.nukkit.potion;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.entity.EntitySmite;
import cn.nukkit.entity.mob.EntityBlaze;
import cn.nukkit.entity.mob.EntityEnderman;
import cn.nukkit.entity.mob.EntitySnowGolem;
import cn.nukkit.entity.passive.EntityStrider;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.EntityPotionEffectEvent;
import cn.nukkit.event.entity.EntityRegainHealthEvent;
import cn.nukkit.event.potion.PotionApplyEvent;

import java.util.Locale;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class Potion implements Cloneable {

    public static final int NO_EFFECTS = 0;
    public static final int WATER = 0;
    public static final int MUNDANE = 1;
    public static final int MUNDANE_II = 2;
    public static final int THICK = 3;
    public static final int AWKWARD = 4;
    public static final int NIGHT_VISION = 5;
    public static final int NIGHT_VISION_LONG = 6;
    public static final int INVISIBLE = 7;
    public static final int INVISIBLE_LONG = 8;
    public static final int LEAPING = 9;
    public static final int LEAPING_LONG = 10;
    public static final int LEAPING_II = 11;
    public static final int FIRE_RESISTANCE = 12;
    public static final int FIRE_RESISTANCE_LONG = 13;
    public static final int SPEED = 14;
    public static final int SPEED_LONG = 15;
    public static final int SPEED_II = 16;
    public static final int SLOWNESS = 17;
    public static final int SLOWNESS_LONG = 18;
    public static final int WATER_BREATHING = 19;
    public static final int WATER_BREATHING_LONG = 20;
    public static final int INSTANT_HEALTH = 21;
    public static final int INSTANT_HEALTH_II = 22;
    public static final int HARMING = 23;
    public static final int HARMING_II = 24;
    public static final int POISON = 25;
    public static final int POISON_LONG = 26;
    public static final int POISON_II = 27;
    public static final int REGENERATION = 28;
    public static final int REGENERATION_LONG = 29;
    public static final int REGENERATION_II = 30;
    public static final int STRENGTH = 31;
    public static final int STRENGTH_LONG = 32;
    public static final int STRENGTH_II = 33;
    public static final int WEAKNESS = 34;
    public static final int WEAKNESS_LONG = 35;
    public static final int WITHER_II = 36;
    public static final int TURTLE_MASTER = 37;
    public static final int TURTLE_MASTER_LONG = 38;
    public static final int TURTLE_MASTER_II = 39;
    public static final int SLOW_FALLING = 40;
    public static final int SLOW_FALLING_LONG = 41;
    public static final int SLOWNESS_LONG_II = 42;
    public static final int SLOWNESS_IV = 43;
    public static final int WIND_CHARGED = 44;
    public static final int WEAVING = 45;
    public static final int OOZING = 46;
    public static final int INFESTED = 47;

    protected static Potion[] potions;

    public static void init() {
        potions = new Potion[256];

        potions[Potion.WATER] = new Potion(Potion.WATER);
        potions[Potion.MUNDANE] = new Potion(Potion.MUNDANE);
        potions[Potion.MUNDANE_II] = new Potion(Potion.MUNDANE_II, 2);
        potions[Potion.THICK] = new Potion(Potion.THICK);
        potions[Potion.AWKWARD] = new Potion(Potion.AWKWARD);
        potions[Potion.NIGHT_VISION] = new Potion(Potion.NIGHT_VISION);
        potions[Potion.NIGHT_VISION_LONG] = new Potion(Potion.NIGHT_VISION_LONG);
        potions[Potion.INVISIBLE] = new Potion(Potion.INVISIBLE);
        potions[Potion.INVISIBLE_LONG] = new Potion(Potion.INVISIBLE_LONG);
        potions[Potion.LEAPING] = new Potion(Potion.LEAPING);
        potions[Potion.LEAPING_LONG] = new Potion(Potion.LEAPING_LONG);
        potions[Potion.LEAPING_II] = new Potion(Potion.LEAPING_II, 2);
        potions[Potion.FIRE_RESISTANCE] = new Potion(Potion.FIRE_RESISTANCE);
        potions[Potion.FIRE_RESISTANCE_LONG] = new Potion(Potion.FIRE_RESISTANCE_LONG);
        potions[Potion.SPEED] = new Potion(Potion.SPEED);
        potions[Potion.SPEED_LONG] = new Potion(Potion.SPEED_LONG);
        potions[Potion.SPEED_II] = new Potion(Potion.SPEED_II, 2);
        potions[Potion.SLOWNESS] = new Potion(Potion.SLOWNESS);
        potions[Potion.SLOWNESS_LONG] = new Potion(Potion.SLOWNESS_LONG);
        potions[Potion.WATER_BREATHING] = new Potion(Potion.WATER_BREATHING);
        potions[Potion.WATER_BREATHING_LONG] = new Potion(Potion.WATER_BREATHING_LONG);
        potions[Potion.INSTANT_HEALTH] = new Potion(Potion.INSTANT_HEALTH);
        potions[Potion.INSTANT_HEALTH_II] = new Potion(Potion.INSTANT_HEALTH_II, 2);
        potions[Potion.HARMING] = new Potion(Potion.HARMING);
        potions[Potion.HARMING_II] = new Potion(Potion.HARMING_II, 2);
        potions[Potion.POISON] = new Potion(Potion.POISON);
        potions[Potion.POISON_LONG] = new Potion(Potion.POISON_LONG);
        potions[Potion.POISON_II] = new Potion(Potion.POISON_II, 2);
        potions[Potion.REGENERATION] = new Potion(Potion.REGENERATION);
        potions[Potion.REGENERATION_LONG] = new Potion(Potion.REGENERATION_LONG);
        potions[Potion.REGENERATION_II] = new Potion(Potion.REGENERATION_II, 2);
        potions[Potion.STRENGTH] = new Potion(Potion.STRENGTH);
        potions[Potion.STRENGTH_LONG] = new Potion(Potion.STRENGTH_LONG);
        potions[Potion.STRENGTH_II] = new Potion(Potion.STRENGTH_II, 2);
        potions[Potion.WEAKNESS] = new Potion(Potion.WEAKNESS);
        potions[Potion.WEAKNESS_LONG] = new Potion(Potion.WEAKNESS_LONG);
        potions[Potion.WITHER_II] = new Potion(Potion.WITHER_II, 2);
        potions[Potion.TURTLE_MASTER] = new Potion(Potion.TURTLE_MASTER);
        potions[Potion.TURTLE_MASTER_LONG] = new Potion(Potion.TURTLE_MASTER_LONG);
        potions[Potion.TURTLE_MASTER_II] = new Potion(Potion.TURTLE_MASTER_II, 2);
        potions[Potion.SLOW_FALLING] = new Potion(Potion.SLOW_FALLING);
        potions[Potion.SLOW_FALLING_LONG] = new Potion(Potion.SLOW_FALLING_LONG);
        potions[Potion.SLOWNESS_LONG_II] = new Potion(Potion.SLOWNESS_LONG_II, 2);
        potions[Potion.SLOWNESS_IV] = new Potion(Potion.SLOWNESS, 4);
        potions[Potion.WIND_CHARGED] = new Potion(Potion.WIND_CHARGED);
        potions[Potion.WEAVING] = new Potion(Potion.WEAVING);
        potions[Potion.OOZING] = new Potion(Potion.OOZING);
        potions[Potion.INFESTED] = new Potion(Potion.INFESTED);
    }

    public static Potion getPotion(int id) {
        if (id >= 0 && id < potions.length && potions[id] != null) {
            return potions[id].clone();
        } else {
            return null;
        }
    }

    public static Potion getPotionByName(String name) {
        try {
            byte id = Potion.class.getField(name.toUpperCase(Locale.ROOT)).getByte(null);
            return getPotion(id);
        } catch (Exception e) {
            return null;
        }
    }

    protected final int id;

    protected final int level;

    protected boolean splash;

    public Potion(int id) {
        this(id, 1);
    }

    public Potion(int id, int level) {
        this(id, level, false);
    }

    public Potion(int id, int level, boolean splash) {
        this.id = id;
        this.level = level;
        this.splash = splash;
    }

    public Effect getEffect() {
        return getEffect(this.id, this.splash);
    }

    public int getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public boolean isSplash() {
        return splash;
    }

    public Potion setSplash(boolean splash) {
        this.splash = splash;
        return this;
    }

    public void applyPotion(Entity entity) {
        applyPotion(entity, 0.5);
    }

    public void applyPotion(Entity entity, double health) {
        if (!(entity instanceof EntityLiving)) {
            return;
        }

        if (this.id == WATER && (entity instanceof EntityEnderman || entity instanceof EntityStrider || entity instanceof EntitySnowGolem || entity instanceof EntityBlaze)) {
            entity.attack(new EntityDamageEvent(entity, DamageCause.MAGIC, 1f));
            return;
        }

        Effect applyEffect = getEffect(this.id, this.splash);

        if (applyEffect == null) {
            return;
        }

        /*if (entity instanceof Player) {
            if (!((Player) entity).isSurvival() && !((Player) entity).isAdventure() && applyEffect.isBad()) {
                return;
            }
        }*/

        PotionApplyEvent event = new PotionApplyEvent(this, applyEffect, entity);

        entity.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        applyEffect = event.getApplyEffect();

        switch (this.id) {
            case INSTANT_HEALTH:
            case INSTANT_HEALTH_II:
                if (!entity.canBeAffected(this.id)) {
                    break;
                }
                if (entity instanceof EntitySmite) {
                    entity.attack(new EntityDamageEvent(entity, DamageCause.MAGIC, (float) (health * (6 << (applyEffect.getAmplifier() + 1)))));
                } else {
                    entity.heal(new EntityRegainHealthEvent(entity, (float) (health * (double) (4 << (applyEffect.getAmplifier() + 1))), EntityRegainHealthEvent.CAUSE_MAGIC));
                }
                break;
            case HARMING:
                if (!entity.canBeAffected(this.id)) {
                    break;
                }
                if (entity instanceof EntitySmite) {
                    entity.heal(new EntityRegainHealthEvent(entity, (float) (health * (double) (4 << (applyEffect.getAmplifier() + 1))), EntityRegainHealthEvent.CAUSE_MAGIC));
                } else {
                    entity.attack(new EntityDamageEvent(entity, DamageCause.MAGIC, (float) (health * 6)));
                }
                break;
            case HARMING_II:
                if (!entity.canBeAffected(this.id)) {
                    break;
                }
                if (entity instanceof EntitySmite) {
                    entity.heal(new EntityRegainHealthEvent(entity, (float) (health * (double) (4 << (applyEffect.getAmplifier() + 1))), EntityRegainHealthEvent.CAUSE_MAGIC));
                } else {
                    entity.attack(new EntityDamageEvent(entity, DamageCause.MAGIC, (float) (health * 12)));
                }
                break;
            default:
                applyEffect.setDuration((int) ((splash ? health : 1) * (double) applyEffect.getDuration() + 0.5));
                entity.addEffect(applyEffect, this.splash ? EntityPotionEffectEvent.Cause.POTION_SPLASH : EntityPotionEffectEvent.Cause.POTION_DRINK);
        }
    }

    @Override
    public Potion clone() {
        try {
            return (Potion) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public static Effect getEffect(int potionType, boolean isSplash) {
        Effect effect;
        switch (potionType) {
            case NO_EFFECTS:
            case MUNDANE:
            case MUNDANE_II:
            case THICK:
            case AWKWARD:
                return null;
            case NIGHT_VISION:
            case NIGHT_VISION_LONG:
                effect = Effect.getEffect(Effect.NIGHT_VISION);
                break;
            case INVISIBLE:
            case INVISIBLE_LONG:
                effect = Effect.getEffect(Effect.INVISIBILITY);
                break;
            case LEAPING:
            case LEAPING_LONG:
            case LEAPING_II:
                effect = Effect.getEffect(Effect.JUMP);
                break;
            case FIRE_RESISTANCE:
            case FIRE_RESISTANCE_LONG:
                effect = Effect.getEffect(Effect.FIRE_RESISTANCE);
                break;
            case SPEED:
            case SPEED_LONG:
            case SPEED_II:
                effect = Effect.getEffect(Effect.SPEED);
                break;
            case SLOWNESS:
            case SLOWNESS_LONG:
            case SLOWNESS_LONG_II:
            case SLOWNESS_IV:
                effect = Effect.getEffect(Effect.SLOWNESS);
                break;
            case WATER_BREATHING:
            case WATER_BREATHING_LONG:
                effect = Effect.getEffect(Effect.WATER_BREATHING);
                break;
            case INSTANT_HEALTH:
            case INSTANT_HEALTH_II:
                return Effect.getEffect(Effect.HEALING);
            case HARMING:
            case HARMING_II:
                return Effect.getEffect(Effect.HARMING);
            case POISON:
            case POISON_LONG:
            case POISON_II:
                effect = Effect.getEffect(Effect.POISON);
                break;
            case REGENERATION:
            case REGENERATION_LONG:
            case REGENERATION_II:
                effect = Effect.getEffect(Effect.REGENERATION);
                break;
            case STRENGTH:
            case STRENGTH_LONG:
            case STRENGTH_II:
                effect = Effect.getEffect(Effect.STRENGTH);
                break;
            case WEAKNESS:
            case WEAKNESS_LONG:
                effect = Effect.getEffect(Effect.WEAKNESS);
                break;
            case WITHER_II:
                effect = Effect.getEffect(Effect.WITHER);
                break;
            case WIND_CHARGED:
                effect = Effect.getEffect(Effect.WIND_CHARGED);
                break;
            case WEAVING:
                effect = Effect.getEffect(Effect.WEAVING);
                break;
            case OOZING:
                effect = Effect.getEffect(Effect.OOZING);
                break;
            case INFESTED:
                effect = Effect.getEffect(Effect.INFESTED);
                break;
            default:
                return null;
        }

        if (getLevel(potionType) > 1) {
            effect.setAmplifier(1);
        }

        if (!isInstant(potionType)) {
            effect.setDuration(20 * getApplySeconds(potionType, isSplash));
        }

        return effect;
    }

    public static int getLevel(int potionType) {
        switch (potionType) {
            case SLOWNESS_IV:
                return 4;
            case MUNDANE_II:
            case LEAPING_II:
            case SPEED_II:
            case INSTANT_HEALTH_II:
            case HARMING_II:
            case POISON_II:
            case REGENERATION_II:
            case STRENGTH_II:
            case WITHER_II:
            case TURTLE_MASTER_II:
            case SLOWNESS_LONG_II:
                return 2;
            default:
                return 1;
        }
    }

    public static boolean isInstant(int potionType) {
        switch (potionType) {
            case INSTANT_HEALTH:
            case INSTANT_HEALTH_II:
            case HARMING:
            case HARMING_II:
                return true;
            default:
                return false;
        }
    }

    public static int getApplySeconds(int potionType, boolean isSplash) {
        if (isSplash) {
            return switch (potionType) {
                case NO_EFFECTS -> 0;
                case MUNDANE -> 0;
                case MUNDANE_II -> 0;
                case THICK -> 0;
                case AWKWARD -> 0;
                case NIGHT_VISION -> 135;
                case NIGHT_VISION_LONG -> 360;
                case INVISIBLE -> 135;
                case INVISIBLE_LONG -> 360;
                case LEAPING -> 135;
                case LEAPING_LONG -> 360;
                case LEAPING_II -> 67;
                case FIRE_RESISTANCE -> 135;
                case FIRE_RESISTANCE_LONG -> 360;
                case SPEED -> 135;
                case SPEED_LONG -> 360;
                case SPEED_II -> 67;
                case SLOWNESS -> 67;
                case SLOWNESS_LONG -> 180;
                case WATER_BREATHING -> 135;
                case WATER_BREATHING_LONG -> 360;
                case INSTANT_HEALTH -> 0;
                case INSTANT_HEALTH_II -> 0;
                case HARMING -> 0;
                case HARMING_II -> 0;
                case POISON -> 33;
                case POISON_LONG -> 90;
                case POISON_II -> 16;
                case REGENERATION -> 33;
                case REGENERATION_LONG -> 90;
                case REGENERATION_II -> 16;
                case STRENGTH -> 135;
                case STRENGTH_LONG -> 360;
                case STRENGTH_II -> 67;
                case WEAKNESS -> 67;
                case WEAKNESS_LONG -> 180;
                case WITHER_II -> 30;
                case SLOWNESS_IV -> 15;
                default -> 0;
            };
        } else {
            return switch (potionType) {
                case NO_EFFECTS -> 0;
                case MUNDANE -> 0;
                case MUNDANE_II -> 0;
                case THICK -> 0;
                case AWKWARD -> 0;
                case NIGHT_VISION -> 180;
                case NIGHT_VISION_LONG -> 480;
                case INVISIBLE -> 180;
                case INVISIBLE_LONG -> 480;
                case LEAPING -> 180;
                case LEAPING_LONG -> 480;
                case LEAPING_II -> 90;
                case FIRE_RESISTANCE -> 180;
                case FIRE_RESISTANCE_LONG -> 480;
                case SPEED -> 180;
                case SPEED_LONG -> 480;
                case SPEED_II -> 90;
                case SLOWNESS -> 90;
                case SLOWNESS_LONG -> 240;
                case WATER_BREATHING -> 180;
                case WATER_BREATHING_LONG -> 480;
                case INSTANT_HEALTH -> 0;
                case INSTANT_HEALTH_II -> 0;
                case HARMING -> 0;
                case HARMING_II -> 0;
                case POISON -> 45;
                case POISON_LONG -> 120;
                case POISON_II -> 22;
                case REGENERATION -> 45;
                case REGENERATION_LONG -> 120;
                case REGENERATION_II -> 22;
                case STRENGTH -> 180;
                case STRENGTH_LONG -> 480;
                case STRENGTH_II -> 90;
                case WEAKNESS -> 90;
                case WEAKNESS_LONG -> 240;
                case WITHER_II -> 30;
                case SLOWNESS_IV -> 20;
                default -> 0;
            };
        }
    }
}
