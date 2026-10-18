package dev.lunaciia.greenhouse;

import java.util.Optional;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

final class ProjectionLines {
    record Style(int color,float width,boolean tracer){}
    static RenderType type(String name,boolean depth){
        var pipeline=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(GreenhouseClient.id("pipeline/"+name))
            .withDepthStencilState(Optional.of(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL,false))).build());
        return RenderType.create(name,RenderSetup.builder(pipeline).createRenderSetup());
    }
    static void line(VertexConsumer vertices,PoseStack.Pose pose,double x0,double y0,double z0,double x1,double y1,double z1,Style style){
        double dx=x1-x0,dy=y1-y0,dz=z1-z0,length=Math.sqrt(dx*dx+dy*dy+dz*dz);if(length<1e-6)return;
        float nx=(float)(dx/length),ny=(float)(dy/length),nz=(float)(dz/length);
        vertices.addVertex(pose,(float)x0,(float)y0,(float)z0).setColor(style.color()).setNormal(pose,nx,ny,nz).setLineWidth(style.width());
        vertices.addVertex(pose,(float)x1,(float)y1,(float)z1).setColor(style.color()).setNormal(pose,nx,ny,nz).setLineWidth(style.width());
    }
}
