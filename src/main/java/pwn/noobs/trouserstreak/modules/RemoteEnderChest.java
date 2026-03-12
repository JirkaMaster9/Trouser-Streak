package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;
import pwn.noobs.trouserstreak.Trouser;

public class RemoteEnderChest extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("RemoteEnderChest");
    private final Setting<Boolean> packetcancel = sgGeneral.add(new BoolSetting.Builder()
            .name("cancel CloseScreenS2CPacket")
            .description("Cancels CloseScreenS2CPacket when using ender chest. May make the GUI stay open more reliably.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Keybind> toggleGui = sgGeneral.add(new KeybindSetting(
            "GUI Key (Press it)",
            "Key to toggle Ender Chest GUI visibility/interactiveness.",
            Keybind.fromKey(GLFW.GLFW_KEY_B),
            value -> {},
            value -> {},
            null,
            () -> {}
    ));
    public RemoteEnderChest() {
        super(Trouser.Main, "RemoteEnderChest", "Access your enderchest anywhere and move freely while it is open.");
    }
    private boolean keepGuiOpen = false;
    private boolean guiHidden = false;
    private boolean lastKeyState = false;
    private GenericContainerScreen savedScreen = null;



    @Override
    public void onDeactivate() {
        keepGuiOpen = false;
        guiHidden = false;
        lastKeyState = false;
        savedScreen = null;
    }
    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (packetcancel.get() && keepGuiOpen && event.packet instanceof CloseScreenS2CPacket) event.cancel();
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        // open via looking at chest
        if (mc.crosshairTarget instanceof BlockHitResult bhr) {
            if (mc.world.getBlockState(bhr.getBlockPos()).getBlock() == Blocks.ENDER_CHEST
                    && mc.options.useKey.isPressed() && !isEnderChestScreen() && !keepGuiOpen && !guiHidden) {
                keepGuiOpen = true;
                guiHidden = false;
                mc.doItemUse();
                mc.doItemUse();
            }
        }

        // hotkey toggle using edge detection
        boolean keyDown = toggleGui.get().isPressed();
        boolean keyJustPressed = keyDown && !lastKeyState;
        lastKeyState = keyDown;

        if (keyJustPressed) {
            if (isEnderChestScreen()) {
                savedScreen = (GenericContainerScreen) mc.currentScreen;
                keepGuiOpen = true;
                mc.setScreen(null);
                guiHidden = true;
            } else if (guiHidden && savedScreen != null) {
                mc.setScreen(savedScreen);
                guiHidden = false;
            }
        }

        // if GUI was closed by something other than us (escape, etc), reset
        if (!isEnderChestScreen() && !guiHidden) {
            keepGuiOpen = false;
            savedScreen = null;
        }
    }
    private boolean isEnderChestScreen() {
        return mc.currentScreen instanceof GenericContainerScreen screen &&
                screen.getScreenHandler().getType() == ScreenHandlerType.GENERIC_9X3 &&
                screen.getTitle().getString().toLowerCase().contains("ender");
    }
}