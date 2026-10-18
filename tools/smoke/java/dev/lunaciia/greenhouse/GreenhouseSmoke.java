package dev.lunaciia.greenhouse;
import java.nio.file.*;
import com.google.gson.*;
import net.minecraft.client.gui.screens.TitleScreen;
final class GreenhouseSmoke {
    private static GreenhouseScreen page;
    static void enqueue(VisualSmoke s){
        s.steps.add(()->{Files.createDirectories(s.out);s.mc.options.languageCode="zh_cn";s.mc.options.guiScale().set(2);s.mc.resizeGui();page=new GreenhouseScreen(new TitleScreen());s.mc.setScreen(page);});
        s.steps.add(()->{
            s.check(GreenhouseCatalog.crops().size()==57,"native catalog loads without a JavaScript runtime");
            page.preset=new GreenhousePreset();var p=page.preset;
            p.plant(0,0,"wheat",false,true);p.plant(1,0,"potato",false,true);p.plant(2,0,"cactus",false,true);p.plant(4,4,"ashwreath",true,true);p.plant(7,7,"veilshroom",true,true);
            for(int i=0;i<6;i++)p.paintGround(9,i,GreenhouseGround.TYPES.get(i));p.validate();
            for(String ground:GreenhouseGround.TYPES)s.check(s.mc.getResourceManager().getResource(GreenhouseGround.texture(ground)).isPresent(),"native ground texture exists: "+ground);
            var cards=(java.util.List<?>)VisualSmoke.field(page,"cards");s.check(cards.size()>5,"crop palette is a multi-column grid");
            var names=page.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.AbstractWidget).map(c->((net.minecraft.client.gui.components.AbstractWidget)c).getMessage().getString()).toList();
            s.check(names.stream().noneMatch(n->n.contains("模拟周期")||n.contains("初始库存")||n.contains("结果 / 库存")),"simulation controls removed");
            s.check(names.stream().noneMatch(n->n.contains("目标空地")),"target category removed");
            int x=(int)VisualSmoke.field(page,"x"),y=(int)VisualSmoke.field(page,"y"),cell=(int)VisualSmoke.field(page,"cell");page.library.autoGround=false;
            page.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x+3*cell+1,y+1,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
            s.check(GreenhouseGround.at(p,0,3)==null&&p.at(0,3)!=null,"auto-ground off keeps a real empty ground cell");
            page.library.autoGround=true;
            page.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x+3*cell+1,y+1,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
            s.check("farmland".equals(GreenhouseGround.at(p,0,3)),"auto-ground on paints the selected crop ground");
            page.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x+3*cell+1,y+1,new net.minecraft.client.input.MouseButtonInfo(1,0)),false);
            s.check(p.at(0,3)==null&&GreenhouseGround.at(p,0,3)==null,"planting right click clears crop and soil");
            p.paintGround(0,3,"sand");page.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x+3*cell+1,y+1,new net.minecraft.client.input.MouseButtonInfo(1,0)),false);
            s.check(GreenhouseGround.at(p,0,3)==null,"planting right click clears soil without a crop");
        });
        s.steps.add(()->s.shot("greenhouse-planner"));
        s.steps.add(()->{
            var binding=(UiTheme.FlatButton)VisualSmoke.field(page,"bindingButton");click(page,binding.getMessage().getString());
            page.keyPressed(new net.minecraft.client.input.KeyEvent(71,0,0));
            var key=net.minecraft.client.KeyMapping.get("key.lunaciia_greenhouse.greenhouse");s.check(key.matches(new net.minecraft.client.input.KeyEvent(71,0,0)),"greenhouse UI captures a keyboard shortcut");
            click(page,binding.getMessage().getString());page.keyPressed(new net.minecraft.client.input.KeyEvent(256,0,0));s.check(key.matches(new net.minecraft.client.input.KeyEvent(71,0,0)),"Escape cancels binding without changing the key");
            click(page,binding.getMessage().getString());page.keyPressed(new net.minecraft.client.input.KeyEvent(261,0,0));s.check(key.isUnbound(),"Delete clears greenhouse shortcut");
        });
        s.steps.add(()->{for(int i=0;i<6;i++)page.mouseScrolled(page.width-30,110,0,-1);});
        s.steps.add(()->s.shot("greenhouse-palette"));
        s.steps.add(()->{page.save();s.mc.setScreen(new GreenhousePresetsScreen(page));});
        s.steps.add(()->{
            var screen=s.mc.screen;var name=(UiTheme.RoundedEditBox)VisualSmoke.field(screen,"name");name.setValue("预设 · Moonflower");click(screen,"另存为");
            String saved=page.library.active;name.setValue("空白布局");click(screen,"新建空白");s.check(page.preset.inputs.isEmpty(),"new preset starts empty");
            page.switchPreset(saved);s.check(page.preset.at(0,0)!=null,"switch restores saved crops");
            s.mc.setScreen(new GreenhousePresetsScreen(page));name=(UiTheme.RoundedEditBox)VisualSmoke.field(s.mc.screen,"name");name.setValue("Moonflower 布局");click(s.mc.screen,"重命名");
            s.check(page.preset.name.equals("Moonflower 布局"),"rename updates active preset");
            var reloaded=GreenhouseLibrary.open(GreenhouseStore.directory());s.check(reloaded.current().name.equals("Moonflower 布局"),"named active preset survives a disk reload");
        });
        s.steps.add(()->s.shot("greenhouse-presets"));
        s.steps.add(()->{s.mc.screen.onClose();s.check(s.mc.screen==page,"preset library returns to original editor");});
        s.steps.add(()->{page.onClose();s.check(s.mc.screen instanceof TitleScreen,"planner returns to parent");s.mc.createWorldOpenFlows().createFreshLevel("greenhouse-test-"+System.currentTimeMillis(),new net.minecraft.world.level.LevelSettings("Greenhouse fixture",net.minecraft.world.level.GameType.CREATIVE,new net.minecraft.world.level.LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL,false,false),true,net.minecraft.world.level.WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(1,false,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createFlatWorldDimensions,new TitleScreen());});
        s.waitUntil(()->s.mc.level!=null&&s.mc.player!=null&&s.mc.screen==null&&s.mc.getOverlay()==null);
        s.steps.add(()->{
            var dispatcher=net.fabricmc.fabric.api.client.command.v2.ClientCommands.getActiveDispatcher();
            s.check(dispatcher.getRoot().getChild("greenhouse")!=null,"standalone greenhouse command registered");
            s.check(!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sanctuary_orbs"),"no LUS installed");
            dispatcher.execute("greenhouse",null);
        });
        s.steps.add(()->{s.check(s.mc.screen instanceof GreenhouseScreen,"greenhouse opens editor");s.mc.screen.onClose();net.fabricmc.fabric.api.client.command.v2.ClientCommands.getActiveDispatcher().execute("gh",null);});
        s.steps.add(()->{s.check(s.mc.screen instanceof GreenhouseScreen,"gh opens editor");s.mc.screen.onClose();
            var key=net.minecraft.client.KeyMapping.get("key.lunaciia_greenhouse.greenhouse");key.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(71));net.minecraft.client.KeyMapping.resetMapping();
            net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(71));
        });
        s.steps.add(()->{s.check(s.mc.screen instanceof GreenhouseScreen,"greenhouse shortcut opens the editor in game");s.mc.screen.onClose();
            net.minecraft.client.KeyMapping.get("key.lunaciia_greenhouse.greenhouse").setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);net.minecraft.client.KeyMapping.resetMapping();s.mc.options.save();
        });
        s.steps.add(()->{
            var origin=s.mc.player.blockPosition().offset(-5,-1,2);
            for(int r=0;r<10;r++)for(int c=0;c<10;c++){s.mc.level.setBlock(origin.offset(c,0,r),net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(),3);s.mc.level.setBlock(origin.offset(c,1,r),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);}
            s.mc.level.setBlock(origin.offset(0,1,0),net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState(),3);
            s.mc.level.setBlock(origin.offset(1,1,0),net.minecraft.world.level.block.Blocks.CARROTS.defaultBlockState(),3);
            var p=new GreenhousePreset();for(int c=0;c<3;c++)p.paint(0,c,"wheat",false);p.paint(3,3,"godseed",true);
            s.mc.hitResult=new net.minecraft.world.phys.BlockHitResult(origin.getCenter(),net.minecraft.core.Direction.UP,origin,false);
            GreenhouseProjection.calibrate(p);var marks=(java.util.List<?>)VisualSmoke.staticField(GreenhouseProjection.class,"marks");
            s.check(marks.size()==4,"world projection has four crop footprints");s.check(((java.util.List<?>)VisualSmoke.staticField(GreenhouseProjection.class,"groundMarks")).size()==12,"projection renders crop footprint ground");
            var color=marks.get(0).getClass().getDeclaredMethod("color");color.setAccessible(true);
            s.check((int)color.invoke(marks.get(0))==0xff55ed8b,"matching crop is green");
            s.check((int)color.invoke(marks.get(1))==0xffff5555,"wrong crop is red");
            s.check((int)color.invoke(marks.get(2))==0xff66bbff,"missing crop is blue");
            var target=marks.get(3).getClass().getDeclaredMethod("target");target.setAccessible(true);
            s.check((boolean)target.invoke(marks.get(3))&&!(boolean)target.invoke(marks.get(0)),"only product targets use the tall frame");
            var tall=GreenhouseProjection.frameBounds(origin,3,true);var low=GreenhouseProjection.frameBounds(origin,3,false);
            s.check(tall.getYsize()>low.getYsize()*5&&tall.getXsize()==low.getXsize()&&tall.getZsize()==low.getZsize(),"target frame is taller while preserving full crop footprint");
            s.mc.player.setYRot(0);s.mc.player.setXRot(20);
        });
        for(int i=0;i<4;i++)s.steps.add(()->{});
        s.steps.add(()->{s.shot("greenhouse-projection");GreenhouseProjection.stop();s.check(((java.util.List<?>)VisualSmoke.staticField(GreenhouseProjection.class,"marks")).isEmpty(),"stop clears projection only");s.mc.disconnectFromWorld(net.minecraft.network.chat.Component.empty());});
        s.steps.add(()->{Files.writeString(s.out.resolve("greenhouse-result.txt"),"PASS "+s.checks);System.out.println("GREENHOUSE_SMOKE_COMPLETE checks="+s.checks);s.mc.stop();});
    }
    private static void click(net.minecraft.client.gui.screens.Screen screen,String label){
        var b=screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.AbstractWidget w&&w.getMessage().getString().equals(label)).map(c->(net.minecraft.client.gui.components.AbstractWidget)c).findFirst().orElseThrow();
        screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(b.getX()+b.getWidth()/2d,b.getY()+b.getHeight()/2d,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
    }
}
