package com.kitmod.client.gui;

import com.kitmod.data.Kit;
import com.kitmod.data.KitStorage;
import com.kitmod.util.InventoryHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KitManagerScreen extends Screen {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------
    private enum State { LIST, NEW_KIT, CONFIRM_LOAD, RENAME }
    private State state = State.LIST;

    private List<Kit> kits = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset  = 0;

    // New-kit / rename
    private EditBox nameField;
    private ItemStack chosenIcon = ItemStack.EMPTY;
    private boolean pickingIcon  = false;

    // All player items keyed by slot (0-40) for icon picker
    private final List<ItemStack> allPlayerItems = new ArrayList<>();

    // Feedback toast
    private String feedbackMessage = "";
    private long   feedbackExpiry  = 0L;

    // Buttons
    private Button btnNewKit, btnLoad, btnDelete, btnRename;
    private Button btnSave, btnCancel, btnConfirmYes, btnConfirmNo;

    // -----------------------------------------------------------------------
    // Layout constants  (all relative to screen width/height, set in init)
    // -----------------------------------------------------------------------
    private static final int LIST_W   = 160; // left kit-list panel width
    private static final int ROW_H    = 32;
    private static final int SLOT_SZ  = 18;  // item slot cell size
    private static final int PADDING  = 8;

    // Palette
    private static final int C_BG       = 0xFF0E0E1A;
    private static final int C_PANEL    = 0xFF161625;
    private static final int C_BORDER   = 0xFF2A2A45;
    private static final int C_SEL      = 0xFF1E1E3A;
    private static final int C_ACCENT   = 0xFF5B4FCF;
    private static final int C_ACCENT2  = 0xFFE94560;
    private static final int C_WHITE    = 0xFFEEEEEE;
    private static final int C_GREY     = 0xFF888899;
    private static final int C_HOVER    = 0xFF222235;
    private static final int C_SLOT_BG  = 0xFF1A1A2E;
    private static final int C_SLOT_BD  = 0xFF333355;

    public KitManagerScreen() {
        super(Component.literal("Kit Manager"));
    }

    // -----------------------------------------------------------------------
    // Init
    // -----------------------------------------------------------------------
    @Override
    protected void init() {
        kits = KitStorage.loadAll();
        buildPlayerItems();
        buildWidgets();
        updateVisibility();
    }

    /** Snapshot player's current inventory into allPlayerItems (slot-indexed list for picker) */
    private void buildPlayerItems() {
        allPlayerItems.clear();
        if (minecraft == null || minecraft.player == null) return;
        var player = minecraft.player;
        for (int i = 0; i < 36; i++) allPlayerItems.add(player.getInventory().getItem(i).copy());
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD})
            allPlayerItems.add(player.getItemBySlot(s).copy());
        allPlayerItems.add(player.getItemBySlot(EquipmentSlot.OFFHAND).copy());
    }

    private void buildWidgets() {
        int rw = rightW();
        int rx = LIST_W + PADDING * 2;

        // --- List panel ---
        btnNewKit = Button.builder(Component.literal("+ New Kit"), b -> startNewKit())
                .bounds(PADDING, height - PADDING - 24, LIST_W, 24).build();
        addRenderableWidget(btnNewKit);

        // --- Detail panel buttons ---
        int bw = rw - PADDING * 2;
        int bx = rx + PADDING;

        btnLoad = Button.builder(Component.literal("Load Kit"), b -> startConfirmLoad())
                .bounds(bx, height - PADDING - 58, bw / 2 - 2, 24).build();
        addRenderableWidget(btnLoad);

        btnDelete = Button.builder(Component.literal("✕ Delete"), b -> deleteSelected())
                .bounds(bx, height - PADDING - 24, bw, 24).build();
        addRenderableWidget(btnDelete);

        btnRename = Button.builder(Component.literal("Rename"), b -> startRename())
                .bounds(bx + bw / 2 + 2, height - PADDING - 58, bw / 2 - 2, 24).build();
        addRenderableWidget(btnRename);

        // --- New kit / rename ---
        nameField = new EditBox(font, bx, height - PADDING - 52, bw, 20,
                Component.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setHint(Component.literal("Kit name…"));
        addRenderableWidget(nameField);

        btnSave = Button.builder(Component.literal("Save Kit"), b -> commitSave())
                .bounds(bx, height - PADDING - 24, bw / 2 - 2, 24).build();
        addRenderableWidget(btnSave);

        btnCancel = Button.builder(Component.literal("Cancel"), b -> cancelFlow())
                .bounds(bx + bw / 2 + 2, height - PADDING - 24, bw / 2 - 2, 24).build();
        addRenderableWidget(btnCancel);

        // --- Confirm load ---
        btnConfirmYes = Button.builder(Component.literal("Yes, Load"), b -> doLoad())
                .bounds(bx, height - PADDING - 24, bw / 2 - 2, 24).build();
        addRenderableWidget(btnConfirmYes);

        btnConfirmNo = Button.builder(Component.literal("Cancel"), b -> cancelFlow())
                .bounds(bx + bw / 2 + 2, height - PADDING - 24, bw / 2 - 2, 24).build();
        addRenderableWidget(btnConfirmNo);
    }

    private void updateVisibility() {
        boolean list    = state == State.LIST;
        boolean hasSel  = selectedIndex >= 0 && selectedIndex < kits.size();
        boolean newKit  = state == State.NEW_KIT;
        boolean rename  = state == State.RENAME;
        boolean confirm = state == State.CONFIRM_LOAD;

        btnNewKit.visible     = list;
        btnLoad.visible       = list && hasSel;
        btnDelete.visible     = list && hasSel;
        btnRename.visible     = list && hasSel;

        nameField.visible     = newKit || rename;
        nameField.setEditable(newKit || rename);
        btnSave.visible       = newKit || rename;
        btnCancel.visible     = newKit || rename;

        btnConfirmYes.visible = confirm;
        btnConfirmNo.visible  = confirm;
    }

    private int rightW() { return width - LIST_W - PADDING * 3; }

    // -----------------------------------------------------------------------
    // State transitions
    // -----------------------------------------------------------------------
    private void startNewKit() {
        state = State.NEW_KIT; nameField.setValue(""); chosenIcon = ItemStack.EMPTY;
        pickingIcon = false; setFocused(nameField); updateVisibility();
    }
    private void startRename() {
        if (!hasSel()) return;
        state = State.RENAME; nameField.setValue(kits.get(selectedIndex).name);
        setFocused(nameField); updateVisibility();
    }
    private void startConfirmLoad() {
        if (!hasSel()) return; state = State.CONFIRM_LOAD; updateVisibility();
    }
    private void cancelFlow() {
        state = State.LIST; pickingIcon = false; updateVisibility();
    }
    private boolean hasSel() { return selectedIndex >= 0 && selectedIndex < kits.size(); }

    private void commitSave() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) { showFeedback("§cEnter a kit name first."); return; }
        if (state == State.NEW_KIT) {
            String iconId = chosenIcon.isEmpty() ? "minecraft:chest"
                    : BuiltInRegistries.ITEM.getKey(chosenIcon.getItem()).toString();
            Kit kit = new Kit(name, iconId);
            if (minecraft != null && minecraft.player != null)
                InventoryHelper.snapshotTo(minecraft.player, kit);
            KitStorage.save(kit);
            kits = KitStorage.loadAll();
            showFeedback("§aKit saved!");
        } else if (state == State.RENAME) {
            Kit kit = kits.get(selectedIndex);
            String old = kit.name; kit.name = name;
            KitStorage.rename(kit, old);
            kits = KitStorage.loadAll();
            showFeedback("§aRenamed.");
        }
        state = State.LIST; pickingIcon = false; updateVisibility();
    }

    private void deleteSelected() {
        if (!hasSel()) return;
        KitStorage.delete(kits.get(selectedIndex));
        kits = KitStorage.loadAll();
        selectedIndex = -1;
        showFeedback("§7Kit deleted.");
        updateVisibility();
    }

    private void doLoad() {
        if (!hasSel() || minecraft == null || minecraft.player == null) return;
        if (!minecraft.player.isCreative()) {
            showFeedback("§cCreative mode required!"); state = State.LIST; updateVisibility(); return;
        }
        InventoryHelper.restoreFrom(minecraft.player, kits.get(selectedIndex));
        showFeedback("§aKit loaded!"); state = State.LIST; updateVisibility();
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------
    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // Full background
        g.fill(0, 0, width, height, C_BG);

        // Left panel background
        g.fill(0, 0, LIST_W + PADDING * 2, height, C_PANEL);
        // Right panel background
        g.fill(LIST_W + PADDING * 2, 0, width, height, C_BG);
        // Divider
        g.fill(LIST_W + PADDING * 2 - 1, 0, LIST_W + PADDING * 2, height, C_BORDER);

        // Panel titles
        g.drawString(font, "§bKits", PADDING, PADDING, C_WHITE);
        drawRightTitle(g);

        renderKitList(g, mx, my);

        switch (state) {
            case LIST         -> renderDetailOrEmpty(g, mx, my);
            case NEW_KIT      -> renderNewKit(g, mx, my);
            case RENAME       -> renderRename(g);
            case CONFIRM_LOAD -> renderConfirm(g);
        }

        // Icon picker overlay (drawn on top of everything before widgets)
        if (pickingIcon) renderIconPicker(g, mx, my);

        super.render(g, mx, my, delta);

        // Toast
        if (System.currentTimeMillis() < feedbackExpiry) {
            int tw = font.width(feedbackMessage) + 12;
            int tx = (width - tw) / 2, ty = height - 56;
            g.fill(tx - 2, ty - 2, tx + tw + 2, ty + 14, 0xDD000000);
            g.drawString(font, feedbackMessage, tx + 6, ty, C_WHITE);
        }
    }

    private void drawRightTitle(GuiGraphics g) {
        int rx = LIST_W + PADDING * 3;
        String title = switch (state) {
            case LIST         -> hasSel() ? kits.get(selectedIndex).name : "§7Select a kit";
            case NEW_KIT      -> "New Kit";
            case RENAME       -> "Rename Kit";
            case CONFIRM_LOAD -> "Load Kit?";
        };
        g.drawString(font, "§b" + title, rx, PADDING, C_WHITE);
    }

    // ---- Kit list (left panel) ----
    private void renderKitList(GuiGraphics g, int mx, int my) {
        int x = PADDING, y = PADDING + 14;
        int maxVis = (height - y - 40) / ROW_H;

        if (kits.isEmpty()) {
            g.drawString(font, "§7No kits yet.", x, y + 4, C_GREY);
            g.drawString(font, "§7Press §f+ New Kit§7.", x, y + 16, C_GREY);
            return;
        }

        for (int i = scrollOffset; i < Math.min(kits.size(), scrollOffset + maxVis); i++) {
            Kit kit = kits.get(i);
            int ry = y + (i - scrollOffset) * ROW_H;
            boolean hov = mx >= x && mx <= LIST_W + PADDING && my >= ry && my < ry + ROW_H;
            boolean sel = i == selectedIndex;

            // Row bg
            if (sel)       g.fill(0, ry, LIST_W + PADDING * 2, ry + ROW_H, C_SEL);
            else if (hov)  g.fill(0, ry, LIST_W + PADDING * 2, ry + ROW_H, C_HOVER);

            // Selection bar
            if (sel) g.fill(0, ry + 2, 3, ry + ROW_H - 2, C_ACCENT);

            // Icon
            g.renderItem(resolveIconStack(kit), x + 2, ry + 7);

            // Name + date
            g.drawString(font, kit.name, x + 22, ry + 7, sel ? C_WHITE : 0xFFCCCCDD);
            g.drawString(font, "§8" + kit.shortDate(), x + 22, ry + 17, C_GREY);

            // Bottom separator
            g.fill(PADDING, ry + ROW_H - 1, LIST_W + PADDING, ry + ROW_H, C_BORDER);
        }

        if (kits.size() > maxVis)
            g.drawString(font, "§8scroll ↕", x, height - 42, C_GREY);
    }

    // ---- Right panel: inventory preview ----
    private void renderDetailOrEmpty(GuiGraphics g, int mx, int my) {
        int rx = LIST_W + PADDING * 3;
        if (!hasSel()) {
            g.drawString(font, "§7← Click a kit to preview it", rx, height / 2, C_GREY);
            return;
        }
        Kit kit = kits.get(selectedIndex);
        renderInventoryPreview(g, kit, rx);

        // Position load/rename/delete buttons just below the preview
        int previewBottom = getPreviewBottomY();
        int bw = rightW() - PADDING * 2;
        int bx = rx + PADDING;
        int half = bw / 2 - 2;

        // Reposition buttons dynamically
        btnLoad.setPosition(bx, previewBottom + 6);
        btnLoad.setWidth(half);
        btnRename.setPosition(bx + half + 4, previewBottom + 6);
        btnRename.setWidth(half);
        btnDelete.setPosition(bx, previewBottom + 34);
        btnDelete.setWidth(bw);
    }

    /** Returns the Y coordinate just below the inventory preview grid */
    private int getPreviewBottomY() {
        int startY = PADDING + 20;
        int sepY   = startY + 10 + 3 * (SLOT_SZ + 2) + 2;
        return sepY + 4 + SLOT_SZ + 14; // bottom of hotbar + label
    }

    /**
     * Renders a mini inventory grid showing every saved item in its exact slot.
     * Layout mirrors the vanilla inventory:
     *   Row 0 (top): slots 36-39 (armor) + slot 40 (offhand) on the right
     *   Rows 1-4: hotbar (0-8) and main (9-35) displayed like vanilla (main on top, hotbar bottom)
     */
    private void renderInventoryPreview(GuiGraphics g, Kit kit, int rx) {
        int startY = PADDING + 20;

        // --- Armor column (slots 36-39 top to bottom: helmet=39, chest=38, legs=37, boots=36) ---
        int armorX = rx;
        int[] armorSlots = {39, 38, 37, 36}; // helmet down to boots
        String[] armorLabels = {"H", "C", "L", "B"};
        g.drawString(font, "§8Armor", armorX, startY - 2, C_GREY);
        for (int i = 0; i < 4; i++) {
            int sy = startY + 10 + i * (SLOT_SZ + 2);
            drawSlot(g, armorX, sy, getKitSlot(kit, armorSlots[i]));
        }

        // Offhand
        int offY = startY + 10 + 4 * (SLOT_SZ + 2) + 4;
        g.drawString(font, "§8Off", armorX, offY - 2, C_GREY);
        drawSlot(g, armorX, offY, getKitSlot(kit, 40));

        // --- Main inventory: 9 cols, rows 9-35 then 0-8 (hotbar at bottom) ---
        int invX = armorX + SLOT_SZ + 10;
        int cols = 9;

        g.drawString(font, "§8Inventory", invX, startY - 2, C_GREY);
        // Main rows (slots 9-35 = 3 rows)
        for (int slot = 9; slot < 36; slot++) {
            int col = (slot - 9) % cols;
            int row = (slot - 9) / cols;
            int sx = invX + col * (SLOT_SZ + 2);
            int sy = startY + 10 + row * (SLOT_SZ + 2);
            drawSlot(g, sx, sy, getKitSlot(kit, slot));
        }
        // Separator line between main and hotbar
        int sepY = startY + 10 + 3 * (SLOT_SZ + 2) + 2;
        g.fill(invX, sepY, invX + cols * (SLOT_SZ + 2) - 2, sepY + 1, C_BORDER);
        // Hotbar (slots 0-8)
        for (int slot = 0; slot < 9; slot++) {
            int sx = invX + slot * (SLOT_SZ + 2);
            int sy = sepY + 4;
            drawSlot(g, sx, sy, getKitSlot(kit, slot));
        }

        // Slot count
        int count = kit.slots == null ? 0 : kit.slots.size();
        int previewBottom = sepY + 4 + SLOT_SZ + 8;
        g.drawString(font, "§8" + count + " item stacks saved", rx, previewBottom, C_GREY);
    }

    private void drawSlot(GuiGraphics g, int x, int y, ItemStack stack) {
        // Slot background
        g.fill(x, y, x + SLOT_SZ, y + SLOT_SZ, C_SLOT_BG);
        // Slot border
        g.fill(x, y, x + SLOT_SZ, y + 1, C_SLOT_BD);
        g.fill(x, y, x + 1, y + SLOT_SZ, C_SLOT_BD);
        g.fill(x, y + SLOT_SZ - 1, x + SLOT_SZ, y + SLOT_SZ, C_SLOT_BD);
        g.fill(x + SLOT_SZ - 1, y, x + SLOT_SZ, y + SLOT_SZ, C_SLOT_BD);
        // Item
        if (!stack.isEmpty()) g.renderItem(stack, x + 1, y + 1);
    }

    private ItemStack getKitSlot(Kit kit, int slot) {
        if (kit.slots == null) return ItemStack.EMPTY;
        String nbt = kit.slots.get(String.valueOf(slot));
        if (nbt == null || nbt.isEmpty()) return ItemStack.EMPTY;
        try {
            var registries = minecraft.player.level().registryAccess();
            var ops = registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            var tag = net.minecraft.nbt.TagParser.parseCompoundFully(nbt);
            return ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    // ---- New kit panel ----
    private void renderNewKit(GuiGraphics g, int mx, int my) {
        int rx = LIST_W + PADDING * 3;
        g.drawString(font, "§7Kit Name:", rx, PADDING + 18, C_GREY);
        // nameField draws itself

        // Icon picker button
        int iconX = rx, iconY = PADDING + 55;
        g.drawString(font, "§7Icon:", iconX, iconY, C_GREY);
        int bx = iconX, by = iconY + 12;
        boolean hov = mx >= bx && mx <= bx + SLOT_SZ + 4 && my >= by && my <= by + SLOT_SZ + 4;
        g.fill(bx, by, bx + SLOT_SZ + 4, by + SLOT_SZ + 4, hov ? C_ACCENT : C_SLOT_BD);
        g.fill(bx + 1, by + 1, bx + SLOT_SZ + 3, by + SLOT_SZ + 3, C_SLOT_BG);
        ItemStack icon = chosenIcon.isEmpty() ? new ItemStack(Items.CHEST) : chosenIcon;
        g.renderItem(icon, bx + 2, by + 2);
        g.drawString(font, hov ? "§eClick to change" : "§7Click to pick icon",
                bx + SLOT_SZ + 8, by + 5, C_GREY);
    }

    // ---- Rename panel ----
    private void renderRename(GuiGraphics g) {
        int rx = LIST_W + PADDING * 3;
        g.drawString(font, "§7New Name:", rx, PADDING + 18, C_GREY);
    }

    // ---- Confirm load panel ----
    private void renderConfirm(GuiGraphics g) {
        int rx = LIST_W + PADDING * 3;
        // Show preview of what will be loaded
        if (hasSel()) {
            renderInventoryPreview(g, kits.get(selectedIndex), rx);
        }
        // Warning overlay at bottom
        int wy = height - PADDING - 60;
        g.fill(rx, wy, width - PADDING, wy + 40, 0xCC200000);
        g.drawString(font, "§cThis will clear your current inventory!", rx + 4, wy + 5, 0xFFFF6666);
        g.drawString(font, "§cAre you sure?", rx + 4, wy + 17, 0xFFFF6666);
        if (minecraft != null && minecraft.player != null && !minecraft.player.isCreative()) {
            g.drawString(font, "§4⚠ Creative mode required!", rx + 4, wy + 29, 0xFFFF4444);
        }
    }

    // ---- Icon picker overlay ----
    private void renderIconPicker(GuiGraphics g, int mx, int my) {
        // Darken background
        g.fill(0, 0, width, height, 0xBB000000);

        int cols = 9, pad = 3;
        // Filter to non-empty items only
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack s : allPlayerItems) if (!s.isEmpty()) items.add(s);

        if (items.isEmpty()) {
            int cx = width / 2, cy = height / 2;
            g.fill(cx - 80, cy - 12, cx + 80, cy + 12, C_PANEL);
            g.drawString(font, "§cNo items in inventory!", cx - 70, cy - 4, 0xFFFF6666);
            return;
        }

        int rows = (items.size() + cols - 1) / cols;
        int pw = cols * (SLOT_SZ + pad) + pad + 8;
        int ph = rows * (SLOT_SZ + pad) + pad + 24;
        int px = (width - pw) / 2, py = (height - ph) / 2;

        // Panel
        g.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, C_ACCENT);
        g.fill(px, py, px + pw, py + ph, C_PANEL);
        g.drawString(font, "§bPick an Icon", px + 4, py + 5, C_WHITE);

        for (int i = 0; i < items.size(); i++) {
            int col = i % cols, row = i / cols;
            int sx = px + 4 + pad + col * (SLOT_SZ + pad);
            int sy = py + 18 + pad + row * (SLOT_SZ + pad);
            boolean hov = mx >= sx && mx < sx + SLOT_SZ && my >= sy && my < sy + SLOT_SZ;
            drawSlot(g, sx, sy, items.get(i));
            if (hov) g.fill(sx, sy, sx + SLOT_SZ, sy + SLOT_SZ, 0x55FFFFFF);
        }

        g.drawString(font, "§8ESC or click outside to cancel", px + 4, py + ph - 12, C_GREY);
    }

    // -----------------------------------------------------------------------
    // Input
    // -----------------------------------------------------------------------

    /**
     * We register this as a Fabric ScreenEvents click listener in KitModClient
     * so it gets called even though we can't @Override the new-signature mouseClicked.
     * This method is public so KitModClient can call it directly.
     */
    public boolean handleClick(double mouseX, double mouseY, int button) {
        // Icon picker overlay
        if (pickingIcon) {
            handleIconPickerClick((int) mouseX, (int) mouseY);
            return true;
        }

        // Kit list click (left panel)
        if (mouseX <= LIST_W + PADDING * 2) {
            int y = PADDING + 14;
            int maxVis = (height - y - 40) / ROW_H;
            int idx = ((int) mouseY - y) / ROW_H + scrollOffset;
            if (idx >= 0 && idx < kits.size() && idx < scrollOffset + maxVis) {
                selectedIndex = idx;
                if (state == State.LIST) updateVisibility();
                return true;
            }
        }

        // Icon button in new-kit panel
        if (state == State.NEW_KIT) {
            int rx = LIST_W + PADDING * 3;
            int iconY = PADDING + 55 + 12;
            int bx = rx, by = iconY;
            if (mouseX >= bx && mouseX <= bx + SLOT_SZ + 4 && mouseY >= by && mouseY <= by + SLOT_SZ + 4) {
                pickingIcon = true;
                return true;
            }
        }
        return false;
    }

    private void handleIconPickerClick(int mx, int my) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack s : allPlayerItems) if (!s.isEmpty()) items.add(s);

        int cols = 9, pad = 3;
        int rows = (items.size() + cols - 1) / cols;
        int pw = cols * (SLOT_SZ + pad) + pad + 8;
        int ph = rows * (SLOT_SZ + pad) + pad + 24;
        int px = (width - pw) / 2, py = (height - ph) / 2;

        for (int i = 0; i < items.size(); i++) {
            int sx = px + 4 + pad + (i % cols) * (SLOT_SZ + pad);
            int sy = py + 18 + pad + (i / cols) * (SLOT_SZ + pad);
            if (mx >= sx && mx < sx + SLOT_SZ && my >= sy && my < sy + SLOT_SZ) {
                chosenIcon = items.get(i).copy();
                pickingIcon = false;
                return;
            }
        }
        // Click outside → cancel picker
        pickingIcon = false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx <= LIST_W + PADDING * 2) {
            int maxVis = (height - PADDING - 14 - 40) / ROW_H;
            scrollOffset = Math.max(0, Math.min(
                    scrollOffset - (int) Math.signum(dy),
                    Math.max(0, kits.size() - maxVis)));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    public boolean handleKey(int keyCode) {
        if (keyCode == 256) { // ESC
            if (pickingIcon) { pickingIcon = false; return true; }
            if (state != State.LIST) { cancelFlow(); return true; }
        }
        if (keyCode == 257 && (state == State.NEW_KIT || state == State.RENAME)) {
            commitSave(); return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private ItemStack resolveIconStack(Kit kit) {
        if (kit.iconItemId == null || kit.iconItemId.isEmpty()) return new ItemStack(Items.CHEST);
        try {
            var opt = BuiltInRegistries.ITEM.get(Identifier.parse(kit.iconItemId));
            if (opt.isPresent()) return new ItemStack(opt.get().value());
        } catch (Exception ignored) {}
        return new ItemStack(Items.CHEST);
    }

    private void showFeedback(String msg) {
        feedbackMessage = msg; feedbackExpiry = System.currentTimeMillis() + 2500;
    }

    @Override public boolean isPauseScreen() { return false; }
}
