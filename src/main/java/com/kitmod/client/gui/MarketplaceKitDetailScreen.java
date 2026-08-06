package com.kitmod.client.gui;

import com.google.gson.Gson;
import com.kitmod.data.Kit;
import com.kitmod.data.KitStorage;
import com.kitmod.marketplace.MarketplaceIndex;
import com.kitmod.marketplace.MarketplaceKit;
import com.kitmod.util.HttpUtil;
import com.kitmod.util.InventoryHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class MarketplaceKitDetailScreen extends Screen {

    private enum LoadState { LOADING, LOADED, ERROR, DOWNLOADED }
    private LoadState loadState = LoadState.LOADING;
    private String statusMsg = "";
    private final Screen parent;
    private final MarketplaceIndex.MarketplaceKitMeta meta;
    private MarketplaceKit fullKit = null;
    private Button btnDownload, btnBack;

    private static final int PAD=10,HDR=30,SLOT=18;
    private static final int C_BG=0xFF0C0C18,C_PANEL=0xFF13131F,C_HDR=0xFF0A0A14,
            C_BORDER=0xFF252538,C_ACCENT=0xFF6C5CE7,C_WHITE=0xFFEEEEFF,
            C_GREY=0xFF8080A0,C_GREEN=0xFF22C55E,C_SLOT_BG=0xFF18182A,C_SLOT_BD=0xFF303050;

    private static final Gson GSON = new Gson();

    public MarketplaceKitDetailScreen(Screen parent, MarketplaceIndex.MarketplaceKitMeta meta) {
        super(Component.literal(meta.name));
        this.parent=parent; this.meta=meta;
    }

    @Override
    protected void init() {
        btnBack=Button.builder(Component.literal("← Back"),b->minecraft.setScreen(parent))
                .bounds(PAD,height-PAD-20,70,18).build();
        addRenderableWidget(btnBack);
        btnDownload=Button.builder(Component.literal("⬇  Download Kit"),b->downloadKit())
                .bounds(width-PAD-140,height-PAD-20,140,18).build();
        btnDownload.active=false;
        addRenderableWidget(btnDownload);
        fetchFullKit();
    }

    private void fetchFullKit() {
        loadState=LoadState.LOADING;
        HttpUtil.get(HttpUtil.RAW_BASE+"/kits/"+meta.id+".json").thenAccept(json -> {
            if(json==null){loadState=LoadState.ERROR;statusMsg="Failed to load kit.";return;}
            try{fullKit=GSON.fromJson(json,MarketplaceKit.class);loadState=LoadState.LOADED;btnDownload.active=true;}
            catch(Exception e){loadState=LoadState.ERROR;statusMsg="Kit data corrupted.";}
        });
    }

    private void downloadKit() {
        if(fullKit==null) return;
        Kit local=new Kit(fullKit.name,fullKit.iconItemId);
        local.slots=fullKit.slots; local.savedAt=fullKit.uploadedAt;
        KitStorage.save(local);
        loadState=LoadState.DOWNLOADED;
        statusMsg="§aDownloaded! Find it in your local kits.";
        btnDownload.active=false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0,0,width,height,C_BG);
        g.fill(0,0,width,HDR,C_HDR);
        g.fill(0,HDR,width,HDR+1,C_BORDER);
        g.drawString(font,"§b"+meta.name,PAD,(HDR-8)/2,C_WHITE);
        g.drawString(font,"§8by §7"+meta.author,PAD+font.width(meta.name)+10,(HDR-8)/2,C_GREY);

        // Green stroke on download button
        if(loadState==LoadState.LOADED&&btnDownload.active){
            int bx=width-PAD-140,by=height-PAD-20;
            g.fill(bx-2,by-2,bx+142,by+20,C_GREEN);
            g.fill(bx-1,by-1,bx+141,by+19,C_BG);
        }

        super.render(g,mx,my,delta);

        int cy=HDR+PAD;
        switch(loadState){
            case LOADING->g.drawString(font,"§7Loading…",width/2-25,height/2,C_GREY);
            case ERROR->g.drawString(font,"§c✗ "+statusMsg,PAD,cy,0xFFFF6666);
            case LOADED,DOWNLOADED->renderContent(g,cy);
        }

        g.fill(0,height-28,width,height-27,C_BORDER);
        g.fill(0,height-27,width,height,C_PANEL);

        if(!statusMsg.isEmpty()&&loadState==LoadState.DOWNLOADED){
            int tw=font.width(statusMsg)+14,tx=(width-tw)/2,ty=height-56;
            g.fill(tx-2,ty-3,tx+tw+2,ty+13,0xEE0A0A14);
            g.fill(tx-2,ty-3,tx+tw+2,ty-2,C_ACCENT);
            g.drawString(font,statusMsg,tx+7,ty,C_WHITE);
        }
    }

    private void renderContent(GuiGraphics g, int startY) {
        if(fullKit==null) return;
        if(meta.description!=null&&!meta.description.isEmpty()){
            g.drawString(font,"§7"+meta.description,PAD,startY,C_GREY); startY+=14;
        }
        g.fill(PAD,startY+2,width-PAD,startY+3,C_BORDER); startY+=10;
        g.drawString(font,"§8Preview",PAD,startY,C_GREY); startY+=12;

        int ax=PAD; int[]armorSlots={39,38,37,36};
        for(int i=0;i<4;i++) drawSlot(g,ax,startY+i*(SLOT+2),getSlot(armorSlots[i]));
        drawSlot(g,ax,startY+4*(SLOT+2)+4,getSlot(40));

        int invX=ax+SLOT+8;
        for(int slot=9;slot<36;slot++)
            drawSlot(g,invX+(slot-9)%9*(SLOT+2),startY+(slot-9)/9*(SLOT+2),getSlot(slot));
        int sepY=startY+3*(SLOT+2)+2;
        g.fill(invX,sepY,invX+9*(SLOT+2)-2,sepY+1,C_BORDER);
        for(int slot=0;slot<9;slot++) drawSlot(g,invX+slot*(SLOT+2),sepY+4,getSlot(slot));

        int count=fullKit.slots!=null?fullKit.slots.size():0;
        g.drawString(font,"§8"+count+" stacks",PAD,sepY+4+SLOT+8,C_GREY);
    }

    private void drawSlot(GuiGraphics g,int x,int y,ItemStack s){
        g.fill(x,y,x+SLOT,y+SLOT,C_SLOT_BG);
        g.fill(x,y,x+SLOT,y+1,C_SLOT_BD); g.fill(x,y,x+1,y+SLOT,C_SLOT_BD);
        g.fill(x,y+SLOT-1,x+SLOT,y+SLOT,C_SLOT_BD); g.fill(x+SLOT-1,y,x+SLOT,y+SLOT,C_SLOT_BD);
        if(!s.isEmpty()) g.renderItem(s,x+1,y+1);
    }

    private ItemStack getSlot(int slot){
        if(fullKit==null||fullKit.slots==null||minecraft==null||minecraft.player==null) return ItemStack.EMPTY;
        String nbt=fullKit.slots.get(String.valueOf(slot));
        if(nbt==null||nbt.isEmpty()) return ItemStack.EMPTY;
        return InventoryHelper.parseStack(nbt,minecraft.player);
    }

    public boolean handleClick(double mx,double my){return false;}
    public boolean handleKey(int key){if(key==256){minecraft.setScreen(parent);return true;}return false;}
    @Override public boolean isPauseScreen(){return false;}
}
