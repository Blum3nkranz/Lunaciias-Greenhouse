package dev.lunaciia.greenhouse;
import com.google.gson.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class GreenhouseCatalog {
    record Crop(String id,String name,int size,String ground,String rarity,boolean mutation){
        int color(){return switch(rarity){case "uncommon"->0xff55ff55;case "rare"->0xff5555ff;case "epic"->0xffaa00aa;case "legendary"->0xffffaa00;case "mythic"->0xffff55ff;default->0xffffffff;};}
    }
    private static Map<String,Crop> crops;
    private static Map<String,String> textures;
    private static JsonObject images;
    static int[] imageSize(String id){if(images==null)images=read("images.json");var a=images.getAsJsonArray(id);return a==null?new int[]{300,300}:new int[]{a.get(0).getAsInt(),a.get(1).getAsInt()};}
    static Map<String,Crop> crops(){if(crops==null){var map=new LinkedHashMap<String,Crop>();var data=read("data.json");for(String group:List.of("crops","mutations"))for(var e:data.getAsJsonObject(group).entrySet()){var c=e.getValue().getAsJsonObject();map.put(e.getKey(),new Crop(e.getKey(),c.get("name").getAsString(),c.get("size").getAsInt(),c.get("ground").getAsString(),c.has("rarity")?c.get("rarity").getAsString():"common",group.equals("mutations")));}crops=Collections.unmodifiableMap(map);}return crops;}
    static String texture(String value){if(textures==null){var map=new HashMap<String,String>();read("head-textures.json").entrySet().forEach(e->map.put(e.getKey(),e.getValue().getAsString()));textures=Map.copyOf(map);}try{var data=JsonParser.parseString(new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8)).getAsJsonObject();var url=data.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();return textures.get(url.substring(url.lastIndexOf('/')+1));}catch(RuntimeException e){return null;}}
    private static JsonObject read(String file){try(var in=GreenhouseCatalog.class.getResourceAsStream("/assets/lunaciia_greenhouse/greenhouse/"+file)){return JsonParser.parseString(new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){throw new IllegalStateException("Greenhouse catalog missing",e);}}
}
