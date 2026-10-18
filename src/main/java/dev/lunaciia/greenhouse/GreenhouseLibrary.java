package dev.lunaciia.greenhouse;

import java.nio.file.*;
import java.util.*;

/** Stable IDs keep display names out of filesystem paths. Switching always saves the outgoing draft. */
final class GreenhouseLibrary {
    String active;
    boolean autoGround=true;
    LinkedHashMap<String,GreenhousePreset> presets=new LinkedHashMap<>();
    static GreenhouseLibrary open(Path directory)throws Exception{
        var file=directory.resolve("presets.json");
        if(Files.exists(file)){
            var library=GreenhouseStore.JSON.fromJson(Files.readString(file),GreenhouseLibrary.class);
            if(library==null||library.presets==null||library.presets.isEmpty()||!library.presets.containsKey(library.active))throw new IllegalArgumentException("Invalid preset library");
            for(var p:library.presets.values())p.validate();return library;
        }
        var library=new GreenhouseLibrary();var current=directory.resolve("current.json");
        var p=Files.exists(current)?GreenhouseStore.JSON.fromJson(Files.readString(current),GreenhousePreset.class):new GreenhousePreset();
        library.add(p,p.name);return library;
    }
    GreenhousePreset current(){var p=copy(presets.get(active));p.mergeTargets();return p;}
    void update(GreenhousePreset p){p.validate();presets.put(active,copy(p));}
    GreenhousePreset switchTo(String id,GreenhousePreset outgoing){if(!presets.containsKey(id))throw new IllegalArgumentException("Unknown preset");update(outgoing);active=id;return current();}
    void add(GreenhousePreset p,String name){var next=copy(p);next.name=validName(name);next.validate();active=UUID.randomUUID().toString();presets.put(active,next);}
    static String validName(String name){if(name==null||name.strip().isEmpty()||name.strip().length()>80)throw new IllegalArgumentException("Name must contain 1–80 characters");return name.strip();}
    void save(Path directory)throws Exception{
        Files.createDirectories(directory);var tmp=directory.resolve("presets.tmp");Files.writeString(tmp,GreenhouseStore.JSON.toJson(this));
        try{Files.move(tmp,directory.resolve("presets.json"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,directory.resolve("presets.json"),StandardCopyOption.REPLACE_EXISTING);}
    }
    static GreenhousePreset copy(GreenhousePreset p){return GreenhouseStore.JSON.fromJson(GreenhouseStore.JSON.toJson(p),GreenhousePreset.class);}
}
