package com.example.hackclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * HackClient - Forge 1.8.9 (mappings stable_22 / MCP 9.19)
 * Modulos: Sprint, Fly, Speed, Fullbright, NoFall, Step, Scaffold, Aimbot
 * Teclas: G=Sprint  F=Fly  V=Speed  B=Fullbright  N=NoFall  H=Step  J=Scaffold  K=Aimbot
 */
@Mod(modid = "hackclient", name = "HackClient", version = "1.0", clientSideOnly = true)
public class HackClient {

    static abstract class Module {
        final String name;
        final KeyBinding key;
        boolean enabled;

        Module(String name, int keyCode) {
            this.name = name;
            this.key = new KeyBinding("Toggle " + name, keyCode, "HackClient");
            ClientRegistry.registerKeyBinding(key);
        }

        void toggle() { enabled = !enabled; if (enabled) onEnable(); else onDisable(); }
        void onEnable() {}
        void onDisable() {}
        void onTick(Minecraft mc) {}
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<Module> modules = new ArrayList<Module>();
    private float oldGamma;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {

        modules.add(new Module("Sprint", Keyboard.KEY_G) {
            void onTick(Minecraft mc) {
                if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isCollidedHorizontally
                        && !mc.thePlayer.isSneaking()) {
                    mc.thePlayer.setSprinting(true);
                }
            }
        });

        modules.add(new Module("Fly", Keyboard.KEY_F) {
            void onTick(Minecraft mc) {
                mc.thePlayer.capabilities.allowFlying = true;
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.capabilities.setFlySpeed(0.05F);
            }
            void onDisable() {
                if (!mc.thePlayer.capabilities.isCreativeMode) {
                    mc.thePlayer.capabilities.allowFlying = false;
                    mc.thePlayer.capabilities.isFlying = false;
                }
                mc.thePlayer.capabilities.setFlySpeed(0.05F);
            }
        });

        modules.add(new Module("Speed", Keyboard.KEY_V) {
            void onTick(Minecraft mc) {
                if (mc.thePlayer.onGround && mc.thePlayer.moveForward > 0) {
                    mc.thePlayer.motionX *= 1.25;
                    mc.thePlayer.motionZ *= 1.25;
                }
            }
        });

        modules.add(new Module("Fullbright", Keyboard.KEY_B) {
            void onEnable() { oldGamma = mc.gameSettings.gammaSetting; }
            void onTick(Minecraft mc) { mc.gameSettings.gammaSetting = 100F; }
            void onDisable() { mc.gameSettings.gammaSetting = oldGamma; }
        });

        modules.add(new Module("NoFall", Keyboard.KEY_N) {
            void onTick(Minecraft mc) {
                if (mc.thePlayer.fallDistance > 2F) {
                    mc.thePlayer.fallDistance = 0F;
                    mc.thePlayer.onGround = true;
                }
            }
        });

        modules.add(new Module("Step", Keyboard.KEY_H) {
            void onTick(Minecraft mc) { mc.thePlayer.stepHeight = 1.0F; }
            void onDisable() { mc.thePlayer.stepHeight = 0.5F; }
        });

        modules.add(new Module("Scaffold", Keyboard.KEY_J) {
            int oldSlot = -1;
            void onEnable() { oldSlot = mc.thePlayer != null ? mc.thePlayer.inventory.currentItem : -1; }
            void onTick(Minecraft mc) { doScaffold(); }
            void onDisable() {
                if (oldSlot >= 0 && mc.thePlayer != null) mc.thePlayer.inventory.currentItem = oldSlot;
            }
        });

        modules.add(new Module("Aimbot", Keyboard.KEY_K) {
            void onTick(Minecraft mc) { doAimbot(); }
        });

        MinecraftForge.EVENT_BUS.register(this);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(this);
    }

    // ---------- Scaffold: coloca bloco embaixo dos pes automaticamente ----------
    private static final double AIM_RANGE = 6.0;      // alcance do aimbot (blocos)
    private static final float AIM_SPEED = 25F;       // graus maximos por tick (menor = mais "humano")

    private boolean isReplaceable(BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        return b.getMaterial().isReplaceable();
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s != null && s.getItem() instanceof ItemBlock && s.stackSize > 0) return i;
        }
        return -1;
    }

    private void doScaffold() {
        BlockPos below = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ);
        if (!isReplaceable(below)) return;

        int slot = findBlockSlot();
        if (slot == -1) return;
        mc.thePlayer.inventory.currentItem = slot;
        ItemStack held = mc.thePlayer.inventory.getStackInSlot(slot);

        for (EnumFacing f : EnumFacing.values()) {
            BlockPos neighbor = below.offset(f);
            if (isReplaceable(neighbor)) continue;

            EnumFacing side = f.getOpposite();
            Vec3 hit = new Vec3(
                    neighbor.getX() + 0.5 + side.getFrontOffsetX() * 0.5,
                    neighbor.getY() + 0.5 + side.getFrontOffsetY() * 0.5,
                    neighbor.getZ() + 0.5 + side.getFrontOffsetZ() * 0.5);

            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, held, neighbor, side, hit)) {
                mc.thePlayer.swingItem();
                return;
            }
        }
    }

    // ---------- Aimbot: mira no jogador mais proximo ----------
    private EntityPlayer getTarget() {
        EntityPlayer best = null;
        double bestDist = AIM_RANGE;
        for (Object o : mc.theWorld.playerEntities) {
            EntityPlayer p = (EntityPlayer) o;
            if (p == mc.thePlayer || p.isDead || p.getHealth() <= 0F) continue;
            double d = mc.thePlayer.getDistanceToEntity(p);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    private void doAimbot() {
        EntityPlayer t = getTarget();
        if (t == null) return;

        double dx = t.posX - mc.thePlayer.posX;
        double dz = t.posZ - mc.thePlayer.posZ;
        double dy = (t.posY + t.height * 0.85) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double flat = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90F;
        float targetPitch = (float) -(Math.atan2(dy, flat) * 180.0 / Math.PI);

        float dYaw = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
        float dPitch = targetPitch - mc.thePlayer.rotationPitch;

        dYaw = MathHelper.clamp_float(dYaw, -AIM_SPEED, AIM_SPEED);
        dPitch = MathHelper.clamp_float(dPitch, -AIM_SPEED, AIM_SPEED);

        mc.thePlayer.rotationYaw += dYaw;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + dPitch, -90F, 90F);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        for (Module m : modules) {
            if (m.key.isPressed()) m.toggle();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        for (Module m : modules) {
            if (m.enabled) m.onTick(mc);
        }
    }

    // HUD: lista de modulos ativos no canto superior direito
    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Text e) {
        FontRenderer fr = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);
        int y = 2;
        fr.drawStringWithShadow("HackClient", 2, 2, 0x55FFFF);
        for (Module m : modules) {
            if (!m.enabled) continue;
            fr.drawStringWithShadow(m.name, sr.getScaledWidth() - fr.getStringWidth(m.name) - 2, y, 0xFFFFFF);
            y += 10;
        }
    }
}

