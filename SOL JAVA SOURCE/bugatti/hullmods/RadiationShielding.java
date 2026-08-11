package bugatti.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class RadiationShielding extends BaseHullMod {
    
    // Protection scales with number of ships
    private static final float BASE_PROTECTION = 0.20f;      // 20% with 1 ship
    private static final float PROTECTION_PER_ADDITIONAL = 0.10f; // +10% per additional ship
    private static final float MAX_PROTECTION = 1.0f;       // Cap at 100% reduction
    private static final float SMOD_WEIGHT_BONUS = 1.0f;
    
    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        if (!member.isFlagship()) return;

        var fleet = member.getFleetData().getFleet();
        if (fleet == null) return;

        // Count ships with radiation shielding by size
        float weightedCount = 0;

        for (var m : fleet.getFleetData().getMembersListCopy()) {
            if (m.getVariant() != null && m.getVariant().hasHullMod("radiation_shielding")) {
                float weight = switch (m.getHullSpec().getHullSize()) {
                    case CAPITAL_SHIP -> 3f;
                    case CRUISER -> 2f;
                    case DESTROYER -> 1.5f;
                    case FRIGATE -> 1f;
                    default -> 0f;
                };
                // S-mod bonus: enhanced contribution
                if (m.getVariant().getSMods().contains("radiation_shielding")) {
                    weight += SMOD_WEIGHT_BONUS;
                }
                weightedCount += weight;
            }
        }

        // Calculate weighted ship count (larger ships contribute more)
        if (weightedCount > 0f) {
            // Calculate protection with scaling
            float protection =
                    BASE_PROTECTION +
                            ((weightedCount - 1f) * PROTECTION_PER_ADDITIONAL);

            protection = Math.min(protection, MAX_PROTECTION); // Cap at max

            float multiplier = 1f - protection;

            // Set fleetwide multipliers
            fleet.getMemoryWithoutUpdate().set("$radiationCrewLossMult", multiplier);
            fleet.getMemoryWithoutUpdate().set("$radiationMarineLossMult", multiplier);
            fleet.getMemoryWithoutUpdate().set("$radiationCRDamageMult", multiplier);

        } else {
            // IMPORTANT: clear stale fleet memory when no shielding is present
            fleet.getMemoryWithoutUpdate().unset("$radiationCrewLossMult");
            fleet.getMemoryWithoutUpdate().unset("$radiationMarineLossMult");
            fleet.getMemoryWithoutUpdate().unset("$radiationCRDamageMult");
        }
    }

    @Override
    public boolean hasSModEffect() {
        return true;
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return Math.round(BASE_PROTECTION * 100) + "%";
        if (index == 1) return Math.round(PROTECTION_PER_ADDITIONAL * 100) + "%";
        if (index == 2) return Math.round(MAX_PROTECTION * 100) + "%";
        if (index == 3) {
            // Weight by hull size for description
            switch (hullSize) {
                case CAPITAL_SHIP: return "3.0";
                case CRUISER: return "2.0";
                case DESTROYER: return "1.5";
                case FRIGATE: return "1.0";
            }
        }
        return null;
    }

    @Override
    public void addSModEffectSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec, boolean isForBuildInList) {

        float pad = 10f;
        var good = Misc.getPositiveHighlightColor();

        tooltip.addPara(
                "This ship contributes an additional %s effective weight toward fleet radiation protection.",
                pad,
                good,
                String.valueOf(SMOD_WEIGHT_BONUS)
        );
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

        float pad = 10f;

        var good = Misc.getPositiveHighlightColor();
        var highlight = Misc.getHighlightColor();
        var bad = Misc.getNegativeHighlightColor();

        tooltip.addSectionHeading("Radiation Protection", Alignment.MID, pad);

        tooltip.addPara("Provides %s base fleet-wide radiation protection.", pad, good, Math.round(BASE_PROTECTION * 100f) + "%");

        tooltip.addPara("Each additional ship equipped increases protection by %s, weighted by hull size.", pad, highlight, Math.round(PROTECTION_PER_ADDITIONAL * 100f) + "%");

        tooltip.addPara("Total protection is capped at %s.", pad, good, Math.round(MAX_PROTECTION * 100f) + "%");

        tooltip.addSpacer(5f);

        tooltip.addPara("Combat readiness degradation from radiation is %s mitigated.", pad, Misc.getNegativeHighlightColor(), "partially");

        if (ship != null && ship.getFleetMember() != null) {
            var fleet = ship.getFleetMember()
                    .getFleetData()
                    .getFleet();
            if (fleet != null) {
                var mult = fleet.getMemoryWithoutUpdate()
                        .getFloat("$radiationCrewLossMult");

                var protection = (1f - mult) * 100f;

                tooltip.addSpacer(5f);
                tooltip.addPara("Current fleet radiation protection: %s", pad, Misc.getPositiveHighlightColor(), Math.round(protection) + "%");
            }
        }
    }
}