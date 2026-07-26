package com.kitmod.client.gui;

import com.kitmod.data.Kit;
import com.kitmod.data.KitStorage;
import com.kitmod.util.InventoryHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.input.KeyEvent;
import net.minecraft.client.gui.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class KitManagerScreen extends Screen {

    private enum State { LIST, NEW_KIT, CONFIRM_LOAD, RENAME }
    private State state = State.LIST;

    private List<Kit> kits = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 28;
    private static final int LIST_WIDTH = 220;

    private EditBox nameField;
    private ItemStack chosenIcon = ItemStack.EMPTY;
    private boolean pickingIcon = false;
    private List<ItemStack> inventoryItems = new ArrayList<>();

    private String feedbackMessage = "";
    private long feedbackExpiry = 0L;

    private Button btnNewKit;
    private Button btnLoad;
    private Button btnDelete;
    private Button btnRename;
    private Button btnSave;
    private Button btnCancel;
    private Button btnConfirmYes;
    private Button btnConfirmNo;

    private static final int COL_BG       = 0xFF1A1A2E;
    private static final int COL_PANEL    = 0xFF16213E;
    private static final int COL_SELECTED = 0xFF0F3460;
    private static final int COL_ACCENT   = 0xFF533483;
    private static final int COL_ACCENT2  = 0xFFE94560;
    private static final int COL_TEXT     = 0xFFE0E0E0;
    private static final int COL_SUBTEXT  = 0xFF888888;
    private static final int COL_HOVER    = 0xFF2A2A4E;

    public KitManagerScreen() {
        super(Component.translatable("kitmod.gui.title"));
    }

    @Override
    protected void init() {
        kits = KitStorage.loadAll();
        buildInventoryItemList();
        buildWidgets();
        updateWidgetVisibility();
    }

    private void buildInventoryItemList() {
        inventoryItems.clear();
        if (minecraft == null || minecraft.player == null) return;
        var player = minecraft.player;
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) inventoryItems.add(s.copy());
        }
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.FEET,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.HEAD}) {
            ItemStack s = player.getItemBySlot(slot);
            if (!s.isEmpty()) inventoryItems.add(s.copy());
        }
        ItemStack off = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        if (!off.isEmpty()) inventoryItems.add(off.copy());
    }

    private void buildWidgets() {
        int btnW = 160;
        int btnX = (width - 200) + 20;

        btnNewKit = Button.builder(Component.translatable("kitmod.gui.new_kit"), b -> startNewKit())
                .bounds(10, height - 36, LIST_WIDTH, 24).build();
        addRenderableWidget(btnNewKit);

        btnLoad = Button.builder(Component.translatable("kitmod.gui.load"), b -> startConfirmLoad())
                .bounds(btnX, height - 100, btnW, 24).build();
        addRenderableWidget(btnLoad);

        btnRename = Button.builder(Component.translatable("kitmod.gui.rename"), b -> startRename())
                .bounds(btnX, height - 70, btnW, 24).build();
        addRenderableWidget(btnRename);

        btnDelete = Button.builder(Component.literal("§cDelete"), b -> deleteSelected())
                .bounds(btnX, height - 40, btnW, 24).build();
        addRenderableWidget(btnDelete);

        btnSave = Button.builder(Component.translatable("kitmod.gui.save"), b -> commitSave())
                .bounds(btnX, height - 70, btnW, 24).build();
        addRenderableWidget(btnSave);

        btnCancel = Button.builder(Component.translatable("kitmod.gui.cancel"), b -> cancelFlow())
                .bounds(btnX, height - 40, btnW, 24).build();
        addRenderableWidget(btnCancel);

        btnConfirmYes = Button.builder(Component.translatable("kitmod.gui.confirm_yes"), b -> doLoad())
                .bounds(btnX, height - 70, btnW, 24).build();
        addRenderableWidget(btnConfirmYes);

        btnConfirmNo = Button.builder(Component.translatable("kitmod.gui.confirm_no"), b -> cancelFlow())
                .bounds(btnX, height - 40, btnW, 24).build();
        addRenderableWidget(btnConfirmNo);

        nameField = new EditBox(font, btnX, height - 130, btnW, 20,
                Component.translatable("kitmod.gui.name_prompt"));
        nameField.setMaxLength(32);
        nameField.setHint(Component.literal("Kit name…"));
        addRenderableWidget(nameField);
    }

    private void updateWidgetVisibility() {
        boolean inList    = state == State.LIST;
        boolean hasSelect = selectedIndex >= 0 && selectedIndex < kits.size();
        boolean inNew     = state == State.NEW_KIT;
        boolean inRename  = state == State.RENAME;
        boolean inConfirm = state == State.CONFIRM_LOAD;

        btnNewKit.visible   = inList;
        btnLoad.visible     = inList && hasSelect;
        btnRename.visible   = inList && hasSelect;
        btnDelete.visible   = inList && hasSelect;

        nameField.visible   = inNew || inRename;
        nameField.setEditable(inNew || inRename);
        btnSave.visible     = inNew || inRename;
        btnCancel.visible   = inNew || inRename;

        btnConfirmYes.visible = inConfirm;
        btnConfirmNo.visible  = inConfirm;
    }

    // -------------------------------------------------------------------------
    // State transitions
    // -------------------------------------------------------------------------

    private void startNewKit() {
        state = State.NEW_KIT;
        nameField.setValue("");
        chosenIcon = ItemStack.EMPTY;
        pickingIcon = false;
        setFocused(nameField);
        updateWidgetVisibility();
    }

    private void startRename() {
        if (selectedIndex < 0 || selectedIndex >= kits.size()) return;
        state = State.RENAME;
        nameField.setValue(kits.get(selectedIndex).name);
        setFocused(nameField);
        updateWidgetVisibility();
    }

    private void startConfirmLoad() {
        if (selectedIndex < 0 || selectedIndex >= kits.size()) return;
        state = State.CONFIRM_LOAD;
        updateWidgetVisibility();
    }

    private void cancelFlow() {
        state = State.LIST;
        pickingIcon = false;
        updateWidgetVisibility();
    }

    private void commitSave() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) { showFeedback("§cPlease enter a kit name."); return; }
        if (state == State.NEW_KIT) {
            String iconId = chosenIcon.isEmpty()
                    ? "minecraft:chest"
                    : BuiltInRegistries.ITEM.getKey(chosenIcon.getItem()).toString();
            Kit kit = new Kit(name, iconId);
            if (minecraft != null && minecraft.player != null) {
                InventoryHelper.snapshotTo(minecraft.player, kit);
            }
            KitStorage.save(kit);
            kits = KitStorage.loadAll();
            showFeedback("§aKit saved!");
        } else if (state == State.RENAME) {
            Kit kit = kits.get(selectedIndex);
            String oldName = kit.name;
            kit.name = name;
            KitStorage.rename(kit, oldName);
            kits = KitStorage.loadAll();
            showFeedback("§aKit renamed.");
        }
        state = State.LIST;
        pickingIcon = false;
        updateWidgetVisibility();
    }

    private void deleteSelected() {
        if (selectedIndex < 0 || selectedIndex >= kits.size()) return;
        KitStorage.delete(kits.get(selectedIndex));
        kits = KitStorage.loadAll();
        selectedIndex = -1;
        showFeedback("§7Kit deleted.");
        updateWidgetVisibility();
    }

    private void doLoad() {
        if (selectedIndex < 0 || selectedIndex >= kits.size()) return;
        if (minecraft == null || minecraft.player == null) return;
        if (!minecraft.player.isCreative()) {
            showFeedback("§cCreative mode required to load kits.");
            state = State.LIST;
            updateWidgetVisibility();
            return;
        }
        InventoryHelper.restoreFrom(minecraft.player, kits.get(selectedIndex));
        showFeedback("§aKit loaded!");
        state = State.LIST;
        updateWidgetVisibility();
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COL_BG);
        ctx.fill(0, 0, LIST_WIDTH + 10, height, COL_PANEL);
        ctx.fill(LIST_WIDTH + 10, 0, LIST_WIDTH + 12, height, COL_ACCENT);
        ctx.drawString(font, "§bKit Manager", LIST_WIDTH + 20, 14, COL_TEXT);

        renderKitList(ctx, mouseX, mouseY);

        switch (state) {
            case LIST         -> renderDetailPanel(ctx);
            case NEW_KIT      -> renderNewKitPanel(ctx, mouseX, mouseY);
            case RENAME       -> renderRenamePanel(ctx);
            case CONFIRM_LOAD -> renderConfirmPanel(ctx);
        }

        if (System.currentTimeMillis() < feedbackExpiry) {
            int tw = font.width(feedbackMessage) + 12;
            int tx = (width - tw) / 2;
            int ty = height - 60;
            ctx.fill(tx, ty - 2, tx + tw, ty + 12, 0xCC000000);
            ctx.drawString(font, feedbackMessage, tx + 6, ty, 0xFFFFFFFF);
        }

        super.render(ctx, mouseX, mouseY, delta);

        if (state == State.NEW_KIT && pickingIcon) {
            renderIconPicker(ctx, mouseX, mouseY);
        }
    }

    private void renderKitList(GuiGraphics ctx, int mouseX, int mouseY) {
        int listY = 10;
        int maxVisible = (height - 50 - listY) / ROW_HEIGHT;

        if (kits.isEmpty()) {
            ctx.drawString(font, "§7No kits saved yet.", 10, listY + 8, COL_SUBTEXT);
            ctx.drawString(font, "§7Press '+ New Kit'", 10, listY + 20, COL_SUBTEXT);
            return;
        }

        for (int i = scrollOffset; i < Math.min(kits.size(), scrollOffset + maxVisible); i++) {
            Kit kit = kits.get(i);
            int rowY = listY + (i - scrollOffset) * ROW_HEIGHT;
            boolean hovered  = mouseX >= 0 && mouseX <= LIST_WIDTH + 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean selected = i == selectedIndex;

            int bg = selected ? COL_SELECTED : (hovered ? COL_HOVER : 0x00000000);
            if (bg != 0) ctx.fill(0, rowY, LIST_WIDTH + 10, rowY + ROW_HEIGHT, bg);

            ctx.renderItem(resolveIconStack(kit), 6, rowY + 6);
            ctx.drawString(font, kit.name, 26, rowY + 5, COL_TEXT);
            ctx.drawString(font, "§8" + kit.shortDate(), 26, rowY + 15, COL_SUBTEXT);

            if (selected) ctx.fill(0, rowY, 3, rowY + ROW_HEIGHT, COL_ACCENT2);
        }

        if (kits.size() > maxVisible) {
            ctx.drawString(font, "§8↑↓ scroll", 10, height - 50, COL_SUBTEXT);
        }
    }

    private void renderDetailPanel(GuiGraphics ctx) {
        if (selectedIndex < 0 || selectedIndex >= kits.size()) {
            ctx.drawString(font, "§7← Select a kit", LIST_WIDTH + 20, height / 2, COL_SUBTEXT);
            return;
        }
        Kit kit = kits.get(selectedIndex);
        int x = LIST_WIDTH + 20, y = 40;
        ctx.renderItem(resolveIconStack(kit), x, y);
        ctx.drawString(font, "§f" + kit.name, x + 24, y + 2, COL_TEXT);
        ctx.drawString(font, "§8Saved " + kit.shortDate(), x + 24, y + 13, COL_SUBTEXT);
        int slotCount = kit.slots == null ? 0 : kit.slots.size();
        ctx.drawString(font, "§7" + slotCount + " item stacks", x, y + 30, COL_SUBTEXT);
    }

    private void renderNewKitPanel(GuiGraphics ctx, int mouseX, int mouseY) {
        int x = LIST_WIDTH + 20;
        ctx.drawString(font, "§bNew Kit", x, 40, COL_TEXT);
        ctx.drawString(font, "Kit Name:", x, height - 148, COL_TEXT);

        int iconBtnY = height - 105, iconX = x, iconY = iconBtnY + 12;
        ctx.drawString(font, "Icon:", x, iconBtnY, COL_TEXT);
        ctx.fill(iconX - 2, iconY - 2, iconX + 18, iconY + 18, COL_ACCENT);
        ctx.renderItem(chosenIcon.isEmpty() ? new ItemStack(Items.CHEST) : chosenIcon, iconX, iconY);

        boolean hoveringIcon = mouseX >= iconX - 2 && mouseX <= iconX + 18
                && mouseY >= iconY - 2 && mouseY <= iconY + 18;
        ctx.drawString(font, hoveringIcon ? "§eClick to pick" : "§7click icon →",
                iconX + 22, iconY + 4, COL_SUBTEXT);
    }

    private void renderRenamePanel(GuiGraphics ctx) {
        int x = LIST_WIDTH + 20;
        ctx.drawString(font, "§bRename Kit", x, 40, COL_TEXT);
        ctx.drawString(font, "New Name:", x, height - 148, COL_TEXT);
    }

    private void renderConfirmPanel(GuiGraphics ctx) {
        int x = LIST_WIDTH + 20;
        ctx.drawString(font, "§eLoad Kit?", x, 40, COL_TEXT);
        if (selectedIndex >= 0 && selectedIndex < kits.size()) {
            ctx.drawString(font, "§f" + kits.get(selectedIndex).name, x, 55, COL_TEXT);
        }
        ctx.drawString(font, "§cThis will clear your entire", x, 80, 0xFFFF6666);
        ctx.drawString(font, "§cinventory. Are you sure?",   x, 91, 0xFFFF6666);
        if (minecraft != null && minecraft.player != null && !minecraft.player.isCreative()) {
            ctx.drawString(font, "§4Requires Creative mode!", x, 107, 0xFFFF4444);
        }
    }

    private void renderIconPicker(GuiGraphics ctx, int mouseX, int mouseY) {
        ctx.fill(0, 0, width, height, 0xAA000000);
        int cols = 9, size = 18, pad = 4, count = inventoryItems.size();
        int rows = (count + cols - 1) / cols;
        int pickerW = cols * (size + pad) + pad;
        int pickerH = rows * (size + pad) + pad + 24;
        int px = (width - pickerW) / 2, py = (height - pickerH) / 2;

        ctx.fill(px - 4, py - 4, px + pickerW + 4, py + pickerH + 4, COL_ACCENT);
        ctx.fill(px, py, px + pickerW, py + pickerH, COL_PANEL);
        ctx.drawString(font, "§bPick an Icon", px + 4, py + 4, COL_TEXT);

        for (int i = 0; i < count; i++) {
            int ix = px + pad + (i % cols) * (size + pad);
            int iy = py + 20 + pad + (i / cols) * (size + pad);
            boolean hovered = mouseX >= ix && mouseX < ix + size && mouseY >= iy && mouseY < iy + size;
            if (hovered) ctx.fill(ix - 1, iy - 1, ix + size + 1, iy + size + 1, COL_ACCENT2);
            ctx.renderItem(inventoryItems.get(i), ix, iy);
        }
    }

    // -------------------------------------------------------------------------
    // Input — 1.21.9+ uses MouseButtonEvent and KeyEvent objects
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();

        if (state == State.NEW_KIT && pickingIcon) {
            handleIconPickerClick((int) mouseX, (int) mouseY);
            return true;
        }
        if (mouseX >= 0 && mouseX <= LIST_WIDTH + 10) {
            int listY = 10, maxVis = (height - 50 - listY) / ROW_HEIGHT;
            int clickedI = ((int) mouseY - listY) / ROW_HEIGHT + scrollOffset;
            if (clickedI >= 0 && clickedI < kits.size() && clickedI < scrollOffset + maxVis) {
                selectedIndex = clickedI;
                if (state == State.LIST) updateWidgetVisibility();
                return true;
            }
        }
        if (state == State.NEW_KIT) {
            int iconBtnY = height - 105, iconX = LIST_WIDTH + 20, iconY = iconBtnY + 12;
            if (mouseX >= iconX - 2 && mouseX <= iconX + 18 && mouseY >= iconY - 2 && mouseY <= iconY + 18) {
                pickingIcon = true;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void handleIconPickerClick(int mouseX, int mouseY) {
        int cols = 9, size = 18, pad = 4, count = inventoryItems.size();
        int rows = (count + cols - 1) / cols;
        int pickerW = cols * (size + pad) + pad;
        int pickerH = rows * (size + pad) + pad + 24;
        int px = (width - pickerW) / 2, py = (height - pickerH) / 2;
        for (int i = 0; i < count; i++) {
            int ix = px + pad + (i % cols) * (size + pad);
            int iy = py + 20 + pad + (i / cols) * (size + pad);
            if (mouseX >= ix && mouseX < ix + size && mouseY >= iy && mouseY < iy + size) {
                chosenIcon = inventoryItems.get(i).copy();
                pickingIcon = false;
                return;
            }
        }
        pickingIcon = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (mouseX <= LIST_WIDTH + 10) {
            int maxVis = (height - 50 - 10) / ROW_HEIGHT;
            scrollOffset -= (int) Math.signum(dy);
            scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, kits.size() - maxVis)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.keyCode();
        if (keyCode == 256) { // ESC
            if (pickingIcon) { pickingIcon = false; return true; }
            if (state != State.LIST) { cancelFlow(); return true; }
        }
        if (keyCode == 257 && (state == State.NEW_KIT || state == State.RENAME)) {
            commitSave();
            return true;
        }
        return super.keyPressed(event);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ItemStack resolveIconStack(Kit kit) {
        if (kit.iconItemId == null || kit.iconItemId.isEmpty()) return new ItemStack(Items.CHEST);
        try {
            var optHolder = BuiltInRegistries.ITEM.get(Identifier.parse(kit.iconItemId));
            if (optHolder.isPresent()) return new ItemStack(optHolder.get().value());
        } catch (Exception ignored) {}
        return new ItemStack(Items.CHEST);
    }

    private void showFeedback(String message) {
        feedbackMessage = message;
        feedbackExpiry  = System.currentTimeMillis() + 2500;
    }
    public boolean isPauseScreen() { return false; }
}
