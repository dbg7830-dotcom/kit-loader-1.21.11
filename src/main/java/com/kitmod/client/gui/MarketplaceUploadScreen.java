package com.kitmod.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kitmod.data.Kit;
import com.kitmod.data.KitStorage;
import com.kitmod.marketplace.MarketplaceKit;
import com.kitmod.util.HttpUtil;
import com.kitmod.util.ImageUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MarketplaceUploadScreen extends Screen {

    private enum UploadState { IDLE, UPLOADING, SUCCESS, ERROR }
    private UploadState uploadState = UploadState.IDLE;
    private String statusMsg = "";

    private EditBox fieldUsername, fieldDescription, fieldImagePath;
    private Button btnPickKit, btnPickImage, btnUpload, btnBack;
    private Kit selectedKit = null;
    private String imageBase64 = null, imageName = "";
    private List<Kit> localKits;
    private int kitScroll = 0;
    private boolean showKitPicker = false;
    private static final int KIT_ROW=22,PAD=10,HDR=30;
    private static final boolean HAS_AWT=checkAWT();
    private static final int C_BG=0xFF0C0C18,C_PANEL=0xFF13131F,C_HDR=0xFF0A0A14,
            C_BORDER=0xFF252538,C_ACCENT=0xFF6C5CE7,C_WHITE=0xFFEEEEFF,
            C_GREY=0xFF8080A0,C_GREEN=0xFF22C55E;
    private final Screen parent;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public MarketplaceUploadScreen(Screen parent) { super(Component.literal("Upload Kit")); this.parent=parent; }

    private static boolean checkAWT(){try{Class.forName("java.awt.FileDialog");return true;}catch(ClassNotFoundException e){return false;}}

    @Override
    protected void init() {
        localKits=KitStorage.loadAll();
        int fw=width-PAD*2,col=PAD,y=HDR+PAD;
        fieldUsername=new EditBox(font,col,y+14,fw,18,Component.literal("Username"));
        fieldUsername.setHint(Component.literal("Your Modrinth / GitHub username…"));
        fieldUsername.setMaxLength(32); addRenderableWidget(fieldUsername); y+=40;
        fieldDescription=new EditBox(font,col,y+14,fw,18,Component.literal("Description"));
        fieldDescription.setHint(Component.literal("Short description (optional)…"));
        fieldDescription.setMaxLength(120); addRenderableWidget(fieldDescription); y+=40;
        btnPickKit=Button.builder(Component.literal(selectedKit==null?"Select a Kit  ▾":"Kit:  "+selectedKit.name),
                b->showKitPicker=!showKitPicker).bounds(col,y+14,fw,20).build();
        addRenderableWidget(btnPickKit); y+=42;
        if(HAS_AWT){
            btnPickImage=Button.builder(Component.literal(imageName.isEmpty()?"Pick Image  (PNG/JPG)":"Image:  "+imageName),
                    b->openFileDialog()).bounds(col,y+14,fw,20).build();
            addRenderableWidget(btnPickImage);
        } else {
            fieldImagePath=new EditBox(font,col,y+14,fw,18,Component.literal("Image"));
            fieldImagePath.setHint(Component.literal("Full path to image file on device…"));
            addRenderableWidget(fieldImagePath);
        }
        btnUpload=Button.builder(Component.literal("⬆  Upload to Marketplace"),b->doUpload())
                .bounds(col,height-PAD-42,fw,20).build();
        addRenderableWidget(btnUpload);
        btnBack=Button.builder(Component.literal("← Back"),b->minecraft.setScreen(parent))
                .bounds(col,height-PAD-18,70,16).build();
        addRenderableWidget(btnBack);
    }

    private void openFileDialog(){
        CompletableFuture.runAsync(()->{
            try{
                java.awt.FileDialog fd=new java.awt.FileDialog((java.awt.Frame)null,"Select Kit Image",java.awt.FileDialog.LOAD);
                fd.setFilenameFilter((d,n)->n.toLowerCase().endsWith(".png")||n.toLowerCase().endsWith(".jpg")||n.toLowerCase().endsWith(".jpeg"));
                fd.setVisible(true);
                String d=fd.getDirectory(),f=fd.getFile();
                if(d!=null&&f!=null){
                    String enc=ImageUtil.encodeFile(new File(d,f));
                    if(enc==null){statusMsg="§cImage too large! Max 192KB.";}
                    else{imageBase64=enc;imageName=f;if(btnPickImage!=null)btnPickImage.setMessage(Component.literal("Image:  "+f));}
                }
            }catch(Exception e){statusMsg="§cCouldn't open file picker.";}
        });
    }

    private void doUpload(){
        String username=fieldUsername.getValue().trim();
        if(username.isEmpty()){statusMsg="§cEnter your username.";return;}
        if(selectedKit==null){statusMsg="§cSelect a kit to upload.";return;}
        if(!HAS_AWT&&fieldImagePath!=null){
            String path=fieldImagePath.getValue().trim();
            if(!path.isEmpty()){String enc=ImageUtil.encodeFile(new File(path));if(enc==null){statusMsg="§cImage not found or too large.";return;}imageBase64=enc;}
        }
        uploadState=UploadState.UPLOADING; statusMsg="§7Uploading…";
        MarketplaceKit p=new MarketplaceKit();
        p.id=username.toLowerCase().replaceAll("[^a-z0-9]","_")+"_"+selectedKit.name.toLowerCase().replaceAll("[^a-z0-9]","_")+"_"+UUID.randomUUID().toString().substring(0,4);
        p.name=selectedKit.name; p.author=username; p.description=fieldDescription.getValue().trim();
        p.iconItemId=selectedKit.iconItemId; p.imageBase64=imageBase64;
        p.uploadedAt=java.time.Instant.now().toString(); p.slots=selectedKit.slots;
        HttpUtil.postJson(HttpUtil.WORKER_BASE+"/upload",GSON.toJson(p)).thenAccept(resp->{
            if(resp==null||resp.contains("error")||resp.contains("Error")){
                uploadState=UploadState.ERROR;
                statusMsg="§cUpload failed. "+(resp!=null?resp.substring(0,Math.min(resp.length(),80)):"No response.");
            } else {uploadState=UploadState.SUCCESS;statusMsg="§aUploaded! It'll appear in the marketplace shortly.";}
        });
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta){
        g.fill(0,0,width,height,C_BG);
        g.fill(0,0,width,HDR,C_HDR); g.fill(0,HDR,width,HDR+1,C_BORDER);
        g.drawString(font,"§b⬆  Upload to Marketplace",PAD,(HDR-8)/2,C_WHITE);
        int y=HDR+PAD;
        g.drawString(font,"§8Username",PAD,y+3,C_GREY);y+=40;
        g.drawString(font,"§8Description",PAD,y+3,C_GREY);y+=40;
        g.drawString(font,"§8Kit",PAD,y+3,C_GREY);y+=42;
        g.drawString(font,HAS_AWT?"§8Image (optional)":"§8Image path (optional)",PAD,y+3,C_GREY);
        if(uploadState!=UploadState.UPLOADING){
            int bx=PAD,by=height-PAD-42;
            g.fill(bx-2,by-2,bx+(width-PAD*2)+2,by+22,C_GREEN);
            g.fill(bx-1,by-1,bx+(width-PAD*2)+1,by+21,C_BG);
        }
        super.render(g,mx,my,delta);
        if(!statusMsg.isEmpty()) g.drawString(font,statusMsg,PAD,height-PAD-58,C_WHITE);
        if(showKitPicker) renderKitPicker(g,mx,my);
    }

    private void renderKitPicker(GuiGraphics g,int mx,int my){
        int pw=width-PAD*4,px=PAD*2,py=HDR+PAD+40+40+38;
        int maxVis=4,ph=Math.min(localKits.size(),maxVis)*KIT_ROW+8;
        if(localKits.isEmpty())ph=24;
        g.fill(px-1,py-1,px+pw+1,py+ph+1,C_ACCENT);
        g.fill(px,py,px+pw,py+ph,C_PANEL);
        if(localKits.isEmpty()){g.drawString(font,"§7No local kits found.",px+6,py+8,C_GREY);return;}
        for(int i=kitScroll;i<Math.min(localKits.size(),kitScroll+maxVis);i++){
            Kit k=localKits.get(i); int ry=py+4+(i-kitScroll)*KIT_ROW;
            boolean hov=mx>=px&&mx<=px+pw&&my>=ry&&my<ry+KIT_ROW;
            if(hov)g.fill(px,ry,px+pw,ry+KIT_ROW,0x33FFFFFF);
            g.drawString(font,k.name,px+6,ry+6,C_WHITE);
        }
    }

    public boolean handleClick(double mx,double my){
        if(showKitPicker){
            int pw=width-PAD*4,px=PAD*2,py=HDR+PAD+40+40+38,maxVis=4;
            for(int i=kitScroll;i<Math.min(localKits.size(),kitScroll+maxVis);i++){
                int ry=py+4+(i-kitScroll)*KIT_ROW;
                if(mx>=px&&mx<=px+pw&&my>=ry&&my<ry+KIT_ROW){
                    selectedKit=localKits.get(i);
                    btnPickKit.setMessage(Component.literal("Kit:  "+selectedKit.name));
                    showKitPicker=false; return true;
                }
            }
            showKitPicker=false; return true;
        }
        return false;
    }

    public boolean handleKey(int key){
        if(key==256){if(showKitPicker){showKitPicker=false;return true;}minecraft.setScreen(parent);return true;}
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx,double my,double dx,double dy){
        if(showKitPicker){kitScroll=Math.max(0,Math.min(kitScroll-(int)Math.signum(dy),Math.max(0,localKits.size()-4)));return true;}
        return super.mouseScrolled(mx,my,dx,dy);
    }

    @Override public boolean isPauseScreen(){return false;}
}
