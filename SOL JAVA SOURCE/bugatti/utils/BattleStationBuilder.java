package bugatti.utils;

import bugatti.plugins.RadiationPlug;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantStationFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.Random;

import static bugatti.utils.DerelictBuilder.*;
import static bugatti.utils.UniHelp.*;

public class BattleStationBuilder {
    /**
     * Creates a Remnant battlestation with optional damage state
     */
    public static CampaignFleetAPI addRemnantStation(StarSystemAPI system, SectorEntityToken orbitFocus,
                                                     String name, float angle, float orbitRadius,
                                                     float orbitDays, boolean isDamaged) {
        // Build station & fleet
        var station = FleetFactoryV3.createEmptyFleet(Factions.REMNANTS, FleetTypes.BATTLESTATION, null);

        // Durability - variant based on damage state
        var stationVariant = isDamaged ? "remnant_station2_Damaged" : "remnant_station2_Standard";
        var member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, stationVariant);
        station.getFleetData().addFleetMember(member);

        // Behavior flags
        //station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, false);

        // Mark station as radiation-adapted
        station.addTag("radiation_adapted");
        station.addTag(Tags.NEUTRINO_HIGH);
        station.setStationMode(true);

        // Add to system
        system.addEntity(station);

        // Orbital configuration with spin
        station.setCircularOrbitWithSpin(orbitFocus, angle, orbitRadius, orbitDays, 100f, 250f);

        // Custom Id & name
        station.setId("sol_remnant_station");
        station.setNoFactionInName(true);
        station.setName(name + " (Lethal)");

        // Interaction config
        RemnantThemeGenerator.addRemnantStationInteractionConfig(station);

        // Abilities
        station.clearAbilities();
        station.addAbility(Abilities.TRANSPONDER);
        station.getAbility(Abilities.TRANSPONDER).activate();
        station.getDetectedRangeMod().modifyFlat("gen", 1000f);
        station.setAI(null);
        station.isNoFactionInName();

        // AI commander
        var random = new Random(orbitFocus.getId().hashCode());
        var commander = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE)
                .createPerson(Commodities.ALPHA_CORE, Factions.REMNANTS, random);
        commander.setName(new FullName("Ion", "Eater", FullName.Gender.ANY));
        commander.getName().setLast("Core");
        station.setCommander(commander);
        station.getFlagship().setCaptain(commander);

        // Configure based on damaged state
        if (isDamaged) {
            station.getMemoryWithoutUpdate().set("$damagedStation", true);
            station.setName(name + " (Hazardous)");
            // Damaged stations spawn fewer, weaker fleets
            system.addScript(new RemnantStationFleetManager(station, 1f, 0, 4, 25f, 6, 12));
        } else {
            // Undamaged stations get full officer integration and spawn more fleets
            RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(station.getFlagship());
            RemnantOfficerGeneratorPlugin.addCommanderSkills(commander, station, null, 5, random);
            system.addScript(new RemnantStationFleetManager(station, 1f, 0, 8, 15f, 8, 24));
        }

        // Station to full CR
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        // Station Unique Name
        member.setShipName("Irradiated " + member.getShipName());

        // Orbiting Custom Entities
        doCustomEntity(system, station, "sol_remnant_cache", "Remnant Cache",
                "weapons_cache_remnant","neutral",0f,200f,-100f);

        // hand-picked recoverable variants
        addSpecificDerelict(system, station, 60f, 300f, "radiant_Standard", ShipCondition.AVERAGE);
        addSpecificDerelict(system, station, 150f, 400f, "radiant_Assault", ShipCondition.GOOD);
        addSpecificDerelict(system, station, 220f, 500f, "radiant_Strike", ShipCondition.PRISTINE);

        // Fleet behavior manager - keeps fleets in radiation zone
        system.addScript(new IrradiatedFleetManager(station, system));

        return station;
    }

    /**
     * Manages Remnant fleets spawned by the station, keeping them in the radiation zone
     */
    private static class IrradiatedFleetManager implements EveryFrameScript {
        private final CampaignFleetAPI station;
        private final StarSystemAPI system;
        private boolean done = false;

        public IrradiatedFleetManager(CampaignFleetAPI station, StarSystemAPI system) {
            this.station = station;
            this.system = system;
        }

        @Override
        public void advance(float amount) {
            // Stop if station is destroyed
            if (station.isExpired() || !station.isAlive()) {
                done = true;
                return;
            }

            for (CampaignFleetAPI fleet : system.getFleets()) {
                // Only affect remnant fleets
                if (!Factions.REMNANTS.equals(fleet.getFaction().getId())) continue;
                if (fleet == station) continue; // Skip station itself
                fleet.setNoFactionInName(true);

                float dist = Misc.getDistance(fleet.getLocation(), station.getLocation());

                // Initial setup for newly spawned fleets
                if (!fleet.hasTag("radiation_adapted")) {
                    setupIrradiatedFleet(fleet);
                }

                // Keep fleets tethered to radiation zone
                if (dist > 600f) {
                    // Fleet is leaving the zone - redirect it back
                    pullFleetBackToZone(fleet);
                } else if (dist <= 500f) {
                    // Fleet is in the zone - keep it there
                    maintainZonePresence(fleet);
                }
            }
        }

        private void setupIrradiatedFleet(CampaignFleetAPI fleet) {
            // Tag and rename fleet
            fleet.addTag("radiation_adapted");
            fleet.setName("Irradiated " + fleet.getName());
            fleet.addFloatingText("Radiation Resonance", Color.RED, 2f, false);

            // Rename all ships in the fleet
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                var shipName = member.getShipName();
                if (shipName != null && !shipName.startsWith("Irradiated ")) {
                    member.setShipName("Irradiated " + shipName);
                }
            }

            // Set initial patrol behavior around station
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_PATROL_DISTANCE, 400f);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SOURCE_MARKET, station);
        }

        private void maintainZonePresence(CampaignFleetAPI fleet) {
            // Keep fleets aggressive and anchored to the zone
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE, true);

            // Ensure patrol stays close
            fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_PATROL_DISTANCE, 400f);

            // If fleet has AI and assignments, check if it needs redirection
            if (fleet.getAI() != null) {
                var assignment = fleet.getAI().getCurrentAssignment();
                if (assignment == null) { // Fleet has no assignment
                    fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, station, 999f, "Ionizing Radiation Zone");
                } else {
                    var target = assignment.getTarget();
                    if (target != null && target != station) {
                        float targetDist = Misc.getDistance(target.getLocation(), station.getLocation());
                        // If target is outside zone, clear assignments and patrol station
                        if (targetDist > 500f) {
                            fleet.clearAssignments();
                            fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, station, 999f, "Ionizing Radiation Zone");
                        }
                    }
                }
            }
        }

        private void pullFleetBackToZone(CampaignFleetAPI fleet) {
            // Fleet has strayed too far - force it back
            if (fleet.getAI() != null) {
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, station, 5f, "returning to radiation zone");
                fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, station, 999f, "patrolling radiation zone");
            }

            // Reset patrol parameters
            fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_PATROL_DISTANCE, 400f);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SOURCE_MARKET, station);
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }
    }
}