package com.kitmod.client.gui;

import com.google.gson.Gson;
import com.kitmod.marketplace.MarketplaceIndex;
import com.kitmod.util.HttpUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class MarketplaceScreen extends Screen {

    private enum LoadState { LOADING, LOADED, ERROR }
    private LoadState loadState = LoadState.LOADING;
    private String errorMsg = "";
    private List<MarketplaceIndex.MarketplaceKitMeta> allKits = new ArrayList<>();
    private List<MarketplaceIndex.MarketplaceKitMeta> filtered = new ArrayList<>();
    private int scroll = 0;
    private static final int CARD_H = 52, PAD = 10, HDR = 30;
    private static final int C_BG=0xFF0C0C18,C_PANEL=0xFF13131F,C_HDR=0xFF0A0A14,C_BORDER=0xFF252538,
            C_ACCENT=0xFF6C5CE7,C_WHITE=0xFFEEEEFF,C_GREY=0xFF8080A0,C_SEL=0xFF1A1A30,C_HOVER=0xFF1E1E32;
    private EditBox searchBox;
    private Button btnUpload, btnBack, btnRefresh;
    private final Screen parent;
    private static final Gson GSON = new Gson();

    public MarketplaceScreen(Screen parent) {
        super(Component.literal("Kit Marketplace"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        searchBox = new EditBox(font, PAD, (HDR-18)/2, width-PAD*2-130, 18, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search by name or author…"));
        searchBox.setResponder(q -> filterKits(q));
        addRenderableWidget(searchBox);
        btnUpload = Button.builder(Component.literal("⬆  Upload Kit"),
                b -> minecraft.setScreen(new MarketplaceUploadScreen(this)))
                .bounds(width-PAD-120, (HDR-18)/2, 120, 18).build();
        addRenderableWidget(btnUpload);
        btnRefresh = Button.builder(Component.literal("↻"), b -> reload())
                .bounds(width-PAD-130-22, (HDR-18)/2, 20, 18).build();
        addRenderableWidget(btnRefresh);
        btnBack = Button.builder(Component.literal("← Back"), b -> minecraft.setScreen(parent))
                .bounds(PAD, height-PAD-20, 70, 18).build();
        addRenderableWidget(btnBack);
        if (allKits.isEmpty()) fetchIndex();
    }

    private void reload() { loadState=LoadState.LOADING; allKits.clear(); filtered.clear(); scroll=0; fetchIndex(); }

    private void fetchIndex() {
        loadState = LoadState.LOADING;
        HttpUtil.get(HttpUtil.RAW_BASE + "/index.json").thenAccept(json -> {
            if (json == null) { loadState=LoadState.ERROR; errorMsg="Could not reach marketplace. Check connection."; return; }
            try {
                MarketplaceIndex idx = GSON.fromJson(json, MarketplaceIndex.class);
                allKits = idx.kits != null ? idx.kits : new ArrayList<>();
                filtered = new ArrayList<>(allKits);
                loadState = LoadState.LOADED;
            } catch (Exception e) { loadState=LoadState.ERROR; errorMsg="Marketplace data corrupted."; }
        });
    }

    private void filterKits(String q) {
        filtered = new ArrayList<>();
        String ql = q.toLowerCase().trim();
        for (var k : allKits)
            if (ql.isEmpty() || k.name.toLowerCase().contains(ql) || k.author.toLowerCase().contains(ql)
                    || (k.description!=null && k.description.toLowerCase().contains(ql)))
                filtered.add(k);
        scroll = 0;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0,0,width,height,C_BG);
        g.fill(0,0,width,HDR,C_HDR);
        g.fill(0,HDR,width,HDR+1,C_BORDER);
        g.drawString(font,"§b✦ Kit Marketplace",PAD,(HDR-8)/2,C_WHITE);
        if (loadState==LoadState.LOADED) {
            String cnt=filtered.size()+" kits";
            g.drawString(font,"§8"+cnt,width/2-font.width(cnt)/2,(HDR-8)/2,C_GREY);
        }
        super.render(g,mx,my,delta);
        int listY=HDR+4, listH=height-listY-30;
        switch (loadState) {
            case LOADING -> { String m="Loading…"; g.drawString(font,"§7"+m,width/2-font.width(m)/2,listY+listH/2,C_GREY); }
            case ERROR   -> { g.drawString(font,"§c✗ "+errorMsg,width/2-font.width(errorMsg)/2,listY+listH/2,0xFFFF6666);
                              String r="Press ↻ to retry"; g.drawString(font,"§8"+r,width/2-font.width(r)/2,listY+listH/2+14,C_GREY); }
            case LOADED  -> renderList(g,mx,my,listY,listH);
        }
        g.fill(0,height-28,width,height-27,C_BORDER);
        g.fill(0,height-27,width,height,C_PANEL);
    }

    private void renderList(GuiGraphics g, int mx, int my, int listY, int listH) {
        if (filtered.isEmpty()) { String m="No kits found."; g.drawString(font,"§7"+m,width/2-font.width(m)/2,listY+listH/2,C_GREY); return; }
        int maxVis=listH/CARD_H, cw=width-PAD*2;
        for (int i=scroll; i<Math.min(filtered.size(),scroll+maxVis); i++) {
            var kit=filtered.get(i);
            int cy=listY+(i-scroll)*CARD_H;
            boolean hov=mx>=PAD&&mx<=PAD+cw&&my>=cy&&my<cy+CARD_H-2;
            g.fill(PAD,cy,PAD+cw,cy+CARD_H-2,hov?C_SEL:C_PANEL);
            g.fill(PAD,cy,PAD+cw,cy+1,C_BORDER);
            g.fill(PAD,cy+CARD_H-3,PAD+cw,cy+CARD_H-2,C_BORDER);
            g.fill(PAD,cy+2,PAD+3,cy+CARD_H-4,C_ACCENT);
            g.renderItem(resolveIcon(kit.iconItemId),PAD+8,cy+(CARD_H-16)/2);
            int tx=PAD+30;
            g.drawString(font,"§f"+kit.name,tx,cy+8,C_WHITE);
            g.drawString(font,"§8by §7"+kit.author,tx,cy+19,C_GREY);
            if (kit.description!=null&&!kit.description.isEmpty()) {
                String d=kit.description.length()>50?kit.description.substring(0,47)+"…":kit.description;
                g.drawString(font,"§8"+d,tx,cy+30,C_GREY);
            }
            g.drawString(font,"§8›",PAD+cw-12,cy+CARD_H/2-4,C_GREY);
        }
        if (scroll>0) g.drawString(font,"§8▲",width/2,listY+2,C_GREY);
        if (scroll+listH/CARD_H<filtered.size()) g.drawString(font,"§8▼",width/2,listY+listH-10,C_GREY);
    }

    public boolean handleClick(double mx, double my) {
        if (loadState!=LoadState.LOADED) return false;
        int listY=HDR+4, listH=height-listY-30, maxVis=listH/CARD_H, cw=width-PAD*2;
        for (int i=scroll; i<Math.min(filtered.size(),scroll+maxVis); i++) {
            int cy=listY+(i-scroll)*CARD_H;
            if (mx>=PAD&&mx<=PAD+cw&&my>=cy&&my<cy+CARD_H-2) {
                minecraft.setScreen(new MarketplaceKitDetailScreen(this,filtered.get(i)));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (loadState!=LoadState.LOADED) return false;
        int maxVis=(height-HDR-34)/CARD_H;
        scroll=Math.max(0,Math.min(scroll-(int)Math.signum(dy),Math.max(0,filtered.size()-maxVis)));
        return true;
    }

    private ItemStack resolveIcon(String id) {
        if (id==null||id.isEmpty()) return new ItemStack(Items.CHEST);
        try { var o=BuiltInRegistries.ITEM.get(Identifier.parse(id)); if(o.isPresent()) return new ItemStack(o.get().value()); } catch(Exception e){}
        return new ItemStack(Items.CHEST);
    }

    @Override public boolean isPauseScreen() { return false; }
}
