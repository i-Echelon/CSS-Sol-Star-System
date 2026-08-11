package bugatti.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.*;
    /* ============================================================================
       EXTERNAL API METHODS
       ============================================================================ */
public class Looter {

    private static final Random random = new Random();
       //   LOOT ENTRY - FLUENT BUILDER
    public static class LootEntry {
        final String id;
        final Rarity rarity;
        final int qtyMin;
        final int qtyMax;
        final String modRequirement;

        private LootEntry(String id, Rarity rarity, int qtyMin, int qtyMax, String modReq) {
            this.id = id;
            this.rarity = rarity;
            this.qtyMin = qtyMin;
            this.qtyMax = qtyMax;
            this.modRequirement = modReq;
        }

        // Factory methods
        public static LootEntry of(String id, Rarity rarity) {
            return new LootEntry(id, rarity, 1, 1, null);
        }

        public static LootEntry of(String id, Rarity rarity, int qtyMin, int qtyMax) {
            return new LootEntry(id, rarity, qtyMin, qtyMax, null);
        }

        // Fluent modifiers
        public LootEntry mod(String modId) {
            return new LootEntry(id, rarity, qtyMin, qtyMax, modId);
        }

        public LootEntry qty(int min, int max) {
            return new LootEntry(id, rarity, min, max, modRequirement);
        }

        // State checks
        boolean isAllowed() {
            return modRequirement == null || ModCheck.mod(modRequirement);
        }

        int weight() {
            return rarity.baseWeight;
        }

        int rollQuantity() {
            return qtyMin >= qtyMax ? qtyMin : randInt(qtyMin, qtyMax);
        }
    }
       //   LOOT TABLE
    public static class LootTable {
        private final List<LootEntry> entries = new ArrayList<>();
        private final LootType type;

        private LootTable(LootType type) {
            this.type = type;
        }

        public static LootTable of(LootType type, LootEntry... entries) {
            LootTable table = new LootTable(type);
            table.entries.addAll(Arrays.asList(entries));
            return table;
        }

        public LootTable add(LootEntry entry) {
            entries.add(entry);
            return this;
        }

        /**
         * Pick a random entry based on weighted rarity.
         */
        LootEntry pick() {
            int totalWeight = 0;
            for (LootEntry e : entries) {
                if (e.isAllowed()) totalWeight += e.weight();
            }

            if (totalWeight <= 0) return null;

            int roll = random.nextInt(totalWeight);
            int accumulated = 0;

            for (LootEntry e : entries) {
                if (!e.isAllowed()) continue;
                accumulated += e.weight();
                if (roll < accumulated) return e;
            }

            return null;
        }
    }
       //   LOOT TYPE ENUM
    public enum LootType {
        COMMODITY,
        WEAPON,
        WING,
        SHIP,
        SPECIAL,
        HULLMOD
    }
       //   RARITY TIER SYSTEM
    public enum Rarity {
        T0(1000),
        T1(100),
        T2(50),
        T3(20),
        T4(10),
        T5(1);

        final int baseWeight;

        Rarity(int weight) {
            this.baseWeight = weight;
        }
    }
       //   STATION CLASS SYSTEM
    public enum StationClass {
        DERELICT(0.7f),       // Stripped/damaged station
        STANDARD(1.0f),       // Normal abandoned station
        MILITARY(1.2f),       // More weapons/ships
        RESEARCH(1.3f),       // Better tech, more specials
        CACHE(1.5f);          // Major find

        final float lootMultiplier;

