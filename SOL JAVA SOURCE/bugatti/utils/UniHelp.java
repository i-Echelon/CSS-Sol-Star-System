package bugatti.utils;
import bugatti.systems.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;
import java.awt.*;

public class UniHelp { // CORE ENTITY HELPERS

    private static final Logger log = Global.getLogger(UniHelp.class);

    // Lightweight record for entities
    public record EntitySpec(String id, String name, String type, String faction, float angle, float orbitRadius, float orbitDays, String special) {}
    // CUSTOM ENTITIES
    public static CustomCampaignEntityAPI doCustomEntity(StarSystemAPI sys, SectorEntityToken focus, String id, String name, String type, String factionId, float angle, float orbitRadius, float orbitDays) {
        CustomCampaignEntityAPI e = sys.addCustomEntity(id, name, type, factionId);
        setOrbit(e, focus, angle, orbitRadius, orbitDays);
        setDiscoverable(e);
        return e;
    }
    // PlANET BUILDER
    public static PlanetAPI doPlanet(StarSystemAPI sys, SectorEntityToken focus, String id, String name, String type, float angle, float radius, float orbitRadius, float orbitDays) {
        PlanetAPI p = sys.addPlanet(id, focus, name, type, angle, radius, orbitRadius, orbitDays);
        // market check
        MarketAPI market = p.getMarket();
        if (market != null) {market.setSurveyLevel(MarketAPI.SurveyLevel.NONE);
        }   return p;
    }
    // MARKET HELPERS
    public static MarketAPI craftMarket(PlanetAPI planet, String factionId, int size, String... baseConditions) {
        String id = planet.getId() + "_market";

        MarketAPI market = Global.getFactory().createMarket(id, planet.getName(), size);
        market.setFactionId(factionId);
        market.setPrimaryEntity(planet);
        market.setPlanetConditionMarketOnly(true);
        // Only apply guaranteed, non-modded conditions here
        if (baseConditions != null) {
            for (String c : baseConditions) {
                market.addCondition(c);
            }
        }

        planet.setMarket(market);
        return market; // IMPORTANT: return it so you can attach modded conditions afterward
    }
    // JUMP HELPERS
    public static void craftJumpPoint(StarSystemAPI sys, SectorEntityToken focus, String id, String name, float angle, float orbitRadius, float orbitDays) {
        JumpPointAPI jp = Global.getFactory().createJumpPoint(id, name);
        jp.setStandardWormholeToHyperspaceVisual();
        //jp.setStandardWormholeToStarfieldVisual();
        jp.setOrbit(Global.getFactory().createCircularOrbit(focus, angle, orbitRadius, orbitDays));
        //jp.setAutoCreateEntranceFromHyperspace(false);
        sys.addEntity(jp);
    }
    public static void doJumpPoints(StarSystemAPI sys, SectorEntityToken focus, Object[][] jumps) {
        for (Object[] j : jumps) {
            craftJumpPoint(sys,
                    focus,
                    (String) j[0],   // id
                    (String) j[1],   // name
                    (float) j[2],    // angle
                    (float) j[3],    // radius
                    (float) j[4]     // days
            );
        }
        // gen hyperspace entrance point if needed
        sys.autogenerateHyperspaceJumpPoints(false, false, false);
    }
    // SOLAR ARRAY SYSTEM (Shades + Mirrors)
    public static void doSolarArray(StarSystemAPI sys, PlanetAPI planet, int shades, int mirrors) {
        var faction = planet.getFaction().getId();

        var planetAngle  = planet.getCircularOrbitAngle();
        var planetPeriod = planet.getCircularOrbitPeriod();
        var baseRadius = 270f + planet.getRadius();

        doOrbitalGroup(sys, planet, faction, Entities.STELLAR_SHADE,  "Stellar Shade",  shades,
                planetAngle + 180f, baseRadius + 25, planetPeriod, 25);

        doOrbitalGroup(sys, planet, faction, Entities.STELLAR_MIRROR, "Stellar Mirror", mirrors,
                planetAngle, baseRadius, planetPeriod, 30);
    }
    // SHADE/MIRROR GENERATOR - (1, 3, or 5 items symmetrically around the planet)
    public static void doOrbitalGroup(StarSystemAPI sys, PlanetAPI planet, String factionId, String entityType, String name,
                                      int count, float baseAngle, float baseRadius, float orbitDays, float spread) {
        if (count <= 0) return;
        // always place the central item
        doCustomEntity(sys, planet, null, name, entityType, factionId, baseAngle, baseRadius, orbitDays);

        if (count >= 3) {
            doCustomEntity(sys, planet, null, name, entityType, factionId, baseAngle - spread, baseRadius, orbitDays);
            doCustomEntity(sys, planet, null, name, entityType, factionId, baseAngle + spread, baseRadius, orbitDays);
        }
        if (count == 5) {
            doCustomEntity(sys, planet, null, name, entityType, factionId, baseAngle - spread * 2, baseRadius, orbitDays);
            doCustomEntity(sys, planet, null, name, entityType, factionId, baseAngle + spread * 2, baseRadius, orbitDays);
        }
    }
    // ASTEROID FIELDS
    public static void doAsteroidField(StarSystemAPI sys, SectorEntityToken focus, float centerAngle, float arcWidth, float innerRadius, float outerRadius, int count, String name) {
        //  Computes actual band area: π(R² − r²) | Multiplies by a density constant | Clamped 10–100 for performance sanity
        if (count < 0) {
            float area = (float) (Math.PI * (outerRadius * outerRadius - innerRadius * innerRadius));
            float density = 0.0001f;   // sparse. tweak more/fewer asteroids .00015 normal, .0025 heavy
            count = Math.min(100, Math.max(10, Math.round(area * density)));
        }
        // Create terrain params
        AsteroidFieldTerrainPlugin.AsteroidFieldParams params =
                new AsteroidFieldTerrainPlugin.AsteroidFieldParams(innerRadius, outerRadius, count, count, 4f, 16f, name);

        SectorEntityToken field = sys.addTerrain(Terrain.ASTEROID_FIELD, params);
        // Orbit distance (center of the band)
        float orbitRadius = (innerRadius + outerRadius) * 0.5f;
        // Very slow rotation
        float orbitDays = 99999f;

        field.setCircularOrbit(focus, centerAngle, orbitRadius, orbitDays);
        // Tagging optional
        field.addTag("sol_asteroid_field");
    }
    // MAGNETIC FIELDS
    public static void doMagneticField(StarSystemAPI sys, PlanetAPI planet, float innerRadius, float outerRadius, Color... colors) {
        // Fallback color set if none provided
        final Color[] finalColors = grabColors(colors);

        MagneticFieldTerrainPlugin.MagneticFieldParams params =
                new MagneticFieldTerrainPlugin.MagneticFieldParams(
                        planet.getRadius() + (outerRadius - innerRadius) / 2f,
                        (innerRadius + outerRadius) / 2f,
                        planet,
                        innerRadius,
                        outerRadius,
                        finalColors[0], 0.5f,
                        finalColors[1], finalColors[2],
                        finalColors[3], finalColors[4],
                        finalColors[5], finalColors[6],
                        finalColors.length > 7 ? finalColors[7] : new Color(80, 170, 255, 35)
                );
        SectorEntityToken field = sys.addTerrain(Terrain.MAGNETIC_FIELD, params);
        // Put field in small orbit around the planet (cosmetic)
        field.setCircularOrbit(planet, 0f, planet.getRadius() + 10f, 100f);

    }
    // BEACON
    public static void doBeacon(StarSystemAPI sys) {
        sys.getHyperspaceAnchor();
        RemnantThemeGenerator.addBeacon(sys,
                RemnantThemeGenerator.RemnantSystemType.RESURGENT);
    }
    // NEBULA
    public static void doNebula(StarSystemAPI sys, float x, float y) {
        //System Nebula
        SectorEntityToken eos1 = Misc.addNebulaFromPNG("data/campaign/terrain/eos_nebula.png",
                0f, 1500f, sys, "terrain", "nebula", 4, 4, StarAge.AVERAGE);
        eos1.setLocation(x, y);
        // Add hyperspace nebula around the whole sys === (we're using the above)
        //StarSystemGenerator.addSystemwideNebula(sys, sys.getAge());
    }
    // CONSTELLATION BUILDER
    public static Constellation doConstellation(StarSystemAPI sys, String constellationName) {
        Constellation constellation = new Constellation(Constellation.ConstellationType.NORMAL, StarAge.OLD);
        // accept dynamic names
        NameGenData nameData = new NameGenData(constellationName, constellationName);
        nameData.addTag(NameGenData.TAG_CONSTELLATION);

        ProcgenUsedNames.NamePick namePick = new ProcgenUsedNames.NamePick(nameData, constellationName, constellationName);
        constellation.setNamePick(namePick);
        //constellation.setNameOverride("Orion Spur");

        // Add sys to constellation both ways
        constellation.getSystems().add(sys);
        sys.setConstellation(constellation);
        // Force the system to report as "in constellation" via memory flag
        sys.getMemoryWithoutUpdate().set("$isInConstellation", true);

        return constellation;
    }

