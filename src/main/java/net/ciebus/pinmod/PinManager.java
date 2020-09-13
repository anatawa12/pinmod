package net.ciebus.pinmod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import scala.collection.parallel.ParIterableLike;
import scala.xml.dtd.MIXED;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class PinManager {
    private static ArrayList<PinData> pins;

    public enum pinState {
        ADD,
        REMOVE
    }

    PinManager() {
        pins = new ArrayList<PinData>();
    }

    public static void addPin(double x, double y, double z, String player, int dimId) {
        int i = 0;
        for (; i < pins.size(); i++) {
            if (pins.get(i).player.equals(player)) {
                pins.set(i, new PinData(x, y, z, player, dimId));
                return;
            }
        }
        PinData pin = new PinData(x, y, z, player, dimId);
        pins.add(pin);
    }

    public static void removePin(String player) {
        for (int i = 0; i < pins.size(); i++) {
            if (pins.get(i).player.equals(player)) {
                pins.remove(i);
                break;
            }
        }
    }

    public static boolean isDelete(double x, double y, double z, String player, int dimId) {
        int i = 0;
        for (; i < pins.size(); i++) {
            if (pins.get(i).player.equals(player)) {
                if ((int) x == (int) pins.get(i).x && (int) y == (int) pins.get(i).y && (int) z == (int) pins.get(i).z) {
                    return true;
                }
            }
        }
        return false;
    }


    private static IntBuffer viewport = GLAllocation.createDirectIntBuffer(16);
    private static FloatBuffer modelview = GLAllocation.createDirectFloatBuffer(16);
    private static FloatBuffer projection = GLAllocation.createDirectFloatBuffer(16);
    private static FloatBuffer objectCoords = GLAllocation.createDirectFloatBuffer(16);
    private static FloatBuffer dobjectCoords = GLAllocation.createDirectFloatBuffer(16);

    float ny = 0f;

    @SubscribeEvent
    public void renderTest(RenderGameOverlayEvent.Post event) {
        float partialTicks = event.partialTicks;
        for (PinData pin : pins) {
            float dy = (pin.dy - Minecraft.getMinecraft().displayHeight / 2f);
            Tessellator tess = Tessellator.instance;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);


            if (!pin.isVisible || pin.dx < 0 || pin.dy < 0 || pin.dx > Minecraft.getMinecraft().displayWidth || pin.dy > Minecraft.getMinecraft().displayHeight) {


                EntityPlayer p = Minecraft.getMinecraft().thePlayer;
                double ddx = p.prevPosX + (p.posX - p.prevPosX) * partialTicks;
                double ddy = p.prevPosY + (p.posY - p.prevPosY) * partialTicks - p.yOffset;
                double ddz = p.prevPosZ + (p.posZ - p.prevPosZ) * partialTicks;



                Vec3 vec = Vec3.createVectorHelper((float) (pin.x - ddx), (float) (pin.y - dy), (float) (pin.z - ddz)).normalize();
                //Vec3 pvec = Minecraft.getMinecraft().renderViewEntity.rayTrace(200, 1.0F).hitVec.normalize();
                Vec3 pvec = Minecraft.getMinecraft().thePlayer.getLookVec().normalize();

                System.out.println(vec.xCoord * pvec.xCoord + vec.zCoord * pvec.zCoord);

                GL11.glPushMatrix();
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glPointSize(20f);
                tess.startDrawing(GL11.GL_POINTS);

                if(vec.subtract(pvec).xCoord < 0) {
                    tess.addVertex(0, -(ny - Minecraft.getMinecraft().displayHeight / 2f) + Minecraft.getMinecraft().displayHeight / 2f, 0);
                } else {
                    tess.addVertex(Minecraft.getMinecraft().displayWidth, -(ny - Minecraft.getMinecraft().displayHeight / 2f) + Minecraft.getMinecraft().displayHeight / 2f, 0);
                }


                tess.draw();
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            } else {
                if (pin.dx * event.resolution.getScaledHeight() / (float) Minecraft.getMinecraft().displayHeight < 0 || (-dy + Minecraft.getMinecraft().displayHeight / 2f) * event.resolution.getScaledWidth() / (float) Minecraft.getMinecraft().displayWidth < 0)
                    return;
                GL11.glPushMatrix();
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glPointSize(20f);
                tess.startDrawing(GL11.GL_POINTS);
                tess.addVertex(pin.dx * event.resolution.getScaledHeight() / (float) Minecraft.getMinecraft().displayHeight, (-dy + Minecraft.getMinecraft().displayHeight / 2f) * event.resolution.getScaledWidth() / (float) Minecraft.getMinecraft().displayWidth, 0);
                tess.draw();
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    @SubscribeEvent
    public void renderPin(RenderWorldLastEvent event) {
        float partialTicks = event.partialTicks;
        for (PinData pin : pins) {
            if (Minecraft.getMinecraft().theWorld.provider.dimensionId != pin.dimId) return;

            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            EntityPlayer p = Minecraft.getMinecraft().thePlayer;

            double dx = p.prevPosX + (p.posX - p.prevPosX) * partialTicks;
            double dy = p.prevPosY + (p.posY - p.prevPosY) * partialTicks - p.yOffset;
            double dz = p.prevPosZ + (p.posZ - p.prevPosZ) * partialTicks;

            GLU.gluProject(
                    (float) (pin.x - dx),
                    (float) (pin.y - dy),
                    (float) (pin.z - dz),
                    modelview, projection, viewport, objectCoords);

            GLU.gluProject(
                    (float) (Minecraft.getMinecraft().renderViewEntity.rayTrace(200, 1.0F).hitVec.xCoord - dx),
                    (float) (pin.y - dy),
                    (float) (Minecraft.getMinecraft().renderViewEntity.rayTrace(200, 1.0F).hitVec.zCoord - dz),
                    modelview, projection, viewport, dobjectCoords);

            ny = dobjectCoords.get(1);

            Vec3 vec = Vec3.createVectorHelper((float) (pin.x - dx), (float) (pin.y - dy), (float) (pin.z - dz)).normalize();
            Vec3 pvec = Minecraft.getMinecraft().thePlayer.getLookVec();


            pin.update((int) objectCoords.get(0), (int) objectCoords.get(1));
            //System.out.println((int) objectCoords.get(0) + ":" + (int) objectCoords.get(1));
            pin.update(vec.xCoord * pvec.xCoord + vec.zCoord * pvec.zCoord > 0);


            Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation("pinmod", "textures/pin_icon_1.png"));
            Tessellator tess = Tessellator.instance;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();

            GL11.glColor3f(254f / 255, 182f / 255, 36f / 255);
            GL11.glTranslated(pin.x - RenderManager.renderPosX, pin.y - RenderManager.renderPosY + 1.6d, pin.z - RenderManager.renderPosZ);

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);

            float f3 = ActiveRenderInfo.rotationX;
            float f5 = ActiveRenderInfo.rotationZ;
            float f6 = ActiveRenderInfo.rotationYZ;
            float f7 = ActiveRenderInfo.rotationXY;
            float f4 = ActiveRenderInfo.rotationXZ;

            tess.startDrawing(GL11.GL_QUADS);
            double var12 = 0.3d;
            tess.addVertexWithUV((double) (0 - f3 * var12 - f6 * var12), (double) (0 - f4 * var12), (double) (0 - f5 * var12 - f7 * var12), (double) 1, (double) 1);
            tess.addVertexWithUV((double) (0 - f3 * var12 + f6 * var12), (double) (0 + f4 * var12), (double) (0 - f5 * var12 + f7 * var12), (double) 1, (double) 0);
            tess.addVertexWithUV((double) (0 + f3 * var12 + f6 * var12), (double) (0 + f4 * var12), (double) (0 + f5 * var12 + f7 * var12), (double) 0, (double) 0);
            tess.addVertexWithUV((double) (0 + f3 * var12 - f6 * var12), (double) (0 - f4 * var12), (double) (0 + f5 * var12 - f7 * var12), (double) 0, (double) 1);

            tess.draw();

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glRotated(Math.atan2(f3, f5) / Math.PI * 180d + 90d, 0, 1, 0);
            tess.startDrawing(GL11.GL_TRIANGLES);
            tess.addVertex(0, -1.6d, 0);
            tess.addVertex(-0.05d, -0.35d, 0);
            tess.addVertex(0.05d, -0.35d, 0);
            tess.draw();

            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            String str = pin.player;
            float s = 0.016666668F * 0.6666667F * 2;
            GL11.glTranslated(0, 0.5, 0);

            GL11.glScalef(s, -s, s);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glColor4f(0, 0, 0, 0.5f);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            tess.startDrawing(GL11.GL_QUADS);
            tess.addVertex((float) fr.getStringWidth(str) / 2 + 2, -6, 0.01f);
            tess.addVertex(-(float) fr.getStringWidth(str) / 2 - 2, -6, 0.01f);
            tess.addVertex(-(float) fr.getStringWidth(str) / 2 - 2, 3, 0.01f);
            tess.addVertex((float) fr.getStringWidth(str) / 2 + 2, 3, 0.01f);
            tess.draw();


            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            fr.drawString(str, -fr.getStringWidth(str) / 2, 0 * 10 - 1 * 5, 0xFFFFFF);


            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

}

class PinData {
    public double x;
    public double y;
    public double z;
    public String player;
    public int dimId;

    public boolean isVisible;
    public int dx;
    public int dy;

    public PinData(double x, double y, double z, String player, int dimId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.player = player;
        this.dimId = dimId;
        dx = 0;
        dy = 0;
        isVisible = false;
    }

    public void update(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public void update(boolean isVisible) {
        this.isVisible = isVisible;
    }
}