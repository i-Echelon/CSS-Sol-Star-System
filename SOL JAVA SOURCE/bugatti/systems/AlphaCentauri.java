package bugatti.systems;

import bugatti.plugins.RadiationPlug;
import bugatti.utils.BattleStationBuilder;
import bugatti.utils.ModCheck;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.lwjgl.util.vector.Vector2f;
import static bugatti.utils.UniHelp.*;

public class AlphaCentauri {

    public static void genSys(SectorAPI sec) {
        var sys = sec.createStarSystem("Alpha Centauri");
        sys.setAge(StarAge.OLD);

        // Try to place relative to Sol if it exists
        var solSys = sec.getStarSystem("Sol");
        final var preferredPos = getPrefPos(solSys);

        var actualPos = circleSearch(preferredPos, 8000f);
        sys.getLocation().set(actualPos.x, actualPos.y);

        Global.getLogger(AlphaCentauri.class).info(
                String.format("[ALPHA] Placed Alpha Centauri at (%.0f, %.0f)", actualPos.x, actualPos.y)
        );
        //sys.getLocation().set(-47052f, -37135f);  // ~4.24 ly from Sol @ ~35° angle

        sys.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_sol_system");
        // Create the trinary system
        doTriSys(sys);
        // Create its own constellation
        //var centaurus = doConstellation(sys, "Centaurus");
        // Add planets and features
        addPlanets(sys);
        addFeatures(sys);
        doHyperSweep(sys);
        // Configuration
        configureSys(sys);
    }
    /** Trinary System Generator */
    private static void doTriSys(StarSystemAPI sys) {
        // Use hidden center for multi-star systems
        SectorEntityToken center = sys.initNonStarCenter();
        center.setId("centauri_center");
        sys.setCenter(center);

        float starOrbitRadius = 2000f;
        float starOrbitDays = 29200f;

        // --- Primary Star (Alpha Centauri A)
        var alphaA = sys.addPlanet("cen_alphaA", center, "Alpha Centauri A",
                StarTypes.YELLOW, 90f, 1500f, starOrbitRadius + 3000f, starOrbitDays);
        alphaA.setName("Rigil Kentaurus");
        sys.addCorona(alphaA, 700f, 12f, 0.6f, 3f);
        sys.setStar(alphaA);

        // --- Secondary Star (Alpha Centauri B)
        var alphaB = sys.addPlanet("cen_alphaB", center, "Alpha Centauri B",
                StarTypes.ORANGE, 0f, 1000f, starOrbitRadius + 8000f, starOrbitDays);
        alphaB.setName("Toliman");
        sys.addCorona(alphaB, 550f, 10f, 0.4f, 2f);
        sys.setSecondary(alphaB);

        // --- Tertiary Star (Proxima Centauri)
        var alphaC = sys.addPlanet("cen_alphaC", center, "Proxima Centauri",
                StarTypes.RED_DWARF, 180f, 750f, starOrbitRadius + 18000f, starOrbitDays * 200f);
        sys.addCorona(alphaC, 350f, 8f, 1.0f, 4f);
        sys.setTertiary(alphaC);

        // Mark system as trinary
        sys.setType(StarSystemGenerator.StarSystemType.TRINARY_1CLOSE_1FAR);

        // Irradiation
        var remy = BattleStationBuilder.addRemnantStation(sys, alphaC, "Irradiated Rig", 180f, 1000, 10f, false);
        // Radiation field (500 unit radius from station)
        var remField = RadiationPlug.createRadiationField(alphaC, 1.0f, 1500f, 1.0f);
        remField.getMemoryWithoutUpdate().set("$defendedByStation", remy);
        ((RadiationPlug) remField.getPlugin()).setCrewLossMult(1.5f);
        ((RadiationPlug) remField.getPlugin()).setMarineLossMult(1.5f); // 50% more dangerous
        ((RadiationPlug) remField.getPlugin()).setCrDamageMult(1.5f);
    }
    private static void addPlanets(StarSystemAPI sys) {
        var alphaC = (PlanetAPI) sys.getEntityById("cen_alphaC");

        // Proxima B - habitable world around Proxima Centauri
        var proximaB = doPlanet(sys, alphaC, "cen_proxima_b", "Proxima B",
                "terran-eccentric", 45f, 250f, 2000f, -270f);
        proximaB.setCustomDescriptionId("cen_ProximaB");

        var pm = craftMarket(proximaB, "neutral", 0,
                "habitable", "very_hot", "dense_atmosphere", "extreme_weather",
                "irradiated", "ore_ultrarich", "rare_ore_ultrarich");
        ModCheck.cond(pm, "IndEvo", "IndEvo_ArtilleryStationCondition");

        // Placeholder .. more entities
    }

    private static void addFeatures(StarSystemAPI sys) {
        var center = sys.getEntityById("centauri_center");
        // hyperspace jump-points
        doJumpPoints(sys, center, new Object[][]{
                {"cen_jump1", "Nadir Point", 180f, 24000f, -100000f},
                //{"cen_jump2", "Zenith Point", 45f, 6000f, -100000f},
        });
        // Small nebula
        doNebula(sys, 0f, 0f);
        // Scattered wrecks
        //addWrecksScattered(sys, center);
    }

    private static void configureSys(StarSystemAPI sys) {
        sys.setProcgen(false);
        sys.setBackgroundTextureFilename("graphics/backgrounds/galaxy0.jpg");
        // Tags
        sys.addTag(Tags.THEME_DERELICT);
        sys.addTag(Tags.THEME_HIDDEN);
        sys.addTag(Tags.THEME_INTERESTING_MINOR);
        sys.addTag(Tags.THEME_RUINS);
        sys.addTag(Tags.THEME_SPECIAL);
    }

    private static Vector2f getPrefPos(StarSystemAPI solSys) {
        Vector2f fallback = new Vector2f(-47052f, -37135f);
        return getRelativePosition(solSys, 4.24f, 35f, fallback);
    }
}