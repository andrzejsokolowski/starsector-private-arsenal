package privatearsenal.industries.reverseEngineering;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import privatearsenal.utils.AbstractSubmarketIndustry;
import privatearsenal.utils.ReverseEngSettings;
import privatearsenal.utils.SaveOneData;

/**
 * Drives one item-type's reverse-engineering loop: pull an item out of the
 * storage submarket, work it for a number of days, then finish it.
 *
 * Every finished item is registered in the produced-set so the integrated
 * Private Arsenal can stock it. The actual blueprint is only produced when the
 * hub is improved (the story-point improvement "also produces a blueprint").
 * Items already in the produced-set are never re-scanned. AI cores only shorten
 * the time.
 */
public abstract class AbstractReverseEngineeringIndustry<T> extends AbstractSubmarketIndustry {
    public static final Logger log = Global.getLogger(AbstractReverseEngineeringIndustry.class);

    private static final String DRONE_REPLICATOR = "drone_replicator";

    protected int daysRequired;
    protected int daysPassed = 0;

    protected T currentReverseEng = null;

    protected String typeReverse;
    protected String producedSetKey;

    public AbstractReverseEngineeringIndustry(String submarketId, String storageColour, String typeReverse,
            String producedSetKey) {
        super(submarketId, storageColour);
        this.typeReverse = typeReverse;
        this.producedSetKey = producedSetKey;
        this.daysRequired = getDayRequired();
    }

    @Override
    public void advance(float amount) {
        if (isFunctional()) {
            onNewDay();
        }
    }

    /**
     * Flat research days for one item: determined by the assigned AI core
     * (no core / gamma / beta / alpha / omega), minus a flat reduction while a
     * Combat Drone Replicator is installed in the hub. All values are LunaLib
     * sliders. Item type no longer affects the time.
     */
    protected int getDayRequired() {
        int base = ReverseEngSettings.daysForCore(getAiCoreIdNotNull());
        if (hasDroneReplicator()) {
            base -= ReverseEngSettings.droneReplicatorDayReduction();
        }
        return Math.max(1, base);
    }

    /** True when a Combat Drone Replicator colony item is installed in the hub. */
    protected boolean hasDroneReplicator() {
        SpecialItemData item = getSpecialItem();
        return item != null && DRONE_REPLICATOR.equals(item.getId());
    }

    protected abstract boolean initDeconstruction();

    protected abstract String getNameReverse();

    protected abstract String getIdReverse();

    protected abstract String getSprite();

    // blueprint = new SpecialItemData("ship_bp", hullId); // weapon_bp / fighter_bp
    protected abstract SpecialItemData getSpecialItem(String id);

    protected void onNewDay() {
        debugLog("New day event triggered.");

        if (currentReverseEng == null) {
            startNewDeconstruction();
        } else if (daysRequired <= daysPassed) {
            completeDeconstruction();
        } else {
            continueDeconstruction();
        }
    }

    public T getCurrentReverseEng() {
        return currentReverseEng;
    }

    private void startNewDeconstruction() {
        if (initDeconstruction()) {
            debugLog("Deconstruction initialized for " + getNameReverse());
            refreshRequiredDays();
            notifyDeconstructionStart();
        } else {
            debugLog("No " + typeReverse + " available for reverse engineering.");
        }
    }

    private void refreshRequiredDays() {
        if (currentReverseEng != null) {
            daysRequired = getDayRequired();
        }
    }

    private String getAiCoreIdNotNull() {
        String save = getAICoreId();
        return save != null ? save : "none";
    }

    private void notifyDeconstructionStart() {
        String name = getNameReverse();
        String marketName = market.getName();
        int requiredDays = daysRequired;

        Global.getSector().getCampaignUI().addMessage(
                String.format("Reverse engineering has begun for a %s at %s. Required days: %s", name, marketName,
                        requiredDays),
                Global.getSettings().getColor("standardTextColor"),
                name,
                marketName,
                Misc.getHighlightColor(),
                market.getFaction().getBrightUIColor());
    }

    private void continueDeconstruction() {
        refreshRequiredDays();
        debugLog("Deconstruction of " + getNameReverse() + " in progress. " + daysPassed + " days passed out of "
                + daysRequired);
        daysPassed++;
    }

