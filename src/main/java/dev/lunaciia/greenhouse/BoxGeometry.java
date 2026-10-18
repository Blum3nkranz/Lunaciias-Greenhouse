package dev.lunaciia.greenhouse;

public final class BoxGeometry {
    @FunctionalInterface public interface Lines { void line(double x1,double y1,double z1,double x2,double y2,double z2); }
    public static void box(Lines lines,double x0,double y0,double z0,double x1,double y1,double z1) {
        for(int i=0;i<4;i++) {
            double x=(i&1)==0?x0:x1,z=(i&2)==0?z0:z1;
            lines.line(x,y0,z,x,y1,z);
        }
        for(double y:new double[]{y0,y1}) {
            lines.line(x0,y,z0,x1,y,z0);lines.line(x1,y,z0,x1,y,z1);
            lines.line(x1,y,z1,x0,y,z1);lines.line(x0,y,z1,x0,y,z0);
        }
    }
}