    /**
     * Creates a constellation with a hidden anchor system to satisfy the >1 system requirement
     * This allows single visible systems to still show constellation names on and while traversing the map
     */
    public static Constellation doConstellationAnchor(StarSystemAPI mainSys, String constellationName) {
        var sector = Global.getSector();
        // do main con
        Constellation constellation = new Constellation(Constellation.ConstellationType.NORMAL, StarAge.AVERAGE);

        NameGenData nameData = new NameGenData(constellationName, constellationName);
        //nameData.addTag(NameGenData.TAG_CONSTELLATION);
        ProcgenUsedNames.NamePick namePick = new ProcgenUsedNames.NamePick(nameData, constellationName, constellationName);
        constellation.setNamePick(namePick);
        // Attach both ways
        constellation.getSystems().add(mainSys);
        mainSys.setConstellation(constellation);
        // (satisfy size > 1 requirement)
        var anchor = sector.createStarSystem(constellationName + "_anchor");
        anchor.getLocation().set(mainSys.getLocation().x + 1f, mainSys.getLocation().y + 1f); // slight nudge
        anchor.setProcgen(false);
        anchor.addTag(Tags.THEME_HIDDEN);
        anchor.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        // off da grid
        var hiddenCenter = anchor.initNonStarCenter();
        hiddenCenter.setDiscoverable(false);
        hiddenCenter.setSensorProfile(0f);
        // pair anchor & con
        constellation.getSystems().add(anchor);
        anchor.setConstellation(constellation);

        Global.getLogger(UniHelp.class).info(String.format("[CONSTELLATION] Created '%s' with anchor for %s",constellationName, mainSys.getName()));

        return constellation;
    }
    public static void genConstellationSibling(SectorAPI sector, StarSystemAPI primary, String siblingId) {
        var sibling = sector.createStarSystem(siblingId);

        // Same hyperspace coordinates
        sibling.getLocation().set(primary.getLocation());

        sibling.setProcgen(false);
        sibling.addTag(Tags.THEME_HIDDEN);

        SectorEntityToken center = sibling.initNonStarCenter();
        center.setDiscoverable(false);
        center.setSensorProfile(0f);

        // Do NOT cut off from hyperspace
    }
    public static void genCentaurusConstellation() {
        var sector = Global.getSector();
        StarSystemAPI alphaSys;
        StarSystemAPI betaSys;

        // Generate Alpha Centauri
        if (sector.getStarSystem("Alpha Centauri") != null) {
            log.warn("[ALPHA] Alpha Centauri already exists! Skipping generation.");
        } else {
            log.info("[ALPHA] Generating Alpha Centauri System...");
            AlphaCentauri.genSys(sector);
        }
        alphaSys = sector.getStarSystem("Alpha Centauri");

        // Generate Beta Centauri
        if (sector.getStarSystem("Beta Centauri") != null) {
            log.warn("[BETA] Beta Centauri already exists! Skipping generation.");
        } else {
            log.info("[BETA] Generating Beta Centauri System...");
            BetaCentauri.genSys(sector);
        }
        betaSys = sector.getStarSystem("Beta Centauri");

        // Create single Centaurus constellation for both systems
        if (alphaSys != null && betaSys != null) {
            var centaurus = UniHelp.doConstellation(alphaSys, "Centaurus");

            // Add Beta to the same constellation
            centaurus.getSystems().add(betaSys);
            betaSys.setConstellation(centaurus);
            betaSys.getMemoryWithoutUpdate().set("$isInConstellation", true);

            log.info(
                    "[CENTAURUS] Created constellation with Alpha & Beta Centauri"
            );
        }
    }

