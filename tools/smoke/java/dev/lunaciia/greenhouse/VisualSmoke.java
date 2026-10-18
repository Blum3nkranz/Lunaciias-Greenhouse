package dev.lunaciia.greenhouse;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;

/** Runs in an isolated offline client, never connects to a multiplayer server. */
public final class VisualSmoke implements ClientModInitializer {
    interface Action {void run()throws Exception;}
    final ArrayDeque<Action> steps=new ArrayDeque<>();
    Minecraft mc;int ticks,checks;long start=System.nanoTime();
    final Path out=Path.of("../screenshots");
    void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("CHECK "+label);}
    static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static Object staticField(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    void waitUntil(java.util.function.BooleanSupplier ready){steps.add(new Action(){public void run(){if(!ready.getAsBoolean())steps.addFirst(this);}});}
    void shot(String name){Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{try(image){image.writeToFile(out.resolve(name+".png"));}catch(Exception e){throw new RuntimeException(e);}});}
    @Override public void onInitializeClient(){
        GreenhouseSmoke.enqueue(this);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            mc=client;
            if(System.nanoTime()-start>240_000_000_000L){System.err.println("GREENHOUSE_SMOKE_TIMEOUT");mc.stop();return;}
            if(mc.getOverlay()!=null||++ticks%12!=0)return;
            try{if(!steps.isEmpty())steps.remove().run();}
            catch(Throwable e){System.err.println("GREENHOUSE_SMOKE_FAILED");e.printStackTrace();mc.stop();}
        });
    }
}
