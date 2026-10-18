package dev.lunaciia.greenhouse;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/** Native planting editor. It never advances growth or performs server actions. */
public final class GreenhouseScreen extends Screen {
    private final Screen parent;
    GreenhouseLibrary library;
    GreenhousePreset preset;
    private String selected="wheat",ground="farmland",status="",query="";
    private int mode,x,y,cell,px,pw,paletteTop,paletteBottom,columns,cardWidth,cardHeight,scrollRow,totalRows;
    private UiTheme.RoundedEditBox search;
    private UiTheme.FlatButton presetsButton,autoButton;
    private UiTheme.FlatButton bindingButton;
    private boolean captureBinding;
    private final List<CropCard> cards=new ArrayList<>();
    private final List<UiTheme.FlatButton> modes=new ArrayList<>();
    public GreenhouseScreen(Screen parent){
        super(Component.literal("Greenhouse"));this.parent=parent;
        try{library=GreenhouseLibrary.open(GreenhouseStore.directory());preset=library.current();}
        catch(Exception e){preset=GreenhouseStore.load();status=tr("预设库读取失败，未覆盖原文件：","Cannot read preset library; original file retained: ")+e.getMessage();}
        preset.mergeTargets();
    }
    static String tr(String zh,String en){return net.minecraft.client.Minecraft.getInstance().options.languageCode.equals("zh_cn")?zh:en;}
    @Override protected void init(){
        UiTheme.apply();cards.clear();modes.clear();
        cell=Math.max(4,Math.min(32,Math.min((height-180)/10,(width/2-27)/10)));x=23;y=94;
        px=x+cell*10+22;pw=width-px-23;paletteTop=91;paletteBottom=height-88;
        presetsButton=button(preset.name+" ▾",20,39,Math.max(85,width/3-24),()->{if(library!=null)minecraft.setScreen(new GreenhousePresetsScreen(this));});
        int tx=width/3+2,tw=Math.max(32,(width-tx-24)/3);
        button(tr("导入","Import"),tx,39,tw-4,this::importClipboard);
        button(tr("复制预设","Copy preset"),tx+tw,39,tw-4,()->{minecraft.keyboardHandler.setClipboard(GreenhouseStore.JSON.toJson(preset));status=tr("已复制预设（包含土地）","Preset copied, including ground");});
        button(tr("导出文件","Export file"),tx+2*tw,39,tw-4,()->{try{save();status=GreenhouseStore.export(preset).toString();}catch(Exception e){error(e);}});
        button(tr("返回","Back"),width-69,13,49,this::onClose);
        String[] labels={tr("种植","Plants"),tr("土地","Ground")};int mw=cell*10/2;
        for(int i=0;i<2;i++){final int index=i;var b=button(labels[i],x+i*mw,67,mw-3,()->{mode=index;scrollRow=0;rebuildPalette();});modes.add(b);}
        search=new UiTheme.RoundedEditBox(font,px,67,pw,20,Component.literal("Search crops"));search.setHint(Component.literal(tr("搜索作物…","Search crops…")));search.setValue(query);addRenderableWidget(search);search.setResponder(s->{query=s;scrollRow=0;rebuildPalette();});
        int autoWidth=Math.min(250,(width-48)*3/5);
        autoButton=button("",20,height-61,autoWidth,()->{if(library!=null){library.autoGround=!library.autoGround;save();}});
        autoButton.setTooltip(Tooltip.create(Component.literal(tr("关闭后保留原土地；土地模式可单独更换。","When off, retain existing ground. Change it separately in Ground mode."))));
        bindingButton=button("",28+autoWidth,height-61,width-48-autoWidth,()->{captureBinding=true;setFocused(null);});
        bindingButton.setTooltip(Tooltip.create(Component.translatable("ui.bind.hint")));
        int bw=(width-43)/4;
        button(tr("开启投影","Project"),20,height-34,bw-3,()->{try{save();GreenhouseProjection.start(preset);status=GreenhouseProjection.status;}catch(Exception e){error(e);}});
        button(tr("停止投影","Stop"),20+bw,height-34,bw-3,GreenhouseProjection::stop);
        button(tr("手动校准","Calibrate"),20+2*bw,height-34,bw-3,()->{GreenhouseProjection.calibrate(preset);status=GreenhouseProjection.status;});
        button(tr("向右旋转","Rotate right"),20+3*bw,height-34,bw-3,()->preset.rotate());
        rebuildPalette();
    }
    private UiTheme.FlatButton button(String text,int xx,int yy,int w,Runnable action){var b=new UiTheme.FlatButton(Component.literal(text),action);b.setRectangle(w,20,xx,yy);return addRenderableWidget(b);}
    void save(){try{if(library!=null){library.update(preset);library.save(GreenhouseStore.directory());}GreenhouseStore.save(preset);}catch(Exception e){error(e);throw new IllegalStateException(status,e);}}
    void switchPreset(String id){try{save();preset=library.switchTo(id,preset);save();GreenhouseProjection.stop();status=tr("已切换：","Selected: ")+preset.name;}catch(Exception e){error(e);}}
    private void error(Throwable e){status=e.getMessage()==null?e.toString():e.getMessage();}
    private void importClipboard(){try{
        String text=minecraft.keyboardHandler.getClipboard().trim();if(text.length()>2_000_000)throw new IllegalArgumentException("Preset too large");
        GreenhousePreset p;
        if(text.startsWith("{")){var json=JsonParser.parseString(text).getAsJsonObject();if(!json.has("inputs")&&!json.has("targets")&&!json.has("groundTiles"))throw new IllegalArgumentException(tr("请导入种植布局预设","Import a planting layout preset"));p=GreenhouseStore.JSON.fromJson(json,GreenhousePreset.class);}else p=GreenhouseCode.decode(text);p.validate();
        if(library==null)throw new IllegalStateException("Preset library unavailable");save();library.add(p,p.name==null?"Imported":p.name);preset=library.current();save();presetsButton.setMessage(Component.literal(preset.name+" ▾"));status=tr("已作为新预设导入","Imported as a new preset");
    }catch(Exception e){error(e);}}
    private void rebuildPalette(){
        for(var b:cards)removeWidget(b);cards.clear();
        for(int i=0;i<modes.size();i++)modes.get(i).selected=i==mode;
        columns=Math.max(1,pw/60);cardWidth=Math.max(30,(pw-8)/columns-4);cardHeight=Math.max(56,16+40);
        List<String> ids=mode==1?GreenhouseGround.TYPES:GreenhouseCatalog.crops().values().stream().filter(c->(c.name()+" "+c.id()).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).map(GreenhouseCatalog.Crop::id).toList();
        totalRows=(ids.size()+columns-1)/columns;int visible=Math.max(1,(paletteBottom-paletteTop)/(cardHeight+4));scrollRow=Math.clamp(scrollRow,0,Math.max(0,totalRows-visible));
        for(int i=scrollRow*columns;i<Math.min(ids.size(),(scrollRow+visible)*columns);i++){
            String id=ids.get(i);var card=new CropCard(id,mode==1);int local=i-scrollRow*columns;card.setRectangle(cardWidth,cardHeight,px+(local%columns)*(cardWidth+4),paletteTop+(local/columns)*(cardHeight+4));cards.add(card);addRenderableWidget(card);
        }
    }
    private final class CropCard extends UiTheme.FlatButton {
        final String id;final boolean soil;
        CropCard(String id,boolean soil){super(Component.literal(soil?GreenhouseGround.name(id):GreenhouseCatalog.crops().get(id).name()),()->{if(soil)ground=id;else GreenhouseScreen.this.selected=id;});this.id=id;this.soil=soil;
            var crop=GreenhouseCatalog.crops().get(id);setTooltip(Tooltip.create(Component.literal(soil?GreenhouseGround.name(id):crop.name()+" · "+crop.size()+"×"+crop.size()+"\n"+GreenhouseGround.name(crop.ground())).withColor(soil?UiTheme.TEXT:crop.color())));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt){
            boolean chosen=id.equals(soil?ground:GreenhouseScreen.this.selected);int color=soil?UiTheme.TEXT:GreenhouseCatalog.crops().get(id).color();
            NativeUi.rounded(g,getX(),getY(),getWidth(),getHeight(),UiTheme.rounding,UiTheme.mix(UiTheme.CARD,UiTheme.ACCENT,chosen?.22f:isHovered()?.12f:0),false);
            NativeUi.rounded(g,getX(),getY(),getWidth(),getHeight(),UiTheme.rounding,chosen?UiTheme.ACCENT:UiTheme.BORDER,true);
            int size=Math.min(30,getWidth()-10),ix=getX()+(getWidth()-size)/2;
            if(soil)drawGround(g,id,ix,getY()+6,size);else drawIcon(g,id,ix,getY()+6,size);
            String name=getMessage().getString();if(font.width(name)>getWidth()-8)name=font.plainSubstrByWidth(name,getWidth()-16)+"…";
            g.text(font,name,getX()+(getWidth()-font.width(name))/2,getBottom()-14,color,false);
            if(!soil){int n=GreenhouseCatalog.crops().get(id).size();String badge=n+"×"+n;g.text(font,badge,getRight()-font.width(badge)-4,getY()+3,UiTheme.MUTED,false);}
        }
    }
    @Override public boolean mouseScrolled(double xx,double yy,double horizontal,double vertical){if(xx>=px&&xx<px+pw&&yy>=paletteTop&&yy<paletteBottom){scrollRow-=Integer.signum((int)Math.signum(vertical));rebuildPalette();return true;}return super.mouseScrolled(xx,yy,horizontal,vertical);}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(captureBinding){finishBinding(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(e.button()));return true;}
        if(e.x()>=x&&e.x()<x+cell*10&&e.y()>=y&&e.y()<y+cell*10&&(e.button()==0||e.button()==1)){
            int row=(int)(e.y()-y)/cell,col=(int)(e.x()-x)/cell;
            try{if(mode==1)preset.paintGround(row,col,e.button()==1?null:ground);else if(e.button()==1)preset.erase(row,col);else preset.plant(row,col,selected,false,library==null||library.autoGround);}catch(Exception err){error(err);}return true;
        }return super.mouseClicked(e,doubleClick);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){
        if(captureBinding){
            if(event.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)finishBinding(null);
            else finishBinding(event.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE||event.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE?com.mojang.blaze3d.platform.InputConstants.UNKNOWN:com.mojang.blaze3d.platform.InputConstants.getKey(event));
            return true;
        }return super.keyPressed(event);
    }
    private void finishBinding(com.mojang.blaze3d.platform.InputConstants.Key key){
        captureBinding=false;
        if(key!=null){net.minecraft.client.KeyMapping.get("key.lunaciia_greenhouse.greenhouse").setKey(key);net.minecraft.client.KeyMapping.resetMapping();minecraft.options.save();}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){UiTheme.apply();
        g.fill(0,0,width,height,0xc0080a0f);NativeUi.panel(g,8,6,width-16,height-12,false);
        g.text(font,tr("温室 · 种植编辑器","Greenhouse · Planting editor"),20,17,UiTheme.TEXT,false);
        NativeUi.rounded(g,x-6,y-5,cell*10+12,cell*10+12,UiTheme.rounding,UiTheme.SIDE,false);
        String hover=null;
        for(int r=0;r<10;r++)for(int c=0;c<10;c++){int xx=x+c*cell,yy=y+r*cell;g.fill(xx,yy,xx+cell-1,yy+cell-1,UiTheme.CARD);String land=GreenhouseGround.at(preset,r,c);if(land!=null)drawGround(g,land,xx+1,yy+1,cell-2);}
        for(var list:List.of(preset.inputs,preset.targets))for(var p:list){var crop=GreenhouseCatalog.crops().get(p.cropId());int xx=x+p.position()[1]*cell,yy=y+p.position()[0]*cell,size=crop.size()*cell;drawIcon(g,p.cropId(),xx+3,yy+3,size-6);}
        if(mx>=x&&mx<x+cell*10&&my>=y&&my<y+cell*10){int r=(my-y)/cell,c=(mx-x)/cell;String land=GreenhouseGround.at(preset,r,c);var p=preset.at(r,c);hover=(p==null?"":GreenhouseCatalog.crops().get(p.cropId()).name()+"\n")+tr("土地：","Ground: ")+(land==null?tr("未放置","Empty"):GreenhouseGround.name(land));UiTheme.border(g,x+c*cell,y+r*cell,cell-1,cell-1,UiTheme.ACCENT);}
        int rows=Math.max(1,(paletteBottom-paletteTop)/(cardHeight+4));if(totalRows>rows){int h=paletteBottom-paletteTop,thumb=Math.max(12,h*rows/totalRows);NativeUi.scrollbar(g,px+pw-5,paletteTop,h,paletteTop+(h-thumb)*scrollRow/(totalRows-rows),thumb,false);}
        g.text(font,font.plainSubstrByWidth(status.isEmpty()?tr("左键放置 · 右键移除作物和土地 · 滚轮浏览作物","Left place · Right clear crop and ground · Scroll crops"):status,width-44),22,height-76,UiTheme.MUTED,false);
        autoButton.setMessage(Component.literal(tr("自动放置对应土地：","Auto-place matching ground: ")+(library==null||library.autoGround?tr("开启","On"):tr("关闭","Off"))));autoButton.selected=library==null||library.autoGround;
        bindingButton.selected=captureBinding;
        bindingButton.setMessage(captureBinding?Component.translatable("ui.bind.wait"):Component.translatable("greenhouse.binding").copy().append(": ").append(net.minecraft.client.KeyMapping.get("key.lunaciia_greenhouse.greenhouse").getTranslatedKeyMessage()));
        super.extractRenderState(g,mx,my,dt);if(hover!=null)g.setTooltipForNextFrame(font,Component.literal(hover),mx,my);
    }
    static void drawGround(GuiGraphicsExtractor g,String ground,int x,int y,int size){g.blit(RenderPipelines.GUI_TEXTURED,GreenhouseGround.texture(ground),x,y,0,0,size,size,16,16,16,16);}
    static void drawIcon(GuiGraphicsExtractor g,String id,int x,int y,int size){var dim=GreenhouseCatalog.imageSize(id);g.blit(RenderPipelines.GUI_TEXTURED,GreenhouseClient.id("textures/greenhouse/"+id+".png"),x,y,0,0,size,size,dim[0],dim[1],dim[0],dim[1]);}
    @Override public void removed(){captureBinding=false;}
    @Override public void onClose(){try{save();minecraft.setScreen(parent);}catch(Exception ignored){}}
    @Override public boolean isPauseScreen(){return false;}
}

