package dev.lunaciia.greenhouse;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Java port of SkyShards designEncoding.ts (MIT, Campion 2026). Stable wire IDs, raw DEFLATE. */
final class GreenhouseCode {
    private static final List<String> IDS=List.of(("wheat potato carrot pumpkin melon cocoa_beans sugar_cane cactus nether_wart red_mushroom brown_mushroom moonflower sunflower wild_rose fire dead_plant fermento "
        +"ashwreath choconut dustgrain gloomgourd lonelily scourroot shadevine veilshroom witherbloom chocoberry cindershade coalroot creambloom duskbloom thornshade blastberry cheesebite chloronite do_not_eat_shroom fleshtrap magic_jellybean noctilume snoozling soggybud chorus_fruit plantboy_advance puffercloud shellfruit startlevine stoplight_petal thunderling turtlellini zombud all_in_aloe devourer glasscorn godseed jerryflower phantomleaf timestalk").split(" "));
    static String encode(GreenhousePreset p)throws IOException{
        if(p.explicitGround){
            var legacy=GreenhouseLibrary.copy(p);legacy.explicitGround=false;legacy.groundTiles=new com.google.gson.JsonArray();
            for(int r=0;r<10;r++)for(int c=0;c<10;c++){String ground=GreenhouseGround.at(p,r,c);var crop=p.at(r,c);
                if(crop!=null){if(!Objects.equals(ground,GreenhouseCatalog.crops().get(crop.cropId()).ground()))throw new IllegalArgumentException("Custom ground requires JSON export");}
                else if(ground!=null){var tile=new com.google.gson.JsonObject();tile.addProperty("ground",ground);var pos=new com.google.gson.JsonArray();pos.add(r);pos.add(c);tile.add("position",pos);legacy.groundTiles.add(tile);}
            }p=legacy;
        }
        p.validate();var inputs=p.inputs.stream().map(GreenhousePreset.Cell::cropId).distinct().toList();var targets=p.targets.stream().map(GreenhousePreset.Cell::cropId).distinct().toList();
        int n=inputs.size()>26||targets.size()>26?2:1;String[] grid=new String[100];Arrays.fill(grid,".".repeat(n));
        for(var tile:p.groundTiles){var a=tile.getAsJsonObject();var pos=a.getAsJsonArray("position");grid[pos.get(0).getAsInt()*10+pos.get(1).getAsInt()]=Integer.toString(GreenhouseGround.TYPES.indexOf(a.get("ground").getAsString())).repeat(n);}
        for(var list:List.of(p.inputs,p.targets))for(var c:list){boolean target=list==p.targets;int i=(target?targets:inputs).indexOf(c.cropId());String token=n==1?""+(char)('a'+i):""+(char)('a'+i/26)+(char)('a'+i%26);grid[c.position()[0]*10+c.position()[1]]=target?token.toUpperCase(Locale.ROOT):token;}
        String raw=indices(inputs)+"|"+indices(targets)+"|"+String.join("",grid);
        var deflater=new Deflater(9,true);try(var bytes=new ByteArrayOutputStream()){
            try(var stream=new DeflaterOutputStream(bytes,deflater)){stream.write(raw.getBytes(StandardCharsets.UTF_8));}
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        }finally{deflater.end();}
    }
    private static String indices(List<String> ids){return String.join(",",ids.stream().map(id->Integer.toString(IDS.indexOf(id),36)).toList());}
    static GreenhousePreset decode(String code)throws IOException{
        if(code.length()>2_000_000)throw new IllegalArgumentException("Preset too large");code=code.strip();
        var match=java.util.regex.Pattern.compile("(?:[?&]layout=|/share/)([^&#/?]+)").matcher(code);if(match.find())code=java.net.URLDecoder.decode(match.group(1),StandardCharsets.UTF_8);
        var inflater=new Inflater(true);String raw;
        try(var in=new InflaterInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(code)),inflater)){byte[] bytes=in.readNBytes(8193);if(bytes.length>8192)throw new IllegalArgumentException("Preset too large");raw=new String(bytes,StandardCharsets.UTF_8);}finally{inflater.end();}
        String[] parts=raw.split("\\|",-1);if(parts.length!=3||(parts[2].length()!=100&&parts[2].length()!=200))throw new IllegalArgumentException("Invalid layout code");
        List<String> inputs=parseIndices(parts[0]),targets=parseIndices(parts[1]);int n=parts[2].length()/100;var p=new GreenhousePreset();
        for(int i=0;i<100;i++){
            String token=parts[2].substring(i*n,(i+1)*n);if(token.equals(".".repeat(n)))continue;
            if(token.charAt(0)>='0'&&token.charAt(0)<='5'&&token.equals(token.substring(0,1).repeat(n))){var tile=new com.google.gson.JsonObject();tile.addProperty("ground",GreenhouseGround.TYPES.get(token.charAt(0)-'0'));var pos=new com.google.gson.JsonArray();pos.add(i/10);pos.add(i%10);tile.add("position",pos);p.groundTiles.add(tile);continue;}
            if(!token.matches("[a-z]+|[A-Z]+"))throw new IllegalArgumentException("Invalid cell token");
            boolean target=Character.isUpperCase(token.charAt(0));String lower=token.toLowerCase(Locale.ROOT);int index=n==1?lower.charAt(0)-'a':(lower.charAt(0)-'a')*26+lower.charAt(1)-'a';var ids=target?targets:inputs;
            if(index>=ids.size())throw new IllegalArgumentException("Invalid crop index");(target?p.targets:p.inputs).add(new GreenhousePreset.Cell(ids.get(index),new int[]{i/10,i%10}));
        }
        p.validate();return p;
    }
    private static List<String> parseIndices(String list){if(list.isEmpty())return List.of();return Arrays.stream(list.split(",")).map(s->{int i=Integer.parseInt(s,36);if(i<0||i>=IDS.size())throw new IllegalArgumentException("Invalid crop ID");return IDS.get(i);}).toList();}
}
