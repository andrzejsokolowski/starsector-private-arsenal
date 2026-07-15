package privatearsenal.industries.reverseEngineering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import privatearsenal.ids.Ids;
import privatearsenal.utils.AbstractSubmarketIndustry;
import privatearsenal.utils.ReverseEngSettings;
import privatearsenal.utils.SaveOneData;

/**
 * Drives one item-type's reverse-engineering loop. Each cycle pulls a batch of
 * up to {@link #getSlotCount()} items out of the storage submarket, works the
 * whole batch for a number of days, then finishes them all together.
 *
 * The number of items researched in parallel is the hub's slot budget
 * (baseResearchSlotsPerUpgradeLevel * hub tier) divided by this item type's slot
 * size, so bigger items (ships) take up more of the budget and fewer of them run
 * at once than weapons.
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

    /** Items currently being researched together on a single shared timer. */
    protected List<T> currentBatch = new ArrayList<T>();

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

    // --- Slot budget -------------------------------------------------------

    /**
     * Current built tier of the Reverse Engineering Hub (1-3), read from the
     * market the same way the Private Arsenal does, or 0 if none is present.
     */
    protected int getHubTier() {
        if (market == null) {
            return 0;
        }
        if (market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
            return 3;
        }
        if (market.hasIndustry(Ids.REVERSE_ENG_2_IND)) {
            return 2;
        }
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
            return 1;
        }
        return 0;
    }

    /** Slots one item of this type consumes from the budget (weapon/fighter/ship). */
    protected abstract int getItemSlotSize();

    /**
     * How many items of this type may be researched in parallel right now:
     * (base slots * hub tier) / item size, at least 1 while the hub is built.
     */
    protected int getSlotCount() {
        int tier = getHubTier();
        if (tier <= 0) {
            return 0;
        }
        int budget = ReverseEngSettings.baseResearchSlots() * tier;
        int size = Math.max(1, getItemSlotSize());
        return Math.max(1, budget / size);
    }

    // --- Research time -----------------------------------------------------

    /**
     * Flat research days for one batch: determined by the assigned AI core
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

    private String getAiCoreIdNotNull() {
        String save = getAICoreId();
        return save != null ? save : "none";
    }

    // --- Per-item hooks implemented by each type ---------------------------

    /**
     * Pick the next available item to research, remove it from the storage
     * submarket, and return it. Returns null when nothing suitable is left.
     * Ids already chosen for the current batch are passed in so the same item
     * is never picked twice in one batch.
     */
    protected abstract T pickNextItem(Set<String> excludeIds);

    protected abstract String getId(T item);

    protected abstract String getName(T item);

    // blueprint = new SpecialItemData("ship_bp", hullId); // weapon_bp / fighter_bp
    protected abstract SpecialItemData getSpecialItem(String id);

    /**
     * Whether this item can be turned into a working blueprint. Some hulls,
     * weapons and wings are tagged {@code no_bp_drop} by their mod (e.g. Tahlan's
     * Legio daemon ships): vanilla refuses to hand out blueprints for them, and a
     * {@code ship_bp}/{@code weapon_bp}/{@code fighter_bp} built for such an id is
     * non-functional (it never sticks in the player's known blueprints). We still
     * reverse-engineer and stock these in the Private Arsenal; we just skip the
     * blueprint step. Defaults to true.
     */
    protected boolean isBlueprintable(T item) {
        return true;
    }

    // --- Daily loop --------------------------------------------------------

    protected void onNewDay() {
        if (currentBatch == null) {
            currentBatch = new ArrayList<T>();
        }
        debugLog("New day event triggered for " + typeReverse + ".");

        if (!currentBatch.isEmpty()) {
            // React to a mid-research AI core / drone change.
            daysRequired = getDayRequired();
            if (daysRequired <= daysPassed) {
                completeBatch();
            }
        }

        if (currentBatch.isEmpty()) {
            // No-gap refill: a finished batch is replaced the same day.
            startNewBatch();
        } else {
            daysPassed++;
        }
    }

    private void startNewBatch() {
        int slots = getSlotCount();
        if (slots <= 0) {
            return;
        }

        List<T> batch = new ArrayList<T>();
        Set<String> pickedIds = new HashSet<String>();
        for (int i = 0; i < slots; i++) {
            T item = pickNextItem(pickedIds);
            if (item == null) {
                break;
            }
            batch.add(item);
            pickedIds.add(getId(item));
        }

        if (batch.isEmpty()) {
            debugLog("No " + typeReverse + " available for reverse engineering.");
            return;
        }

        currentBatch = batch;
        daysRequired = getDayRequired();
        daysPassed = 0;
        notifyBatchStart(batch);
    }

    private void completeBatch() {
        List<T> batch = currentBatch;
        debugLog("Reverse engineering of " + batch.size() + " " + typeReverse + "(s) completed.");

        boolean improved = isImproved();
        int copies = improved ? Math.max(1, ReverseEngSettings.improveBlueprintCopies()) : 0;
        int blueprintsMade = 0;
        int blueprintsFailed = 0;
        int blueprintsSkipped = 0;

        for (T item : batch) {
            String id = getId(item);
            // Every finished item is unlocked in the integrated Private Arsenal.
            registerProduced(id);
            // The story-point improvement additionally produces the actual blueprint in storage.
            if (improved) {
                // Items the game forbids as blueprints (no_bp_drop) stay Arsenal-only.
                if (!isBlueprintable(item)) {
                    blueprintsSkipped++;
                } else if (generateBlueprint(id, copies)) {
                    blueprintsMade++;
                } else {
                    blueprintsFailed++;
                }
            }
        }

        notifyBatchCompletion(batch, copies, blueprintsMade, blueprintsFailed, blueprintsSkipped);

        currentBatch = new ArrayList<T>();
        daysPassed = 0;
    }

    // --- Notifications (batched: one message per cycle, not per item) ------

    private void notifyBatchStart(List<T> batch) {
        int n = batch.size();
        String countLabel = n + " " + pluralize(typeReverse, n);
        String marketName = market.getName();

        Global.getSector().getCampaignUI().addMessage(
                String.format("Reverse engineering started on %s at %s. %s days each.",
                        countLabel, marketName, "" + daysRequired),
                Global.getSettings().getColor("standardTextColor"),
                countLabel,
                marketName,
                Misc.getHighlightColor(),
                market.getFaction().getBrightUIColor());
    }

    protected void notifyBatchCompletion(List<T> batch, int copies, int made, int failed, int skipped) {
        int n = batch.size();
        String countLabel = n + " " + pluralize(typeReverse, n);
        String names = joinNames(batch, 12);

        MessageIntel intel = new MessageIntel("Reverse engineering finished: %s.",
                Misc.getTextColor(),
                new String[] { countLabel },
                Misc.getHighlightColor());

        intel.addLine(BaseIntelPlugin.BULLET + "%s",
                Misc.getHighlightColor(),
                new String[] { names });

        intel.addLine(BaseIntelPlugin.BULLET + "Now stocked in the Private Arsenal.",
                Misc.getHighlightColor(),
                new String[] {});

        if (made > 0) {
            intel.addLine(BaseIntelPlugin.BULLET + "Blueprints added to storage for %s (x%s each).",
                    Misc.getHighlightColor(),
                    new String[] { made + " " + pluralize("item", made), "" + copies });
        }
        if (failed > 0) {
            intel.addLine(BaseIntelPlugin.BULLET + "No storage space for %s blueprint(s).",
                    Misc.getNegativeHighlightColor(),
                    new String[] { "" + failed });
        }
        if (skipped > 0) {
            intel.addLine(BaseIntelPlugin.BULLET + "%s item(s) cannot be made into a blueprint; Arsenal-only.",
                    Misc.getGrayColor(),
                    new String[] { "" + skipped });
        }

        intel.setIcon(Global.getSettings().getSpriteName("PrivateArsenal", "notif"));
        Global.getSector().getCampaignUI().addMessage(intel);
        intel.setSound(BaseIntelPlugin.getSoundMinorMessage());
    }

    private String joinNames(List<T> batch, int cap) {
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(cap, batch.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(getName(batch.get(i)));
        }
        int more = batch.size() - shown;
        if (more > 0) {
            sb.append(", and ").append(more).append(" more");
        }
        return sb.toString();
    }

    private static String pluralize(String base, int n) {
        return n == 1 ? base : base + "s";
    }

    // --- Blueprint / produced-set bookkeeping ------------------------------

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

    /**
     * The hull we actually reverse-engineer for a given ship. A degraded "(D)" hull
     * maps to its clean base hull, so a Hammerhead (D) is the same product as a
     * Hammerhead (matching vanilla, where blueprints and known-ship entries are keyed
     * by the base hull). Non-D hulls are returned unchanged. Shared by the ship
     * industry (what it produces) and the storage submarket (what it refuses), so the
     * two always agree.
     */
    public static ShipHullSpecAPI reverseEngHull(ShipHullSpecAPI spec) {
        if (spec != null && spec.isDHull()) {
            ShipHullSpecAPI base = spec.getBaseHull();
            if (base != null) {
                return base;
            }
        }
        return spec;
    }

    /** Base-normalized hull id for a ship, or null if the spec is null. See {@link #reverseEngHull}. */
    public static String reverseEngHullId(ShipHullSpecAPI spec) {
        ShipHullSpecAPI effective = reverseEngHull(spec);
        return effective != null ? effective.getHullId() : null;
    }

    protected void debugLog(String message) {
        if (ReverseEngSettings.debugLogging()) {
            log.info(message);
        }
    }

    // --- Industry plumbing -------------------------------------------------

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

            int n = (currentBatch == null) ? 0 : currentBatch.size();
            if (n > 0) {
                int remaining = Math.max(0, daysRequired - daysPassed);
                tooltip.addPara("Reverse engineering %s. Time remaining: %s days.", 5f, Misc.getHighlightColor(),
                        n + " " + pluralize(typeReverse, n),
                        "" + remaining);
            } else {
                tooltip.addPara("No " + typeReverse + " is currently being reverse engineered. Capacity: %s slot(s).",
                        5f, Misc.getHighlightColor(),
                        "" + getSlotCount());
            }
        }
    }

    @Override
    public boolean canImprove() {
        return true;
    }
}
