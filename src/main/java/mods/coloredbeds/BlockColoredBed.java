package mods.coloredbeds;

import java.util.Random;
import net.minecraft.src.Block;
import net.minecraft.src.BlockBed;
import net.minecraft.src.ITileEntityProvider;
import net.minecraft.src.IconRegister;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;
import net.minecraft.src.Direction;
import net.minecraft.src.Icon;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.World;
import net.minecraft.src.EntityPlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockColoredBed extends BlockBed implements ITileEntityProvider {

    private static final ThreadLocal<Integer> lastColor = new ThreadLocal<Integer>();

    @SideOnly(Side.CLIENT)
    private Icon[] bedFeetTopIcons;
    @SideOnly(Side.CLIENT)
    private Icon[] bedFeetSideIcons;
    @SideOnly(Side.CLIENT)
    private Icon[] bedFeetEndIcons;
    @SideOnly(Side.CLIENT)
    private Icon[] bedHeadTopIcons;
    @SideOnly(Side.CLIENT)
    private Icon[] bedHeadSideIcons;
    @SideOnly(Side.CLIENT)
    private Icon[] bedHeadEndIcons;

    public static final String[] COLOR_NAMES = new String[]{
        "white", "orange", "magenta", "light_blue",
        "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue",
        "brown", "green", "red", "black"
    };

    public BlockColoredBed(int id) {
        super(id);
        this.isBlockContainer = true;
    }

    @Override
    public boolean hasTileEntity() {
        return true;
    }

    public static int getDirection(int meta) {
        return meta & 3;
    }

    public static boolean isBlockHeadOfBed(int meta) {
        return (meta & 8) != 0;
    }

    public static boolean isBedOccupied(int meta) {
        return (meta & 4) != 0;
    }

    @Override
    public TileEntity createNewTileEntity(World world) {
        return new TileEntityColoredBed();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        boolean result = super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
        if (!world.isRemote) {
            int meta = world.getBlockMetadata(x, y, z);
            int dir = getDirection(meta);
            int headX = x;
            int headZ = z;
            if (!isBlockHeadOfBed(meta)) {
                headX += footBlockToHeadBlockMap[dir][0];
                headZ += footBlockToHeadBlockMap[dir][1];
            }
            int footX = headX - footBlockToHeadBlockMap[dir][0];
            int footZ = headZ - footBlockToHeadBlockMap[dir][1];

            world.markBlockForUpdate(headX, y, headZ);
            world.markBlockForUpdate(footX, y, footZ);
        }
        return result;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, int id, int meta) {
        int c = getColorAt(world, x, y, z);
        if (c >= 0 && c <= 15) {
            lastColor.set(c);
        }
        super.breakBlock(world, x, y, z, id, meta);
    }

    @Override
    public boolean onBlockEventReceived(World world, int x, int y, int z, int eventID, int eventParam) {
        super.onBlockEventReceived(world, x, y, z, eventID, eventParam);
        TileEntity tile = world.getBlockTileEntity(x, y, z);
        return tile != null ? tile.receiveClientEvent(eventID, eventParam) : false;
    }

    private int getColorAt(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getBlockTileEntity(x, y, z);
        if (tile instanceof TileEntityColoredBed) {
            int c = ((TileEntityColoredBed) tile).getColor();
            if (c >= 0 && c <= 15) return c;
        }

        // Fallback: check other half of bed if this half's tile entity isn't ready
        int meta = world.getBlockMetadata(x, y, z);
        int dir = getDirection(meta);
        boolean isHead = isBlockHeadOfBed(meta);

        int otherX = x;
        int otherZ = z;
        if (isHead) {
            otherX -= footBlockToHeadBlockMap[dir][0];
            otherZ -= footBlockToHeadBlockMap[dir][1];
        } else {
            otherX += footBlockToHeadBlockMap[dir][0];
            otherZ += footBlockToHeadBlockMap[dir][1];
        }

        TileEntity otherTile = world.getBlockTileEntity(otherX, y, otherZ);
        if (otherTile instanceof TileEntityColoredBed) {
            int c = ((TileEntityColoredBed) otherTile).getColor();
            if (c >= 0 && c <= 15) return c;
        }

        Integer cached = lastColor.get();
        if (cached != null && cached >= 0 && cached <= 15) {
            return cached.intValue();
        }

        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Icon getBlockTexture(IBlockAccess world, int x, int y, int z, int side) {
        if (side == 0) {
            return Block.planks.getBlockTextureFromSide(side);
        }

        int meta = world.getBlockMetadata(x, y, z);
        int dir = getDirection(meta);
        int bedSide = Direction.bedDirection[dir][side];
        boolean isHead = isBlockHeadOfBed(meta);

        int color = getColorAt(world, x, y, z);

        if (isHead) {
            if (bedSide == 2) {
                return this.bedHeadEndIcons[color];
            } else if (bedSide == 4 || bedSide == 5) {
                return this.bedHeadSideIcons[color];
            } else {
                return this.bedHeadTopIcons[color];
            }
        } else {
            if (bedSide == 3) {
                return this.bedFeetEndIcons[color];
            } else if (bedSide == 4 || bedSide == 5) {
                return this.bedFeetSideIcons[color];
            } else {
                return this.bedFeetTopIcons[color];
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(int side, int meta) {
        if (side == 0) {
            return Block.planks.getBlockTextureFromSide(side);
        }

        int dir = getDirection(meta);
        int bedSide = Direction.bedDirection[dir][side];
        boolean isHead = isBlockHeadOfBed(meta);

        int color = 0;

        if (isHead) {
            if (bedSide == 2) {
                return this.bedHeadEndIcons[color];
            } else if (bedSide == 4 || bedSide == 5) {
                return this.bedHeadSideIcons[color];
            } else {
                return this.bedHeadTopIcons[color];
            }
        } else {
            if (bedSide == 3) {
                return this.bedFeetEndIcons[color];
            } else if (bedSide == 4 || bedSide == 5) {
                return this.bedFeetSideIcons[color];
            } else {
                return this.bedFeetTopIcons[color];
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IconRegister reg) {
        super.registerIcons(reg);
        this.bedFeetTopIcons = new Icon[16];
        this.bedFeetSideIcons = new Icon[16];
        this.bedFeetEndIcons = new Icon[16];
        this.bedHeadTopIcons = new Icon[16];
        this.bedHeadSideIcons = new Icon[16];
        this.bedHeadEndIcons = new Icon[16];

        for (int i = 0; i < 16; i++) {
            String c = COLOR_NAMES[i];
            this.bedFeetTopIcons[i] = reg.registerIcon("coloredbeds:bed_" + c + "_feet_top");
            this.bedFeetSideIcons[i] = reg.registerIcon("coloredbeds:bed_" + c + "_feet_side");
            this.bedFeetEndIcons[i] = reg.registerIcon("coloredbeds:bed_" + c + "_feet_end");
            this.bedHeadTopIcons[i] = reg.registerIcon("coloredbeds:bed_" + c + "_head_top");
            this.bedHeadSideIcons[i] = reg.registerIcon("coloredbeds:bed_" + c + "_head_side");
            this.bedHeadEndIcons[i] = reg.registerIcon("coloredbeds:bed_" + c + "_head_end");
        }
    }

    @Override
    public int idDropped(int meta, Random rand, int fortune) {
        return isBlockHeadOfBed(meta) ? 0 : ColoredBedsMod.coloredBedItem.itemID;
    }

    @Override
    public void dropBlockAsItemWithChance(World world, int x, int y, int z, int meta, float chance, int fortune) {
        if (!isBlockHeadOfBed(meta)) {
            int color = getColorAt(world, x, y, z);
            super.dropBlockAsItem_do(world, x, y, z, new ItemStack(ColoredBedsMod.coloredBedItem, 1, color));
        }
    }

    @Override
    public int idPicked(World world, int x, int y, int z) {
        return ColoredBedsMod.coloredBedItem.itemID;
    }

    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        int color = getColorAt(world, x, y, z);
        return new ItemStack(ColoredBedsMod.coloredBedItem, 1, color);
    }
}
