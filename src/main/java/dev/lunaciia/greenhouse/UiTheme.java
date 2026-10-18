package dev.lunaciia.greenhouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Native Minecraft widgets and rounded sprites; no LUS or NanoVG runtime. */
final class UiTheme {
    static final int BG=0xff15171d,SIDE=0xff101217,CARD=0xff20232b,BORDER=0xff343945,
        ACCENT=0xffc4b5fd,TEXT=0xffeeedf4,MUTED=0xff9a9eac;
    static final float rounding=5;
    static void apply(){}
    static class RoundedEditBox extends net.minecraft.client.gui.components.EditBox {
        RoundedEditBox(net.minecraft.client.gui.Font font,int x,int y,int w,int h,Component label){super(font,x,y,w,h,label);}
    }
    static class FlatButton extends Button {
        boolean selected;
        FlatButton(Component label,Runnable action){super(0,0,100,22,label,b->action.run(),DEFAULT_NARRATION);}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt){
            var mc=Minecraft.getInstance();boolean focus=isFocused()&&mc.getLastInputType().isKeyboard();
            int fill=mix(CARD,ACCENT,selected?.3f:isHovered()||focus?.16f:0);
            NativeUi.rounded(g,getX(),getY(),getWidth(),getHeight(),rounding,fill,false);
            NativeUi.rounded(g,getX(),getY(),getWidth(),getHeight(),rounding,selected||focus?ACCENT:BORDER,true);
            String label=getMessage().getString();
            if(mc.font.width(label)>getWidth()-10)label=mc.font.plainSubstrByWidth(label,Math.max(0,getWidth()-18))+"…";
            g.text(mc.font,label,getX()+(getWidth()-mc.font.width(label))/2,getY()+(getHeight()-mc.font.lineHeight)/2,active?TEXT:MUTED,false);
        }
    }
    static int mix(int a,int b,float t){int result=0;for(int shift=0;shift<=24;shift+=8)result|=Math.round(((a>>>shift)&255)*(1-t)+((b>>>shift)&255)*t)<<shift;return result;}
    static void border(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){NativeUi.rounded(g,x,y,w,h,0,color,true);}
}
