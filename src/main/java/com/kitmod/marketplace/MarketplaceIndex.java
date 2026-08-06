package com.kitmod.marketplace;
import java.util.List;
public class MarketplaceIndex {
    public List<MarketplaceKitMeta> kits;
    public static class MarketplaceKitMeta {
        public String id, name, author, description, iconItemId, imageBase64, uploadedAt;
    }
}
