package bugatti.utils;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;

import java.util.Arrays;
import java.util.Random;

/**
 * Decorate a single inactive gate with Domain-era clutter:
 * - Debris ring
 * - Derelict ships (via DerelictBuilder)
 * - Probes & survey ship
 * - Small cache
 * - Optional gate hauler or mothership
 */
public class Decolicter {

    // Track what we've spawned globally to ensure at least one of each
    private static boolean hasSpawnedMother = false;
    private static boolean hasSpawnedHauler = false;
    private static int gatesDecorated = 0;
    private static final int totalGates = 2; // Newton, Charon

    private static SectorEntityToken spawnDerelict(StarSystemAPI system, SectorEntityToken focus, Random random, String type, String name, float radius, float days) {
        SectorEntityToken e = system.addCustomEntity(Misc.genUID(), name, type, Factions.NEUTRAL);
        e.setDiscoverable(true);
        e.setCircularOrbit(focus, random.nextFloat() * 360f, radius, days);
        return e;
    }

    public static void spawnDecolict(StarSystemAPI system, SectorEntityToken entity) {
        Random random = new Random(entity.getId().hashCode() + System.currentTimeMillis());
        gatesDecorated++;

        // DEBRIS RING
        DebrisFieldTerrainPlugin.DebrisFieldParams debrisParams =
                new DebrisFieldTerrainPlugin.DebrisFieldParams(1000f, 0.5f, 999999f, 0f);

        debrisParams.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
        debrisParams.name = "Derelict Breakfield";
        debrisParams.middleRadius = 500f;
        debrisParams.defFaction = Factions.DERELICT;
        debrisParams.defenderProb = 100f;
        debrisParams.minStr = 30;
        debrisParams.maxStr = 50;
        debrisParams.maxDefenderSize = 100;

        SectorEntityToken debris = Misc.addDebrisField(system, debrisParams, random);
        debris.addTag(Tags.DEBRIS_FIELD);
        debris.setDiscoverable(true);
        debris.getMemoryWithoutUpdate().set("$hasDefenders", true);
        debris.setCircularOrbit(entity, random.nextFloat() * 360f, 600f, 2600f);

        // DERELICT SHIPS
        for (int i = 0; i < 6; i++) {
            DerelictBuilder.addRandomDerelict(system, entity, random.nextFloat() * 360f, 550f + random.nextFloat() * 150f, null);
        }
        // PROBE, CACHE, SURVEY SHIP, ETC
        var a = spawnDerelict(system, entity, random, "large_cache", null, 500f, 2000f);
        var b = spawnDerelict(system, entity, random, "supply_cache", null, 1100f, 3000f);
        var c = spawnDerelict(system, entity, random, "derelict_probe", null, 650f, 2000f);
        var d = spawnDerelict(system, entity, random, "derelict_survey_ship", null, 750f, 2300f);
        for (SectorEntityToken sectorEntityToken : Arrays.asList(a, b, c, d)) {
            UniHelp.setDiscoverable(sectorEntityToken);
        }

        boolean isGate = entity.hasTag(Tags.GATE)
                || "inactive_gate".equals(entity.getCustomEntityType());

        if (!isGate) return;
        // GUARANTEED ROLL
        // if last gate and haven't spawned something, force it
        boolean forceMothership = !hasSpawnedMother && gatesDecorated >= totalGates;
        boolean forceHauler = !hasSpawnedHauler && gatesDecorated >= totalGates;

        // Decide what to spawn at a gate (only one or the other)
        boolean spawnMothership = false;
        boolean spawnHauler = false;

        if (forceMothership) {
            spawnMothership = true;
        } else if (forceHauler) {
            spawnHauler = true;
        } else {
            // random choice, but preference
            float mothershipChance = hasSpawnedMother ? 0.25f : 0.5f;
            float haulerChance = hasSpawnedHauler ? 0.25f : 0.5f;

            float roll = random.nextFloat();
            if (roll < mothershipChance && !hasSpawnedHauler) {
                spawnMothership = true;
            } else if (roll < (mothershipChance + haulerChance)) {
                spawnHauler = true;
            }
        }
        // SPAWN MOTHERSHIP
        if (spawnMothership) {
            SectorEntityToken mother = spawnDerelict(system, entity, random,
                    "derelict_mothership", "Domain-era Mothership", 900f, 3500f);
            mother.setSensorProfile(700f);
            mother.setDiscoverable(true);
            hasSpawnedMother = true;
        }

        // SPAWN GATE HAULER
        if (spawnHauler) {
            SectorEntityToken hauler = spawnDerelict(system, entity, random,
                    "derelict_gatehauler", "Domain-era Gate Hauler", 1300f, 4200f);
            hauler.setSensorProfile(700f);
            hauler.setDiscoverable(true);
            hauler.getMemoryWithoutUpdate().set("$gateHauler", true);
            hasSpawnedHauler = true;
        }
    }
    // to reset the state (for new games/testing)
    public static void decoReset() {
        hasSpawnedMother = false;
        hasSpawnedHauler = false;
        gatesDecorated = 0;
    }
}