    // LABELS
    /**
     * Creates a hyperspace label near a system
     *
     * @param sys The system to place the label near
     * @param labelId Unique ID for this label entity
     * @param labelName Display name for the label
     * @param angle Angle in degrees from system center (0-360)
     * @param distance Distance from system's hyperspace anchor
     */
    public static void doLabel(StarSystemAPI sys, String labelId, String labelName, float angle, float distance) {
        LocationAPI hyper = Global.getSector().getHyperspace();
        SectorEntityToken anchor = sys.getHyperspaceAnchor();

        if (anchor == null) {
            Global.getLogger(UniHelp.class).warn(
                    String.format("[LABEL] Cannot create label '%s' for %s - no hyperspace anchor found",
                            labelName, sys.getName())
            );
            return;
        }
        // Create the label
        SectorEntityToken label = hyper.addCustomEntity(labelId, labelName, "Sol_Labels", null);
        // Place at the specified angle & distance from system's anchor
        label.setCircularOrbit(anchor, angle, distance, 180f); // semiannual

        Global.getLogger(UniHelp.class).info(
                String.format("[LABEL] Created label '%s' for %s at %.1f degrees and %.0f units",
                        labelName, sys.getName(), angle, distance)
        );
    }
    /**
     * Creates a label pointing toward another system (if it exists)
     */
    public static void doLabelPointer(StarSystemAPI fromSys, String toSystemId, String labelId, String labelName, float offsetDistance) {
        StarSystemAPI toSys = Global.getSector().getStarSystem(toSystemId);

        if (toSys == null) {
            Global.getLogger(UniHelp.class).info(
                    String.format("[LABEL] Skipping label '%s' - target system '%s' not found",
                            labelName, toSystemId)
            );
            return;
        }
        // Calculate angle toward the other system
        Vector2f fromPos = fromSys.getLocation();
        Vector2f toPos = toSys.getLocation();
        float angle = Misc.getAngleInDegrees(fromPos, toPos);
        // Place label at calculated angle
        doLabel(fromSys, labelId, labelName, angle, offsetDistance);

        Global.getLogger(UniHelp.class).info(
                String.format("[LABEL] Created label '%s' on %s pointing toward %s at %.1f degrees",
                        labelName, fromSys.getName(), toSys.getName(), angle)
        );
    }
    // Hyperspace HyperSweep
    public static void doHyperSweep(StarSystemAPI sys) {
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float totalRadius = sys.getMaxRadiusInHyperspace() + plugin.getTileSize() * 1.5f;
        editor.clearArc(sys.getLocation().x, sys.getLocation().y, 0f, totalRadius, 0f, 360f);
        editor.clearArc(sys.getLocation().x, sys.getLocation().y, 0f, totalRadius, 0f, 360f, 0.25f);
    }
    // QUICK ORBIT POINTER
    public static void setOrbit(SectorEntityToken entity, SectorEntityToken focus, float angle, float radius, float days) {
        entity.setCircularOrbitPointingDown(focus, angle, radius, days);
    }
    // DISCOVERY XP
    public static void setDiscoverable(SectorEntityToken e) {
        MiscellaneousThemeGenerator.makeDiscoverable(e, 1000f, 2000f);
        e.setDiscoverable(true);
    }
    // RELATIVE POSITIONING
    /**
     * Calculates position relative to another system using light-years and angle
     * @param anchorSys Reference system (or null for absolute positioning)
     * @param lightYears Distance in light years from anchor
     * @param angleDegrees Angle in degrees from anchor
     * @param fallbackPos Absolute position if anchor is null
     * @return Calculated position vector
     */
    public static Vector2f getRelativePosition(StarSystemAPI anchorSys, float lightYears, float angleDegrees, Vector2f fallbackPos) {
        if (anchorSys == null || anchorSys.getLocation() == null) {
            return new Vector2f(fallbackPos);
        }

        float SCALE = 2000f; // hyperspace units per light year
        float distance = lightYears * SCALE;
        Vector2f anchorPos = anchorSys.getLocation();

        return new Vector2f(
                anchorPos.x + (float) Math.cos(Math.toRadians(angleDegrees)) * distance,
                anchorPos.y + (float) Math.sin(Math.toRadians(angleDegrees)) * distance
        );
    }
    /**
     * Overload: Calculate relative position with no fallback (returns anchor location if null)
     */
    public static Vector2f getRelativePosition(StarSystemAPI anchorSys, float lightYears, float angleDegrees) {
        if (anchorSys == null || anchorSys.getLocation() == null) {
            Global.getLogger(UniHelp.class).warn("[POSITION] Anchor system is null, returning origin");
            return new Vector2f(0f, 0f);
        }
        return getRelativePosition(anchorSys, lightYears, angleDegrees, new Vector2f(0f, 0f));
    }
    /**
     * Finds a clear location near the preferred position, avoiding all existing systems
     * @param preferredLoc Desired location
     * @param minClearance Minimum distance from any other system
     * @return Safe location for system placement
     */
    public static Vector2f circleSearch(Vector2f preferredLoc, float minClearance) {
        SectorAPI sector = Global.getSector();
        Vector2f testLoc = new Vector2f(preferredLoc);
        int maxAttempts = 100;
        int attempt = 0;

        while (attempt < maxAttempts) {
            boolean isClear = true;
            // check against all existing systems
            for (StarSystemAPI existing : sector.getStarSystems()) {
                // skip if checking against self (shouldn't happen, but light check)
                if (existing.getLocation() == null) continue;

                float dist = Misc.getDistance(testLoc, existing.getLocation());
                float requiredDist = minClearance + existing.getMaxRadiusInHyperspace();

                if (dist < requiredDist) {
                    isClear = false;
                    // push away from the conflicting system
                    float angle = Misc.getAngleInDegrees(existing.getLocation(), testLoc);
                    float pushDist = requiredDist - dist + 500f; // Extra buffer

                    testLoc.x += (float) Math.cos(Math.toRadians(angle)) * pushDist;
                    testLoc.y += (float) Math.sin(Math.toRadians(angle)) * pushDist;

                    Global.getLogger(UniHelp.class).info(
                            String.format("[SPACESEARCH] Adjusted %.0f units away from %s",
                                    pushDist, existing.getName())
                    );
                    break; // recheck from new position
                }
            }

            if (isClear) {
                Global.getLogger(UniHelp.class).info(
                        String.format("[SPACESEARCH] Found clear space at (%.0f, %.0f) after %d attempts",
                                testLoc.x, testLoc.y, attempt)
                );
                return testLoc;
            }

            attempt++;
        }
        // Fallback: spiral outward from preferred location
        Global.getLogger(UniHelp.class).warn(
                "[SPACESEARCH] Could not find clear space after " + maxAttempts + " attempts, using spiral search"
        );
        return spiralSearch(preferredLoc, minClearance);
    }
    /**
     * Spiral search pattern for finding clear space (fallback)
     */
    private static Vector2f spiralSearch(Vector2f center, float minClearance) {
        SectorAPI sector = Global.getSector();
        float step = 500f; // distance between spiral checks
        float angle = 0f;
        float radius = minClearance;

        for (int i = 0; i < 200; i++) {
            Vector2f testLoc = new Vector2f(
                    center.x + (float) Math.cos(Math.toRadians(angle)) * radius,
                    center.y + (float) Math.sin(Math.toRadians(angle)) * radius
            );

            boolean isClear = true;
            for (StarSystemAPI existing : sector.getStarSystems()) {
                if (existing.getLocation() == null) continue;

                float dist = Misc.getDistance(testLoc, existing.getLocation());
                if (dist < minClearance + existing.getMaxRadiusInHyperspace()) {
                    isClear = false;
                    break;
                }
            }

            if (isClear) return testLoc;
            // Spiral parameters
            angle += 30f;
            radius += step * 0.1f;
        }
        // Ultimate fallback
        return new Vector2f(center.x + minClearance * 3f, center.y + minClearance * 3f);
    }

    public static Vector2f getSolLocation() {
        var x = -54000.0f;
        var y = -36000.0f;

        if (ModCheck.hasLL) {
            var lx = LunaSettings.getFloat("bugatti.Sol", "sol_pos_x");
            var ly = LunaSettings.getFloat("bugatti.Sol", "sol_pos_y");
            if (lx != null) x = lx;
            if (ly != null) y = ly;
        }

        return UniHelp.circleSearch(new Vector2f(x, y), 6000f);
    }

    // COLOR ARRAY
    public static Color[] grabColors(Color[] colors) {
        Color[] defaultColors = new Color[]{
                new Color(70, 30, 120, 50),
                new Color(100, 40, 55, 195),
                new Color(130, 10, 220, 200),
                new Color(120, 80, 85, 185),
                new Color(85, 100, 120, 180),
                new Color(95, 30, 165, 195),
                new Color(110, 60, 155, 185),
                new Color(140, 155, 100, 170)
        };
        // Ensure we have a full 7–8 colors available
        return (colors != null && colors.length >= 8) ? colors : defaultColors;
    }
}