        StationClass(float mult) {
            this.lootMultiplier = mult;
        }
    }
    /**
     * Add loot using defaults, class modifiers, basic resources, or specific-mixed w/ count.
     */
    public static void addAbandonedLoot(CustomCampaignEntityAPI station) {
        addAbandonedLoot(station, StationClass.STANDARD);
    }
    public static void addAbandonedLoot(CustomCampaignEntityAPI station, StationClass stationClass) {
        if (station == null) return;
        CargoAPI cargo = station.getMarket().getSubmarket("storage").getCargo();
        if (cargo == null) return;

        float mult = stationClass.lootMultiplier;

        // Basic resources
        rollBasics(cargo, mult);

        // Loot categories with class-scaled quantities
        rollLoot(COMMODITIES, cargo, scale(3, 7, mult));
        rollLoot(WEAPONS, cargo, scale(3, 6, mult));
        rollLoot(WINGS, cargo, scale(1, 3, mult));
        rollLoot(SPECIALS, cargo, scale(1, 1, mult));
        rollLoot(HULLMODS, cargo, scale(1, 3, mult));
        // Ships handled separately due to mothball mechanics
        rollShips(cargo, scale(1, 2, mult));
    }

    // ========== SPECIFIC LOOT POOL METHODS ==========
    public static void addWeapons(CargoAPI cargo, int count) {
        rollLoot(WEAPONS, cargo, count);
    }
    public static void addCommodities(CargoAPI cargo, int count) {
        rollLoot(COMMODITIES, cargo, count);
    }
    public static void addSpecials(CargoAPI cargo, int count) {
        rollLoot(SPECIALS, cargo, count);
    }
    public static void addHullmods(CargoAPI cargo, int count) {
        rollLoot(HULLMODS, cargo, count);
    }
    public static void addWings(CargoAPI cargo, int count) {
        rollLoot(WINGS, cargo, count);
    }
    public static void addShips(CargoAPI cargo, int count) {
        rollShips(cargo, count);
    }
    public static void addBasics(CargoAPI cargo, float multiplier) {
        rollBasics(cargo, multiplier);
    }
    public static void addCustomMix(CargoAPI cargo, int weapons, int ships, int wings, int commodities, int specials, int hullmods) {
        if (weapons > 0) addWeapons(cargo, weapons);
        if (ships > 0) addShips(cargo, ships);
        if (wings > 0) addWings(cargo, wings);
        if (commodities > 0) addCommodities(cargo, commodities);
        if (specials > 0) addSpecials(cargo, specials);
        if (hullmods > 0) addHullmods(cargo, hullmods);
    }
    public static void addRandomBlueprints(CargoAPI cargo, int shipCount, int weaponCount, int wingCount) {
        List<String> allShipIds = new ArrayList<>();
        List<String> allWeaponIds = new ArrayList<>();
        List<String> allWingIds = new ArrayList<>();

        // Ships
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (!spec.hasTag("rare_bp") || spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.NO_BP_DROP)) {
                continue;
            }
            allShipIds.add(spec.getHullId());
        }

        // Weapons
        for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
            if (!spec.hasTag("rare_bp") || spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.NO_BP_DROP)) {
                continue;
            }
            allWeaponIds.add(spec.getWeaponId());
        }

        // Fighter wings
        for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
            if (!spec.hasTag("rare_bp") || spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.NO_BP_DROP)) {
                continue;
            }
            allWingIds.add(spec.getId());
        }

        // Add blueprints
        for (int i = 0; i < shipCount && !allShipIds.isEmpty(); i++) {
            cargo.addSpecial(new SpecialItemData(Items.SHIP_BP, allShipIds.get(random.nextInt(allShipIds.size()))), 1f);
        }

        for (int i = 0; i < weaponCount && !allWeaponIds.isEmpty(); i++) {
            cargo.addSpecial(new SpecialItemData(Items.WEAPON_BP, allWeaponIds.get(random.nextInt(allWeaponIds.size()))), 1f);
        }

        for (int i = 0; i < wingCount && !allWingIds.isEmpty(); i++) {
            cargo.addSpecial(new SpecialItemData(Items.FIGHTER_BP, allWingIds.get(random.nextInt(allWingIds.size()))), 1f);
        }
    }
    /* ============================================================================
        INTERNAL API METHODS
       ============================================================================ */

    //  VALIDITY CHECK
    private static class Validator {

        private static void log(String type, String id) {
            // guarantees deterministic behavior regardless of the user’s system locale?
            String label = type.toUpperCase(Locale.ROOT);

            if (!Settings.DEBUG_LOOTER) {
                Global.getLogger(Looter.class).error(
                        String.format("[LOOTER] INVALID %s: '%s'", label, id)
                );
            } else {
                Global.getLogger(Looter.class).warn(
                        String.format("[LOOTER] Invalid %s ID skipped: '%s'", label, id)
                );
            }
        }
        static boolean weapon(String id) {
            boolean exists = Global.getSettings().getWeaponSpec(id) != null;
            if (!exists) log("weapon", id);
            return exists;
        }
        static boolean wing(String id) {
            boolean exists = Global.getSettings().getFighterWingSpec(id) != null;
            if (!exists) log("wing", id);
            return exists;
        }
        static boolean commodity(String id) {
            boolean exists = Global.getSettings().getCommoditySpec(id) != null;
            if (!exists) log("commodity", id);
            return exists;
        }
        static boolean special(String id) {
            boolean exists = Global.getSettings().getSpecialItemSpec(id) != null;
            if (!exists) log("special", id);
            return exists;
        }
        static boolean hullmod(String id) {
            boolean exists = Global.getSettings().getHullModSpec(id) != null;
            if (!exists) log("hullmod", id);
            return exists;
        }
        static boolean shipVariant(String id) {
            boolean exists = Global.getSettings().getVariant(id) != null;
            if (!exists) log("ship variant", id);
            return exists;
        }
    }
    //  LOOT GENERATION LOGIC
    private static void rollBasics(CargoAPI cargo, float multiplier) {
        //cargo.addCrew(scale(50, 100, multiplier));
        cargo.addFuel(scale(100, 300, multiplier));
        cargo.addSupplies(scale(50, 150, multiplier));
        cargo.addMarines(scale(30, 60, multiplier));
    }
    private static void rollLoot(LootTable table, CargoAPI cargo, int count) {
        for (int i = 0; i < count; i++) {
            LootEntry entry = table.pick();
            if (entry == null) continue;

            try {
                switch (table.type) {
                    case COMMODITY -> {
                        if (Validator.commodity(entry.id)) {
                            cargo.addCommodity(entry.id, entry.rollQuantity());
                        }
                    }
                    case WEAPON -> {
                        if (Validator.weapon(entry.id)) {
                            cargo.addWeapons(entry.id, entry.rollQuantity());
                        }
                    }
                    case WING -> {
                        if (Validator.wing(entry.id)) {
                            cargo.addFighters(entry.id, entry.rollQuantity());
                        }
                    }
                    case SPECIAL -> {
                        if (Validator.special(entry.id)) {
                            cargo.addSpecial(new SpecialItemData(entry.id, null), entry.rollQuantity());
                        }
                    }
                    case HULLMOD -> {
                        if (Validator.hullmod(entry.id)) {
                            cargo.addHullmods(entry.id, entry.rollQuantity());
                        }
                    }
                }
            } catch (Exception e) {
                Global.getLogger(Looter.class).warn(
                        String.format("[LOOTER] Failed to add %s '%s': %s",
                                table.type, entry.id, e.getMessage())
                );
            }
        }
    }
    private static void rollShips(CargoAPI cargo, int count) {
        if (count <= 0) return;

        // Ensure mothballed data exists
        //cargo.initMothballedShips(factionId);

        for (int i = 0; i < count; i++) {
            LootEntry entry = SHIPS.pick();
            if (entry == null) continue;

            // Validate ship variant exists
            if (!Validator.shipVariant(entry.id)) continue;

            try {
                // Add via the official API method
                cargo.addMothballedShip(
                        FleetMemberType.SHIP,
                        entry.id,
                        null // optional name (would affect all)
                );
                // Retrieve and customize the ship
                List<FleetMemberAPI> ships = cargo.getMothballedShips().getMembersListCopy();
                if (ships.isEmpty()) continue;

                FleetMemberAPI fm = ships.get(ships.size() - 1);
                DModManager.addDMods(fm, true, 4, new Random());
                fm.getRepairTracker().setCR(0.4f); // Low CR for derelicts

            } catch (Exception ex) {
                Global.getLogger(Looter.class).warn(
                        String.format("[LOOTER] Failed to add ship '%s': %s",
                                entry.id, ex.getMessage())
                );
            }
        }
    }
    //  UTILITIES
    private static int randInt(int min, int max) {
        if (max < min) return min;
        return min + random.nextInt(max - min + 1);
    }
    private static int scale(int min, int max, float multiplier) {
        int scaledMin = Math.round(min * multiplier);
        int scaledMax = Math.round(max * multiplier);
        return randInt(scaledMin, scaledMax);
    }
    /** =================================================================
     *   LOOT TABLES - ORGANIZED BY CATEGORY
        ================================================================= */
    private static final LootTable COMMODITIES = LootTable.of(LootType.COMMODITY,

            // In-house
            LootEntry.of("cig_c", Rarity.T0, 50, 250).mod("addiction"),
            LootEntry.of("cigar_c", Rarity.T2, 50, 250).mod("addiction"),
            LootEntry.of("coffee_c", Rarity.T1, 50, 250).mod("addiction"),
            LootEntry.of("kush_c", Rarity.T3, 50, 250).mod("addiction"),

            // Common staples
            LootEntry.of("food", Rarity.T1, 10, 100),
            LootEntry.of("organics", Rarity.T1, 10, 100),
            LootEntry.of("metals", Rarity.T1, 10, 100),
            LootEntry.of("ore", Rarity.T1, 10, 100),
            LootEntry.of("rare_ore", Rarity.T2, 5, 50),
            LootEntry.of("rare_metals", Rarity.T2, 5, 50),
            LootEntry.of("volatiles", Rarity.T2, 5, 50),
            LootEntry.of("heavy_machinery", Rarity.T2, 5, 50),
            LootEntry.of("domestic_goods", Rarity.T3, 5, 50),

            // Rare goods
            LootEntry.of("organs", Rarity.T4, 3, 30),
            LootEntry.of("drugs", Rarity.T4, 5, 50),
            LootEntry.of("hand_weapons", Rarity.T4, 5, 50),
            LootEntry.of("luxury_goods", Rarity.T4, 5, 50),
            LootEntry.of("lobster", Rarity.T4, 5, 50),
            LootEntry.of("ships", Rarity.T4, 5, 50),

            // AI cores
            LootEntry.of("ai_cores", Rarity.T4),
            LootEntry.of("alpha_core", Rarity.T4),
            LootEntry.of("beta_core", Rarity.T3),
            LootEntry.of("gamma_core", Rarity.T2),
            LootEntry.of("omega_core", Rarity.T5),

            // (AoTD)
            LootEntry.of("advanced_components", Rarity.T2, 1, 50).mod("aotd_vok"),
            LootEntry.of("refined_metal", Rarity.T2, 1, 50).mod("aotd_vok"),
            LootEntry.of("purified_rare_metal", Rarity.T3, 1, 50).mod("aotd_vok"),
            LootEntry.of("research_databank", Rarity.T2, 1, 10).mod("aotd_vok"),
            LootEntry.of("domain_heavy_machinery", Rarity.T3, 1, 50).mod("aotd_vok"),
            LootEntry.of("compound", Rarity.T3, 1, 100).mod("aotd_vok"),
            LootEntry.of("wwlb_cerulean_vapors", Rarity.T3, 1, 100).mod("aotd_vok"),

            // (Substance Abuse)
            LootEntry.of("alcoholism_stout_c", Rarity.T3, 10, 100).mod("alcoholism"),
            LootEntry.of("alcoholism_absynth_c", Rarity.T3, 10, 100).mod("alcoholism"),
            LootEntry.of("alcoholism_tears_c", Rarity.T1, 10, 100).mod("alcoholism"),
            LootEntry.of("alcoholism_fuel_c", Rarity.T1, 10, 100).mod("alcoholism"),
            LootEntry.of("alcoholism_blood_c", Rarity.T1, 10, 100).mod("alcoholism"),
            LootEntry.of("alcoholism_sunshine_c", Rarity.T2, 10, 100).mod("alcoholism"),
            LootEntry.of("alcoholism_king_c", Rarity.T1, 10, 100).mod("alcoholism"),
            LootEntry.of("alcoholism_freedom_c", Rarity.T2, 10, 100).mod("alcoholism"),
            LootEntry.of("alcoholism_water_c", Rarity.T3, 10, 100).mod("alcoholism"),
            LootEntry.of("alcoholism_tea_c", Rarity.T3, 10, 100).mod("alcoholism")
    );

    private static final LootTable WEAPONS = LootTable.of(LootType.WEAPON,
            // Common vanilla ballistics
            LootEntry.of("lightmg", Rarity.T1, 1, 3),
            LootEntry.of("heavymg", Rarity.T1, 1, 3),
            // Uncommon vanilla
            LootEntry.of("lightac", Rarity.T3, 1, 3),
            LootEntry.of("lightmortar", Rarity.T3, 1, 3),
            LootEntry.of("heavyac", Rarity.T3, 1, 3),
            LootEntry.of("heavymortar", Rarity.T3, 1, 3),
            LootEntry.of("mininglaser", Rarity.T1, 1, 3),
            LootEntry.of("miningblaster", Rarity.T2, 1, 3),

            // Nuclear Weapons
            LootEntry.of("nuclearwarheadlauncher1", Rarity.T1, 1, 2).mod("NuclearWeapons"),
            LootEntry.of("nuclearwarheadlauncher1_guided", Rarity.T1, 1, 2).mod("NuclearWeapons"),

            // AotD
            LootEntry.of("gungnir_railgun", Rarity.T3, 1, 2).mod("aotd_vok"),
            LootEntry.of("aotd_shadowlance", Rarity.T4, 1, 1).mod("aotd_vok"),
            LootEntry.of("aotd_shroud_railgun", Rarity.T4, 1, 1).mod("aotd_vok"),

            // SuperWeapons - Rare T
            LootEntry.of("sw_tartarus", Rarity.T3, 1, 2).mod("superweapons"),
            LootEntry.of("sw_terror", Rarity.T3, 1, 2).mod("superweapons"),
            LootEntry.of("sw_prismdriver", Rarity.T3, 1, 2).mod("superweapons"),
            LootEntry.of("sw_tesla_coil", Rarity.T3, 1, 2).mod("superweapons"),

            // SuperWeapons - Ultra rare
            LootEntry.of("sw_fusionbeam", Rarity.T3, 1, 2).mod("superweapons"),
            LootEntry.of("sw_gauss", Rarity.T3, 1, 2).mod("superweapons"),
            LootEntry.of("sw_photonlense", Rarity.T3, 1, 2).mod("superweapons"),

            // SuperWeapons - Mythic
            LootEntry.of("sw_lightspear", Rarity.T4, 1, 1).mod("superweapons"),
            LootEntry.of("sw_devastator", Rarity.T4, 1, 1).mod("superweapons"),
            LootEntry.of("sw_railgun", Rarity.T4, 1, 1).mod("superweapons"),
            LootEntry.of("sw_hyperpulse", Rarity.T4, 1, 1).mod("superweapons"),
            LootEntry.of("sw_novax", Rarity.T4, 1, 1).mod("superweapons"),

            // SuperWeapons - Legendary
            LootEntry.of("sw_phalanx", Rarity.T5, 1, 1).mod("superweapons"),
            LootEntry.of("sw_plasmastorm", Rarity.T5, 1, 1).mod("superweapons"),
            LootEntry.of("sw_bfg", Rarity.T5, 1, 1).mod("superweapons"),

            // SuperWeapons - Support
            LootEntry.of("sw_blackhole", Rarity.T5, 1, 1).mod("superweapons"),
            LootEntry.of("sw_supernova", Rarity.T5, 1, 1).mod("superweapons"),
            LootEntry.of("sw_reflective_shield", Rarity.T4, 1, 1).mod("superweapons"),
            LootEntry.of("sw_clustertorpedo", Rarity.T3, 1, 2).mod("superweapons"),
            LootEntry.of("sw_lightning_storm", Rarity.T3, 1, 2).mod("superweapons")
    );

    private static final LootTable WINGS = LootTable.of(LootType.WING,
            // Common vanilla fighters
            LootEntry.of("talon_wing", Rarity.T1),
            LootEntry.of("broadsword_wing", Rarity.T1),

            // Uncommon vanilla
            LootEntry.of("thunder_wing", Rarity.T3),
            LootEntry.of("gladius_wing", Rarity.T3),
            LootEntry.of("mining_drone_wing", Rarity.T3),

            // Rare vanilla
            LootEntry.of("borer_wing", Rarity.T4),

            // Modded wings (II_BG)
            LootEntry.of("trident_ii_wing", Rarity.T1).mod("II_BG"),
            LootEntry.of("broadsword_ii_wing", Rarity.T3).mod("II_BG"),
            LootEntry.of("warthog_ii_wing", Rarity.T3).mod("II_BG")
    );

    private static final LootTable SHIPS = LootTable.of(LootType.SHIP,
            // Common vanilla destroyers/frigates
            LootEntry.of("lasher_Standard", Rarity.T1),
            LootEntry.of("lasher_Assault", Rarity.T1),
            LootEntry.of("falcon_Attack", Rarity.T1),
            LootEntry.of("eagle_Assault", Rarity.T1),

            // Uncommon vanilla variants
            LootEntry.of("wolf_Assault", Rarity.T2),
            LootEntry.of("wolf_Strike", Rarity.T2),
            LootEntry.of("wolf_Overdriven", Rarity.T2),

            // Rare modded ships (II_BG) - extensive collection
            LootEntry.of("champion_ii_Assault", Rarity.T3).mod("II_BG"),
            LootEntry.of("champion_ii_Elite", Rarity.T3).mod("II_BG"),
            LootEntry.of("champion_ii_Martyr", Rarity.T3).mod("II_BG"),
            LootEntry.of("champion_ii_Support", Rarity.T3).mod("II_BG"),
            LootEntry.of("condor_iia_Support", Rarity.T3).mod("II_BG"),
            LootEntry.of("conquest_ii_Assault", Rarity.T3).mod("II_BG"),
            LootEntry.of("conquest_ii_Elite", Rarity.T3).mod("II_BG"),
            LootEntry.of("conquest_ii_Martyr", Rarity.T3).mod("II_BG"),
            LootEntry.of("eagle_ii_Elite", Rarity.T3).mod("II_BG"),
            LootEntry.of("eagle_ii_Martyr", Rarity.T3).mod("II_BG"),
            LootEntry.of("eagle_ii_Support", Rarity.T3).mod("II_BG"),
            LootEntry.of("hammerhead_ii_Artillery", Rarity.T3).mod("II_BG"),
            LootEntry.of("hammerhead_ii_Martyr", Rarity.T3).mod("II_BG"),
            LootEntry.of("hammerhead_ii_Standard", Rarity.T3).mod("II_BG"),
            LootEntry.of("invictus_ii_Standard", Rarity.T3).mod("II_BG"),
            LootEntry.of("kite_iia_Hunter", Rarity.T3).mod("II_BG"),
            LootEntry.of("kite_iia_Martyr", Rarity.T3).mod("II_BG"),
            LootEntry.of("kite_iia_Support", Rarity.T3).mod("II_BG"),
            LootEntry.of("shepherd_iia_Support", Rarity.T3).mod("II_BG"),
            LootEntry.of("sunder_ii_Dueling", Rarity.T3).mod("II_BG"),
            LootEntry.of("sunder_ii_Support", Rarity.T3).mod("II_BG"),
            LootEntry.of("venture_iia_Standard", Rarity.T3).mod("II_BG"),
            LootEntry.of("venture_iia_Support", Rarity.T3).mod("II_BG")
    );

    private static final LootTable SPECIALS = LootTable.of(LootType.SPECIAL,
            // In-house
            LootEntry.of("cig_item", Rarity.T1).mod("addiction"),
            LootEntry.of("cigar_item", Rarity.T3).mod("addiction"),
            LootEntry.of("coffee_item", Rarity.T2).mod("addiction"),
            LootEntry.of("kush_item", Rarity.T4).mod("addiction"),

            // Industry items - uncommon to ultra
            LootEntry.of("corrupted_nanoforge", Rarity.T3),
            LootEntry.of("pristine_nanoforge", Rarity.T4),
            LootEntry.of("synchrotron", Rarity.T4),
            LootEntry.of("orbital_fusion_lamp", Rarity.T4),
            LootEntry.of("coronal_portal", Rarity.T4),
            LootEntry.of("mantle_bore", Rarity.T4),
            LootEntry.of("catalytic_core", Rarity.T3),
            LootEntry.of("soil_nanites", Rarity.T3),
            LootEntry.of("biofactory_embryo", Rarity.T3),
            LootEntry.of("fullerene_spool", Rarity.T3),
            LootEntry.of("plasma_dynamo", Rarity.T3),
            LootEntry.of("cryoarithmetic_engine", Rarity.T3),
            LootEntry.of("drone_replicator", Rarity.T3),
            LootEntry.of("dealmaker_holosuite", Rarity.T3),

            // Blueprint packages - all ultra rare
            LootEntry.of("low_tech_package", Rarity.T4),
            LootEntry.of("midline_package", Rarity.T4),
            LootEntry.of("high_tech_package", Rarity.T4),
            LootEntry.of("missile_package", Rarity.T4),
            LootEntry.of("pirate_package", Rarity.T4),
            LootEntry.of("LP_package", Rarity.T4),
            LootEntry.of("LC_package", Rarity.T4),
            LootEntry.of("XIV_package", Rarity.T4),
            LootEntry.of("heg_aux_package", Rarity.T4),

            // Modded special items (AOTD)
            LootEntry.of("aotd_shrouded_nanoforge", Rarity.T4).mod("aotd_vok"),
            LootEntry.of("aotd_shrouded_synchrotron", Rarity.T4).mod("aotd_vok"),
            LootEntry.of("aotd_atmospheric_driver", Rarity.T4).mod("aotd_vok"),
            LootEntry.of("aotd_shrouded_catalytic_core", Rarity.T4).mod("aotd_vok")
    );

    private static final LootTable HULLMODS = LootTable.of(LootType.HULLMOD,
            // In-House
            LootEntry.of("radiation_shielding", Rarity.T0),

            // Common logistics vanilla
            LootEntry.of("additional_berthing", Rarity.T1),
            LootEntry.of("augmentedengines", Rarity.T1),
            LootEntry.of("auxiliary_fuel_tanks", Rarity.T1),
            LootEntry.of("efficiency_overhaul", Rarity.T1),
            LootEntry.of("expanded_cargo_holds", Rarity.T1),
            LootEntry.of("hiressensors", Rarity.T1),
            LootEntry.of("surveying_equipment", Rarity.T1),

            // Uncommon vanilla
            LootEntry.of("insulatedengine", Rarity.T3),
            LootEntry.of("militarized_subsystems", Rarity.T3),
            LootEntry.of("solar_shielding", Rarity.T3),

            // Rare vanilla
            LootEntry.of("autorepair", Rarity.T3),

            // Modded - Aptly Simple Hullmods
            LootEntry.of("ash_advance_radar_systems", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_circuit_breakers", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_convicted_crewmates", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_external_cargo_holds", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_external_cryo_pods", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_fuel_ramscoop", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_industrial_machine_forge", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_marine_training_facility", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_mineral_refinery", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_reactor_bay", Rarity.T2).mod("aptlysimplehullmods"),
            LootEntry.of("ash_supplies_recycler", Rarity.T2).mod("aptlysimplehullmods"),

            // Modded - Yunru Hullmods
            LootEntry.of("yunru_quickstart", Rarity.T2).mod("yunruhullmods"),
            LootEntry.of("yunru_ramburn", Rarity.T2).mod("yunruhullmods"),
            LootEntry.of("yunru_drive_efficiency", Rarity.T2).mod("yunruhullmods"),
            LootEntry.of("yunru_drive_optimisation", Rarity.T2).mod("yunruhullmods"),
            LootEntry.of("yunru_et", Rarity.T2).mod("yunruhullmods")
    );
}