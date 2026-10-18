package dev.lunaciia.greenhouse;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Native atlas geometry with the original dropdown's matte appearance and independent window layers. */
public final class NativeUi {
    private static final Identifier[] FILLS=new Identifier[11],BORDERS=new Identifier[11];
    static {for(int r=1;r<=10;r++){
        FILLS[r]=Identifier.fromNamespaceAndPath("lunaciia_greenhouse","glass/fill_"+r);
        BORDERS[r]=Identifier.fromNamespaceAndPath("lunaciia_greenhouse","glass/border_"+r);
    }}
    private NativeUi(){}
    public static void rounded(GuiGraphicsExtractor g,int x,int y,int w,int h,float radius,int color,boolean border){
        if(w<=0||h<=0)return;
        int r=Math.clamp(Math.round(Math.min(radius,Math.min(w,h)/2f-1)),0,10);
        if(r>0){g.blitSprite(RenderPipelines.GUI_TEXTURED,(border?BORDERS:FILLS)[r],x,y,w,h,color);return;}
        if(!border)g.fill(x,y,x+w,y+h,color);
        else {g.fill(x,y,x+w,y+1,color);g.fill(x,y+h-1,x+w,y+h,color);g.fill(x,y,x+1,y+h,color);g.fill(x+w-1,y,x+w,y+h,color);}
    }
    public static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean focused){
        panel(g,x,y,w,h,focused,1,true);
    }
    private static int alpha(int color,double opacity){return ((int)Math.round((color>>>24)*opacity)<<24)|(color&0xffffff);}
    public static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean focused,double opacity,boolean separator){
        float radius=UiTheme.rounding;
        rounded(g,x,y,w,h,radius,alpha(UiTheme.BG,opacity),false);
        rounded(g,x,y,w,h,radius,alpha(focused?UiTheme.mix(UiTheme.BORDER,UiTheme.ACCENT,.22f):UiTheme.BORDER,opacity),true);
        if(separator){
            int inset=Math.max(3,Math.round(radius));
            rounded(g,x+inset,y+1,w-2*inset,2,1,alpha(UiTheme.ACCENT,opacity),false);
        }
    }
    public static void scrollbar(GuiGraphicsExtractor g,int x,int y,int height,int thumbY,int thumbHeight,boolean hovered){
        rounded(g,x+1,y,4,height,2,0x483f556c,false);
        rounded(g,x,thumbY,6,thumbHeight,3,UiTheme.mix(UiTheme.ACCENT,UiTheme.TEXT,hovered?.4f:.05f),false);
    }
}
