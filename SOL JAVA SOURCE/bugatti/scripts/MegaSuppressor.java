package bugatti.scripts;

import bugatti.utils.Settings;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.kaysaar.aotd.vok.campaign.econ.globalproduction.models.megastructures.GPBaseMegastructure;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public final class MegaSuppressor {

    private static final Logger log = Global.getLogger(MegaSuppressor.class);

    private MegaSuppressor() {}

    public static void run() {

        if (!Settings.DESPAWN_MEGAS) {
            log.info("[SOL] MegaSuppressor suppressed (FEATURES_DISABLED).");
            return;
        }

        log.info("[SOL] Suppressing AotD megastructures");

        clearPlayerLocks();
        removeVisualEntities();  // cleans planets
    }

    /* ---------------------------------- */

    private static void clearPlayerLocks() {
        var mem = Global.getSector().getPlayerMemoryWithoutUpdate();

        // Kaysaar's Keys
        var plutoKey = "$aotd_mega_system_id_aotd_pluto_station";
        var nidavelirKey = "$aotd_mega_system_id_aotd_nidavelir";

        if (mem.contains(plutoKey)) {
            mem.unset(plutoKey);
            log.info("[SOL] Cleared player lock: " + plutoKey);
        }

        if (mem.contains(nidavelirKey)) {
            mem.unset(nidavelirKey);
            log.info("[SOL] Cleared player lock: " + nidavelirKey);
        }
    }

    private static void removeVisualEntities() {
        List<MarketAPI> marketsToClean = new ArrayList<>();

        for (var system : Global.getSector().getStarSystems()) {

            // HARD SOL EXCLUSION
            if ("Sol".equals(system.getId())) {
                log.info("[SOL] Skipping AotD suppression for Sol system");
                continue;
            }

            List<CustomCampaignEntityAPI> toRemove = new ArrayList<>();
            PlanetAPI nidavelirPlanet = null;

            for (var entity : system.getCustomEntities()) {
                var type = entity.getCustomEntityType();
                if (type == null) continue;

                if (type.equals("aotd_pluto_station")) {
                    toRemove.add(entity);

                    // Pluto orbits a planet - find it normally
                    var focus = entity.getOrbitFocus();
                    if (focus instanceof PlanetAPI && focus.getMarket() != null) {
                        marketsToClean.add(focus.getMarket());
                        log.info(String.format("[SOL] Marked planet %s for cleanup (had %s in it's orbit.)", focus.getName(), type));
                    }
                }
                else if (type.equals("nid_shipyards_damaged")) {
                    toRemove.add(entity);

                    // Nidavelir doesn't orbit - it wraps
                    for (var planet : system.getPlanets()) {
                        if (planet.isStar()) continue;
                        if (planet.getMemoryWithoutUpdate().contains(GPBaseMegastructure.memKey) ||
                                planet.getMemoryWithoutUpdate().contains("$aotd_megastructure")) {

                            var mega = planet.getMemoryWithoutUpdate().get(GPBaseMegastructure.memKey);
                            if (mega != null) {
                                nidavelirPlanet = planet;
                                if (planet.getMarket() != null) {
                                    marketsToClean.add(planet.getMarket());
                                }
                                log.info(String.format("[SOL] Found Nidavelir planet: %s in system %s", planet.getName(), system.getName()));
                                break;
                            }
                        }
                    }
                }
            }

            for (CustomCampaignEntityAPI entity : toRemove) {
                var type = entity.getCustomEntityType();
                var systemName = system.getName();

                var focusName = "unknown";
                var relation  = "orbiting";

                if ("aotd_pluto_station".equals(type)) {
                    var focus = entity.getOrbitFocus();
                    if (focus != null) {
                        focusName = focus.getName();
                    }
                }
                else if ("nid_shipyards_damaged".equals(type) && nidavelirPlanet != null) {
                    focusName = nidavelirPlanet.getName();
                    relation  = "attached to";
                }

                system.removeEntity(entity);

                log.info(String.format("[SOL] Removed AotD entity '%s' from system '%s' (%s %s)", type, systemName, relation, focusName));
            }
        }

        // clean planets that had megas
        for (var market : marketsToClean) {
            cleanMarket(market);
        }
    }

    private static void cleanMarket(MarketAPI market) {
        var modified = false;

        // get planet entity - where VoK stores the megadata
        var planet = market.getPrimaryEntity();

        if (planet == null) {
            log.warn("[SOL] No primary entity for market: " + market.getName());
            return;
        }

        var planetMem = planet.getMemoryWithoutUpdate();

        // remove mega keys
        if (planetMem.contains(GPBaseMegastructure.memKey)) {
            planetMem.unset(GPBaseMegastructure.memKey);
            modified = true;
            log.info("[SOL] Removed GPBaseMegastructure.memKey from " + planet.getName());
        }

        if (planetMem.contains("$aotd_megastructure")) {
            planetMem.unset("$aotd_megastructure");
            planet.removeTag(Tags.NOT_RANDOM_MISSION_TARGET);
            modified = true;
            log.info("[SOL] Removed $aotd_megastructure from " + planet.getName());
        }

        // remove market conditions
        if (market.hasCondition("aotd_pluto_station")) {
            market.removeCondition("aotd_pluto_station");
            modified = true;
            log.info("[SOL] Removed aotd_pluto_station condition from " + market.getName());
        }

        if (market.hasCondition("aotd_nidavelir_complex")) {
            market.removeCondition("aotd_nidavelir_complex");
            modified = true;
            log.info("[SOL] Removed aotd_nidavelir_complex condition from " + market.getName());
        }

        if (modified) {
            market.reapplyConditions();
            log.info("[SOL] Cleaned up " + planet.getName() + " successfully");
        } else {
            log.warn("[SOL] No AOTD data found on " + planet.getName());
        }
    }
}