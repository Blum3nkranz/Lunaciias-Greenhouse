package dev.lunaciia.greenhouse;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.gui.Font;

/** Passive expected/actual overlay, inspired by Litematica's verifier categories.
 * Garden grid and crop evidence adapted from Skyblocker 64fd061 (LGPL-3.0).
 */
public final class GreenhouseProjection {
    private record Mark(BlockPos pos,String id,String name,int size,int color,boolean target){}
    private static List<Mark> marks=List.of();
    private record GroundMark(BlockPos pos,String ground,int color){}
    private static List<GroundMark> groundMarks=List.of();
    private static GreenhousePreset preset;
    private static BlockPos origin;
    private static Object world;
    private static int ticks;
    private static RenderType lines;
    static String status="";
    public static void init(){
        lines=ProjectionLines.type("greenhouse_projection",true);
        ClientTickEvents.END_CLIENT_TICK.register(GreenhouseProjection::tick);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(ctx->{
            var mc=Minecraft.getInstance();if((marks.isEmpty()&&groundMarks.isEmpty())||mc.player==null||mc.level!=world||mc.options.hideGui)return;
            var camera=ctx.levelState().cameraRenderState;if(!camera.initialized)return;
            for(var m:groundMarks){
                var buffer=ctx.bufferSource().getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(GreenhouseGround.texture(m.ground)));
                var p=ctx.poseStack().last();float x=(float)(m.pos.getX()-camera.pos.x),y=(float)(m.pos.getY()+1.008-camera.pos.y),z=(float)(m.pos.getZ()-camera.pos.z);int tint=(m.color&0xffffff)|0xa0000000;
                buffer.addVertex(p,x,y,z).setColor(tint).setUv(0,0).setOverlay(655360).setLight(0xF000F0).setNormal(p,0,1,0);
                buffer.addVertex(p,x,y,z+1).setColor(tint).setUv(0,1).setOverlay(655360).setLight(0xF000F0).setNormal(p,0,1,0);
                buffer.addVertex(p,x+1,y,z+1).setColor(tint).setUv(1,1).setOverlay(655360).setLight(0xF000F0).setNormal(p,0,1,0);
                buffer.addVertex(p,x+1,y,z).setColor(tint).setUv(1,0).setOverlay(655360).setLight(0xF000F0).setNormal(p,0,1,0);
            }
            var out=ctx.bufferSource().getBuffer(lines);var pose=ctx.poseStack().last();
            float pulse=(float)(.2+.2*(1+Math.sin(System.nanoTime()/650_000_000d))/2);
            for(var m:marks){var b=frameBounds(m.pos,m.size,m.target).move(camera.pos.scale(-1));var style=new ProjectionLines.Style(0xffffffff,m.target?3:2,false);
                BoxGeometry.box((x,y,z,xx,yy,zz)->ProjectionLines.line(out,pose,x,y,z,xx,yy,zz,style),b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);
                if(m.target){var corner=new ProjectionLines.Style(((int)(175+200*pulse)<<24)|0xffffff,4,false);
                    for(int i=0;i<4;i++){int sx=(i&1)==0?-1:1,sz=(i&2)==0?-1:1;double x=sx<0?b.minX:b.maxX,z=sz<0?b.minZ:b.maxZ;
                        ProjectionLines.line(out,pose,x,b.maxY,z,x-sx*.23,b.maxY,z,corner);
                        ProjectionLines.line(out,pose,x,b.maxY,z,x,b.maxY,z-sz*.23,corner);
                        ProjectionLines.line(out,pose,x,b.maxY,z,x,b.maxY-.3,z,corner);
                    }
                }
            }
            ctx.bufferSource().endBatch(lines);
            for(var m:marks){var v=m.pos.getCenter().add((m.size-1)/2d,m.target?1.55:.5,(m.size-1)/2d).subtract(camera.pos);var stack=ctx.poseStack();stack.pushPose();stack.translate(v.x,v.y,v.z);stack.mulPose(mc.gameRenderer.getMainCamera().rotation());
                var texture=net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(GreenhouseClient.id("textures/greenhouse/"+m.id+".png"));
                var image=ctx.bufferSource().getBuffer(texture);var imagePose=stack.last();float s=m.target?.48f:.38f;
                image.addVertex(imagePose,-s,-s,0).setColor((m.color&0xffffff)|0xb3000000).setUv(0,1).setOverlay(655360).setLight(0xF000F0).setNormal(imagePose,0,0,1);
                image.addVertex(imagePose,s,-s,0).setColor((m.color&0xffffff)|0xb3000000).setUv(1,1).setOverlay(655360).setLight(0xF000F0).setNormal(imagePose,0,0,1);
                image.addVertex(imagePose,s,s,0).setColor((m.color&0xffffff)|0xb3000000).setUv(1,0).setOverlay(655360).setLight(0xF000F0).setNormal(imagePose,0,0,1);
                image.addVertex(imagePose,-s,s,0).setColor((m.color&0xffffff)|0xb3000000).setUv(0,0).setOverlay(655360).setLight(0xF000F0).setNormal(imagePose,0,0,1);
                stack.translate(0,m.target?.58:.45,0);stack.scale(.009f,-.009f,.009f);
                mc.font.drawInBatch(ComponentText(m.name),-mc.font.width(m.name)/2f,0,m.color,false,stack.last().pose(),ctx.bufferSource(),Font.DisplayMode.NORMAL,0x55000000,0xF000F0);stack.popPose();}
        });
    }
    static AABB frameBounds(BlockPos pos,int size,boolean target){return new AABB(pos.getX()+.05,pos.getY()+.05,pos.getZ()+.05,pos.getX()+size-.05,pos.getY()+.05+(target?1.5:.25),pos.getZ()+size-.05);}
    private static net.minecraft.util.FormattedCharSequence ComponentText(String name){return net.minecraft.network.chat.Component.literal(name).getVisualOrderText();}
    static BlockPos origin(int x,int z){return new BlockPos(Math.floorDiv(x+240,96)*96-197,73,Math.floorDiv(z+240,96)*96-197);}
    public static void start(GreenhousePreset p){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||!allowed(mc)){status=GreenhouseScreen.tr("请先进入 Garden 温室","Enter a Garden Greenhouse first");return;}
        origin=origin(mc.player.blockPosition().getX(),mc.player.blockPosition().getZ());
        // Source's y=73 is a heuristic; require nearby visible farm ground before using it.
        int ground=0;for(int r=0;r<10;r++)for(int c=0;c<10;c++){var pos=origin.offset(c,0,r);if(mc.level.hasChunkAt(pos)&&validGround(mc.level.getBlockState(pos).getBlock()))ground++;}
        if(ground<10||mc.player.distanceToSqr(origin.getCenter())>1600){status=GreenhouseScreen.tr("未确认温室土地：请进入已加载的温室","Greenhouse ground not confirmed: enter a loaded plot");return;}
        activate(p,mc);
    }
    private static boolean allowed(Minecraft mc){return GardenLocation.isGarden()||mc.hasSingleplayerServer();}
    private static void activate(GreenhousePreset p,Minecraft mc){p.validate();preset=GreenhouseStore.JSON.fromJson(GreenhouseStore.JSON.toJson(p),GreenhousePreset.class);preset.mergeTargets();world=mc.level;ticks=0;status=GreenhouseScreen.tr("投影开启：绿色正确，红色错误，蓝色未种，黄色未知","Projection: green correct, red wrong, blue missing, yellow unknown");tick(mc);}
    static void calibrate(GreenhousePreset p){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||!allowed(mc)||!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||!validGround(mc.level.getBlockState(hit.getBlockPos()).getBlock())){status=GreenhouseScreen.tr("请看向温室西北角的土地后校准","Look at the northwest ground cell to calibrate");return;}origin=hit.getBlockPos();activate(p,mc);}
    static boolean validGround(net.minecraft.world.level.block.Block b){return b==Blocks.FARMLAND||b==Blocks.SAND||b==Blocks.SOUL_SAND||b==Blocks.MYCELIUM||b==Blocks.NETHERRACK||b==Blocks.END_STONE;}
    public static void stop(){preset=null;marks=List.of();groundMarks=List.of();world=null;}
    private static void tick(Minecraft mc){
        if(preset==null)return;if(mc.level==null||mc.player==null||mc.level!=world||!allowed(mc)){stop();return;}if(ticks++%5!=0)return;
        var actual=observe(mc);var next=new ArrayList<Mark>();
        var grounds=new ArrayList<GroundMark>();
        for(int r=0;r<10;r++)for(int c=0;c<10;c++){String expected=GreenhouseGround.at(preset,r,c);if(expected==null)continue;var pos=origin.offset(c,0,r);boolean loaded=mc.level.hasChunkAt(pos);var block=loaded?mc.level.getBlockState(pos).getBlock():null;
            grounds.add(new GroundMark(pos,expected,!loaded?0xffffd35c:block==Blocks.AIR?0xff66bbff:groundMatches(block,expected)?0xff55ed8b:0xffff5555));}
        groundMarks=List.copyOf(grounds);
        for(var list:List.of(preset.inputs,preset.targets))for(var c:list){var pos=origin.offset(c.position()[1],1,c.position()[0]);
            var crop=GreenhouseCatalog.crops().get(c.cropId());boolean unknown=false,wrong=false,missing=false;
            for(int r=0;r<crop.size();r++)for(int col=0;col<crop.size();col++){
                int rr=c.position()[0]+r,cc=c.position()[1]+col;String a=actual[rr][cc];
                if(a==null)unknown=true;else if(a.isEmpty())missing=true;else if(!a.equals(c.cropId()))wrong=true;
                var ground=origin.offset(cc,0,rr);String expected=GreenhouseGround.at(preset,rr,cc);if(!mc.level.hasChunkAt(ground))unknown=true;else if(expected!=null&&!groundMatches(mc.level.getBlockState(ground).getBlock(),expected))wrong=true;
            }
            int color=classify(unknown,wrong,missing,false);
            boolean target=crop.mutation();
            next.add(new Mark(pos,c.cropId(),(target?GreenhouseScreen.tr("产物 · ","Target · "):"")+crop.name(),crop.size(),color,target));
        }
        marks=List.copyOf(next);
    }
    static int classify(boolean unknown,boolean wrong,boolean missing,boolean target){return wrong?0xffff5555:unknown?0xffffd35c:missing&&!target?0xff66bbff:0xff55ed8b;}
    static int[] anchor(String id,int row,int col){return switch(id){case "snoozling"->new int[]{row-2,col-1};case "godseed"->new int[]{row-1,col-1};default->new int[]{row,col};};}
    private static String normalize(String s){return s.replaceAll("§.", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","").replace("lonelilly","lonelily").replace("plantboyadvance","plantboy").replace("jerryseed","jerryflower").replace("snoozlingflower","snoozling");}
    private static String[][] observe(Minecraft mc){
        String[][] cells=new String[10][10];
        for(int r=0;r<10;r++)for(int c=0;c<10;c++){var pos=origin.offset(c,1,r);cells[r][c]=mc.level.hasChunkAt(pos)?blockCrop(mc.level.getBlockState(pos).getBlock()):null;}
        // One bounded entity query every five ticks; no per-frame world scan.
        for(var e:mc.level.getEntities(null,new AABB(origin).expandTowards(9,6,9)))if(e instanceof ArmorStand a){
            int row=(int)Math.floor(a.getZ())-origin.getZ(),col=(int)Math.floor(a.getX())-origin.getX();
            if(row<0||row>=10||col<0||col>=10)continue;
            var item=a.getItemBySlot(EquipmentSlot.HEAD);if(item.isEmpty())continue;String name=normalize(item.getHoverName().getString());
            if(name.contains("plantboyroots")||name.contains("godseedpillar"))continue;
            String id=null;var profile=item.get(net.minecraft.core.component.DataComponents.PROFILE);
            if(profile!=null)for(var property:profile.partialProfile().properties().get("textures")){id=GreenhouseCatalog.texture(property.value());if(id!=null)break;}
            if(id==null)for(var crop:GreenhouseCatalog.crops().values())if(name.contains(normalize(crop.id()))){id=crop.id();break;}
            if(id==null){cells[row][col]=null;continue;}
            var crop=GreenhouseCatalog.crops().get(id);int[] anchor=anchor(id,row,col);
            for(int r=0;r<crop.size();r++)for(int c=0;c<crop.size();c++){int rr=anchor[0]+r,cc=anchor[1]+c;if(rr>=0&&rr<10&&cc>=0&&cc<10)cells[rr][cc]=id;}
        }
        return cells;
    }
    private static boolean groundMatches(net.minecraft.world.level.block.Block b,String ground){return switch(ground){case "farmland"->b==Blocks.FARMLAND;case "sand"->b==Blocks.SAND;case "soul_sand"->b==Blocks.SOUL_SAND;case "mycelium"->b==Blocks.MYCELIUM;case "netherrack"->b==Blocks.NETHERRACK;case "end_stone"->b==Blocks.END_STONE;default->validGround(b);};}
    private static String blockCrop(net.minecraft.world.level.block.Block b){
        if(b==Blocks.AIR)return "";if(b==Blocks.WHEAT)return "wheat";if(b==Blocks.CARROTS)return "carrot";if(b==Blocks.POTATOES)return "potato";if(b==Blocks.SUGAR_CANE)return "sugar_cane";if(b==Blocks.NETHER_WART)return "nether_wart";if(b==Blocks.FIRE)return "fire";if(b==Blocks.DEAD_BUSH)return "dead_plant";if(b==Blocks.RED_MUSHROOM)return "red_mushroom";if(b==Blocks.BROWN_MUSHROOM)return "brown_mushroom";if(b==Blocks.CACTUS)return "cactus";if(b==Blocks.COCOA)return "cocoa";return "other";
    }
}
