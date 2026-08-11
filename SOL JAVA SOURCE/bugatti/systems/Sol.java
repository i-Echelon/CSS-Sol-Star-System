package bugatti.systems;

import bugatti.plugins.RadiationPlug;
import bugatti.utils.*;
import bugatti.scripts.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.loading.specs.PlanetSpec;
import java.awt.Color;
import java.util.*;
import java.util.function.Consumer;


import static bugatti.utils.UniHelp.*;
import static bugatti.utils.DerelictBuilder.*;

public class Sol { // Currently 10 planets + 14 moons
    // MAIN GEN
    public static void genSys(SectorAPI sec) {
        // Create Sol System
        var sys = sec.createStarSystem("Sol");

        // Find clear space dynamically
        var solPos = getSolLocation();
        sys.getLocation().set(solPos);

        Global.getLogger(Sol.class).info(
                String.format("[SOL] Placed Sol at (%.0f, %.0f)", solPos.x, solPos.y)
        );
        //sys.getLocation().set(-54000f, -36000f);

        // Create Sol Star
        var star = sys.initStar("sol_star", "star_yellow", 1250f, 500f);
        PlanetSpec starSpec = (PlanetSpec) star.getSpec();
        // Add Constellation
        var oort = doConstellation(sys, "Oort Cloud");

        // Build Sol planets + features
        conSys(sys, star, starSpec);
        addPlanets(sys, star);

        Map<String, PlanetAPI> planets = grabPlanets(sys);

        addAsteroidBelts(sys, star);
        addEntities(sys, star, planets);
        addGateDeco(sys);
        //addRadiation(sys);
        addJumpPoints(sys);
        addJunkFields(sys);
        addWrecksRing(sys, star, 12);
        doBeacon(sys);
        doNebula(sys, 4000f, -4000f);
        doHyperSweep(sys);
        doLabel(sys, "oort_label","Oort",300, 1250);
        doLabel(sys, "oort_label","Cloud",120, 1250);
        // Set up zodiac orbit AFTER everything is built
        /*setupZodiacOrbit(sys, sec);*/
    }
    // SYSTEM  & STAR CONFIG
    private static void conSys(StarSystemAPI sys, PlanetAPI star, PlanetSpec spec) {
        sys.setProcgen(false);
        sys.addCorona(star, 1000f, 10f, 0.44f, 7.25f);
        sys.setAge(StarAge.AVERAGE);
        sys.setBackgroundTextureFilename("graphics/backgrounds/galaxy0.jpg");
        sys.setLightColor(new Color(255, 245, 225)); // subtle warm
        sys.getMemoryWithoutUpdate().set("$US_skipSystem", true);
        sys.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_sol_system");
        // Configure System Tags
        sys.addTag(Tags.THEME_DERELICT);
        sys.addTag(Tags.THEME_HIDDEN);
        sys.addTag(Tags.THEME_UNSAFE);
        sys.addTag(Tags.THEME_SPECIAL);
        sys.addTag(Tags.THEME_REMNANT_RESURGENT);
        //if (ModCheck.hasIND) {sys.addTag(Ids.TAG_SYSTEM_HAS_ARTILLERY);}
        // SOL SPECS
        spec.name = "G-Type Main-Sequence";
        spec.planetType = "star_white";
        spec.iconTexture = "graphics/warroom/icon_sol.png";
        //spec.setStarscapeIcon(Global.getSettings().getSpriteName("warroom","icon_sol"));
        spec.setCoronaTexture(Global.getSettings().getSpriteName("coronas", "sol_halo"));
        spec.setTexture(Global.getSettings().getSpriteName("stars","sol"));
        spec.setCloudTexture(Global.getSettings().getSpriteName("clouds","clouds_StarWhite"));
        spec.setCloudRotation(spec.getRotation() - 3);
        spec.setRotation(0.44f);
        spec.setScaleMultStarscapeIcon(1.25f);
        spec.setIconColor(new Color(240,230,120,255));
        spec.setCloudColor(new Color(250, 250, 50, 255));
        spec.setGlowColor(new Color(248, 55, 106, 255));
        spec.setPlanetColor(new Color(235, 235, 195, 255));
        spec.setUseReverseLightForGlow(false);
        //star.setName("Sol");
        star.setCustomDescriptionId("sol_Sol");
        star.applySpecChanges();

    }
    // CELESTIAL TABLE (dynamic, null-safe)
    private static Map<String, PlanetAPI> grabPlanets(StarSystemAPI system) {
        Map<String, PlanetAPI> planets = new HashMap<>();
        for (String id : List.of("mercury","venus","earth","mars","ceres","jupiter","saturn","uranus","neptune","pluto",
        "luna","phobos","deimos","io","europa","ganymede","callisto","enceladus","titan","oberon","titania","miranda","triton","charon"))
        {
            SectorEntityToken e = system.getEntityById("sol_" + id);
            if (e instanceof PlanetAPI p) planets.put(id, p);
        }
        return planets;
    }
    // PLANETS
    private static void addPlanets(StarSystemAPI sys, PlanetAPI star) {
        createMercury(sys, star);
        createVenus(sys, star);
        createEarth(sys, star);
        createMars(sys, star);
        createJupiter(sys, star);
        createSaturn(sys, star);
        createUranus(sys, star);
        createNeptune(sys, star);
        createCeres(sys, star);
        createPluto(sys, star);
    }
    private static void createMercury(StarSystemAPI sys, PlanetAPI star) {
        var mercury = doPlanet(sys, star, "sol_mercury", "Mercury", "barren-bombarded", 270f, 60f, 2500f, -120f);
        mercury.setCustomDescriptionId("sol_Mercury");
        mercury.getSpec().setTexture(Global.getSettings().getSpriteName("planets","mercury"));
        mercury.getSpec().setTilt(-0.03f);
        mercury.getSpec().setIconColor(new Color(227, 151, 118, 255));
        mercury.getSpec().setRotation(0.01175f);
        mercury.applySpecChanges();
        var mm = craftMarket(mercury, "neutral",0, "hot", "irradiated", "low_gravity", "ore_ultrarich", "rare_ore_ultrarich", "thin_atmosphere", "toxic_atmosphere");
    }
    private static void createVenus(StarSystemAPI sys, PlanetAPI star) {
        var venus = doPlanet(sys, star, "sol_venus", "Venus", "toxic", 180f, 140f, 3200f, -450f);
        venus.setCustomDescriptionId("sol_Venus");
        venus.getSpec().setTexture(Global.getSettings().getSpriteName("planets","venus"));
        venus.getSpec().setCloudTexture(Global.getSettings().getSpriteName("clouds","clouds_Heavy"));
        venus.getSpec().setTilt(177.4f);
        venus.getSpec().setAtmosphereColor(new Color(140, 60, 80, 255));
        venus.getSpec().setAtmosphereThickness(1.0f);
        venus.getSpec().setAtmosphereThicknessMin(5.0f);
        venus.getSpec().setCloudColor(new Color(250, 250, 50, 25));
        venus.getSpec().setCloudRotation(0.012375f);
        venus.getSpec().setRotation(-0.02475f);
        venus.applySpecChanges();
        venus.addTag(Tags.NOT_RANDOM_MISSION_TARGET);

        var vm = craftMarket(venus, "neutral",0, "very_hot", "dense_atmosphere", "irradiated", "extreme_tectonic_activity", "ore_ultrarich", "rare_ore_ultrarich", "solar_array");
        ModCheck.cond(vm, "IndEvo", "IndEvo_mineFieldCondition");
        ModCheck.cond(vm, "aotd_vok", "pre_collapse_facility");
    }
    private static void createEarth(StarSystemAPI sys, PlanetAPI star) {
        var earthType = ModCheck.hasUS ? "US_continent" : "terran";
        var earth = doPlanet(sys, star, "sol_earth", "Earth", earthType, 90f, 180f, 4400f, -730f);
        ((PlanetSpec) earth.getSpec()).name = "Terrestrial";

        earth.getSpec().setTexture(Global.getSettings().getSpriteName("planets","earth")); //earth_nuked
        earth.getSpec().setCloudTexture(Global.getSettings().getSpriteName("clouds","clouds_Earth")); //clouds_EarthSmog
        earth.getSpec().setAtmosphereColor(new Color(50, 150, 255, 150));
        earth.getSpec().setAtmosphereThickness(1.0f);
        earth.getSpec().setAtmosphereThicknessMin(10.0f);
        earth.getSpec().setCloudColor(new Color(230, 240, 250, 180));
        earth.getSpec().setIconColor(new Color(155, 185, 255, 255));
        earth.getSpec().setCloudRotation(-2.5f);
        earth.getSpec().setTilt(-23.4f);
        earth.getSpec().setRotation(6f);
        earth.applySpecChanges();
        earth.setCustomDescriptionId("sol_Earth");
        var em = craftMarket(earth, "neutral",0, "habitable", "farmland_poor", "mild_climate", "pollution", "volturnian_lobster_pens","ore_ultrarich", "organics_plentiful", "ruins_widespread");
        ModCheck.cond(em, "US", "US_elevator", "US_religious", "US_magnetic", "US_base");
        ModCheck.cond(em, "assortment_of_things", "rat_ancient_megacities");
        ModCheck.cond(em, "IndEvo", "IndEvo_RuinsCondition");
        ModCheck.tag(earth, "assortment_of_things", "rat_relic_condition", "rat_ancient_megacities");
        ModCheck.tag(earth, "IndEvo", Tags.NOT_RANDOM_MISSION_TARGET);
        ModCheck.mem(earth, "IndEvo", "$IndEvo_ruinsIndustryId", "IndEvo_Memorial");
        doMagneticField(sys, earth, earth.getRadius() + 90f, earth.getRadius() + 270f);

        // === MOON: Luna
        var luna = doPlanet(sys, earth, "sol_luna", "Luna", "barren", 0f, 40f, 350f, -56f);
        luna.setCustomDescriptionId("sol_Luna");
        luna.getSpec().setTexture(Global.getSettings().getSpriteName("planets","luna"));
        luna.setInteractionImage("illustrations", "luna_bootPrint");
        luna.getSpec().setTilt(-5.14f);
        luna.getSpec().setRotation(0.22f);
        luna.applySpecChanges();
        var lm = craftMarket(luna, "neutral",0, "ruins_vast", "no_atmosphere", "low_gravity", "ore_sparse", "rare_ore_rich");
        ModCheck.cond(lm, "US_base", "US_tunnels", "US_crystals");
        ModCheck.cond(lm, "assortment_of_things","rat_ancient_industries");
        ModCheck.tag(luna,"assortment_of_things", "rat_relic_condition", "rat_ancient_industries");
    }
    private static void createMars(StarSystemAPI sys, PlanetAPI star) {
        var mars = doPlanet(sys, star, "sol_mars", "Mars", "barren-desert", 360f, 80f, 6000f, -1374f);
        mars.setCustomDescriptionId("sol_Mars");
        mars.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "mars"));
        mars.getSpec().setCloudTexture(Global.getSettings().getSpriteName("clouds", "clouds_Mars"));
        mars.getSpec().setCloudRotation(-6.325f);
        mars.getSpec().setTilt(-25.2f);
        mars.getSpec().setIconColor(new Color(255, 68, 14, 250));
        mars.getSpec().setRotation(11.65f);
        mars.applySpecChanges();
        var mm = craftMarket(mars, "neutral",0, "cold", "thin_atmosphere", "low_gravity", "ruins_vast", "ore_moderate", "rare_ore_moderate");
        ModCheck.cond(mm, "aotd_vok", "aotd_nidavelir_complex");
        ModCheck.cond(mm, "IndEvo", "IndEvo_RuinsCondition");
        ModCheck.cond(mm, "US", "US_elevator", "US_shrooms");
        ModCheck.cond(mm, "assortment_of_things", "rat_ancient_military_hub", "rat_kinetic_launchsystem");
        ModCheck.tag(mars,"assortment_of_things", "rat_relic_condition", "rat_ancient_military_hub", "rat_kinetic_launchsystem");
        ModCheck.mem(mars, "IndEvo", "$IndEvo_ruinsIndustryId", "IndEvo_HullDecon");

        // === MOON: PHOBOS
        var phobos = doPlanet(sys, mars, "sol_phobos", "Phobos", "barren_castiron", 90f, 19f, 180f, -2f);
        phobos.setCustomDescriptionId("sol_Phobos");
        phobos.getSpec().setTexture(Global.getSettings().getSpriteName("planets","phobos"));
        phobos.getSpec().setTilt(0f);
        phobos.getSpec().setRotation(0.25f);
        phobos.applySpecChanges();
        var pm = craftMarket(phobos, "neutral",0, "no_atmosphere", "low_gravity", "ore_sparse", "rare_ore_abundant");
        // === MOON: DEIMOS
        var deimos = doPlanet(sys, mars, "sol_deimos", "Deimos", "barren-bombarded", 210f, 15f, 320f, -5f);
        deimos.setCustomDescriptionId("sol_Deimos");
        deimos.getSpec().setTexture(Global.getSettings().getSpriteName("planets","deimos"));
        deimos.getSpec().setTilt(0f);
        deimos.getSpec().setRotation(0.15f);
        deimos.applySpecChanges();
        var dm = craftMarket(deimos, "neutral",0, "no_atmosphere", "low_gravity", "ore_sparse");
        ModCheck.cond(dm, "IndEvo", "IndEvo_RuinsCondition");
        ModCheck.mem(deimos, "IndEvo", "$IndEvo_ruinsIndustryId", "IndEvo_HullForge");
    }
    private static void createCeres(StarSystemAPI sys, PlanetAPI star) {
        var ceres = doPlanet(sys, star, "sol_ceres", "Ceres", "rocky_ice", 270f, 20f, 7700f, -3364f);
        ceres.setCustomDescriptionId("sol_Ceres");
        ceres.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "ceres"));
        ceres.getSpec().setTilt(-4f);
        ceres.getSpec().setRotation(15.79f);
        ceres.applySpecChanges();
        var cm = craftMarket(ceres,"independent",0,  "no_atmosphere", "low_gravity", "ore_sparse", "rare_ore_abundant", "meteor_impacts");
        ModCheck.cond(cm, "US", "US_crystals");
        ceres.addTag(Tags.NEUTRINO_LOW);
    }
    private static void createJupiter(StarSystemAPI sys, PlanetAPI star) {
        var jupiterType = ModCheck.hasUS ? "US_gas_giant" : "gas_giant";
        var jupiter = doPlanet(sys, star, "sol_jupiter", "Jupiter", jupiterType, 150f, 500f, 9800f, -8666f);

        jupiter.setCustomDescriptionId("sol_Jupiter");
        jupiter.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "jupiter"));
        jupiter.getSpec().setCloudTexture(Global.getSettings().getSpriteName("clouds", "clouds_Giant"));
        jupiter.getSpec().setAtmosphereThickness(1.0f);
        jupiter.getSpec().setAtmosphereThicknessMin(10.0f);
        jupiter.getSpec().setCloudColor(new Color(120, 150, 180, 100));
        jupiter.getSpec().setCloudRotation(-43.33f);
        jupiter.getSpec().setTilt(-3.1f);
        jupiter.getSpec().setRotation(14.635f);
        jupiter.applySpecChanges();
        jupiter.addTag(Tags.NOT_RANDOM_MISSION_TARGET);

        var jm = craftMarket(jupiter, "neutral",0, "dense_atmosphere", "high_gravity", "volatiles_plentiful", "ruins_vast", "meteor_impacts");
        ModCheck.cond(jm, "US", "US_base", "US_floating");
        ModCheck.cond(jm, "assortment_of_things", "rat_ancient_fuel_hub");
        ModCheck.tag(jupiter,"assortment_of_things", "rat_relic_condition", "rat_ancient_fuel_hub");
        //indevoArty(jupiter, false, "missile"); // - spawns hostile w/ solo watchtower - owned by nearestFaction?
        //ModCheck.tag(jupiter,"IndEvo", "IndEvo_Entity_has_artillery");
        //ModCheck.mem(jupiter, "IndEvo", "$hasStation", true);
        //ModCheck.mem(jupiter, "IndEvo", "$hasDefenders", true);
        //jm.setPlanetConditionMarketOnly(false);

        ModCheck.cond(jm, "IndEvo", "IndEvo_ArtilleryStationCondition"); // - spawns damaged w/ system-wide watchtowers
        //ModCheck.indy(jm, "IndEvo","IndEvo_Artillery_railgun", null,null,false); // - needs factionId
        // Jupiter's Rings & Mag Field
        sys.addRingBand(jupiter, "misc", "rings_dust0", 128f, 0, new Color(190, 120, 20, 255), 128f, 858, -90f, Terrain.RING, "Thebe Ring");
        sys.addRingBand(jupiter, "misc", "rings_ice0", 128f, 1, new Color(190, 150, 50, 255), 128f, 630f, -90f, Terrain.RING, "Almathea Ring");
        doMagneticField(sys, jupiter, jupiter.getRadius() + 50f, jupiter.getRadius() + 750f);

        // === MOON: IO
        var io = doPlanet(sys, jupiter, "sol_io", "Io", "lava", 120f, 50f, 725f, -3.54f);
        io.setCustomDescriptionId("sol_Io");
        io.getSpec().setTexture(Global.getSettings().getSpriteName("planets","io"));
        io.getSpec().setTilt(-1.0f);
        io.getSpec().setCloudRotation(-1.695f);
        io.getSpec().setPlanetColor(new Color(238, 235, 206, 255));
        io.getSpec().setGlowColor(new Color(250, 150, 75, 225));
        io.getSpec().setUseReverseLightForGlow(true);
        io.getSpec().setRotation(3.39f);
        io.applySpecChanges();
        var im = craftMarket(io, "neutral",0, "poor_light", "very_hot", "thin_atmosphere", "low_gravity", "extreme_tectonic_activity", "ore_abundant", "rare_ore_abundant");
        // === MOON: EUROPA
        var europa = doPlanet(sys, jupiter, "sol_europa", "Europa", "rocky_ice", 0f, 40f, 1125f, -7f);
        europa.setCustomDescriptionId("sol_Europa");
        europa.getSpec().setTexture(Global.getSettings().getSpriteName("planets","europa"));
        europa.getSpec().setTilt(1.79f);
        europa.getSpec().setCloudRotation(1.5f);
        europa.getSpec().setRotation(1.69f);
        europa.applySpecChanges();
        var em = craftMarket(europa, "neutral",0, "cold", "low_gravity", "thin_atmosphere", "poor_light", "volatiles_plentiful",  "ore_moderate", "rare_ore_moderate", "ruins_widespread");
        ModCheck.cond(em, "assortment_of_things", "rat_bionic_plantlife");
        // === MOON: GANYMEDE
        var ganymede = doPlanet(sys, jupiter, "sol_ganymede", "Ganymede", "rocky_ice", 240f, 70f, 1400f, -14.2f);
        ganymede.setCustomDescriptionId("sol_Ganymede");
        ganymede.getSpec().setTexture(Global.getSettings().getSpriteName("planets","ganymede"));
        ganymede.getSpec().setTilt(-23.5f);
        ganymede.getSpec().setRotation(0.84f);
        ganymede.applySpecChanges();
        var gm = craftMarket(ganymede, "neutral",0, "cold", "thin_atmosphere", "low_gravity", "poor_light", "ore_moderate", "rare_ore_moderate", "ruins_extensive", "volatiles_abundant");
        ModCheck.cond(gm, "assortment_of_things", "rat_rampant_military_core");
        ModCheck.cond(gm, "US", "US_bedrock");
        ModCheck.tag(jupiter,"assortment_of_things", "rat_relic_condition", "rat_rampant_military_core");

        // === MOON: CALLISTO
        var callisto = doPlanet(sys, jupiter, "sol_callisto", "Callisto", "rocky_ice", 160f, 55f, 1800f, -34f);
        callisto.setCustomDescriptionId("sol_Callisto");
        callisto.getSpec().setTexture(Global.getSettings().getSpriteName("planets","callisto"));
        callisto.getSpec().setTilt(-23.5f);
        callisto.getSpec().setRotation(0.36f);
        callisto.applySpecChanges();
        var cm = craftMarket(callisto, "neutral",0, "cold", "no_atmosphere", "low_gravity", "poor_light", "ore_abundant", "rare_ore_abundant", "ruins_extensive", "volatiles_abundant");
        ModCheck.cond(cm, "aotd_vok", "pre_collapse_facility");
        ModCheck.cond(cm, "US", "US_tunnels", "US_crystals");
        callisto.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
    }
    private static void createSaturn(StarSystemAPI sys, PlanetAPI star) {
        var saturnType = ModCheck.hasUS ? "US_gas_giantB" : "gas_giant";
        var saturn = doPlanet(sys, star, "sol_saturn", "Saturn", saturnType, 300f, 425f, 14000f, -21512f);

        saturn.setCustomDescriptionId("sol_Saturn");
        saturn.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "saturn"));
        saturn.getSpec().setCloudTexture(Global.getSettings().getSpriteName("clouds", "clouds_Giant"));
        saturn.getSpec().setCloudColor(new Color(180, 160, 140, 15));
        saturn.getSpec().setIconColor(new Color(220, 200, 170, 150));
        saturn.getSpec().setTilt(-26.7f);
        saturn.getSpec().setRotation(13.3325f);
        saturn.getSpec().setAtmosphereColor(new Color(220, 200, 170, 50));
        saturn.getSpec().setAtmosphereThickness(1.0f);
        saturn.getSpec().setAtmosphereThicknessMin(5.0f);
        saturn.applySpecChanges();
        var sm = craftMarket(saturn, "independent",0, "ruins_vast", "volatiles_plentiful", "extreme_weather", "high_gravity");
        ModCheck.cond(sm, "US", "US_floating", "US_fluorescent");
        ModCheck.cond(sm, "assortment_of_things", "rat_ancient_fuel_hub");
        ModCheck.tag(saturn,"assortment_of_things", "rat_relic_condition", "rat_ancient_fuel_hub");
        // Saturn's Rings
        sys.addRingBand(saturn, "misc", "saturn_belt", 256f, 0, new Color(200, 180, 150, 150), 256f, 700f, -90f, Terrain.RING, "C Ring");
        sys.addRingBand(saturn, "misc", "saturn_belt", 256f, 1, new Color(220, 200, 170, 150), 256f, 950f, -90f, Terrain.RING, "B Ring");
        sys.addRingBand(saturn, "misc", "saturn_belt", 256f, 2, new Color(210, 190, 160, 150), 256f, 1200f, -90f, Terrain.RING, "A Ring");
        sys.addRingBand(saturn, "misc", "saturn_belt", 256f, 3, new Color(180, 160, 140, 150), 256f, 1450f, -90f, Terrain.RING, "Cassini Division");

        // === MOON: ENCELADUS
        var enceladus = doPlanet(sys, saturn, "sol_enceladus", "Enceladus", "cryovolcanic", 180f, 25f, 1360f, -2.74f);
        enceladus.setCustomDescriptionId("sol_Enceladus");
        enceladus.getSpec().setTexture(Global.getSettings().getSpriteName("planets","enceladus"));
        enceladus.getSpec().setTilt(-23.5f);
        enceladus.getSpec().setCloudRotation(4.0f);
        enceladus.getSpec().setRotation(4.38f);
        enceladus.getSpec().setPlanetColor(new Color(200, 200, 200, 255));
        enceladus.getSpec().setGlowColor(new Color(155, 155, 255, 90));
        enceladus.getSpec().setAtmosphereColor(new Color(155, 155, 255, 155));
        enceladus.getSpec().setUseReverseLightForGlow(true);
        enceladus.applySpecChanges();
        var em = craftMarket(enceladus, "independent",0, "very_cold", "ruins_scattered", "tectonic_activity", "low_gravity", "poor_light", "ore_moderate", "rare_ore_rich", "volatiles_diffuse");
        ModCheck.cond(em, "aotd_vok", "pre_collapse_facility");
        ModCheck.cond(em, "US", "US_crash");
        enceladus.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        // === MOON: TITAN
        var titan = doPlanet(sys, saturn, "sol_titan", "Titan", "toxic_cold", 0f, 55f, 2000f, -32f);
        titan.setCustomDescriptionId("sol_Titan");
        titan.getSpec().setTexture(Global.getSettings().getSpriteName("planets","titan"));
        titan.getSpec().setCloudTexture(Global.getSettings().getSpriteName("clouds","clouds_Titan"));
        titan.getSpec().setCloudRotation(0.1875f);
        titan.getSpec().setAtmosphereColor(new Color(235, 225, 125, 200));
        titan.getSpec().setTilt(-23.5f);
        titan.getSpec().setRotation(0.3755f);
        titan.applySpecChanges();
        var tm = craftMarket(titan, "independent",0, "cold", "inimical_biosphere", "poor_light", "ore_moderate", "rare_ore_moderate", "volatiles_plentiful", "organics_common");
        ModCheck.cond(tm, "US", "US_crash", "US_bedrock", "US_base");
        ModCheck.cond(tm, "assortment_of_things", "rat_warscape");
        ModCheck.tag(saturn,"assortment_of_things", "rat_relic_condition", "rat_warscape");
    }
    private static void createUranus(StarSystemAPI sys, PlanetAPI star) {
        String uranusType = ModCheck.hasUS ? "US_iceA" : "gas_giant";
        var uranus = doPlanet(sys, star, "sol_uranus", "Uranus", uranusType, 0f, 350f, 18000f, -61374f);

        uranus.setCustomDescriptionId("sol_Uranus");
        uranus.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "uranus"));
        uranus.getSpec().setCloudTexture(Global.getSettings().getSpriteName("clouds", "clouds_Swept"));
        uranus.getSpec().setCloudColor(new Color(240, 120, 250, 30));
        uranus.getSpec().setAtmosphereColor(new Color(250, 120, 250, 15));
        uranus.getSpec().setAtmosphereThickness(0.2f);
        uranus.getSpec().setAtmosphereThicknessMin(2.0f);
        uranus.getSpec().setIconColor(new Color(150, 170, 180, 255));
        uranus.getSpec().setTilt(82.2f);
        uranus.getSpec().setRotation(16.665f);
        uranus.getSpec().setGlowColor(new Color(150, 170, 180, 255));
        uranus.getSpec().setUseReverseLightForGlow(true);
        uranus.applySpecChanges();
        var um = craftMarket(uranus, "independent",0, "poor_light", "extreme_weather", "dense_atmosphere", "toxic_atmosphere", "very_cold", "high_gravity", "volatiles_abundant");
        ModCheck.cond(um, "US", "US_cryosanctum");
        // Uranus' Ring
        sys.addRingBand(uranus, "misc", "rings_ice0", 256f, 3, new Color(250, 120, 250, 50), 128f, 450f, -45f, Terrain.RING, "Epsilon Ring");

        // === MOON: OBERON
        var oberon = doPlanet(sys, uranus, "sol_oberon", "Oberon", "rocky_ice", 30f, 60f, 1800f, -26f);
        oberon.setCustomDescriptionId("sol_Oberon");
        oberon.getSpec().setTexture(Global.getSettings().getSpriteName("planets","oberon"));
        oberon.getSpec().setCloudRotation(0f);
        oberon.getSpec().setTilt(-97.8f);
        oberon.getSpec().setRotation(0.445f);
        oberon.applySpecChanges();
        var om = craftMarket(oberon,"independent",0, "poor_light","no_atmosphere","very_cold","low_gravity", "ore_sparse");
        // === MOON: TITANIA
        var titania = doPlanet(sys, uranus, "sol_titania", "Titania", "rocky_ice", 120f, 90f, 1200f, -39f);
        titania.setCustomDescriptionId("sol_Titania");
        titania.getSpec().setTexture(Global.getSettings().getSpriteName("planets","titania"));
        titania.getSpec().setCloudTexture(Global.getSettings().getSpriteName("clouds","clouds_Soft"));
        titania.getSpec().setTilt(-78.9f);
        titania.getSpec().setCloudRotation(0.9f);
        titania.getSpec().setRotation(0.935f);
        titania.applySpecChanges();
        var tm = craftMarket(titania, "independent",0, "thin_atmosphere", "low_gravity", "poor_light", "ore_sparse", "organics_trace", "ruins_extensive");
        ModCheck.cond(tm, "aotd_vok", "pre_collapse_facility");
        ModCheck.cond(tm, "US", "US_tunnels", "US_shrooms");
        titania.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        // === MOON: MIRANDA
        var miranda = doPlanet(sys, uranus, "sol_miranda", "Miranda", "rocky_unstable", 210f, 30f, 650f, -52f);
        miranda.setCustomDescriptionId("sol_Miranda");
        miranda.getSpec().setTexture(Global.getSettings().getSpriteName("planets","miranda"));
        miranda.getSpec().setTilt(-89.7f);
        miranda.getSpec().setRotation(0.3905f);
        miranda.applySpecChanges();
        var mm = craftMarket(miranda, "independent",0, "cold", "no_atmosphere", "low_gravity", "poor_light", "ore_sparse", "organics_trace", "ruins_widespread");
        ModCheck.cond(mm, "US", "US_base", "US_tunnels");
    }
    private static void createNeptune(StarSystemAPI sys, PlanetAPI star) {
        var neptuneType = ModCheck.hasUS ? "US_iceB" : "gas_giant";
        var neptune = doPlanet(sys, star, "sol_neptune", "Neptune", neptuneType, 60f, 340f, 21000f, -120380f);

        neptune.setCustomDescriptionId("sol_Neptune");
        neptune.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "neptune"));
        neptune.getSpec().setCloudTexture(Global.getSettings().getSpriteName("clouds", "clouds_Swept"));
        neptune.getSpec().setCloudColor(new Color(130, 140, 150, 20));
        neptune.getSpec().setTilt(-28.3f);
        neptune.getSpec().setRotation(8.3325f);
        neptune.getSpec().setGlowColor(new Color(255, 255, 255, 255));
        neptune.getSpec().setAtmosphereThicknessMin(3.25f);
        neptune.getSpec().setAtmosphereColor(new Color(100, 150, 250, 100));
        neptune.getSpec().setAtmosphereThickness(1.0f);
        neptune.getSpec().setIconColor(new Color(70, 70, 250, 255));
        neptune.applySpecChanges();
        var nm = craftMarket(neptune, "neutral",0, "dark", "very_cold", "high_gravity", "ruins_scattered", "volatiles_diffuse");
        ModCheck.cond(nm, "US", "US_cryosanctum");
        // Neptune's Rings
        sys.addRingBand(neptune, "misc", "rings_dust0", 256f, 2, new Color(130, 150, 180, 255), 128f, 656f, -120f, Terrain.RING, "Galle Ring");
        sys.addRingBand(neptune, "misc", "rings_ice0", 256f, 3, new Color(140, 160, 190, 255), 128f, 428f, -90f, Terrain.RING, "Le Verrier Ring");

        // === MOON: TRITON
        var triton = doPlanet(sys, neptune, "sol_triton", "Triton", "cryovolcanic", 0f, 50f, 900f, -11.8f);
        triton.setCustomDescriptionId("sol_Triton");
        triton.getSpec().setTexture(Global.getSettings().getSpriteName("planets","triton"));
        triton.getSpec().setTilt(-23.5f);
        triton.getSpec().setCloudRotation(1.0f);
        triton.getSpec().setRotation(2.04f);
        triton.applySpecChanges();
        var tm = craftMarket(triton, "neutral",0, "very_cold", "tectonic_activity", "low_gravity", "dark", "rare_ore_sparse", "ore_moderate", "ruins_scattered", "volatiles_abundant");
        ModCheck.cond(tm, "aotd_vok", "pre_collapse_facility");
        triton.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
    }
    private static void createPluto(StarSystemAPI sys, PlanetAPI star) {
        var pluto = doPlanet(sys, star, "sol_pluto", "Pluto", "frozen", 240f, 32f, 23000f, -181120f);
        pluto.setCustomDescriptionId("sol_Pluto");
        pluto.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "pluto"));
        pluto.getSpec().setTilt(-122.5f);
        pluto.getSpec().setRotation(1.875f);
        pluto.applySpecChanges();
        var pm = craftMarket(pluto, "neutral",0, "dark", "low_gravity", "very_cold", "thin_atmosphere", "ore_ultrarich", "rare_ore_ultrarich", "meteor_impacts");
        ModCheck.cond(pm, "aotd_vok", "aotd_pluto_station");
        pluto.addTag(Tags.NEUTRINO_HIGH);
        // === MOON: CHARON
        var charon = doPlanet(sys, pluto, "sol_charon", "Charon", "rocky_ice", 0f, 16f, 120f, -12.8f);
        charon.setCustomDescriptionId("sol_Charon");
        charon.getSpec().setTexture(Global.getSettings().getSpriteName("planets","charon"));
        charon.getSpec().setTilt(-23.5f);
        charon.getSpec().setRotation(0.9375f);
        charon.applySpecChanges();
        var cm = craftMarket(charon, "neutral",0, "cold", "no_atmosphere", "ore_ultrarich", "rare_ore_ultrarich", "meteor_impacts");
        ModCheck.cond(cm, "IndEvo", "IndEvo_RuinsCondition");
        ModCheck.mem(charon, "IndEvo", "$IndEvo_ruinsIndustryId", "IndEvo_ResLab");
    }
    /** ---------------- END PLANET METHODS ----------------*/
    // ENTITIES
    private static void addEntities(StarSystemAPI sys, PlanetAPI star, Map<String, PlanetAPI> p) {
        // SOLAR ARRAYS
        doSolarArray(sys, p.get("mercury"), 3, 1);
        // REMNANT STATION
        // IndEvo Arty
        var vam = sys.addCustomEntity("sol_remy_base", "Derelict Vambrace", "derelict_vambrace", null);
        vam.setFixedLocation(6000f, -18000f);
        var remy = BattleStationBuilder.addRemnantStation(sys, vam, "Irradiated Rig", 180f, 500, 100f, false);
        // Radiation field (500 unit radius from station)
        var remField = RadiationPlug.createRadiationField(remy, 1.0f, 1000f, 500f, 1500f, 0.25f);
        remField.getMemoryWithoutUpdate().set("$defendedByStation", remy);
        // Make this a particularly deadly radiation zone!
        if (remField.getPlugin() instanceof RadiationPlug plugin) {
            plugin.setCrewLossMult(1.5f);
            plugin.setMarineLossMult(1.5f); // 50% more deadly than normal
            plugin.setCrDamageMult(1.5f);
        }

        // ABANDONED STATIONS
        var iss = doCustomEntity(sys, p.get("earth"), "sol_iss","International Space Station","station_side00","neutral", 135f,250f,-365f);
        iss.setCustomDescriptionId("sol_ISS");
        Misc.setAbandonedStationMarket("sol_iss_market", iss);
        var cargo1 = iss.getMarket().getSubmarket("storage").getCargo();
        cargo1.addHullmods("radiation_shielding", 1);
        if (ModCheck.hasIIBG) {
            //Looter.addRandomBlueprints(cargo1, 2, 3, 2); currently broke for now
            Looter.addCustomMix(cargo1, 5, 0, 0, 6, 1, 2);
            cargo1.addMothballedShip(FleetMemberType.SHIP, "apogee_ii_Elite", null);
            cargo1.addMothballedShip(FleetMemberType.SHIP, "apogee_ii_Martyr", null);
            cargo1.addMothballedShip(FleetMemberType.SHIP, "apogee_ii_Support", null);
        } else {
            Looter.addAbandonedLoot(iss, Looter.StationClass.CACHE);
        }
        // Titan's Eye
        var titansEye = doCustomEntity(sys, p.get("saturn"), "sol_titans_eye","Titan's Eye","station_side05","neutral", 0f,500f,-1000f);
        titansEye.setCustomDescriptionId("sol_SiphonStation");
        Misc.setAbandonedStationMarket("sol_titans_eye_market", titansEye);
        var cargo2 = titansEye.getMarket().getSubmarket("storage").getCargo();
        if (ModCheck.hasIIBG) {
            cargo2.addMothballedShip(FleetMemberType.SHIP, "heron_ii_Bomber", "Bloody Eyes");
            cargo2.addMothballedShip(FleetMemberType.SHIP, "heron_ii_Support", "Bloody Lips");
            cargo2.addMothballedShip(FleetMemberType.SHIP, "heron_ii_Suppression", "Bloody Ears");
            for (String s : Arrays.asList("broadsword_ii_wing", "trident_ii_wing", "warthog_ii_wing")) {
                cargo2.addFighters(s, 2);
            }
            Looter.addCustomMix(cargo2, 3, 0, 0, 8, 2, 0);
        } else { // fallback to loot pool
            Looter.addAbandonedLoot(iss, Looter.StationClass.RESEARCH);
        }
        // Gungnir Dockyard
        var redStone = doCustomEntity(sys, p.get("mars"),
                "sol_redstone","Gungnir Dockyard","orbital_dockyard","neutral", 0f,540f,-1000f);
        redStone.setCustomDescriptionId("sol_RedStone");
        Misc.setAbandonedStationMarket("sol_redstone_market", redStone);
        var cargo3 = redStone.getMarket().getSubmarket("storage").getCargo();

        if (ModCheck.hasVOK) {
            cargo3.addWeapons("gungnir_railgun", 2);
        }
        if (ModCheck.hasIIBG) {
            cargo3.addMothballedShip(FleetMemberType.SHIP, "brawler_ii_Anti_air", "Bloody Knuckles");
            cargo3.addMothballedShip(FleetMemberType.SHIP, "brawler_ii_Artillery", "Bloody Nose");
            cargo3.addMothballedShip(FleetMemberType.SHIP, "brawler_ii_Support", "Bloody Knees");
            Looter.addCustomMix(cargo3, 5, 0, 0, 3, 1, 1);
        } else { // fallback to loot pool
            Looter.addAbandonedLoot(redStone, Looter.StationClass.MILITARY);
        }
        // Dead Station
        var telepylos = doCustomEntity(sys, p.get("neptune"), "sol_telepylos","Telepylos Station","station_sporeship_derelict","neutral", 90f,540f,-250f);
        Misc.setAbandonedStationMarket("sol_telepylos_market", telepylos);
        var cargo4 = telepylos.getMarket().getSubmarket("storage").getCargo();
        if (ModCheck.hasIIBG) {
            cargo4.addMothballedShip(FleetMemberType.SHIP, "atlas_iia_Support", "Bloody Ass");
            Looter.addCustomMix(cargo4, 5, 0, 0, 7, 2, 0);
        } else { // fallback to loot pool
            Looter.addAbandonedLoot(telepylos, Looter.StationClass.STANDARD);
        }
        var teleField = RadiationPlug.createRadiationField(telepylos, 0f, 500f, 100f, 1000f, 0.15f);

        // ENTITY DEFINITIONS
        final var entities = grabEntitySpecs();
        // SPECIAL HANDLERS
        Map<String, Consumer<CustomCampaignEntityAPI>> handlers = Map.of(
                "junk", e -> {},
                "arsenal", e -> {},
                "pet", e -> {},
                "ark", e -> {},
                "dsn", e -> e.setCustomDescriptionId("sol_DSN"),
                "ssn", e -> e.setCustomDescriptionId("sol_SSN"),
                "found", e -> e.addTag("rat_relic_station"),
                "ratco", e -> e.addTag("rat_relic_station"),
                "cryo", CryoSpawner::setupCryochamber
        );
        // SPAWN ENTITIES
        for (EntitySpec spec : entities) {

            PlanetAPI anchor = star; // Default to Star
            // Only check special if it's not null
            if (spec.special() != null) {
                anchor = switch (spec.special()) {
                    case "ark" -> p.get("neptune");
                    case "junk" -> p.get("jupiter");
                    case "arsenal" -> p.get("venus");
                    case "pet" -> p.get("ceres");
                    case "ratco" -> p.get("titania");
                    default -> star;
                };
            }
            var e = doCustomEntity(
                    sys, anchor,
                    spec.id(), spec.name(), spec.type(), spec.faction(),
                    spec.angle(), spec.orbitRadius(), spec.orbitDays()
            );
            // Run handler if special exists and is not null
            if (spec.special() != null && handlers.containsKey(spec.special()))
                handlers.get(spec.special()).accept(e);
        }
    }

    private static List<EntitySpec> grabEntitySpecs() {
        List<EntitySpec> entities = new ArrayList<>();
        // VANILLA ENTITIES
        entities.add(new EntitySpec("sol_newton_gate", "Newton Gate", "inactive_gate", null, 135f, 18765f, -1000f, null));
        entities.add(new EntitySpec("sol_charon_gate", "Charon Gate", "inactive_gate", null, 315f, 12345f, -1000f, null));
        entities.add(new EntitySpec("sol_stable1", "Leading Point", "stable_location", null, 150f, 4400f, -730f, null));
        entities.add(new EntitySpec("sol_stable2", "Trailing Point", "stable_location", null, 30f, 4400f, -730f, null));
        entities.add(new EntitySpec("sol_dsn", "Deep Space Network", "comm_relay", "neutral", 305f, 5000f, -100000f, "dsn"));
        entities.add(new EntitySpec("sol_ssn", "Space Surveillance Network", "sensor_array", "neutral", 180f, 8000f, -100000f, "ssn"));
        entities.add(new EntitySpec("sol_ssn2","Space Surveillance Network II","sensor_array", "neutral", 360f, 8000f, -100000f, "ssn"));
        entities.add(new EntitySpec("sol_hubble", "Hubble Buoy", "nav_buoy", "neutral", 90f, 8000f, -100000f, null));
        entities.add(new EntitySpec("sol_eidolon", "Eidolon Habitat", "orbital_habitat", "neutral", 180f, 18000f, 1000f, null));
        entities.add(new EntitySpec("sol_copernicus","Copernicus Research Station", "station_research", "neutral", 0f, 3600f, -1000f, null));
        entities.add(new EntitySpec("sol_dredge", "Remnant Dredge Platform", "station_mining_remnant", "neutral", 270f, 7000f, -1000f, null));
        entities.add(new EntitySpec("sol_voyager", "Voyager Relay", "derelict_probe", "derelict", 90f, 11000f, -1000f, null));
        entities.add(new EntitySpec("sol_endurance","Survey Vessel Endurance","derelict_survey_ship","derelict",250f,12000f,-1000f,null));
        entities.add(new EntitySpec("sol_foundation","Foundation","derelict_cryosleeper","derelict",210f,18000f,-100000f,"found"));
        entities.add(new EntitySpec("sol_tech_cache","X Files","technology_cache","neutral",255f,4500f,-1100f,null));
        entities.add(new EntitySpec("sol_solitary","The Inexorable Stasis","Sol_Solitary","neutral",270f,14000f,-100000f,"cryo"));


        if (ModCheck.hasRAT) {  // RAT
            entities.add(new EntitySpec("sol_rat_construction","Titania Construction Yard","rat_orbital_construction_station","neutral",0f,270f,-1000f,"ratco"));
        }
        if (ModCheck.hasIND) { // IND-EVO
            entities.add(new EntitySpec("sol_jupiter_junkyard","Jupiter Junkyard","IndEvo_GachaStation","neutral",90f,2250f,-180f,"junk"));
            entities.add(new EntitySpec("sol_arsenal_station","Venus Orbital Command","IndEvo_arsenalStation","neutral",0f,250f,-1337f,"arsenal"));
            entities.add(new EntitySpec("sol_icarus_pet","ICARUS Initiative","IndEvo_abandonedPetCenter","neutral",0f,200f,-1337f,"pet"));
        }
        String prometheusType = ModCheck.hasDOP ? "ark" : "derelict_cryosleeper";
        entities.add(new EntitySpec("sol_prometheus", "Prometheus Initiative", prometheusType, "neutral", 90f, 3600f, -100f, "ark"));

        return entities;
    }

    // ASTEROID BELTS
    private static void addAsteroidBelts(StarSystemAPI sys, PlanetAPI star) {
        //sys.addRingBand(star, "misc", "rings_dust0", 256f, 3, Color.decode("#8b5a2b"), 256f, 6900f, 305f, null, null);
        sys.addRingBand(star, "misc", "rings_ice0", 256f, 2, new Color(175, 100, 75, 25), 1024f, 24500f, -2400f, Terrain.RING, "Kuiper Field");
        sys.addAsteroidBelt(star, 500, 7000f, 1024f, -60f, -120f, Terrain.ASTEROID_BELT, "Main Belt");
        sys.addAsteroidBelt(star, 250, 24000f, 512f, -600f, -2400f, Terrain.ASTEROID_BELT, "Edgeworth-Kuiper Belt");
        sys.addAsteroidBelt(star, 250, 24500f, 512f, -600f, -2400f, Terrain.ASTEROID_BELT, "Edgeworth-Kuiper Belt");
        sys.addAsteroidBelt(star, 250, 25000f, 512f, -600f, -2400f, Terrain.ASTEROID_BELT, "Edgeworth-Kuiper Belt");
        sys.addOrbitalJunk(star,"orbital_junk",250,16,32,7000f,512f,-60f,-120f,100f,200f);
    }
    // FIELDS & JUNK
    private static void addJunkFields(StarSystemAPI sys) {
        // Centered ring
        var jp1 = sys.getEntityById("sol_jump1");
        var jp2 = sys.getEntityById("sol_jump2");
        var sp1 = sys.getEntityById("sol_stable1");
        var sp2 = sys.getEntityById("sol_stable2");

        doAsteroidField(sys, jp1, 45f, 90f, 30f, 120f, 5, "Newtonian Debris Belt");
        doAsteroidField(sys, jp1, 225, 90f, 45f, 150f, 5, "Newtonian Debris Belt");
        doAsteroidField(sys, jp2, -45f, 90f, 60f, 180f, 5, "Northern Shrapnel Belt");
        doAsteroidField(sys, jp2, -225f, 90f, 90f, 180f, 5, "Southern Shrapnel Belt");

        sys.addOrbitalJunk(sp1,"orbital_junk",7,16,16,90f,128f,-30f,-60f,100f,200f);
        sys.addOrbitalJunk(sp2,"orbital_junk",7,16,16,90f,128f,-30f,-60f,100f,200f);
        sys.addOrbitalJunk(jp1,"orbital_junk",5,16,16,90f,128f,-30f,-60f,100f,200f);
        sys.addOrbitalJunk(jp2,"orbital_junk",5,16,16,90f,128f,-30f,-60f,100f,200f);
    }
    // DOMAIN DECO
    private static void addGateDeco(StarSystemAPI sys) {
        var ng = sys.getEntityById("sol_newton_gate");var cg = sys.getEntityById("sol_charon_gate");
        var fc = sys.getEntityById("sol_foundation");var pc = sys.getEntityById("sol_prometheus");
        for (SectorEntityToken token : Arrays.asList(ng, cg, fc, pc)) {
            Decolicter.spawnDecolict(sys, token);
        }
    }
    // RADIATION
    /*private static void addRadiation(StarSystemAPI sys) {
        String[] gateIds = { "sol_newton_gate", "sol_charon_gate" };
        var storm = 0f;
        var width = 3000f;
        var midRadius = 300f;
        var visualBuffer = 0f;
        var alpha = 0f;

        var crewMult = 3.0f;
        var marineMult = 3.0f;
        var crDamageMult = 3.0f;

        for (String id : gateIds) {
            var gate = sys.getEntityById(id);
            if (gate == null) continue;

            var field = RadiationPlug.createRadiationField(gate, storm, width, midRadius, visualBuffer, alpha);
            var plug = (RadiationPlug) field.getPlugin();
            plug.setCrewLossMult(crewMult);
            plug.setMarineLossMult(marineMult);
            plug.setCrDamageMult(crDamageMult);
        }
    }*/
    // JUMP POINTS
    private static void addJumpPoints(StarSystemAPI sys) {
        var star = (PlanetAPI) sys.getEntityById("sol_star");

        doJumpPoints(sys, star, new Object[][]{
                {"sol_jump1", "Einstein-Rosen Bridge", 0f, 2345f, -100000f},
                {"sol_jump2", "Fringe Quantum Tunnel", 30f, 11111f, -100000f},
        });
    }
    private static void setupZodiacOrbit(StarSystemAPI sys, SectorAPI sec) {
        var hyper = sec.getHyperspace();
        var solAnchor = sys.getHyperspaceAnchor();

        // Create invisible zodiac center
        var zodiacCenter = hyper.addCustomEntity("sol_Barycenter", "Barycenter", "stable_location", null);
        zodiacCenter.setLocation(-78000f, -42000f);  // Your base position
        zodiacCenter.setSensorProfile(0f);
        zodiacCenter.setDiscoverable(false);

        // Set Sol's orbit through the zodiac constellations
        var orbitRadius = 3000f;     // orbit size
        var orbitPeriod = 365f;     // 12 years (365 * 12 = 4380f)
        var startingAngle = 0f;      // 3 o'clock position

        solAnchor.setCircularOrbit(zodiacCenter, startingAngle, orbitRadius, orbitPeriod);

        // Place zodiac labels
        doZodiacLabels(hyper, zodiacCenter, orbitRadius);
    }

    private static void doZodiacLabels(LocationAPI hyper, SectorEntityToken center, float radius) {
        String[] zodiacSigns = {
                "Aries", "Taurus", "Gemini", "Cancer",
                "Leo", "Virgo", "Libra", "Scorpio",
                "Sagittarius", "Capricorn", "Aquarius", "Pisces"
        };

        for (int i = 0; i < zodiacSigns.length; i++) {
            var angle = (360f / zodiacSigns.length) * i;

            var label = hyper.addCustomEntity(
                    "zodiac_" + zodiacSigns[i].toLowerCase(),
                    zodiacSigns[i],
                    "Sol_Labels",
                    "neutral"
            );

            // Place labels outside the orbit
            label.setCircularOrbit(center, angle, radius + 2000f, 999999f);
        }
    }
}