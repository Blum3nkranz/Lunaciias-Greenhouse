package dev.lunaciia.greenhouse;

import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;

/** Only Hypixel's confirmed location event enables multiplayer projection. */
final class GardenLocation {
    private static boolean garden;
    private static String server="";
    static void init(){
        var api=HypixelModAPI.getInstance();
        api.createHandler(ClientboundLocationPacket.class,packet->{
            var connection=Minecraft.getInstance().getConnection();
            Minecraft.getInstance().execute(()->{
                if(connection==null||connection!=Minecraft.getInstance().getConnection())return;
                boolean next="SkyBlock".equalsIgnoreCase(packet.getServerType().map(t->t.getName()).orElse(""))&&"garden".equals(packet.getMode().orElse(""));
                if(garden!=next||!server.equals(packet.getServerName()))GreenhouseProjection.stop();
                garden=next;server=packet.getServerName();
            });
        });
        api.subscribeToEventPacket(ClientboundLocationPacket.class);
    }
    static boolean isGarden(){return garden;}
    static void clear(){garden=false;server="";}
}
