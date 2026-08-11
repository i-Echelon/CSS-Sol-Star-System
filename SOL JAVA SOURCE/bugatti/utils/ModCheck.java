package bugatti.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import indevo.industries.artillery.utils.ArtilleryStationPlacer;
import org.widehorizons.api.WideHorizonsAPI;

import static indevo.industries.artillery.scripts.ArtilleryStationScript.TYPE_KEY;

public class ModCheck {
    public static boolean hasLL     = mod("lunalib");
    public static boolean hasIIBG   = mod("II_BG");
    public static boolean hasRAT    = mod("assortment_of_things");
    public static boolean hasIND    = mod("IndEvo");
    public static boolean hasDOP    = mod("Cryo_but_better");
    public static boolean hasVOK    = mod("aotd_vok");
    public static boolean hasUS     = mod("US");
    public static boolean hasSA     = mod("alcoholism");
    public static boolean hasWH     = mod("WideHorizons");
    public static boolean hasWHB    = mod("WideHorizonsBasic");

    public static boolean mod(String id) {
        return Global.getSettings().getModManager().isModEnabled(id);
    }

    // safe condition application
    public static void cond(MarketAPI market, String modId, String condition) {
        if (mod(modId)) market.addCondition(condition);
    }
    // safe conditions list
    public static void cond(MarketAPI market, String modId, String... conditions) {
        if (!mod(modId)) return;
        for (String c : conditions) {
            market.addCondition(c);
        }
    }
    // safe industry data
    public static Industry indy(MarketAPI market, String modId, String industryId, String aiCoreId, SpecialItemData item, boolean improved) {
        Industry result = null;
        if (mod(modId)) {
            if (!market.hasIndustry(industryId)) {
                market.addIndustry(industryId);
            }
            Industry ind = market.getIndustry(industryId);
            if (ind != null) {
                if (aiCoreId != null) {
                    ind.setAICoreId(aiCoreId);
                }
                if (item != null) {
                    ind.setSpecialItem(item);
                }
                if (improved) {
                    ind.setImproved(true);
                }
                result = ind;
            }
        }

        return result;
    }
    // safe tag application
    public static void tag(SectorEntityToken entity, String modId, String tag) {
        if (mod(modId)) entity.addTag(tag);
    }
    // multiple
    public static void tag(SectorEntityToken entity, String modId, String... tags) {
        if (!mod(modId)) return;
        for (String t : tags) {
            entity.addTag(t);
        }
    }
    // safe memory set
    public static void mem(SectorEntityToken entity, String modId, String key, Object value) {
        if (mod(modId)) entity.getMemoryWithoutUpdate().set(key, value);
    }
    // multiple
    public static void mem(SectorEntityToken entity, String modId, Object... kvPairs) {
        if (!mod(modId)) return;
        if (kvPairs.length % 2 != 0)
            throw new IllegalArgumentException("mem(): must supply pairs: key, value, key, value...");

        MemoryAPI mem = entity.getMemoryWithoutUpdate();
        for (int i = 0; i < kvPairs.length; i += 2) {
            String key = (String) kvPairs[i];
            Object value = kvPairs[i + 1];
            mem.set(key, value);
        }
    }
    public static void indevoArty(SectorEntityToken token, boolean destroyed, String artilleryType) {
        if (!hasIND || token == null || token.getMarket() == null) return;
        if (artilleryType != null) {
            token.getMarket().getMemoryWithoutUpdate().set(TYPE_KEY, artilleryType);
            token.getMarket().addCondition("IndEvo_ArtilleryStationCondition");
        }
        ArtilleryStationPlacer.addArtilleryToPlanet(token, destroyed);
    }
    // WH support
    public static void registerWideHorizons() {
        boolean WH = hasWH || hasWHB;
        if (!WH) return;

        var sol = Global.getSector().getStarSystem("Sol");
        var alpha = Global.getSector().getStarSystem("Alpha Centauri");
        var beta = Global.getSector().getStarSystem("Beta Centauri");

        if (sol != null) {
            WideHorizonsAPI.registerConstellation(
                    "Sol", sol.getLocation().x, sol.getLocation().y
            );
        }
        if (alpha != null) {
            WideHorizonsAPI.registerConstellation(
                    "Alpha Centauri", alpha.getLocation().x, alpha.getLocation().y
            );
        }
        if (beta != null) {
            WideHorizonsAPI.registerConstellation(
                    "Beta Centauri", beta.getLocation().x, beta.getLocation().y
            );
        }
    }
}