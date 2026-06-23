package privatearsenal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;

import privatearsenal.ids.Ids;
import privatearsenal.industries.reverseEngineering.ReverseEngineeringIndustry;
import privatearsenal.intels.factionSystem.FactionSystemIntel;
import privatearsenal.intels.factionSystem.FactionSystemManager;
import privatearsenal.utils.ReverseEngSettings;

public class PrivateArsenalModPlugin extends BaseModPlugin {
    public static Logger log = Global.getLogger(PrivateArsenalModPlugin.class);
    protected static boolean isNewGame = false;

    private static final String DRONE_REPLICATOR = "drone_replicator";

    @Override
    public void onApplicationLoad() throws Exception {
        enableDroneReplicatorInHub();
    }

    /**
     * Lets the Combat Drone Replicator colony item be installed into the Reverse
     * Engineering Hub (it is normally limited to ground defenses / heavy batteries),
     * where it shortens research time. The vanilla ground-defense effect is skipped
     * for the hub; the -X day bonus is applied by the hub reading its installed item.
     */
    private void enableDroneReplicatorInHub() {
        String[] hubIds = { Ids.REVERSE_ENG_1_IND, Ids.REVERSE_ENG_2_IND, Ids.REVERSE_ENG_3_IND };

        SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(DRONE_REPLICATOR);
        if (spec != null) {
            spec.setParams(spec.getParams() + ", " + String.join(", ", hubIds));
        }

        InstallableItemEffect vanilla = ItemEffectsRepo.ITEM_EFFECTS.get(DRONE_REPLICATOR);
        if (vanilla != null) {
            ItemEffectsRepo.ITEM_EFFECTS.put(DRONE_REPLICATOR, new DroneReplicatorEffectWrapper(vanilla));
        }
    }

    @Override
    public void configureXStream(XStream x) {
        x.alias("FactionSystemIntel", FactionSystemIntel.class);
    }

    @Override
    public void onNewGame() {
        isNewGame = true;
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore) {
        super.onEnabled(wasEnabledBefore);
    }

    @Override
    public void onGameLoad(boolean newGame) {
        isNewGame = newGame;

        FactionSystemManager manager = FactionSystemManager.createManager();
        manager.refreshProfiles();
    }

    /**
     * Wraps the vanilla Combat Drone Replicator effect. On the Reverse Engineering
     * Hub it shows a research-time description and applies nothing (the day bonus is
     * read from the installed item by the hub); on any other industry it behaves
     * exactly like vanilla.
     */
    private static class DroneReplicatorEffectWrapper implements InstallableItemEffect {

        private final InstallableItemEffect vanilla;

        DroneReplicatorEffectWrapper(InstallableItemEffect vanilla) {
            this.vanilla = vanilla;
        }

        private boolean isHub(Industry industry) {
            return industry instanceof ReverseEngineeringIndustry;
        }

        @Override
        public void apply(Industry industry) {
            if (isHub(industry)) {
                return;
            }
            vanilla.apply(industry);
        }

        @Override
        public void unapply(Industry industry) {
            if (isHub(industry)) {
                return;
            }
            vanilla.unapply(industry);
        }

        @Override
        public void addItemDescription(Industry industry, TooltipMakerAPI tooltip, SpecialItemData data,
                InstallableIndustryItemPlugin.InstallableItemDescriptionMode mode) {
            if (isHub(industry)) {
                tooltip.addPara("Reduces reverse-engineering time by %s day(s) per item.", 10f,
                        Misc.getHighlightColor(), "" + ReverseEngSettings.droneReplicatorDayReduction());
                return;
            }
            vanilla.addItemDescription(industry, tooltip, data, mode);
        }

        @Override
        public List<String> getUnmetRequirements(Industry industry) {
            if (isHub(industry)) {
                return new ArrayList<String>();
            }
            return vanilla.getUnmetRequirements(industry);
        }

        @Override
        public List<String> getUnmetRequirements(Industry industry, boolean checkChain) {
            if (isHub(industry)) {
                return new ArrayList<String>();
            }
            return vanilla.getUnmetRequirements(industry, checkChain);
        }

        @Override
        public List<String> getRequirements(Industry industry) {
            if (isHub(industry)) {
                return new ArrayList<String>();
            }
            return vanilla.getRequirements(industry);
        }

        @Override
        public Set<String> getConditionsRelatedToRequirements(Industry industry) {
            if (isHub(industry)) {
                return new HashSet<String>();
            }
            return vanilla.getConditionsRelatedToRequirements(industry);
        }
    }
}
