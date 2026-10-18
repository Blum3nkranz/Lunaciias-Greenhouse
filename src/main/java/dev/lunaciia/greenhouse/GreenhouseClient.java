package dev.lunaciia.greenhouse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class GreenhouseClient implements ClientModInitializer {
    private static boolean open;
    public static Identifier id(String path){return Identifier.fromNamespaceAndPath("lunaciia_greenhouse",path);}
    @Override public void onInitializeClient(){
        try { GreenhouseStore.migrateLegacy(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()); }
        catch(Exception e){org.slf4j.LoggerFactory.getLogger("lunaciia_greenhouse").warn("Could not copy legacy Greenhouse presets; original files retained",e);}
        GardenLocation.init();GreenhouseProjection.init();
        var category=KeyMapping.Category.register(id("settings"));
        var key=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.lunaciia_greenhouse.greenhouse",InputConstants.UNKNOWN.getValue(),category));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->{
            var root=dispatcher.register(literal("greenhouse").executes(c->{open=true;return 1;}));
            dispatcher.register(literal("gh").executes(c->{open=true;return 1;}).redirect(root));
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            while(key.consumeClick())if(mc.screen==null&&mc.player!=null&&mc.isWindowActive())open=true;
            if(open){open=false;mc.setScreen(new GreenhouseScreen(null));}
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,mc)->reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->reset());
    }
    private static void reset(){open=false;GardenLocation.clear();GreenhouseProjection.stop();}
}
