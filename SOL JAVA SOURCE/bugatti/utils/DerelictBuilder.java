package bugatti.utils;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictType;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.Random;

public class DerelictBuilder {
    private static final Random RNG = new Random();
    /**
     * Add a *random* recoverable derelict (vanilla-style random variant & condition).
     */
    public static void addRandomDerelict(StarSystemAPI system, SectorEntityToken focus, float angle, float radius, DerelictType type) {
        //(no faction)
        DerelictShipData params = DerelictShipEntityPlugin.createRandom(
                pickRandomFactionWeighted(),
                null,
                null
        );

        if (params == null) return; // should never be null now

        params.ship.addDmods = true;
        params.ship.sModProb = 100.0f;

        SectorEntityToken wreck = BaseThemeGenerator.addSalvageEntity(
                null,
                system,
                Entities.WRECK,
                Factions.NEUTRAL,
                params
        );

        wreck.addTag(Tags.TAG_RETAIN_SMODS_ON_RECOVERY);
        wreck.addTag("sol_domain");
        wreck.setExpired(false);
        wreck.setDiscoverable(true);
        Misc.setSalvageSpecial(wreck, params);
        //wreck.addTag("sc_has_officer_special");
        //SalvageSpecialAssigner.assignSpecials(wreck);
        float orbitDays = Math.max(200f, radius * 0.1f);
        wreck.setCircularOrbit(focus, angle, radius, orbitDays);
    }
    /**
     * Add a specific variant as a recoverable derelict.
     */
    public static void addSpecificDerelict(StarSystemAPI system, SectorEntityToken focus, float angle, float radius, String variantId, ShipCondition condition) {
        // createVariant also returns DerelictShipData
        DerelictShipData params = DerelictShipEntityPlugin.createVariant(
                variantId,
                null,
                100.0f
        );

        // set the explicit condition and d-mod behavior
        params.ship.condition = condition;
        params.ship.addDmods = true;
        params.ship.sModProb = 100.0f;

        SectorEntityToken wreck = BaseThemeGenerator.addSalvageEntity(
                null,
                system,
                Entities.WRECK,
                Factions.NEUTRAL,
                params
        );

        wreck.addTag(Tags.TAG_RETAIN_SMODS_ON_RECOVERY);
        wreck.addTag("sol_domain");
        wreck.setExpired(false);
        wreck.setDiscoverable(true);

        float orbitDays = Math.max(200f, radius * 0.1f);
        wreck.setCircularOrbit(focus, angle, radius, orbitDays);
    }
    // Convenience helpers (rings / mixed / scattered)
    public static void addWreck(StarSystemAPI system, SectorEntityToken focus) {
        addRandomDerelict(system, focus, RNG.nextFloat() * 360f, 8000f + RNG.nextFloat() * 1000f, null);
    }
    public static void addWrecksRing(StarSystemAPI system, SectorEntityToken focus, int count) {
        float step = 360f / Math.max(1, count);
        for (int i = 0; i < count; i++) {
            float angle = i * step;
            float radius = 6500f + RNG.nextFloat() * 2000f;
            addRandomDerelict(system, focus, angle, radius, null);
        }
    }
    public static void addWrecksScattered(StarSystemAPI system, SectorEntityToken focus) {
        float min = 1000f;
        float max = 20000f;

        for (int i = 0; i < 8; i++) {
            float angle = RNG.nextFloat() * 360f;                      // fully random angle
            float radius = min + RNG.nextFloat() * (max - min);       // random radius 1000–20000

            addRandomDerelict(system, focus, angle, radius, null);
        }
    }
    // RNG Shipper
    private static String pickRandomFactionWeighted() {
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

        picker.add(Factions.PIRATES,       5f); // 17.24%
        picker.add(Factions.INDEPENDENT,   5f); // 17.24%
        picker.add(Factions.DERELICT,      4f); // 13.79%
        picker.add(Factions.PLAYER,        4f); // 13.79%
        picker.add(Factions.MERCENARY,     3f); // 10.34%
        picker.add(Factions.REMNANTS,      3f); // 10.34%
        picker.add(Factions.THREAT,        2f); // 6.90%
        picker.add(Factions.DWELLER,       2f); // 6.90%
        picker.add(Factions.OMEGA,         1f); // 3.45%

        return picker.pick();
    }
}
