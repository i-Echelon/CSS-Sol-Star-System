package bugatti.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class RadiationPlug extends MagneticFieldTerrainPlugin {

    // Multipliers
    private float crewLossMult = 1f;
    private float marineLossMult = 1f;
    private float crDamageMult = 1f;

    // Per-day rates
    public static final float CR_DAMAGE_FIELD = 0.05f;
    public static final float CREW_LOSS_FIELD = 0.10f;
    public static final float MARINE_LOSS_FIELD = 0.08f;
    public static final float CR_DAMAGE_STORM = 0.15f;
    public static final float CREW_LOSS_STORM = 0.25f;
    public static final float MARINE_LOSS_STORM = 0.20f;

    // Memory keys
    protected static final String MEMKEY_CR_DMG_MULT = "$radiationCRDamageMult";
    protected static final String MEMKEY_CREW_FRAC = "$radiationCrewLossFrac";
    protected static final String MEMKEY_CREW_MSG_ACCUM = "$radiationCrewMsgAccum";
    protected static final String MEMKEY_CREW_LOSS_MULT = "$radiationCrewLossMult";
    protected static final String MEMKEY_MARINE_FRAC = "$radiationMarineLossFrac";
    protected static final String MEMKEY_MARINE_MSG_ACCUM = "$radiationMarineMsgAccum";
    protected static final String MEMKEY_MARINE_LOSS_MULT = "$radiationMarineLossMult";

    // Buff duration for remnant fleets (shorter to ensure reapplication)
    protected static final float BUFF_DURATION = 3.0f;

    // Visibility
    protected float alphaMult = 1f;

    // Throttling
    protected float daysSinceLastMessage = 0f;
    public static final float MESSAGE_INTERVAL = 1f; // days
    public static final float MESSAGE_FORCE_THRESHOLD = 1000f; // force after 50 accumulated deaths

    // Setters
    public void setAlpha(float alphaMult) {this.alphaMult = alphaMult;}
    public void setCrewLossMult(float mult) {this.crewLossMult = mult;}
    public void setMarineLossMult(float mult) {this.marineLossMult = mult;}
    public void setCrDamageMult(float mult) {this.crDamageMult = mult;}

    /* ========================= TOOLTIP ========================= */
    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        var pad = 10f;
        var small = 5f;
        var highlight = Misc.getHighlightColor();
        var bad = Misc.getNegativeHighlightColor();

        var player = Global.getSector().getPlayerFleet();
        var storm = flareManager.isInActiveFlareArc(player);

        var title = storm ? "Radiation Storm" : "Radiation Field";
        tooltip.addTitle(title);

        tooltip.addPara(Global.getSettings().getDescription(getTerrainId(), Description.Type.TERRAIN).getText1(), pad);

        var crewRate = getCrewLossRate(storm) * 100f;
        var marineRate = getMarineLossRate(storm) * 100f;
        var crRate = getCRDamageRate(storm) * 100f;

        var nextPad = pad;
        if (expanded) {
            tooltip.addSectionHeading("Hazards", Alignment.MID, pad);
            nextPad = small;
        }

        tooltip.addPara("Crew Attrition: %s per day.", nextPad, bad, Math.round(crewRate) + "%");
        tooltip.addPara("Marine Attrition: %s per day.", small, bad, Math.round(marineRate) + "%");
        tooltip.addPara("CR Degradation: %s per day.", small, bad, Math.round(crRate) + "%");
        tooltip.addPara("Field interference %s while inside.", small, bad, "disrupts all repairs");

        if (expanded) {
            tooltip.addSectionHeading("Notes", Alignment.MID, pad);
            tooltip.addPara("Remnant fleets bearing the %s signature thrive in this environment, " +
                            "gaining enhanced sensor range, stealth, mobility, and acceleration. " +
                            "These fleets will fiercely defend the radiation zone.",
                    nextPad, highlight, "Irradiated");
            tooltip.addPara("Storm phases dramatically amplify both hazards and remnant bonuses.",
                    small, highlight, "Storm phases");
            tooltip.addPara("Counteractive logistics require MarShield hullmods",
                    nextPad, highlight, "MarShield");
        }
    }

    /* ========================= HELPERS ========================= */
    private float getMemFloat(CampaignFleetAPI fleet, String key) {
        if (!fleet.getMemoryWithoutUpdate().contains(key)) {
            return 0f;
        }
        return fleet.getMemoryWithoutUpdate().getFloat(key);
    }

    private void setMemFloat(CampaignFleetAPI fleet, String key, float value) {
        fleet.getMemoryWithoutUpdate().set(key, value);
    }

    private float getCrewLossRate(boolean storm) {
        return storm ? CREW_LOSS_STORM : CREW_LOSS_FIELD;
    }

    private float getMarineLossRate(boolean storm) {
        return storm ? MARINE_LOSS_STORM : MARINE_LOSS_FIELD;
    }

    private float getCRDamageRate(boolean storm) {
        return storm ? CR_DAMAGE_STORM : CR_DAMAGE_FIELD;
    }

    /* ========================= REMNANT ========================= */
    protected boolean isRemnantOrAdapted(CampaignFleetAPI fleet) {
        // Check both faction ID and radiation_adapted tag
        return "remnant".equals(fleet.getFaction().getId()) ||
                fleet.hasTag("radiation_adapted");
    }

    protected void applyRemnantBuffs(CampaignFleetAPI fleet, float days) {
        var baseId = getModId();
        var storm = flareManager.isInActiveFlareArc(fleet);

        // Already buffed this frame - prevent duplicate application
        if (fleet.getStats().hasMod(baseId + "_rem_sensor")) {
            return;
        }

        // Remnants notified when player enters field?
        if (storm && !fleet.getMemoryWithoutUpdate().getBoolean("$inRadStorm")) {
            fleet.getMemoryWithoutUpdate().set("$inRadStorm", true, 0.5f);
            if (fleet.isPlayerFleet()) {
                Global.getSector().getCampaignUI().addMessage(
                        "Entering radiation storm - hostile remnant forces are significantly strengthened",
                        Misc.getNegativeHighlightColor()
                );
            }
        } else if (!storm) {
            fleet.getMemoryWithoutUpdate().unset("$inRadStorm");
        }

        // Apply stronger buffs during storms
        var sensorMult = storm ? 2.5f : 1.75f;
        var detectedMult = storm ? 0.4f : 0.65f;
        var burnBonus = storm ? 6f : 4f;
        var accelMult = storm ? 3.5f : 2.75f;

        fleet.getStats().addTemporaryModMult(BUFF_DURATION, baseId + "_rem_sensor",
                "Radiation Resonance", sensorMult, fleet.getStats().getSensorRangeMod());

        fleet.getStats().addTemporaryModMult(BUFF_DURATION, baseId + "_rem_detected",
                "Radiation-Shrouded Signature", detectedMult, fleet.getStats().getDetectedRangeMod());

        fleet.getStats().addTemporaryModFlat(BUFF_DURATION, baseId + "_rem_burn",
                "Radiation Surge Mobility", burnBonus, fleet.getStats().getFleetwideMaxBurnMod());

        fleet.getStats().addTemporaryModMult(BUFF_DURATION, baseId + "_rem_accel",
                "Radiation-Adapted Drives", accelMult, fleet.getStats().getAccelerationMult());

        // Mark fleet as currently buffed
        fleet.getMemoryWithoutUpdate().set("$radiationBuffed", true, BUFF_DURATION + 0.5f);
    }

    /* ========================= DAMAGE / CREW ========================= */
    protected void applyShipDamage(CampaignFleetAPI fleet, float days, boolean storm) {
        var crLoss = getCRDamageRate(storm) * crDamageMult * days;
        var crId = getModId() + "_radiation_cr";

        // Check for custom CR damage multiplier in fleet memory
        if (fleet.getMemoryWithoutUpdate().contains(MEMKEY_CR_DMG_MULT)) {
            crLoss *= fleet.getMemoryWithoutUpdate().getFloat(MEMKEY_CR_DMG_MULT);
        }


        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            // Apply CR damage
            member.getRepairTracker().applyCREvent(-crLoss, crId, "Radiation Exposure");
            // Always suspend repairs while in radiation field
            member.getRepairTracker().setSuspendRepairs(true);
        }
    }

    protected void applyCrewAttrition(CampaignFleetAPI fleet, float days, boolean storm) {
        var crew = fleet.getCargo().getCrew();
        if (crew <= 0) return; // No crew to damage

        var rate = getCrewLossRate(storm) * crewLossMult;

        // Check for custom crew loss multiplier in fleet memory
        if (fleet.getMemoryWithoutUpdate().contains(MEMKEY_CREW_LOSS_MULT)) {
            rate *= fleet.getMemoryWithoutUpdate().getFloat(MEMKEY_CREW_LOSS_MULT);
        }

        if (rate <= 0f) return; // No damage if rate is zero or negative

        // Accumulate fractional crew losses
        var frac = getMemFloat(fleet, MEMKEY_CREW_FRAC);
        frac += crew * rate * days;

        // Convert fractional losses to actual crew deaths
        var lost = (int) frac;
        if (lost > 0) {
            lost = Math.min(lost, crew); // Can't lose more crew than we have
            fleet.getCargo().removeCrew(lost);
            frac -= lost; // Keep the remaining fraction for next calculation

            // Accumulate for message display
            var accum = getMemFloat(fleet, MEMKEY_CREW_MSG_ACCUM);
            setMemFloat(fleet, MEMKEY_CREW_MSG_ACCUM, accum + lost);
        }

        setMemFloat(fleet, MEMKEY_CREW_FRAC, frac);
    }

    protected void applyMarineAttrition(CampaignFleetAPI fleet, float days, boolean storm) {
        var marines = fleet.getCargo().getMarines();
        if (marines <= 0) return; // No marines to damage

        var rate = getMarineLossRate(storm) * marineLossMult;

        // Check for custom marine loss multiplier in fleet memory
        if (fleet.getMemoryWithoutUpdate().contains(MEMKEY_MARINE_LOSS_MULT)) {
            rate *= fleet.getMemoryWithoutUpdate().getFloat(MEMKEY_MARINE_LOSS_MULT);
        }

        if (rate <= 0f) return; // No damage if rate is zero or negative

        // Accumulate fractional marine losses
        var frac = getMemFloat(fleet, MEMKEY_MARINE_FRAC);
        frac += marines * rate * days;

        // Convert fractional losses to actual marine deaths
        var lost = (int) frac;
        if (lost > 0) {
            lost = Math.min(lost, marines); // Can't lose more marines than we have
            fleet.getCargo().removeMarines(lost);
            frac -= lost; // Keep the remaining fraction for next calculation

            // Accumulate for message display
            var accum = getMemFloat(fleet, MEMKEY_MARINE_MSG_ACCUM);
            setMemFloat(fleet, MEMKEY_MARINE_MSG_ACCUM, accum + lost);
        }

        setMemFloat(fleet, MEMKEY_MARINE_FRAC, frac);
    }

    protected void displayCasualtyMessages(CampaignFleetAPI fleet, boolean storm) {
        if (fleet != Global.getSector().getPlayerFleet()) return;

        var crewAccum = getMemFloat(fleet, MEMKEY_CREW_MSG_ACCUM);
        var marineAccum = getMemFloat(fleet, MEMKEY_MARINE_MSG_ACCUM);

        // Check if we should display a message
        var intervalPassed = daysSinceLastMessage >= MESSAGE_INTERVAL;
        var forceMessage = (crewAccum + marineAccum) >= MESSAGE_FORCE_THRESHOLD;
        var hasCasualties = (crewAccum >= 1 || marineAccum >= 1);

        if (hasCasualties && (intervalPassed || forceMessage)) {
            var message = new StringBuilder();

            if (storm) {// Storm messages
                if (crewAccum >= 1 && marineAccum >= 1) {
                    message.append("A total of ")
                            .append((int) crewAccum)
                            .append(" crew and ")
                            .append((int) marineAccum)
                            .append(" marines were killed in a Radiation Storm.");
                } else if (crewAccum >= 1) {
                    message.append("A total of ")
                            .append((int) crewAccum)
                            .append(" crew were killed in a Radiation Storm.");
                } else {
                    message.append("A total of ")
                            .append((int) marineAccum)
                            .append(" marines were killed in a Radiation Storm.");
                }
            } else {// Field messages
                if (crewAccum >= 1 && marineAccum >= 1) {
                    message.append("Radiation exposure killed ")
                            .append((int) crewAccum)
                            .append(" crew and ")
                            .append((int) marineAccum)
                            .append(" marines.");
                } else if (crewAccum >= 1) {
                    message.append("Radiation exposure killed ")
                            .append((int) crewAccum)
                            .append(" crew members.");
                } else {
                    message.append("Radiation exposure killed ")
                            .append((int) marineAccum)
                            .append(" marines.");
                }
            }

            if (!message.isEmpty()) {
                Global.getSector().getCampaignUI().addMessage(message.toString(),
                        Misc.getNegativeHighlightColor());
                setMemFloat(fleet, MEMKEY_CREW_MSG_ACCUM, 0f);
                setMemFloat(fleet, MEMKEY_MARINE_MSG_ACCUM, 0f);
                daysSinceLastMessage = 0f;
            }
        }
    }

    /* ========================= APPLY EFFECT ========================= */
    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (!(entity instanceof CampaignFleetAPI fleet)) return;

        var storm = flareManager.isInActiveFlareArc(fleet);

        if (fleet.isPlayerFleet()) { // Only increment message timer for player fleet
            daysSinceLastMessage += days;

            // Player entry/storm warnings
            if (storm && !fleet.getMemoryWithoutUpdate().getBoolean("$inRadStorm")) {
                fleet.getMemoryWithoutUpdate().set("$inRadStorm", true, 0.5f);
                Global.getSector().getCampaignUI().addMessage(
                        "Entering radiation storm - extreme danger to crew and ships",
                        Misc.getNegativeHighlightColor()
                );
            } else if (!storm && fleet.getMemoryWithoutUpdate().getBoolean("$inRadStorm")) {
                fleet.getMemoryWithoutUpdate().unset("$inRadStorm");
            }

            // First entry to radiation field (not just storm)
            if (!fleet.getMemoryWithoutUpdate().getBoolean("$inRadField")) {
                fleet.getMemoryWithoutUpdate().set("$inRadField", true, 0.5f);
                Global.getSector().getCampaignUI().addMessage(
                        "Entering radiation field - crew and marine casualties expected",
                        Misc.getNegativeHighlightColor()
                );
            }
        }

        // Remnant/adapted fleets get buffs instead of damage
        if (isRemnantOrAdapted(fleet)) {
            applyRemnantBuffs(fleet, days);
            // Set resonance flag for other systems
            fleet.getMemoryWithoutUpdate().set("$radiationResonance", true, BUFF_DURATION + 0.5f);
            // Don't apply damage to adapted fleets
            super.applyEffect(entity, days);
            return;
        }

        // Apply negative effects to non-adapted
        applyCrewAttrition(fleet, days, storm);
        applyMarineAttrition(fleet, days, storm);
        applyShipDamage(fleet, days, storm);

        displayCasualtyMessages(fleet, storm); // Display combined casualty messages

        super.applyEffect(entity, days);
    }

    @Override
    public String getTerrainName() {
        return flareManager.isInActiveFlareArc(Global.getSector().getPlayerFleet())
                ? "Radiation Storm"
                : "Radiation Field";
    }

    @Override
    public String getNameForTooltip() {
        return "Radiation Zone";
    }

    @Override
    public boolean hasAIFlag(Object flag) {
        return  flag == TerrainAIFlags.CR_DRAIN ||
                flag == TerrainAIFlags.NOT_SUPER_DANGEROUS_UNLESS_GO_SLOW;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        renderer.render(viewport.getAlphaMult() * alphaMult);
    }

    /* ========================= TERRAIN CREATION ========================= */
    /**
     * Creates a radiation field terrain around a celestial body.
     *
     * @param centerEntity The entity to center the radiation field around
     * @param flareProbability Probability of radiation storms (0.0 to 1.0)
     * @param width Width of the radiation band
     * @param midRadius Middle radius for the magnetic field effect (typically 300-500)
     * @param visualBuffer Extra radius beyond the band for visual fade-out effect
     * @param alphaMult Alpha multiplier for visual transparency (0.0 to 1.0)
     * @return The created radiation field terrain
     */
    public static CampaignTerrainAPI createRadiationField(SectorEntityToken centerEntity,
                                                          float flareProbability,
                                                          float width,
                                                          float midRadius,
                                                          float visualBuffer,
                                                          float alphaMult) {
        var system = centerEntity.getStarSystem();

        // Calculate radiation field dimensions
        var bandWidth = centerEntity.getRadius() + width;
        var visStartRadius = centerEntity.getRadius();
        var visEndRadius = centerEntity.getRadius() + width + visualBuffer;

        // Define radiation field colors
        var baseColor = new Color(255, 50, 50, 100);
        Color[] auroraColors = {
                new Color(235, 100, 140),  // Pink-red
                new Color(210, 110, 180),  // Magenta
                new Color(190, 140, 150),  // Dusty rose
                new Color(210, 190, 140),  // Tan
                new Color(170, 200, 90),   // Yellow-green
                new Color(250, 230, 65),   // Bright yellow
                new Color(220, 70, 20)     // Deep orange-red
        };

        // Create the terrain
        var radField = (CampaignTerrainAPI) system.addTerrain("radiation_field",
                new MagneticFieldParams(bandWidth, midRadius, centerEntity, visStartRadius, visEndRadius,
                        baseColor, flareProbability, auroraColors));

        radField.setCircularOrbit(centerEntity, 0, 0, 100);

        // Apply custom alpha if using our plugin
        if (radField.getPlugin() instanceof RadiationPlug) {
            ((RadiationPlug) radField.getPlugin()).setAlpha(alphaMult);
        }

        return radField;
    }

    /**
     * Creates a radiation field with default visual parameters.
     *
     * @param centerEntity The entity to center the radiation field around
     * @param flareProbability Probability of radiation storms (0.0 to 1.0)
     * @param width Width of the radiation band
     * @param alphaMult Alpha multiplier for visual transparency (0.0 to 1.0)
     * @return The created radiation field terrain
     */
    public static CampaignTerrainAPI createRadiationField(SectorEntityToken centerEntity,
                                                          float flareProbability,
                                                          float width,
                                                          float alphaMult) {
        return createRadiationField(centerEntity, flareProbability, width, 350f, 50f, alphaMult);
    }
}