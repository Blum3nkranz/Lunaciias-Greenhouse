package dev.lunaciia.greenhouse;

import com.google.gson.*;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;

public final class GreenhouseStore {
    /** Copy validated legacy presets once; never move or overwrite either library. */
    static void migrateLegacy(Path config)throws Exception{
        Path old=config.resolve("lus-greenhouse"),next=config.resolve("lunaciia-greenhouse");
        if(Files.exists(next.resolve("presets.json"))||Files.exists(next.resolve("current.json")))return;
        if(!Files.exists(old.resolve("presets.json"))&&!Files.exists(old.resolve("current.json")))return;
        GreenhouseLibrary library=GreenhouseLibrary.open(old);
        library.save(next);
    }
    static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    static Path directory(){return FabricLoader.getInstance().getConfigDir().resolve("lunaciia-greenhouse");}
    static Path current(){return directory().resolve("current.json");}
    static GreenhousePreset load(){try{var p=JSON.fromJson(Files.readString(current()),GreenhousePreset.class);p.validate();return p;}catch(Exception e){return new GreenhousePreset();}}
    static void save(GreenhousePreset p)throws Exception{p.validate();Files.createDirectories(directory());var tmp=directory().resolve("current.tmp");Files.writeString(tmp,JSON.toJson(p));Files.move(tmp,current(),StandardCopyOption.REPLACE_EXISTING);}
    static Path export(GreenhousePreset p)throws Exception{save(p);var file=directory().resolve("preset-"+System.currentTimeMillis()+".json");Files.writeString(file,JSON.toJson(p));return file;}
}