    private void completeDeconstruction() {
        String id = getIdReverse();
        debugLog("Reverse engineering of " + getNameReverse() + " completed.");

        // Every finished item is unlocked in the integrated Private Arsenal.
        registerProduced(id);

        // The story-point improvement additionally produces the actual blueprint in storage.
        int copies = 0;
        boolean made = false;
        if (isImproved()) {
            copies = Math.max(1, ReverseEngSettings.improveBlueprintCopies());
            made = generateBlueprint(id, copies);
        }

        notifyDeconstructionCompletion(copies, made);
        resetDeconstructionVariables();
    }

    private void resetDeconstructionVariables() {
        daysRequired = getDayRequired();
        daysPassed = 0;
        currentReverseEng = null;
    }

    protected void notifyDeconstructionCompletion(int copies, boolean made) {
        MessageIntel intel = new MessageIntel("Reverse engineering of the %s has finished.",
                Misc.getTextColor(),
                new String[] { getNameReverse() },
                Misc.getHighlightColor());

        // Always unlocked in the arsenal; the blueprint itself is the improvement's bonus.
        intel.addLine(BaseIntelPlugin.BULLET + "The item is now stocked in the Private Arsenal.",
                Misc.getHighlightColor(),
                new String[] {});

        if (isImproved()) {
            if (made) {
                intel.addLine(BaseIntelPlugin.BULLET + "A blueprint was also added to storage (x%s).",
                        Misc.getHighlightColor(),
                        new String[] { String.valueOf(copies) });
            } else {
                intel.addLine(BaseIntelPlugin.BULLET + "No storage was available to receive the blueprint.",
                        Misc.getNegativeHighlightColor(),
                        new String[] {});
            }
        }

        intel.setIcon(Global.getSettings().getSpriteName("PrivateArsenal", "notif"));
        Global.getSector().getCampaignUI().addMessage(intel);
        intel.setSound(BaseIntelPlugin.getSoundMinorMessage());
    }

    private boolean generateBlueprint(String id, int copies) {
        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            debugLog("Error: No storage submarket found.");
            return false;
        }

        CargoAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
        SpecialItemData blueprint = getSpecialItem(id);
        storage.addSpecial(blueprint, copies);
        return true;
    }

    /** Records that this item has been reverse-engineered here, for the arsenal. */
    private void registerProduced(String id) {
        SaveOneData<HashSet<String>> store = new SaveOneData<HashSet<String>>(producedSetKey, new HashSet<String>());
        HashSet<String> set = store.getData();
        if (!set.contains(id)) {
            set.add(id);
            store.setData(set);
        }
    }

    /** Read-only access to a produced-set; used by the arsenal submarket. */
    public static Set<String> getProducedSet(String key) {
        SaveOneData<HashSet<String>> store = new SaveOneData<HashSet<String>>(key, new HashSet<String>());
        return store.getData();
    }

    protected void debugLog(String message) {
        if (ReverseEngSettings.debugLogging()) {
            log.info(message);
        }
    }

    @Override
    public boolean isAvailableToBuild() {
        return market.isPlayerOwned();
    }

    @Override
    public boolean canBeDisrupted() {
        return false;
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, Industry.IndustryTooltipMode mode) {
        addCurrentProjectTooltip(tooltip, mode);
    }

    public void addCurrentProjectTooltip(TooltipMakerAPI tooltip, Industry.IndustryTooltipMode mode) {
        if (getMarket() != null && !isBuilding() && isFunctional()
                && mode.equals(Industry.IndustryTooltipMode.NORMAL)) {
            FactionAPI faction = market.getFaction();
            tooltip.addSectionHeading("Current Project " + typeReverse, faction.getBaseUIColor(),
                    faction.getDarkUIColor(),
                    Alignment.MID, 10f);

            if (currentReverseEng != null) {
                TooltipMakerAPI text = tooltip.beginImageWithText(getSprite(), 48);
                text.addPara("Reverse engineering: %s. Time remaining: %s days.", 5f, Misc.getHighlightColor(),
                        getNameReverse(),
                        String.valueOf(daysRequired - daysPassed));
                tooltip.addImageWithText(5f);
            } else {
                tooltip.addPara("No " + typeReverse + " is currently being reverse engineered.", 5f);
            }
        }
    }

    @Override
    public boolean canImprove() {
        return true;
    }
}
