package dev.lunaciia.greenhouse;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GreenhousePresetsScreen extends Screen {
    private final GreenhouseScreen parent;
    private UiTheme.RoundedEditBox name;
    private final List<UiTheme.FlatButton> rows=new ArrayList<>();
    private int scroll;
    private String status="";
    GreenhousePresetsScreen(GreenhouseScreen parent){super(Component.literal(GreenhouseScreen.tr("本地预设","Local presets")));this.parent=parent;}
    @Override protected void init(){
        int x=Math.max(20,width/2-220),w=width-2*x;
        name=new UiTheme.RoundedEditBox(font,x,43,w,22,Component.literal("Preset name"));name.setMaxLength(80);name.setValue(parent.preset.name);addRenderableWidget(name);
        button(GreenhouseScreen.tr("重命名","Rename"),x,72,w/3-4,()->{parent.preset.name=GreenhouseLibrary.validName(name.getValue());parent.save();rebuild();});
        button(GreenhouseScreen.tr("另存为","Save as"),x+w/3,72,w/3-4,()->create(false));
        button(GreenhouseScreen.tr("新建空白","New blank"),x+2*w/3,72,w/3-4,()->create(true));
        button(GreenhouseScreen.tr("返回编辑器","Back to editor"),x,height-35,w,this::onClose);rebuild();
    }
    private void create(boolean empty){String next=GreenhouseLibrary.validName(name.getValue());parent.save();parent.library.add(empty?new GreenhousePreset():parent.preset,next);parent.preset=parent.library.current();parent.save();GreenhouseProjection.stop();rebuild();}
    private void button(String label,int x,int y,int w,Runnable action){var b=new UiTheme.FlatButton(Component.literal(label),()->{try{action.run();status="";}catch(Exception e){status=e.getMessage();}});b.setRectangle(w,22,x,y);addRenderableWidget(b);}
    private void rebuild(){
        for(var b:rows)removeWidget(b);rows.clear();int rowHeight=32,visible=Math.max(1,(height-161)/rowHeight);
        scroll=Math.clamp(scroll,0,Math.max(0,parent.library.presets.size()-visible));int i=0;
        for(var e:parent.library.presets.entrySet()){
            if(i++<scroll)continue;if(rows.size()>=visible)break;var p=e.getValue();String label=p.name+"   ·   "+(p.inputs.size()+p.targets.size())+GreenhouseScreen.tr(" 个作物"," crops");
            var b=new UiTheme.FlatButton(Component.literal(label),()->{parent.switchPreset(e.getKey());name.setValue(parent.preset.name);rebuild();});b.selected=e.getKey().equals(parent.library.active);
            b.setRectangle(name.getWidth(),rowHeight-4,name.getX(),106+rows.size()*rowHeight);rows.add(b);addRenderableWidget(b);
        }
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll-=Integer.signum((int)Math.signum(v));rebuild();return true;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){UiTheme.apply();
        g.fill(0,0,width,height,0xc0080a0f);NativeUi.panel(g,8,6,width-16,height-12,false);g.text(font,getTitle(),name.getX(),20,UiTheme.TEXT,false);
        g.text(font,font.plainSubstrByWidth(status,width-40),20,height-51,UiTheme.MUTED,false);super.extractRenderState(g,mx,my,dt);
    }
    @Override public void onClose(){try{parent.save();minecraft.setScreen(parent);}catch(Exception e){status=e.getMessage();}}
    @Override public boolean isPauseScreen(){return false;}
}
