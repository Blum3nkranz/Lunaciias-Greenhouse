package dev.lunaciia.greenhouse;

import java.util.List;
import net.minecraft.resources.Identifier;

/** Ground is inferred over each complete crop/target footprint, just as in SkyShards layouts. */
final class GreenhouseGround {
    static final List<String> TYPES=List.of("farmland","sand","soul_sand","mycelium","netherrack","end_stone");
    static String at(GreenhousePreset preset,int row,int col){
        var crop=preset.at(row,col);
        if(crop!=null&&!preset.explicitGround)return GreenhouseCatalog.crops().get(crop.cropId()).ground();
        for(var tile:preset.groundTiles){var o=tile.getAsJsonObject();var p=o.getAsJsonArray("position");if(p.get(0).getAsInt()==row&&p.get(1).getAsInt()==col)return o.get("ground").getAsString();}
        return null;
    }
    static Identifier texture(String ground){return Identifier.fromNamespaceAndPath("minecraft","textures/block/"+(ground.equals("mycelium")?"mycelium_top":ground)+".png");}
    static String name(String ground){return switch(ground){case "farmland"->"Farmland";case "sand"->"Sand";case "soul_sand"->"Soul Sand";case "mycelium"->"Mycelium";case "netherrack"->"Netherrack";case "end_stone"->"End Stone";default->ground;};}
}
