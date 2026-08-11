package bugatti.scripts;

import bugatti.utils.Settings;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDMegastructureRules;
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator;
import data.kaysaar.aotd.vok.campaign.econ.globalproduction.impl.bifrost.sections.BifrostSection;
import data.kaysaar.aotd.vok.campaign.econ.globalproduction.impl.bifrost.ui.dialog.BifrostLocationData;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public final class MegaSpawner {

    private static final Logger log = Global.getLogger(MegaSpawner.class);

    private static final String GATE_NAME = "Gaia Transfrost Gate";

    private MegaSpawner() {}

    public static void run(StarSystemAPI sol) {

        var spawnPlouton    = Settings.SPAWN_PLOUTON;
        var spawnGungnir    = Settings.SPAWN_GUNGNIR;
        var spawnTransfrost = Settings.SPAWN_TRANSFROST;
        var spawnTap        = Settings.SPAWN_TAP;

        //boolean instaBuild = "Postfab".equalsIgnoreCase(Settings.FROST_MODE);
        boolean instaBuild = true;

        /* -------------------- ENTITY LOOKUP -------------------- */

        var earth = findPlanet(sol, "sol_earth");
        var mars  = findPlanet(sol, "sol_mars");
        var pluto = findPlanet(sol, "sol_pluto");
        var star = findStar(sol);

        /* -------------------- SPAWNS -------------------- */

        if (spawnTransfrost && earth != null &&
                !earth.getMemoryWithoutUpdate().contains("$sol_transfrost_added")) {
            spawnTransFrost(sol, earth, instaBuild);
        }
        if (spawnPlouton && pluto != null &&
                !pluto.getMemoryWithoutUpdate().contains("$sol_plouton_added")) {
            spawnPlouton(pluto);
        }
        if (spawnGungnir && mars != null &&
                !mars.getMemoryWithoutUpdate().contains("$sol_gungnir_added")) {
            spawnGungnir(mars);
        }
        if (spawnTap && star != null &&
                !star.getMemoryWithoutUpdate().contains("$sol_tap_added")) {
            spawnHypershunt(sol, star);
        }
    }

    /* -------------------- BIFROST SAFE -------------------- */

    private static void spawnTransFrost(StarSystemAPI system, PlanetAPI planet, boolean instaBuild) {

        var bifrost = BifrostBootstrap.getOrCreateNetwork(planet);

        var loc = buildLocation(planet);
        bifrost.addNewBifrostGate(system, loc);

        List<BifrostSection> sections = bifrost.getSections();
        if (sections.isEmpty()) {
            log.error("[SOL] Transfrost section creation failed.");
            return;
        }

        var section = sections.get(sections.size() - 1);

        if (instaBuild) {
            section.setProgressOfRestoration(section.getSpec().daysForRenovation);
            section.setRestoring(false);
            section.isRestored = true;
            section.upkeepMult.setBaseValue(0f);
            section.penaltyFromLackOfResources = 0f;
            section.setDisabled(true);
            section.aboutToReconstructSection();

            section.setGateTiedTo(planet); // needed?
        } else {
            log.error("[SOL] Transfrost instability uncertainty.");
            return;
            //section.startReconstruction();
        }

        renameGate(system);
        planet.getMemoryWithoutUpdate().set("$sol_transfrost_added", true);

        log.info("[SOL] TransFrost Gate spawned (" + "Postfab" + ").");
        //log.info("[SOL] TransFrost Gate spawned (" + (instaBuild ? "Postfab" : "Prefab") + ").");
    }

    /* -------------------- HELPERS -------------------- */

    private static BifrostLocationData buildLocation(PlanetAPI planet) {
        var orbitRadius = planet.getRadius() + 360f;
        var angle = planet.getCircularOrbitAngle() - 60f;
        var rad = (float) Math.toRadians(angle);

        var loc = new Vector2f(
                planet.getLocation().x + orbitRadius * (float) Math.cos(rad),
                planet.getLocation().y + orbitRadius * (float) Math.sin(rad)
        );

        return new BifrostLocationData(planet, orbitRadius, loc, angle);
    }

    private static void renameGate(StarSystemAPI system) {
        for (var e : system.getAllEntities()) {
            if ("bifrost_gate".equals(e.getCustomEntityType())) {
                e.setName(GATE_NAME);
                e.addTag("sol_transfrost");
            }
        }
    }

    private static PlanetAPI findPlanet(StarSystemAPI system, String id) {
        for (PlanetAPI p : system.getPlanets()) {
            if (id.equalsIgnoreCase(p.getId())) return p;
        }
        return null;
    }

    private static SectorEntityToken findStar(StarSystemAPI system) {
        for (var e : system.getAllEntities()) {
            if ("sol_star".equalsIgnoreCase(e.getId())) return e;
        }
        return null;
    }

    /* -------------------- OTHER MEGAS -------------------- */

    private static void spawnPlouton(PlanetAPI planet) {

        var pluton = AoTDMegastructureRules.putMegastructure(planet, "aotd_pluto_station");

        var station =
            planet.getStarSystem().addCustomEntity("sol_plouton_station", "Plutonium Mining Array", "aotd_pluto_station", "neutral");

        if (station != null) {
            var angle = planet.getCircularOrbitAngle();
            var period = planet.getCircularOrbitPeriod();

            station.setCircularOrbitPointingDown(planet, angle, planet.getRadius() + 340f, period);
            station.setCustomDescriptionId("sol_Plouton");
            station.addTag("sol_pluto_mega");
            station.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "aotd_mega");

            MiscellaneousThemeGenerator.makeDiscoverable(station, 25000f, 3000f);
        }

        planet.getMemoryWithoutUpdate().set("$sol_plouton_added", true);
        log.info("[SOL] Plutonium Mining Array spawned.");
    }

    private static void spawnGungnir(PlanetAPI planet) {
        var shipyard = AoTDMegastructureRules.putMegastructure(planet, "aotd_nidavelir");
        var market = planet.getMarket();
        if (market != null) market.setSurveyLevel(MarketAPI.SurveyLevel.SEEN);
        planet.addTag("sol_mars_mega");
        planet.getMemoryWithoutUpdate().set("$sol_gungnir_added", true);
    }

    private static void spawnHypershunt(StarSystemAPI system, SectorEntityToken star) {
        var tap = system.addCustomEntity("sol_sunTap", "Domain-era Hypershunt", "coronal_tap", null);
        tap.setCircularOrbit(star, 270f, 1500f, -999999f);
        tap.addTag("sol_star_mega");
        MiscellaneousThemeGenerator.makeDiscoverable(tap, 30000f, 5000f);
        star.getMemoryWithoutUpdate().set("$sol_tap_added", true);
        system.addTag(Tags.HAS_CORONAL_TAP);
    }
}