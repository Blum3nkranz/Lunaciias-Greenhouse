package dev.lunaciia.greenhouse;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class StandaloneMigrationTest {
    @TempDir Path config;
    @Test void legacyLibraryCopiesOnceWithoutChangingSource()throws Exception{
        Path old=config.resolve("lus-greenhouse"),next=config.resolve("lunaciia-greenhouse");
        var library=GreenhouseLibrary.open(old);var p=library.current();p.plant(1,2,"godseed",false,true);library.update(p);library.save(old);
        String before=Files.readString(old.resolve("presets.json"));
        GreenhouseStore.migrateLegacy(config);
        assertEquals("godseed",GreenhouseLibrary.open(next).current().at(1,2).cropId());
        assertEquals(before,Files.readString(old.resolve("presets.json")));
        var independent=GreenhouseLibrary.open(next);independent.add(new GreenhousePreset(),"独立布局");independent.save(next);
        GreenhouseStore.migrateLegacy(config);
        assertEquals("独立布局",GreenhouseLibrary.open(next).current().name);
        assertEquals(before,Files.readString(old.resolve("presets.json")));
    }
    @Test void legacyCurrentAndInvalidSource()throws Exception{
        Path old=config.resolve("lus-greenhouse");Files.createDirectories(old);
        var p=new GreenhousePreset();p.name="Legacy";Files.writeString(old.resolve("current.json"),GreenhouseStore.JSON.toJson(p));
        GreenhouseStore.migrateLegacy(config);assertEquals("Legacy",GreenhouseLibrary.open(config.resolve("lunaciia-greenhouse")).current().name);
        Path other=config.resolve("other"),bad=other.resolve("lus-greenhouse");Files.createDirectories(bad);Files.writeString(bad.resolve("presets.json"),"broken");
        assertThrows(Exception.class,()->GreenhouseStore.migrateLegacy(other));assertFalse(Files.exists(other.resolve("lunaciia-greenhouse/presets.json")));
    }
}
