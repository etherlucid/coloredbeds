package mods.coloredbeds;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import net.minecraft.src.Block;
import net.minecraft.src.CraftingManager;
import net.minecraft.src.IRecipe;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraftforge.common.Configuration;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

@Mod(modid = "ColoredBeds", name = "Colored Beds Mod", version = "1.0")
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
public class ColoredBedsMod {

    @Mod.Instance("ColoredBeds")
    public static ColoredBedsMod instance;

    @SidedProxy(clientSide = "mods.coloredbeds.client.ClientProxy", serverSide = "mods.coloredbeds.CommonProxy")
    public static CommonProxy proxy;

    public static Block coloredBedBlock;
    public static Item coloredBedItem;

    public static int coloredBedBlockID = 26;
    public static int coloredBedItemID = 355;

    public static final String[] DISPLAY_NAMES = new String[]{
        "White Bed", "Orange Bed", "Magenta Bed", "Light Blue Bed",
        "Yellow Bed", "Lime Bed", "Pink Bed", "Gray Bed",
        "Light Gray Bed", "Cyan Bed", "Purple Bed", "Blue Bed",
        "Brown Bed", "Green Bed", "Red Bed", "Black Bed"
    };

    @Mod.PreInit
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        coloredBedBlockID = 26; // Enforce vanilla Bed Block ID (26)
        coloredBedItemID = 355;  // Enforce vanilla Bed Item ID (355)
        config.save();
    }

    @Mod.Init
    public void init(FMLInitializationEvent event) {
        // 1. Replace Block.blocksList[26] and Block.bed
        Block.blocksList[26] = null;
        BlockColoredBed customBlock = new BlockColoredBed(26);
        ((Block) customBlock).setUnlocalizedName("bed");
        coloredBedBlock = customBlock;
        Block.blocksList[26] = customBlock;
        setStaticField(Block.class, "bed", customBlock);

        // 2. Replace Item.itemsList[355] and Item.bed
        Item.itemsList[355] = null;
        ItemColoredBed customItem = new ItemColoredBed(355 - 256);
        ((Item) customItem).setUnlocalizedName("bed");
        coloredBedItem = customItem;
        Item.itemsList[355] = customItem;
        setStaticField(Item.class, "bed", customItem);

        // 3. Register TileEntity and Item
        try {
            GameRegistry.class.getMethod("registerBlock", Object.class, String.class).invoke(null, coloredBedBlock, "coloredBedBlock");
        } catch (Throwable t) {
            try {
                for (java.lang.reflect.Method m : GameRegistry.class.getMethods()) {
                    if (m.getName().equals("registerBlock") && m.getParameterTypes().length == 2 && m.getParameterTypes()[1] == String.class) {
                        m.invoke(null, coloredBedBlock, "coloredBedBlock");
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }

        try {
            for (java.lang.reflect.Method m : GameRegistry.class.getMethods()) {
                if (m.getName().equals("registerItem") && m.getParameterTypes().length == 2 && m.getParameterTypes()[1] == String.class) {
                    m.invoke(null, coloredBedItem, "coloredBedItem");
                    break;
                }
            }
        } catch (Throwable ignored) {}

        try {
            for (java.lang.reflect.Method m : GameRegistry.class.getMethods()) {
                if (m.getName().equals("registerTileEntity") && m.getParameterTypes().length == 2 && m.getParameterTypes()[1] == String.class) {
                    m.invoke(null, TileEntityColoredBed.class, "TileEntityColoredBed");
                    break;
                }
            }
        } catch (Throwable ignored) {}

        for (int i = 0; i < 16; i++) {
            LanguageRegistry.addName(new ItemStack(coloredBedItem, 1, i), DISPLAY_NAMES[i]);
        }

        // 4. Clean up old vanilla bed recipes
        List recipeList = CraftingManager.getInstance().getRecipeList();
        Iterator iterator = recipeList.iterator();
        while (iterator.hasNext()) {
            Object r = iterator.next();
            if (r instanceof IRecipe) {
                ItemStack out = ((IRecipe) r).getRecipeOutput();
                if (out != null && out.itemID == 355) {
                    iterator.remove();
                }
            }
        }

        // 5. Direct Crafting Recipes: 3 Wool (meta i) + 3 Planks (any) -> Colored Bed (meta i)
        for (int i = 0; i < 16; i++) {
            try {
                for (java.lang.reflect.Method m : GameRegistry.class.getMethods()) {
                    if (m.getName().equals("addRecipe") && m.getParameterTypes().length == 2 && m.getParameterTypes()[1] == Object[].class) {
                        m.invoke(null, new ItemStack(coloredBedItem, 1, i), new Object[]{
                            "WWW", "PPP",
                            'W', new ItemStack(Block.cloth, 1, i),
                            'P', Block.planks
                        });
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 6. Re-dyeing Recipes: Any Colored Bed + Dye (meta d) -> Colored Bed (woolMeta = 15 - d)
        for (int d = 0; d < 16; d++) {
            int woolMeta = 15 - d;
            for (int b = 0; b < 16; b++) {
                try {
                    for (java.lang.reflect.Method m : GameRegistry.class.getMethods()) {
                        if (m.getName().equals("addShapelessRecipe") && m.getParameterTypes().length == 2 && m.getParameterTypes()[1] == Object[].class) {
                            m.invoke(null, new ItemStack(coloredBedItem, 1, woolMeta), new Object[]{
                                new ItemStack(coloredBedItem, 1, b),
                                new ItemStack(Item.dyePowder, 1, d)
                            });
                            break;
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        proxy.registerRenderers();
    }

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = null;
            try {
                field = clazz.getField(fieldName);
            } catch (NoSuchFieldException e) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (f.getName().equals(fieldName)) {
                        field = f;
                        break;
                    }
                }
            }
            if (field != null) {
                field.setAccessible(true);
                try {
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                } catch (Throwable ignored) {}
                field.set(null, value);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
