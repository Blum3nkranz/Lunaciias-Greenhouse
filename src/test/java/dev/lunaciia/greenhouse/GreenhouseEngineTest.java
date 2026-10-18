package dev.lunaciia.greenhouse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import com.google.gson.*;
final class GreenhouseEngineTest {
    @TempDir Path directory;
    @Test void websiteCodesAndGroundRoundTrip()throws Exception{
        var p=new GreenhousePreset();p.plant(0,0,"wheat",false,true);p.plant(2,2,"ashwreath",true,true);p.paintGround(9,9,"sand");
        String code=GreenhouseCode.encode(p);var decoded=GreenhouseCode.decode("https://skyshards.com/greenhouse?layout="+code);
        assertEquals("wheat",decoded.at(0,0).cropId());assertEquals("soul_sand",GreenhouseGround.at(decoded,2,2));assertEquals("sand",GreenhouseGround.at(decoded,9,9));
        assertEquals(code,GreenhouseCode.encode(decoded));
        p.paintGround(0,0,"end_stone");assertThrows(IllegalArgumentException.class,()->GreenhouseCode.encode(p));
        var json=GreenhouseStore.JSON.fromJson(GreenhouseStore.JSON.toJson(p),GreenhousePreset.class);json.validate();assertEquals("end_stone",GreenhouseGround.at(json,0,0));
    }
    @Test void libraryPersistsNamesCopiesAndDrafts()throws Exception{
        var old=new GreenhousePreset();old.name="旧布局";old.paint(1,1,"wheat",false);Files.writeString(directory.resolve("current.json"),GreenhouseStore.JSON.toJson(old));
        var lib=GreenhouseLibrary.open(directory);String first=lib.active;var draft=lib.current();draft.plant(2,2,"potato",false,true);lib.update(draft);
        lib.add(draft,"测试 / 第二套");String second=lib.active;var copy=lib.current();copy.plant(1,1,null,false,false);lib.switchTo(first,copy);
        lib.autoGround=false;lib.save(directory);var loaded=GreenhouseLibrary.open(directory);assertFalse(loaded.autoGround);assertEquals(first,loaded.active);
        assertNotNull(loaded.current().at(1,1));assertNotNull(loaded.current().at(2,2));assertNull(loaded.presets.get(second).at(1,1));assertEquals("测试 / 第二套",loaded.presets.get(second).name);
    }
    @Test void autoGroundPreservesManualGroundWhenDisabled(){
        var p=new GreenhousePreset();p.plant(0,0,"wheat",false,false);assertNull(GreenhouseGround.at(p,0,0));
        p.paintGround(0,0,"sand");p.plant(0,0,"potato",false,false);assertEquals("sand",GreenhouseGround.at(p,0,0));
        p.plant(0,0,"ashwreath",true,true);assertEquals("soul_sand",GreenhouseGround.at(p,0,0));
        p.plant(0,0,null,false,false);assertEquals("soul_sand",GreenhouseGround.at(p,0,0));p.paintGround(0,0,null);assertNull(GreenhouseGround.at(p,0,0));p.validate();
    }
    @Test void legacyGroundAndLargeFootprintRotateLosslessly(){
        var p=new GreenhousePreset();p.paint(1,2,"godseed",true);String land=GreenhouseCatalog.crops().get("godseed").ground();assertEquals(land,GreenhouseGround.at(p,3,4));
        p.materializeGround();p.paintGround(9,0,"sand");p.validate();String before=new Gson().toJson(p);
        for(int i=0;i<4;i++){p.rotate();p.validate();}assertEquals(before,new Gson().toJson(p));
        assertThrows(IllegalArgumentException.class,()->p.plant(9,9,"godseed",true,true));
    }
    @Test void invalidCodesAndDuplicateGroundRejected(){
        assertThrows(Exception.class,()->GreenhouseCode.decode("not a code"));var p=new GreenhousePreset();p.paintGround(0,0,"sand");p.groundTiles.add(p.groundTiles.get(0).deepCopy());assertThrows(IllegalArgumentException.class,p::validate);
        assertThrows(IllegalArgumentException.class,()->GreenhouseLibrary.validName("   "));
    }
    @Test void rarityAndCropOrderComeFromSourceCatalog(){var crops=GreenhouseCatalog.crops();assertEquals("wheat",crops.keySet().iterator().next());assertEquals(0xffffffff,crops.get("ashwreath").color());assertEquals(0xff55ff55,crops.get("chocoberry").color());assertTrue(crops.get("godseed").mutation());}
    @Test void legacyTargetsBecomeCropsAndErasingClearsWholeFootprint(){
        var p=new GreenhousePreset();p.paint(1,2,"godseed",true);p.paintGround(9,9,"sand");p.mergeTargets();
        assertTrue(p.targets.isEmpty());assertEquals("godseed",p.inputs.getFirst().cropId());assertNotNull(GreenhouseGround.at(p,3,4));
        p.erase(3,4);assertNull(p.at(1,2));for(int r=1;r<=3;r++)for(int c=2;c<=4;c++)assertNull(GreenhouseGround.at(p,r,c));
        assertEquals("sand",GreenhouseGround.at(p,9,9));p.erase(9,9);assertNull(GreenhouseGround.at(p,9,9));p.validate();
    }
    @Test void projectionCategoriesAndLargeCropAnchors(){
        assertEquals(0xff55ed8b,GreenhouseProjection.classify(false,false,false,false));assertEquals(0xffff5555,GreenhouseProjection.classify(false,true,false,false));
        assertEquals(0xffffd35c,GreenhouseProjection.classify(true,false,false,false));assertEquals(0xff66bbff,GreenhouseProjection.classify(false,false,true,false));
        assertArrayEquals(new int[]{2,3},GreenhouseProjection.anchor("snoozling",4,4));assertArrayEquals(new int[]{3,3},GreenhouseProjection.anchor("godseed",4,4));
    }
}
