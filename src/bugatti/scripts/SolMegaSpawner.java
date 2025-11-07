package bugatti.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDMegastructureRules;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;

public class SolMegaSpawner implements EveryFrameScript {
    private static final Logger log = Global.getLogger(SolMegaSpawner.class);
    private boolean done = false;

    // Luna field IDs & defaults
    private static final String MOD_ID = "sol-modded";
    private static final String FIELD_SPAWN_PLOUTON = "spawn_plouton";
    private static final String FIELD_SPAWN_GUNGNIR  = "spawn_gungnir";
    private static final boolean DEFAULT_SPAWN_PLOUTON = true;
    private static final boolean DEFAULT_SPAWN_GUNGNIR  = true;

    @Override
	public void advance(float amount) { // internals
		if (done || Global.getSector() == null) return;

        StarSystemAPI sol = Global.getSector().getStarSystem("Sol");
        if (sol == null) {
            log.warn("[SOL] System 'Sol' not found - aborting megastructure spawn.");
            return;
        }

        // Read toggles (LunaSettings if present; otherwise default)
        boolean spawnPlouton = DEFAULT_SPAWN_PLOUTON;
        boolean spawnGungnir = DEFAULT_SPAWN_GUNGNIR;

        try {
            if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
                // luna's getBoolean() returns a boxed Boolean - may be null
                Boolean lp = LunaSettings.getBoolean(MOD_ID, FIELD_SPAWN_PLOUTON);
                Boolean lg = LunaSettings.getBoolean(MOD_ID, FIELD_SPAWN_GUNGNIR);
                if (lp != null) spawnPlouton = lp;
                if (lg != null) spawnGungnir = lg;
                log.info(String.format("[SOL] LunaSettings detected - spawnPlouton=%s, spawnGungnir=%s", spawnPlouton, spawnGungnir));
            } else {
                log.info("[SOL] LunaLib not found - using default spawn options.");
            }
        } catch (Throwable t) {
            log.warn("[SOL] LunaSettings read failed - using defaults. (" + t.getMessage() + ")", t);
        }

		PlanetAPI pluto = getPlanetByName(sol, "Pluto");
		PlanetAPI mars = getPlanetByName(sol, "Mars");

        if (pluto == null && mars == null) { // safety check
            log.warn("[SOL] Neither Pluto nor Mars found in Sol System - nothing to spawn.");
            done = true;
            return;
        }

	try {
		if (spawnPlouton && pluto != null && !pluto.getMemoryWithoutUpdate().contains("$sol_plouton_added")) {
			log.info("[SOL] Found Pluto - attempting to spawn Plouton Mining Station...");
			spawnPlouton(pluto);
		} else if (pluto != null) {
			log.info("[SOL] Pluto already has Plouton Station marked as added or disabled in settings.");
		}

		if (spawnGungnir && mars != null && !mars.getMemoryWithoutUpdate().contains("$sol_gungnir_added")) {
			log.info("[SOL] Found Mars - attempting to spawn Gungnir Complex...");
			spawnGungnir(mars);
		} else if (mars != null) {
			log.info("[SOL] Mars already has Gungnir Complex marked as added or disabled in settings.");
		}
	} catch (Exception ex) {
		log.error("[SOL] Exception while spawning megastructures: " + ex.getMessage(), ex);
	}

    done = true;
}
    // validate (case-insensitive)
	private PlanetAPI getPlanetByName(StarSystemAPI system, String name) {
		for (PlanetAPI planet : system.getPlanets()) {
			if (planet.getName() != null && planet.getName().equalsIgnoreCase(name)) {
				return planet;
			}
		}
        log.warn("[SOL] Planet '" + name + "' not found in system '" + system.getId() + "'.");
		return null;
	}


	private void spawnPlouton(PlanetAPI planet) {
        try {
            // Kaysaar's  logic
            AoTDMegastructureRules.putMegastructure(planet, "aotd_pluto_station");

            // orbiting entity (station)
            SectorEntityToken station = planet.getStarSystem().addCustomEntity(
                    "sol_plouton_station", "Plouton Mining Array", "aotd_pluto_station", "neutral");

            if (station == null) { // safety
                log.warn("[SOL] Failed to create Plouton Station entity (returned null).");
                return;
            }
            //
            if (station.getName() == null) station.setName("Plouton Mining Station"); // rename here
            station.addTag("sol_pluto_mega");
            station.setCustomDescriptionId("sol_Plouton");
            // materialize
            float angle = planet.getCircularOrbitAngle();
            float period = planet.getCircularOrbitPeriod();
            station.setCircularOrbitPointingDown(planet, angle, planet.getRadius() + 270 + 70, period);
            // music & discovery
            station.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "aotd_mega");
            MiscellaneousThemeGenerator.makeDiscoverable(station, 25000, 3000f);
            planet.getMemoryWithoutUpdate().set("$sol_plouton_added", true); // mark

            log.info("[SOL] Successfully spawned Plouton Mining Station orbiting " + planet.getName() + ".");
        } catch (Exception ex) {
            log.error("[SOL] Exception during Plouton Station spawn: " + ex.getMessage(), ex);
        }
    }
	private void spawnGungnir(PlanetAPI planet) {
        try {
            // similar not congruent
            AoTDMegastructureRules.putMegastructure(planet, "aotd_nidavelir");

            SectorEntityToken shipyard = planet.getStarSystem().addCustomEntity(
                    "sol_gungnir_complex","Gungnir Shipyard","nid_shipyards_damaged","neutral");

            if (shipyard == null) {
                log.warn("[SOL] Failed to create Gungnir Complex entity (returned null).");
                return;
            }

            if (shipyard.getName() == null) shipyard.setName("Gungnir Complex"); // rename works?
            shipyard.addTag("sol_mars_mega");

            planet.setDiscoverable(true);
            MarketAPI market = planet.getMarket();
            if (market != null) {
                market.setSurveyLevel(MarketAPI.SurveyLevel.SEEN);
            }

            planet.getMemoryWithoutUpdate().set("$sol_gungnir_added", true); // mark

            log.info("[SOL] Successfully spawned Gungnir Complex orbiting " + planet.getName() + ".");
        } catch (Exception ex) {
            log.error("[SOL] Exception during Gungnir Complex spawn: " + ex.getMessage(), ex);
            }
        }

    @Override
    public boolean isDone() { return done; }
    @Override
    public boolean runWhilePaused() { return true; }
}