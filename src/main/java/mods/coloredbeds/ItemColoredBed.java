package mods.coloredbeds;

import java.util.List;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Icon;
import net.minecraft.src.IconRegister;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemColoredBed extends Item {
    @SideOnly(Side.CLIENT)
    private Icon[] icons;

    public ItemColoredBed(int id) {
        super(id);
        super.setHasSubtypes(true);
        super.setMaxDamage(0);
        super.setCreativeTab(CreativeTabs.tabDecorations);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        } else if (side != 1) {
            return false;
        } else {
            y++;
            int dir = MathHelper.floor_double((double)(player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            byte dx = 0;
            byte dz = 0;

            if (dir == 0) dz = 1;
            if (dir == 1) dx = -1;
            if (dir == 2) dz = -1;
            if (dir == 3) dx = 1;

            if (player.canPlayerEdit(x, y, z, side, stack) && player.canPlayerEdit(x + dx, y, z + dz, side, stack)) {
                if (world.isAirBlock(x, y, z) && world.isAirBlock(x + dx, y, z + dz) && world.doesBlockHaveSolidTopSurface(x, y - 1, z) && world.doesBlockHaveSolidTopSurface(x + dx, y - 1, z + dz)) {
                    int blockId = ColoredBedsMod.coloredBedBlock.blockID;
                    int color = stack.getItemDamage();

                    world.setBlock(x, y, z, blockId, dir, 3);
                    TileEntity tileFoot = world.getBlockTileEntity(x, y, z);
                    if (tileFoot instanceof TileEntityColoredBed) {
                        ((TileEntityColoredBed) tileFoot).setColor(color);
                    }

                    world.setBlock(x + dx, y, z + dz, blockId, dir | 8, 3);
                    TileEntity tileHead = world.getBlockTileEntity(x + dx, y, z + dz);
                    if (tileHead instanceof TileEntityColoredBed) {
                        ((TileEntityColoredBed) tileHead).setColor(color);
                    }

                    stack.stackSize--;
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int color = MathHelper.clamp_int(stack.getItemDamage(), 0, 15);
        return "item.coloredbed." + BlockColoredBed.COLOR_NAMES[color];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIconFromDamage(int damage) {
        int color = MathHelper.clamp_int(damage, 0, 15);
        return this.icons[color];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IconRegister reg) {
        this.icons = new Icon[16];
        for (int i = 0; i < 16; i++) {
            this.icons[i] = reg.registerIcon("coloredbeds:bed_" + BlockColoredBed.COLOR_NAMES[i]);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(int itemId, CreativeTabs tab, List list) {
        for (int i = 0; i < 16; i++) {
            list.add(new ItemStack(itemId, 1, i));
        }
    }
}
