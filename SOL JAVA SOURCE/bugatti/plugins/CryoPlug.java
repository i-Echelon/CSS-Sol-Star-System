package bugatti.plugins;

import bugatti.utils.CryoInteraction;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class CryoPlug extends BaseCampaignPlugin {
    
    @Override
    public String getId() { return "CryoPlug"; }

    @Override
    public boolean isTransient() { return false; }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        // Check if this entity is your cryochamber
        if (interactionTarget.hasTag("sol_solitary")) {
            return new PluginPick<>(
                    new CryoInteraction(),
                    PickPriority.MOD_SPECIFIC
            );
        }
        return null;
    }
}