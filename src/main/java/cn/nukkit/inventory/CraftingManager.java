package cn.nukkit.inventory;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.inventory.special.RepairItemRecipe;
import cn.nukkit.item.Item;
import cn.nukkit.network.protocol.BatchPacket;
import cn.nukkit.network.protocol.CraftingDataPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.*;
import io.netty.util.collection.CharObjectHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nullable;
import java.util.*;
import java.util.zip.Deflater;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class CraftingManager {

    private static BatchPacket packet313;
    private static BatchPacket packet340;
    private static BatchPacket packet361;
    private static BatchPacket packet354;
    private static BatchPacket packet388;
    private static BatchPacket packet407;
    private static BatchPacket packet419;
    private static BatchPacket packet431;
    private static BatchPacket packet440;
    private static BatchPacket packet448;
    private static BatchPacket packet465;
    private static BatchPacket packet471;
    private static BatchPacket packet486;
    private static BatchPacket packet503;
    private static BatchPacket packet527;
    private static BatchPacket packet544;
    private static BatchPacket packet554;
    private static BatchPacket packet560;
    private static BatchPacket packet567;
    private static BatchPacket packet575;
    private static BatchPacket packet582;
    private static BatchPacket packet589;
    private static BatchPacket packet594;
    private static BatchPacket packet618;
    private static BatchPacket packet622;
    private static BatchPacket packet630;
    private static BatchPacket packet649;
    private static BatchPacket packet662;
    private static BatchPacket packet671;
    private static BatchPacket packet685;
    private static BatchPacket packet712;
    private static BatchPacket packet729;
    private static BatchPacket packet748;
    private static BatchPacket packet766;
    private static BatchPacket packet776;

    public final Collection<Recipe> recipes = new ArrayDeque<>();

    private final Map<Integer, Map<UUID, ShapedRecipe>> shapedRecipes = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<UUID, ShapelessRecipe>> shapelessRecipes = new Int2ObjectOpenHashMap<>();

    public final Map<UUID, MultiRecipe> multiRecipes = new HashMap<>();

    public final Map<Integer, FurnaceRecipe> furnaceRecipes = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, BlastFurnaceRecipe> blastFurnaceRecipes = new Int2ObjectOpenHashMap<>();

    public final Map<Integer, BrewingRecipe> brewingRecipes = new Int2ObjectOpenHashMap<>();
    public final Map<Integer, ContainerRecipe> containerRecipes = new Int2ObjectOpenHashMap<>();
    public final Map<Integer, CampfireRecipe> campfireRecipes = new Int2ObjectOpenHashMap<>();
    private final Map<UUID, SmithingRecipe> smithingRecipes = new Object2ObjectOpenHashMap<>();

    private final Object2DoubleOpenHashMap<Recipe> recipeXpMap = new Object2DoubleOpenHashMap<>();

    private static int RECIPE_COUNT = 0;
    static int NEXT_NETWORK_ID = 1;

    public static final Comparator<Item> recipeComparator = (i1, i2) -> {
        if (i1.getId() > i2.getId()) {
            return 1;
        } else if (i1.getId() < i2.getId()) {
            return -1;
        } else {
            if (!i1.isNull() && !i2.isNull()) {
                int i = MinecraftNamespaceComparator.compareFNV(i1.getNamespaceId(ProtocolInfo.CURRENT_PROTOCOL), i2.getNamespaceId(ProtocolInfo.CURRENT_PROTOCOL));
                if (i != 0) {
                    return i;
                }
            }
            if (i1.getDamage() > i2.getDamage()) {
                return 1;
            } else if (i1.getDamage() < i2.getDamage()) {
                return -1;
            } else return Integer.compare(i1.getCount(), i2.getCount());
        }
    };

    @SuppressWarnings("unchecked")
    public CraftingManager() {
        MainLogger.getLogger().debug("Loading recipes...");

        this.registerMultiRecipe(new RepairItemRecipe());

        ConfigSection recipesConfig = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("recipes.json")).getRootSection();
        ConfigSection recipes_smithing_config = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("recipes_smithing.json")).getRootSection();
        Config furnaceXpConfig = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("recipes/furnace_xp.json"));

        this.loadRecipes(recipesConfig, furnaceXpConfig);

        // Smithing recipes 锻造配方
        for (Map<String, Object> recipe : (List<Map<String, Object>>)recipes_smithing_config.get((Object)"smithing")) {
            List<Map> outputs = ((List<Map>) recipe.get("output"));
            if (outputs.size() > 1) {
                continue;
            }

            String recipeId = (String) recipe.get("id");
            int priority = Math.max(Utils.toInt(recipe.get("priority")) - 1, 0);

            Map<String, Object> first = outputs.get(0);
            Item item = Item.fromJson(first, true);

            List<Item> sorted = new ArrayList<>();
            for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
                sorted.add(Item.fromJson(ingredient, true));
            }

            this.registerRecipe(new SmithingRecipe(recipeId, priority, sorted, item));
        }

        this.rebuildPacket();
        MainLogger.getLogger().debug("Loaded " + this.recipes.size() + " recipes");
    }

    private void loadRecipes(ConfigSection configSection, Config furnaceXpConfig) {
        List<Map<String, Object>> shapedRecipesList = new ArrayList<>();
        List<Map<String, Object>> shapelessRecipesList = new ArrayList<>();
        List<Map<String, Object>> furnaceRecipesList = new ArrayList<>();
        List<Map<String, Object>> shulkerBoxRecipesList = new ArrayList<>();

        for (Map<String, Object> entry : (List<Map<String, Object>>) configSection.get("recipes")) {
            switch ((Integer) entry.getOrDefault("type", -1)) {
                case 0: // shapeless - Check block
                    shapelessRecipesList.add(entry);
                    break;
                case 1: // shaped
                    shapedRecipesList.add(entry);
                    break;
                case 3: // furnace
                    furnaceRecipesList.add(entry);
                    break;
                case 4: // hardcoded recipes
                    // Ignore type 4
                    break;
                case 5:
                    shulkerBoxRecipesList.add(entry);
                    break;
            }
        }
        for (ShapedRecipe recipe : loadShapedRecipes(shapedRecipesList)) {
            this.registerRecipe(recipe);
        }

        for (ShapelessRecipe recipe : loadShapelessRecips(shapelessRecipesList)) {
            this.registerRecipe(recipe);
        }

        for (SmeltingRecipe recipe : loadSmeltingRecipes(furnaceRecipesList, furnaceXpConfig)) {
            this.registerRecipe(recipe);
        }
        // TODO: shapeless_shulker_box
    }

    private List<ShapedRecipe> loadShapedRecipes(List<Map<String, Object>> recipes) {
        List<ShapedRecipe> recipesList = new ObjectArrayList<>();
        for (Map<String, Object> recipe : recipes) {
            top:
            {
                if (!"crafting_table".equals(recipe.get("block"))) {
                    // Ignore other recipes than crafting table ones
                    continue;
                }
                List<Map> outputs = ((List<Map>) recipe.get("output"));
                Map<String, Object> first = outputs.remove(0);
                String[] shape = ((List<String>) recipe.get("shape")).toArray(new String[0]);
                Map<Character, Item> ingredients = new CharObjectHashMap();
                List<Item> extraResults = new ArrayList();
                Map<String, Map<String, Object>> input = (Map) recipe.get("input");
                for (Map.Entry<String, Map<String, Object>> ingredientEntry : input.entrySet()) {
                    char ingredientChar = ingredientEntry.getKey().charAt(0);
                    Item ingredient = Item.fromJson(ingredientEntry.getValue(), true);
                    if (ingredient == null) {
                        break top;
                    }

                    ingredients.put(ingredientChar, ingredient);
                }

                for (Map<String, Object> data : outputs) {
                    Item eItem = Item.fromJson(data, true);
                    if (eItem == null) {
                        break top;
                    }
                    extraResults.add(eItem);
                }

                String recipeId = (String) recipe.get("id");
                int priority = Utils.toInt(recipe.get("priority"));
                Item result = Item.fromJson(first, true);
                if (result == null) {
                    continue ;
                }

                recipesList.add(new ShapedRecipe(recipeId, priority, result, shape, ingredients, extraResults));
            }
        }

        return recipesList;
    }

    private List<ShapelessRecipe> loadShapelessRecips(List<Map<String, Object>> recipes) {
        ArrayList<ShapelessRecipe> recipesList = new ArrayList<>();
        for (Map<String, Object> recipe : recipes) {
            top:
            {
                if (!"crafting_table".equals(recipe.get("block"))) {
                    // Ignore other recipes than crafting table ones
                    continue;
                }
                // TODO: handle multiple result items
                List<Map> outputs = ((List<Map>) recipe.get("output"));
                if (outputs.size() > 1) {
                    continue;
                }

                String recipeId = (String) recipe.get("id");
                int priority = Math.max(Utils.toInt(recipe.get("priority")) - 1, 0);

                Map<String, Object> first = outputs.get(0);
                Item item = Item.fromJson(first, true);
                if (item == null) {
                    continue;
                }

                List<Item> sorted = new ArrayList<>();
                for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
                    Item sortedItem = Item.fromJson(ingredient, true);
                    if (sortedItem == null) {
                        break top;
                    }
                    sorted.add(sortedItem);
                }
                // Bake sorted list
                sorted.sort(recipeComparator);

                recipesList.add(new ShapelessRecipe(recipeId, priority, item, sorted));
            }
        }
        return recipesList;
    }

    private List<SmeltingRecipe> loadSmeltingRecipes(List<Map<String, Object>> recipes, Config furnaceXpConfig) {
        ArrayList<SmeltingRecipe> recipesList = new ArrayList<>();
        for (Map<String, Object> recipe : recipes) {
            String craftingBlock = (String)recipe.get("block");
            if (!"furnace".equals(craftingBlock)
                    && !"blast_furnace".equals(craftingBlock)
                    && !"campfire".equals(craftingBlock)) {
                continue;
            }

            Map<String, Object> resultMap = (Map) recipe.get("output");
            Item resultItem = Item.fromJson(resultMap, true);
            if (resultItem == null) {
                continue;
            }
            Map<String, Object> inputMap = (Map) recipe.get("input");
            Item inputItem = Item.fromJson(inputMap, true);
            if (inputItem == null) {
                continue;
            }
            switch (craftingBlock) {
                case "furnace": {
                    FurnaceRecipe furnaceRecipe = new FurnaceRecipe(resultItem, inputItem);
                    double xp = furnaceXpConfig.getDouble(inputItem.getNamespaceId(ProtocolInfo.CURRENT_PROTOCOL) + ":" + inputItem.getDamage(), 0d);
                    if (xp != 0) {
                        this.setRecipeXp(furnaceRecipe, xp);
                    }
                    recipesList.add(furnaceRecipe);
                    break;
                }
                case "blast_furnace": {
                    BlastFurnaceRecipe furnaceRecipe = new BlastFurnaceRecipe(resultItem, inputItem);
                    double xp = furnaceXpConfig.getDouble(inputItem.getNamespaceId(ProtocolInfo.CURRENT_PROTOCOL) + ":" + inputItem.getDamage(), 0d);
                    if (xp != 0) {
                        this.setRecipeXp(furnaceRecipe, xp);
                    }
                    recipesList.add(furnaceRecipe);
                    break;
                }
                case "campfire": {
                    recipesList.add(new CampfireRecipe(resultItem, inputItem));
                }
            }
        }
        return recipesList;
    }

    private BatchPacket packetFor(int protocol) {
        CraftingDataPacket pk = new CraftingDataPacket();
        pk.protocol = protocol;
        recipeLoop:
        for (Recipe recipe : this.recipes) {
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                for (Item resultItem : shapedRecipe.getAllResults()) {
                    if (!resultItem.isSupportedOn(protocol)) {
                        continue recipeLoop;
                    }
                }
                for (Item ingredient : shapedRecipe.getIngredientList()) {
                    if (!ingredient.isSupportedOn(protocol)) {
                        continue recipeLoop;
                    }
                }
                pk.addShapedRecipe(shapedRecipe);
            } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                if (!recipe.getResult().isSupportedOn(protocol)) {
                    continue;
                }
                for (Item ingredient : shapelessRecipe.getIngredientList()) {
                    if (!ingredient.isSupportedOn(protocol)) {
                        continue recipeLoop;
                    }
                }
                pk.addShapelessRecipe(shapelessRecipe);
            }
        }
        for (SmithingRecipe recipe : this.smithingRecipes.values()) {
            if (!recipe.getTemplate().isSupportedOn(protocol)
                    || !recipe.getIngredient().isSupportedOn(protocol)
                    || !recipe.getEquipment().isSupportedOn(protocol)
                    || !recipe.getResult().isSupportedOn(protocol)) {
                continue;
            }
            pk.addShapelessRecipe(recipe);
        }
        for (FurnaceRecipe recipe : this.furnaceRecipes.values()) {
            if (!recipe.getInput().isSupportedOn(protocol)
                    || !recipe.getResult().isSupportedOn(protocol)) {
                continue;
            }
            pk.addFurnaceRecipe(recipe);
        }
        if (protocol >= ProtocolInfo.v1_13_0) {
            for (BrewingRecipe recipe : this.brewingRecipes.values()) {
                if (!recipe.getInput().isSupportedOn(protocol)
                        || !recipe.getIngredient().isSupportedOn(protocol)
                        || !recipe.getResult().isSupportedOn(protocol)) {
                    continue;
                }
                pk.addBrewingRecipe(recipe);
            }
            for (ContainerRecipe recipe : this.containerRecipes.values()) {
                if (!recipe.getInput().isSupportedOn(protocol)
                        || !recipe.getIngredient().isSupportedOn(protocol)
                        || !recipe.getResult().isSupportedOn(protocol)) {
                    continue;
                }
                pk.addContainerRecipe(recipe);
            }
            if (protocol >= ProtocolInfo.v1_16_0) {
                for (MultiRecipe recipe : this.multiRecipes.values()) {
                    pk.addMultiRecipe(recipe);
                }
            }
        }
        pk.tryEncode();
        return pk.compress(Deflater.BEST_COMPRESSION);
    }

    public void rebuildPacket() {
        //TODO Multiversion 添加新版本支持时修改这里
        packet776 = null;
        packet766 = null;
        packet748 = null;
        packet729 = null;
        packet712 = null;
        packet685 = null;
        packet671 = null;
        packet662 = null;
        packet649 = null;
        packet630 = null;
        packet622 = null;
        packet618 = null;
        packet594 = null;
        packet589 = null;
        packet582 = null;
        packet575 = null;
        packet567 = null;
        packet560 = null;
        packet554 = null;
        packet544 = null;
        packet527 = null;
        packet503 = null;
        packet486 = null;
        packet471 = null;
        packet465 = null;
        packet448 = null;
        packet440 = null;
        packet431 = null;
        packet419 = null;
        packet407 = null;
        packet388 = null;
        packet361 = null;
        packet354 = null;
        packet340 = null;
        packet313 = null;

        this.getCachedPacket(ProtocolInfo.CURRENT_PROTOCOL); // 缓存当前协议版本的数据包
    }

    /**
     * 获取缓存的数据包，根据不同的协议版本返回对应的数据包实例。<br/>
     * 该方法通过检查协议版本号，选择合适的缓存数据包。如果缓存中没有对应的数据包，则创建一个新的并缓存起来。<br/>
     * Get cached data packet based on the protocol version.<br/>
     * Choose the appropriate cached data packet based on the protocol version. If no cached data packet is found, create a new one and cache it.
     *
     * @param protocol 协议版本号，用于确定使用哪个缓存的数据包 <br/>
     *                 Protocol version used to determine which cached data packet to use
     * @return 返回对应协议版本的缓存数据包，如果没有找到对应的缓存，则返回null <br/>
     * Return the cached data packet for the specified protocol version, or null if no cached data packet is found
     */
    public BatchPacket getCachedPacket(int protocol) {
        //TODO Multiversion 添加新版本支持时修改这里
        if (protocol >= ProtocolInfo.v1_21_60) {
            if (packet776 == null) {
                packet776 = packetFor(ProtocolInfo.v1_21_60);
            }
            return packet776;
        } else if (protocol >= ProtocolInfo.v1_21_50_26) {
            if (packet766 == null) {
                packet766 = packetFor(ProtocolInfo.v1_21_50);
            }
            return packet766;
        } else if (protocol >= ProtocolInfo.v1_21_40) {
            if (packet748 == null) {
                packet748 = packetFor(ProtocolInfo.v1_21_40);
            }
            return packet748;
        } else if (protocol >= ProtocolInfo.v1_21_30) {
            if (packet729 == null) {
                packet729 = packetFor(ProtocolInfo.v1_21_30);
            }
            return packet729;
        } else if (protocol >= ProtocolInfo.v1_21_20) {
            if (packet712 == null) {
                packet712 = packetFor(ProtocolInfo.v1_21_20);
            }
            return packet712;
        } else if (protocol >= ProtocolInfo.v1_21_0) {
            if (packet685 == null) {
                packet685 = packetFor(ProtocolInfo.v1_21_0);
            }
            return packet685;
        } else if (protocol >= ProtocolInfo.v1_20_80) {
            if (packet671 == null) {
                packet671 = packetFor(ProtocolInfo.v1_20_80);
            }
            return packet671;
        } else if (protocol >= ProtocolInfo.v1_20_70) {
            if (packet662 == null) {
                packet662 = packetFor(ProtocolInfo.v1_20_70);
            }
            return packet662;
        } else if (protocol >= ProtocolInfo.v1_20_60) {
            if (packet649 == null) {
                packet649 = packetFor(ProtocolInfo.v1_20_60);
            }
            return packet649;
        } else if (protocol >= ProtocolInfo.v1_20_50) {
            if (packet630 == null) {
                packet630 = packetFor(ProtocolInfo.v1_20_50);
            }
            return packet630;
        } else if (protocol >= ProtocolInfo.v1_20_40) {
            if (packet622 == null) {
                packet622 = packetFor(ProtocolInfo.v1_20_40);
            }
            return packet622;
        } else if (protocol >= ProtocolInfo.v1_20_30_24) {
            if (packet618 == null) {
                packet618 = packetFor(ProtocolInfo.v1_20_30);
            }
            return packet618;
        } else if (protocol >= ProtocolInfo.v1_20_10_21) {
            if (packet594 == null) {
                packet594 = packetFor(ProtocolInfo.v1_20_10);
            }
            return packet594;
        } else if (protocol >= ProtocolInfo.v1_20_0_23) {
            if (packet589 == null) {
                packet589 = packetFor(ProtocolInfo.v1_20_0);
            }
            return packet589;
        } else if (protocol >= ProtocolInfo.v1_19_80) {
            if (packet582 == null) {
                packet582 = packetFor(ProtocolInfo.v1_19_80);
            }
            return packet582;
        } else if (protocol >= ProtocolInfo.v1_19_70_24) {
            if (packet575 == null) {
                packet575 = packetFor(ProtocolInfo.v1_19_70);
            }
            return packet575;
        } else if (protocol >= ProtocolInfo.v1_19_60) {
            if (packet567 == null) {
                packet567 = packetFor(ProtocolInfo.v1_19_60);
            }
            return packet567;
        } else if (protocol >= ProtocolInfo.v1_19_50_20) {
            if (packet560 == null) {
                packet560 = packetFor(ProtocolInfo.v1_19_50);
            }
            return packet560;
        } else if (protocol >= ProtocolInfo.v1_19_30_23) {
            if (packet554 == null) {
                packet554 = packetFor(ProtocolInfo.v1_19_30);
            }
            return packet554;
        } else if (protocol >= ProtocolInfo.v1_19_20) {
            if (packet544 == null) {
                packet544 = packetFor(ProtocolInfo.v1_19_20);
            }
            return packet544;
        } else if (protocol >= ProtocolInfo.v1_19_0_29) {
            if (packet527 == null) {
                packet527 = packetFor(ProtocolInfo.v1_19_0);
            }
            return packet527;
        } else if (protocol >= ProtocolInfo.v1_18_30) {
            if (packet503 == null) {
                packet503 = packetFor(ProtocolInfo.v1_18_30);
            }
            return packet503;
        } else if (protocol >= ProtocolInfo.v1_18_10_26) {
            if (packet486 == null) {
                packet486 = packetFor(ProtocolInfo.v1_18_10);
            }
            return packet486;
        } else if (protocol >= ProtocolInfo.v1_17_40) {
            if (packet471 == null) {
                packet471 = packetFor(ProtocolInfo.v1_17_40);
            }
            return packet471;
        } else if (protocol >= ProtocolInfo.v1_17_30) {
            if (packet465 == null) {
                packet465 = packetFor(ProtocolInfo.v1_17_30);
            }
            return packet465;
        } else if (protocol >= ProtocolInfo.v1_17_10) {
            if (packet448 == null) {
                packet448 = packetFor(ProtocolInfo.v1_17_10);
            }
            return packet448;
        } else if (protocol >= ProtocolInfo.v1_17_0) {
            if (packet440 == null) {
                packet440 = packetFor(ProtocolInfo.v1_17_0);
            }
            return packet440;
        } else if (protocol >= ProtocolInfo.v1_16_220) {
            if (packet431 == null) {
                packet431 = packetFor(ProtocolInfo.v1_16_220);
            }
            return packet431;
        } else if (protocol >= ProtocolInfo.v1_16_100) {
            if (packet419 == null) {
                packet419 = packetFor(ProtocolInfo.v1_16_100);
            }
            return packet419;
        } else if (protocol >= ProtocolInfo.v1_16_0) {
            if (packet407 == null) {
                packet407 = packetFor(ProtocolInfo.v1_16_0);
            }
            return packet407;
        } else if (protocol >= ProtocolInfo.v1_13_0) {
            if (packet388 == null) {
                packet388 = packetFor(ProtocolInfo.v1_13_0);
            }
            return packet388;
        } else if (protocol == ProtocolInfo.v1_12_0) {
            if (packet361 == null) {
                packet361 = packetFor(ProtocolInfo.v1_12_0);
            }
            return packet361;
        } else if (protocol == ProtocolInfo.v1_11_0) {
            if (packet354 == null) {
                packet354 = packetFor(ProtocolInfo.v1_11_0);
            }
            return packet354;
        } else if (protocol == ProtocolInfo.v1_10_0) {
            if (packet340 == null) {
                packet340 = packetFor(ProtocolInfo.v1_10_0);
            }
            return packet340;
        } else if (protocol == ProtocolInfo.v1_9_0 || protocol == ProtocolInfo.v1_8_0 || protocol == ProtocolInfo.v1_7_0) { // these should work just fine
            if (packet313 == null) {
                packet313 = packetFor(ProtocolInfo.v1_8_0);
            }
            return packet313;
        }
        return null;
    }

    public Map<UUID, SmithingRecipe> getSmithingRecipes() {
        return smithingRecipes;
    }

    public Collection<Recipe> getRecipes() {
        return this.recipes;
    }

    public Map<Integer, FurnaceRecipe> getFurnaceRecipes() {
        return this.furnaceRecipes;
    }

    public Map<Integer, BlastFurnaceRecipe> getBlastFurnaceRecipes() {
        return this.blastFurnaceRecipes;
    }

    public Map<Integer, ContainerRecipe> getContainerRecipes() {
        return this.containerRecipes;
    }

    public Map<Integer, BrewingRecipe> getBrewingRecipes() {
        return this.brewingRecipes;
    }

    public Map<UUID, MultiRecipe> getMultiRecipes() {
        return this.multiRecipes;
    }

    public MultiRecipe getMultiRecipe(Player player, Item outputItem, List<Item> inputs) {
        return this.multiRecipes.values().stream().filter(multiRecipe -> multiRecipe.canExecute(player, outputItem, inputs)).findFirst().orElse(null);
    }

    public FurnaceRecipe matchFurnaceRecipe(Item input) {
        Map<Integer, FurnaceRecipe> recipes = this.getFurnaceRecipes();
        FurnaceRecipe recipe = recipes.get(getItemHash(input));
        if (recipe == null) recipe = recipes.get(getItemHash(input, 0));
        return recipe;
    }

    public FurnaceRecipe matchBlastFurnaceRecipe(Item input) {
        Map<Integer, BlastFurnaceRecipe> recipes = this.getBlastFurnaceRecipes();
        if (recipes == null) {
            return null;
        }
        FurnaceRecipe recipe = recipes.get(getItemHash(input));
        if (recipe == null) recipe = recipes.get(getItemHash(input, 0));
        return recipe;
    }

    public static UUID getMultiItemHash(Collection<Item> items) {
        BinaryStream stream = new BinaryStream(items.size() * 5);
        for (Item item : items) {
            stream.putVarInt(getFullItemHash(item)); //putVarInt 5 byte
        }
        return UUID.nameUUIDFromBytes(stream.getBuffer());
    }

    private static int getFullItemHash(Item item) {
        //return 31 * getItemHash(item) + item.getCount();
        return (getItemHash(item) << 6) | (item.getCount() & 0x3f);
    }

    public void registerFurnaceRecipe(FurnaceRecipe recipe) {
        if (recipe instanceof BlastFurnaceRecipe) {
            this.registerBlastFurnaceRecipe((BlastFurnaceRecipe) recipe);
            return;
        }
        this.furnaceRecipes.put(getItemHash(recipe.getInput()), recipe);
    }

    public void registerBlastFurnaceRecipe(BlastFurnaceRecipe recipe) {
        this.getBlastFurnaceRecipes().put(getItemHash(recipe.getInput()), recipe);
    }

    public void registerCampfireRecipe(CampfireRecipe recipe) {
        Item input = recipe.getInput();
        this.campfireRecipes.put(getItemHash(input), recipe);
    }

    private static int getItemHash(Item item) {
        return getItemHash(item, item.getDamage());
    }

    private static int getItemHash(Item item, int meta) {
        int id = item.getId() == Item.STRING_IDENTIFIED_ITEM ? item.getNetworkId(ProtocolInfo.CURRENT_PROTOCOL) : item.getId();
        return (id << 12) | (meta & 0xfff);
    }

    public Map<Integer, Map<UUID, ShapedRecipe>> getShapedRecipes() {
        return this.shapedRecipes;
    }

    public void registerShapedRecipe(ShapedRecipe recipe) {
        int resultHash = getItemHash(recipe.getResult());
        Map<UUID, ShapedRecipe> map = this.shapedRecipes.computeIfAbsent(resultHash, n -> new HashMap<>());
        map.put(getMultiItemHash(new LinkedList<>(recipe.getIngredientsAggregate())), recipe);
    }

    public void registerRecipe(Recipe recipe) {
        if (recipe instanceof CraftingRecipe) {
            UUID id = Utils.dataToUUID(String.valueOf(++RECIPE_COUNT), String.valueOf(recipe.getResult().getId()), String.valueOf(recipe.getResult().getDamage()), String.valueOf(recipe.getResult().getCount()), Arrays.toString(recipe.getResult().getCompoundTag()));
            ((CraftingRecipe) recipe).setId(id);
            this.recipes.add(recipe);
        } else {
            recipe.registerToCraftingManager(this);
        }
    }

    public Map<Integer, Map<UUID, ShapelessRecipe>> getShapelessRecipes() {
        return this.shapelessRecipes;
    }

    public void registerShapelessRecipe(ShapelessRecipe recipe) {
        List<Item> list = recipe.getIngredientsAggregate();
        UUID hash = getMultiItemHash(list);
        int resultHash = getItemHash(recipe.getResult());
        Map<UUID, ShapelessRecipe> map = shapelessRecipes.computeIfAbsent(resultHash, k -> new HashMap<>());
        map.put(hash, recipe);
    }

    private static int getPotionHash(Item ingredient, Item potion) {
        int ingredientHash = ((ingredient.getId() & 0x3FF) << 6) | (ingredient.getDamage() & 0x3F);
        int potionHash = ((potion.getId() & 0x3FF) << 6) | (potion.getDamage() & 0x3F);
        return ingredientHash << 16 | potionHash;
    }

    private static int getPotionHashOld(int ingredientId, int potionType) {
        //return (ingredientId << 6) | potionType;
        return (ingredientId << 15) | potionType;
    }

    private static int getContainerHash(int ingredientId, int containerId) {
        //return (ingredientId << 9) | containerId;
        return (ingredientId << 15) | containerId;
    }

    public void registerSmithingRecipe(SmithingRecipe recipe) {
        UUID multiItemHash = getMultiItemHash(recipe.getIngredientsAggregate());
        this.smithingRecipes.put(multiItemHash, recipe);
    }

    public void registerBrewingRecipe(BrewingRecipe recipe) {
        Item input = recipe.getIngredient();
        Item potion = recipe.getInput();
        int potionHash = getPotionHash(input, potion);
        this.brewingRecipes.put(potionHash, recipe);
    }

    public void registerContainerRecipe(ContainerRecipe recipe) {
        Item input = recipe.getIngredient();
        Item potion = recipe.getInput();
        this.containerRecipes.put(getContainerHash(input.getId(), potion.getId()), recipe);
    }

    public BrewingRecipe matchBrewingRecipe(Item input, Item potion) {
        return this.brewingRecipes.get(getPotionHash(input, potion));
    }

    public CampfireRecipe matchCampfireRecipe(Item input) {
        CampfireRecipe recipe = this.campfireRecipes.get(getItemHash(input));
        if (recipe == null) recipe = this.campfireRecipes.get(getItemHash(input, 0));
        return recipe;
    }

    public ContainerRecipe matchContainerRecipe(Item input, Item potion) {
        return this.containerRecipes.get(getContainerHash(input.getId(), potion.getId()));
    }

    public CraftingRecipe matchRecipe(List<Item> inputList, Item primaryOutput, List<Item> extraOutputList) {
        int outputHash = getItemHash(primaryOutput);
        if (this.getShapedRecipes().containsKey(outputHash)) {
            inputList.sort(recipeComparator);

            UUID inputHash = getMultiItemHash(inputList);

            Map<UUID, ShapedRecipe> recipeMap = this.getShapedRecipes().get(outputHash);

            if (recipeMap != null) {
                ShapedRecipe recipe = recipeMap.get(inputHash);

                if (recipe != null && (recipe.matchItems(inputList, extraOutputList) || matchItemsAccumulation(recipe, inputList, primaryOutput, extraOutputList))) {
                    return recipe;
                }

                for (ShapedRecipe shapedRecipe : recipeMap.values()) {
                    if (shapedRecipe.matchItems(inputList, extraOutputList) || matchItemsAccumulation(shapedRecipe, inputList, primaryOutput, extraOutputList)) {
                        return shapedRecipe;
                    }
                }
            }
        }

        if (this.getShapelessRecipes().containsKey(outputHash)) {
            inputList.sort(recipeComparator);

            Map<UUID, ShapelessRecipe> recipes = this.getShapelessRecipes().get(outputHash);

            if (recipes == null) {
                return null;
            }

            UUID inputHash = getMultiItemHash(inputList);
            ShapelessRecipe recipe = recipes.get(inputHash);

            if (recipe != null && (recipe.matchItems(inputList, extraOutputList) || matchItemsAccumulation(recipe, inputList, primaryOutput, extraOutputList))) {
                return recipe;
            }

            for (ShapelessRecipe shapelessRecipe : recipes.values()) {
                if (shapelessRecipe.matchItems(inputList, extraOutputList) || matchItemsAccumulation(shapelessRecipe, inputList, primaryOutput, extraOutputList)) {
                    return shapelessRecipe;
                }
            }
        }

        return null;
    }

    private static boolean matchItemsAccumulation(CraftingRecipe recipe, List<Item> inputList, Item primaryOutput, List<Item> extraOutputList) {
        Item recipeResult = recipe.getResult();
        if (primaryOutput.equals(recipeResult, recipeResult.hasMeta(), recipeResult.hasCompoundTag()) && primaryOutput.getCount() % recipeResult.getCount() == 0) {
            int multiplier = primaryOutput.getCount() / recipeResult.getCount();
            return recipe.matchItems(inputList, extraOutputList, multiplier);
        }
        return false;
    }

    public void registerMultiRecipe(MultiRecipe recipe) {
        this.multiRecipes.put(recipe.getId(), recipe);
    }

    @Deprecated
    public SmithingRecipe matchSmithingRecipe(Item equipment, Item ingredient) {
        return matchSmithingRecipe(Arrays.asList(equipment, ingredient));
    }

    @Nullable
    public SmithingRecipe matchSmithingRecipe(List<Item> inputList) {
        inputList.sort(recipeComparator);
        UUID inputHash = getMultiItemHash(inputList);

        Map<UUID, SmithingRecipe> recipeMap = this.getSmithingRecipes();

        if (recipeMap != null) {
            SmithingRecipe recipe = recipeMap.get(inputHash);

            if (recipe != null && recipe.matchItems(inputList)) {
                return recipe;
            }

            ArrayList<Item> list = new ArrayList<>();
            for (Item item : inputList) {
                Item clone = item.clone();
                clone.setCount(1);
                if ((item.isTool() || item.isArmor()) && item.getDamage() > 0) {
                    clone.setDamage(0);
                }
                list.add(clone);
            }

            for (SmithingRecipe smithingRecipe : recipeMap.values()) {
                if (smithingRecipe.matchItems(list)) {
                    return smithingRecipe;
                }
            }
        }
        return null;
    }

    public static class Entry {
        final int resultItemId;
        final int resultMeta;
        final int ingredientItemId;
        final int ingredientMeta;
        final String recipeShape;
        final int resultAmount;

        public Entry(int resultItemId, int resultMeta, int ingredientItemId, int ingredientMeta, String recipeShape, int resultAmount) {
            this.resultItemId = resultItemId;
            this.resultMeta = resultMeta;
            this.ingredientItemId = ingredientItemId;
            this.ingredientMeta = ingredientMeta;
            this.recipeShape = recipeShape;
            this.resultAmount = resultAmount;
        }
    }

    public double getRecipeXp(Recipe recipe) {
        return recipeXpMap.getOrDefault(recipe, 0.0);
    }

    public Object2DoubleOpenHashMap<Recipe> getRecipeXpMap() {
        return recipeXpMap;
    }

    public void setRecipeXp(Recipe recipe, double xp) {
        recipeXpMap.put(recipe, xp);
    }
}
