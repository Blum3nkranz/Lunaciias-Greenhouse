package dev.lunaciia.greenhouse;

import com.google.gson.*;
import java.util.*;

/** Layout is the same coordinate and identity format as SkyShards designEncoding. */
public final class GreenhousePreset {
    public int version=1;
    public String name="Greenhouse";
    public List<Cell> inputs=new ArrayList<>(),targets=new ArrayList<>();
    public JsonArray groundTiles=new JsonArray();
    public boolean explicitGround;
    public JsonObject startingInventory=new JsonObject();
    public JsonObject settings;
    public JsonObject scenario;
    public record Cell(String cropId,int[] position){}
    /** Legacy target placements are retained as crops after removal of the target category. */
    public void mergeTargets(){validate();inputs.addAll(targets);targets.clear();scenario=null;}
    public void erase(int row,int col){
        var crop=at(row,col);int r=crop==null?row:crop.position()[0],c=crop==null?col:crop.position()[1];
        int size=crop==null?1:GreenhouseCatalog.crops().get(crop.cropId()).size();
        plant(row,col,null,false,false);
        for(int rr=r;rr<r+size;rr++)for(int cc=c;cc<c+size;cc++)paintGround(rr,cc,null);
    }
    public void validate(){
        if(version!=1||inputs==null||targets==null||inputs.size()+targets.size()>100)throw new IllegalArgumentException("Invalid preset");
        Set<Integer> used=new HashSet<>();
        for(var list:List.of(inputs,targets))for(var c:list){
            if(c==null||c.cropId()==null||!c.cropId().matches("[a-z0-9_]{1,64}")||c.position()==null||c.position().length!=2
                ||c.position()[0]<0||c.position()[0]>9||c.position()[1]<0||c.position()[1]>9)throw new IllegalArgumentException("Invalid cell");
            var crop=GreenhouseCatalog.crops().get(c.cropId());if(crop==null)throw new IllegalArgumentException("Unknown crop: "+c.cropId());
            if(c.position()[0]+crop.size()>10||c.position()[1]+crop.size()>10)throw new IllegalArgumentException("Crop extends outside plot");
            for(int r=0;r<crop.size();r++)for(int col=0;col<crop.size();col++)if(!used.add((c.position()[0]+r)*10+c.position()[1]+col))throw new IllegalArgumentException("Overlapping crops");
        }
        if(startingInventory==null)startingInventory=new JsonObject();
        for(var e:startingInventory.entrySet())if(!e.getValue().isJsonPrimitive()||!e.getValue().getAsJsonPrimitive().isNumber()||!Double.isFinite(e.getValue().getAsDouble())||e.getValue().getAsDouble()<0)throw new IllegalArgumentException("Invalid inventory count");
        if(groundTiles==null)groundTiles=new JsonArray();
        if(groundTiles.size()>100)throw new IllegalArgumentException("Too many ground tiles");
        Set<Integer> groundUsed=new HashSet<>();
        for(var tile:groundTiles){
            var o=tile.getAsJsonObject();var p=o.getAsJsonArray("position");
            if(p==null||p.size()!=2||!Set.of("farmland","sand","soul_sand","mycelium","netherrack","end_stone").contains(o.get("ground").getAsString()))throw new IllegalArgumentException("Invalid ground");
            int r=p.get(0).getAsInt(),c=p.get(1).getAsInt();if(r<0||r>9||c<0||c>9||!groundUsed.add(r*10+c)||(!explicitGround&&used.contains(r*10+c)))throw new IllegalArgumentException("Invalid or overlapping ground");
        }
    }
    public void materializeGround(){
        if(explicitGround)return;
        String[][] ground=new String[10][10];for(int r=0;r<10;r++)for(int c=0;c<10;c++)ground[r][c]=GreenhouseGround.at(this,r,c);
        explicitGround=true;groundTiles=new JsonArray();for(int r=0;r<10;r++)for(int c=0;c<10;c++)if(ground[r][c]!=null)paintGround(r,c,ground[r][c]);
    }
    public void paintGround(int row,int col,String ground){
        if(row<0||row>9||col<0||col>9||(ground!=null&&!GreenhouseGround.TYPES.contains(ground)))throw new IllegalArgumentException("Invalid ground");
        materializeGround();groundTiles.asList().removeIf(t->{var p=t.getAsJsonObject().getAsJsonArray("position");return p.get(0).getAsInt()==row&&p.get(1).getAsInt()==col;});
        if(ground!=null){var tile=new JsonObject();tile.addProperty("ground",ground);var pos=new JsonArray();pos.add(row);pos.add(col);tile.add("position",pos);groundTiles.add(tile);}scenario=null;
    }
    public void plant(int row,int col,String crop,boolean target,boolean autoGround){
        int size=crop==null?1:GreenhouseCatalog.crops().get(crop).size();if(row<0||col<0||row+size>10||col+size>10)throw new IllegalArgumentException("Outside Greenhouse");
        materializeGround();
        for(var list:List.of(inputs,targets))list.removeIf(c->{int n=GreenhouseCatalog.crops().get(c.cropId()).size();return row<c.position()[0]+n&&row+size>c.position()[0]&&col<c.position()[1]+n&&col+size>c.position()[1];});
        if(crop!=null){(target?targets:inputs).add(new Cell(crop,new int[]{row,col}));if(autoGround)for(int r=row;r<row+size;r++)for(int c=col;c<col+size;c++)paintGround(r,c,GreenhouseCatalog.crops().get(crop).ground());}scenario=null;
    }
    public void paint(int row,int col,String crop,boolean target){
        int size=crop==null?1:GreenhouseCatalog.crops().get(crop).size();
        if(row<0||col<0||row+size>10||col+size>10)throw new IllegalArgumentException("Outside Greenhouse");
        for(var list:List.of(inputs,targets))list.removeIf(c->{int n=GreenhouseCatalog.crops().get(c.cropId()).size();return row<c.position()[0]+n&&row+size>c.position()[0]&&col<c.position()[1]+n&&col+size>c.position()[1];});
        groundTiles.asList().removeIf(t->{var p=t.getAsJsonObject().getAsJsonArray("position");return p.get(0).getAsInt()>=row&&p.get(0).getAsInt()<row+size&&p.get(1).getAsInt()>=col&&p.get(1).getAsInt()<col+size;});
        if(crop!=null)(target?targets:inputs).add(new Cell(crop,new int[]{row,col}));
        scenario=null;
    }
    public Cell at(int row,int col){for(var list:List.of(inputs,targets))for(var c:list){int n=GreenhouseCatalog.crops().get(c.cropId()).size();if(row>=c.position()[0]&&row<c.position()[0]+n&&col>=c.position()[1]&&col<c.position()[1]+n)return c;}return null;}
    public void rotate(){
        for(var list:List.of(inputs,targets))for(int i=0;i<list.size();i++){var c=list.get(i);int n=GreenhouseCatalog.crops().get(c.cropId()).size();list.set(i,new Cell(c.cropId(),new int[]{c.position()[1],10-n-c.position()[0]}));}
        for(var tile:groundTiles){var o=tile.getAsJsonObject();var p=o.getAsJsonArray("position");var a=new JsonArray();a.add(p.get(1).getAsInt());a.add(9-p.get(0).getAsInt());o.add("position",a);}
        scenario=null;
    }
}
