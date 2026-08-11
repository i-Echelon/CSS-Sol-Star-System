package bugatti.utils;

import bugatti.scripts.CryoSpawner;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import java.util.List;
import java.util.Map;

public class CryoInteraction implements InteractionDialogPlugin {

    private InteractionDialogAPI dialog;
    private TextPanelAPI textPanel;
    private OptionPanelAPI options;
    private SectorEntityToken entity;

    private List<PersonAPI> inmates;

    private enum OptionId {
        APPROACH,
        CONTINUE,
        LEAVE
    }
    @SuppressWarnings("unchecked")
    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.textPanel = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        this.entity = dialog.getInteractionTarget();

        // Get inmates from entity memory
        this.inmates = (List<PersonAPI>) entity.getMemoryWithoutUpdate().get(CryoSpawner.KEY_INMATES);

        // Initial text
        textPanel.addPara("Your fleet moves into the shadow of the Solitary Cryochamber Facility, a derelict Domain-era solitary confinement vault drifting in silent orbit. Its armored hull is cracked and frost-scored, but faint power signatures still pulse through internal conduits like a lingering heartbeat.");
        textPanel.addPara("Cryogenic containment nodes remain active — degraded, recursive, and partially corrupted. Accessing them will risk triggering long-dormant security directives written during the Collapse. Whatever remains inside has been waiting for centuries, suspended between duty and oblivion.");

        options.addOption("Attempt to breach the station", OptionId.APPROACH);
        options.addOption("Leave", OptionId.LEAVE);
        options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionData == null) return;

        if (optionData instanceof OptionId option) {

            switch (option) {
                case APPROACH -> showApproachText();
                case CONTINUE -> showInmateSelection();
                case LEAVE -> dialog.dismiss();
            }
        } else if (optionData instanceof PersonAPI) {
            selectInmate((PersonAPI) optionData);
        }
    }

    private void showApproachText() {
        options.clearOptions();

        textPanel.addPara("Closing with the cryostation requires slow, deliberate maneuvering. Automated targeting arrays track your fleet with sluggish, half-dead sensors — not hostile, but not entirely inert either. Your techs broadcast layers of spoofed Domain command-tags, hoping the old systems still recognize them.");

        textPanel.addPara("Your boarding team breaches the outer shell through a hull segment warped by centuries of micrometeor impacts. Inside, the air is stale and motionless. Frozen bulkheads open reluctantly, and long-dead officer logs flicker across cracked holoscreens as if trying to remember their last orders.");

        textPanel.addPara("In the central vault, cryopods stand arranged around a dormant containment warhead — part deterrent, part executioner. After a tense override procedure, your specialists manage to disarm the fail-safe. Only then do the pod diagnostics sync, revealing survivors who were never meant to wake again.");

        options.addOption("Continue", OptionId.CONTINUE);
    }

    private void showInmateSelection() {
        options.clearOptions();

        textPanel.addPara(
                "A full sweep of the vault returns fragmented life-signs: " + inmates.size() +
                        " pods still contain viable occupants. The rest show catastrophic failures, corrupted kill-orders, or sealed redacted classifications. " +
                        "The station's command systems destabilize as your intrusion deepens — you will only have time to release " +
                        "one before the cryostation reasserts its automated protocols.",
                Misc.getTextColor(), Misc.getHighlightColor(),
                String.valueOf(inmates.size()), "one"
        );

        // Show inmates preview in tooltip
        TooltipMakerAPI tooltip = textPanel.beginTooltip();
        tooltip.addSectionHeading("CRYO-VAULT LOG: SOLITARY STORAGE UNIT 7-SOL", Alignment.MID, 5f);

        for (PersonAPI person : inmates) {
            tooltip.addSpacer(10f);
            TooltipMakerAPI imageTooltip = tooltip.beginImageWithText(person.getPortraitSprite(), 48f);
            imageTooltip.addPara(person.getNameString(), 0f, Misc.getTextColor(), Misc.getHighlightColor(),
                    person.getNameString());

            String personality = getPersonalityName(person);
            imageTooltip.addPara("Personality: " + personality, 3f, Misc.getGrayColor(), Misc.getHighlightColor(),
                    personality);

            String rapSheet = (String) person.getMemoryWithoutUpdate().get(CryoSpawner.KEY_RAPSHEET);
            if (rapSheet != null) {
                imageTooltip.addPara("Offense: " + rapSheet, 3f, Misc.getStoryBrightColor(), Misc.getHighlightColor());
            }

            tooltip.addImageWithText(0f);
        }

        textPanel.addTooltip();

        // Check if player can recruit
        int maxOfficers = (int) Global.getSector().getCharacterData().getPerson().getStats()
                .getOfficerNumber().getModifiedValue();
        int currentOfficers = Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy().size();

        boolean canRecruit = currentOfficers < maxOfficers;

        // Add selection options for each inmate
        for (final PersonAPI person : inmates) {
            String optionName = "Release: " + person.getNameString();
            options.addOption(optionName, person);

            // Add detailed tooltip when hovering
            options.addOptionTooltipAppender(person, (tooltip1, hadOtherText) -> {
                tooltip1.addSectionHeading(person.getNameString(), Alignment.MID, 3f);
                tooltip1.addImage(person.getPortraitSprite(), 128f, 3f);
                tooltip1.addPara("Psyche: " + getPersonalityName(person), 5f, Misc.getGrayColor(),
                        Misc.getHighlightColor(), getPersonalityName(person));

                String rapSheet = (String) person.getMemoryWithoutUpdate().get(CryoSpawner.KEY_RAPSHEET);
                if (rapSheet != null) {
                    tooltip1.addPara(rapSheet, 5f, Misc.getStoryDarkBrighterColor(), Misc.getHighlightColor());
                }

                tooltip1.addPara("Their skills and capabilities will be revealed upon recruitment.",
                        5f, Misc.getGrayColor());
            });

            if (!canRecruit) {
                options.setEnabled(person, false);
                options.setTooltip(person, "Fleet is at maximum number of officers.");
            }
        }

        options.addOption("Leave", OptionId.LEAVE);
        options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void selectInmate(PersonAPI person) {
        options.clearOptions();

        textPanel.addPara("As you authorize the release sequence, the selected cryopod cycles through a cascade of decompression, warming fluid, and neural reactivation pulses. Across the chamber, the remaining pods abruptly hard-lock, their vitals flatlining as the system initiates purge protocols.");

        textPanel.addPara("The revived sleeper is extracted by your medics — disoriented, frostbitten, and blinking against light they haven’t seen in centuries. Their identity stabilizes on your datapad as their memories begin to surface. Whatever sentence they once served is now irrelevant; their future lies with your fleet.");

        textPanel.addPara(
                "Officer " + person.getNameString() + " has joined your fleet.",
                Misc.getPositiveHighlightColor()
        );

        Global.getSector().getPlayerFleet().getFleetData().addOfficer(person);

        options.addOption("Leave", OptionId.LEAVE);
        options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);

        // Fade out and remove the entity
        Misc.fadeAndExpire(entity);
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {}
    @Override
    public void advance(float amount) {}
    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {}
    @Override
    public Object getContext() {
        return null;
    }
    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
    // Name Helper
    private String getPersonalityName(PersonAPI person) {
        String personalityId = person.getPersonalityAPI().getId();
        return personalityId.substring(0, 1).toUpperCase() + personalityId.substring(1).toLowerCase();
    }
}