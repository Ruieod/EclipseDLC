package code.essence.features.impl.player.autoMine.region.states;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nikitavodolaz
 * @since 09.02.2026
 */

public class RegionBlockMaps {
    public static final Map<Block, RegionBlockState> IMMUTABLE_BLOCK_MAP = new HashMap<>() {{
        put(Blocks.DIAMOND_ORE, new RegionBlockState(0x2500FFFF, 0));
//        put(Blocks.EMERALD_ORE, new RegionBlockState(0x2500FF50, 1));
//        put(Blocks.GOLD_ORE, new RegionBlockState(0x25FF5050, 2));
//        put(Blocks.LAPIS_ORE, new RegionBlockState(0x250000FF, 3));
//        put(Blocks.IRON_ORE, new RegionBlockState(0x25505050, 4));
//        put(Blocks.COAL_ORE, new RegionBlockState(0xFFFFFFFF, 5));
    }};
}
