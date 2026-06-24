package privatearsenal.industries.reverseEngineering;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import privatearsenal.ids.Ids;
import privatearsenal.utils.DailyCycleTracker;
import privatearsenal.utils.MultiTierIndustry;
import privatearsenal.utils.ReverseEngSettings;

public class ReverseEngineeringIndustry extends MultiTierIndustry {

    private DailyCycleTracker dailyCycleTracker;

    public ReverseEngineeringIndustry() {
        super(Ids.REVERSE_ENG_SUB, "repaReverseEngHubStorageColour", false);
        this.dailyCycleTracker = new DailyCycleTracker();
        initializeIndustries();
    }

    public boolean conditionToAdvance(float amount) {
        return dailyCycleTracker.newDay();
    }

    private void initializeIndustries() {
        // Ajout des industries avec des IDs uniques
        addIndustry(Ids.REVERSE_ENG_1_IND, 1, new ReverseEngineeringWeaponIndustry());
        addIndustry(Ids.REVERSE_ENG_2_IND, 2, new ReverseEngineeringFighterWingIndustry());
        addIndustry(Ids.REVERSE_ENG_3_IND, 3, new ReverseEngineeringShipIndustry());
    }

    /** The hub owns both the reverse-engineering storage and the Private Arsenal. */
    @Override
    protected List<String[]> getManagedSubmarkets() {
        List<String[]> list = super.getManagedSubmarkets();
        list.add(new String[] { Ids.PRIVATE_ARSENAL_SUB, "repaPlayerWeaponShopColor" });
        return list;
    }

    @Override
    public String getDescriptionOverride() {
        String toReturn = "A high-tech facility dedicated to analyzing and reproducing advanced technology through reverse engineering. By deconstructing recovered items, it allows you to expand your faction's capabilities.\n\nPlace items into the storage to reverse engineer them. Each finished item is unlocked in the integrated Private Arsenal submarket, where you can buy copies. Items already reverse-engineered here are refused by the storage. The hub researches several items of each type in parallel; capacity grows with the hub tier, and larger items take up more of it (a ship uses more capacity than a weapon).\n\nResearch speed and Arsenal prices both depend on the assigned AI core (an Omega core makes the Arsenal free). Improving the hub also produces the item's actual blueprint in storage, and installing a Combat Drone Replicator colony item speeds up research.";

        toReturn += "\n\n";
        if (isTier(1)) {
            toReturn += "Tier 1: Only weapons are allowed.";
        } else if (isTier(2)) {
            toReturn += "Tier 2: Only weapons and fighter wings are allowed.";
        } else if (isTier(3)) {
            toReturn += "Tier 3: Weapons, fighter wings, and ships are allowed.";
        }
        toReturn += "\n\n";
        toReturn += "Choose in priority the item you don t have discovered yet.";
        return toReturn;
    }

    @Override
    public void addAICoreSection(TooltipMakerAPI tooltip, String coreId, Industry.AICoreDescriptionMode mode) {
        if (ReverseEngSettings.OMEGA_CORE.equals(coreId)) {
            addCoreDescription(tooltip, mode, "Omega", coreId);
        } else if (Commodities.ALPHA_CORE.equals(coreId)) {
            addCoreDescription(tooltip, mode, "Alpha", coreId);
        } else if (Commodities.BETA_CORE.equals(coreId)) {
            addCoreDescription(tooltip, mode, "Beta", coreId);
        } else if (Commodities.GAMMA_CORE.equals(coreId)) {
            addCoreDescription(tooltip, mode, "Gamma", coreId);
        }
    }

    private void addCoreDescription(TooltipMakerAPI tooltip, Industry.AICoreDescriptionMode mode, String coreLevel,
            String coreId) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();
        String pre = (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST
                || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP)
                        ? coreLevel + "-level AI core. "
                        : coreLevel + "-level AI core currently assigned. ";

        String days = "" + ReverseEngSettings.daysForCore(coreId);
        String price = formatPriceMult(ReverseEngSettings.priceMultForCore(coreId));
        String desc = pre + "Reverse engineering takes %s days per item. Arsenal price: %s.";

        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(coreId);
            TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48.0F);
            text.addPara(desc, 0.0F, highlight, days, price);
            tooltip.addImageWithText(opad);
        } else {
            tooltip.addPara(desc, opad, highlight, days, price);
        }
    }

    private static String formatPriceMult(float mult) {
        if (mult <= 0f) {
            return "free";
        }
        return String.format("%.1fx base value", mult);
    }

    @Override
    public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
        float opad = 10f;
        int copies = Math.max(1, ReverseEngSettings.improveBlueprintCopies());
        String copiesText = copies > 1 ? (copies + " copies of its blueprint") : "its blueprint";
        info.addPara("Each reverse-engineered item also produces %s in storage. Un-improved hubs only stock the "
                + "Private Arsenal.", 0f, Misc.getHighlightColor(), copiesText);
        info.addSpacer(opad);
        super.addImproveDesc(info, mode);
    }

    /** Allow installing the Combat Drone Replicator colony item into the hub. */
    @Override
    public boolean wantsToUseSpecialItem(SpecialItemData data) {
        return data != null && "drone_replicator".equals(data.getId());
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, Industry.IndustryTooltipMode mode) {
        if (isTier(3)) {
            ReverseEngineeringShipIndustry shipIndustry = (ReverseEngineeringShipIndustry) getIndustryById(
                    Ids.REVERSE_ENG_3_IND);
            if (shipIndustry != null) {
                shipIndustry.addRightAfterDescriptionSection(tooltip, mode);
            }
        }
        if (isTier(2) || isTier(3)) {
            ReverseEngineeringFighterWingIndustry fighterWingIndustry = (ReverseEngineeringFighterWingIndustry) getIndustryById(
                    Ids.REVERSE_ENG_2_IND);
            if (fighterWingIndustry != null) {
                fighterWingIndustry.addRightAfterDescriptionSection(tooltip, mode);
            }
        }
        ReverseEngineeringWeaponIndustry weaponIndustry = (ReverseEngineeringWeaponIndustry) getIndustryById(
                Ids.REVERSE_ENG_1_IND);
        if (weaponIndustry != null) {
            weaponIndustry.addRightAfterDescriptionSection(tooltip, mode);
        }
    }

    public void addCurrentProjectTooltip(TooltipMakerAPI tooltip, Industry.IndustryTooltipMode mode) {
        ReverseEngineeringShipIndustry shipIndustry = (ReverseEngineeringShipIndustry) getIndustryById(
                Ids.REVERSE_ENG_3_IND);
        if (shipIndustry != null) {
            shipIndustry.addCurrentProjectTooltip(tooltip, mode);
        }
    }

    @Override
    public boolean canImprove() {
        return true;
    }

    @Override
    public boolean canBeDisrupted() {
        return false;
    }

    @Override
    public boolean isAvailableToBuild() {
        return market.isPlayerOwned();
    }
}

class ReverseEngineeringShipIndustry extends AbstractReverseEngineeringIndustry<ShipVariantAPI> {
    public ReverseEngineeringShipIndustry() {
        super(Ids.REVERSE_ENG_SUB, "repaReverseEngHubStorageColour", "ship", Ids.PRODUCED_SHIPS_MEMORY);
    }

    @Override
    protected int getItemSlotSize() {
        return ReverseEngSettings.sizeShip();
    }

    @Override
    protected ShipVariantAPI pickNextItem(Set<String> excludeIds) {
        SubmarketAPI sub = market.getSubmarket(Ids.REVERSE_ENG_SUB);

        if (sub == null) {
            debugLog("Error: SubmarketAPI is null.");
            return null;
        }

        CargoAPI storage = sub.getCargo();
        if (storage == null || storage.getMothballedShips() == null) {
            debugLog("Error: CargoAPI or Mothballed ships list is null.");
            return null;
        }

        List<FleetMemberAPI> ships = storage.getMothballedShips().getMembersListCopy();
        if (ships.isEmpty()) {
            return null;
        }

        Set<String> unlockedShips = Global.getSector().getPlayerFaction().getKnownShips();
        Set<String> produced = getProducedSet(producedSetKey);
        List<FleetMemberAPI> availableShips = new ArrayList<>();
        List<FleetMemberAPI> allInCargoShips = new ArrayList<>();

        for (FleetMemberAPI ship : ships) {
            String shipHullId = ship.getHullId();
            // Never re-scan something already produced or already chosen for this batch.
            if (produced.contains(shipHullId) || excludeIds.contains(shipHullId)) {
                continue;
            }
            if (!unlockedShips.contains(shipHullId)) {
                availableShips.add(ship);
            }
            allInCargoShips.add(ship);
        }

        if (availableShips.isEmpty()) {
            availableShips.addAll(allInCargoShips);
        }

        for (FleetMemberAPI ship : availableShips) {
            if (ship != null) {
                ShipVariantAPI variant = ship.getVariant();
                transferWeaponsAndWingsToStorage(ship);
                storage.getMothballedShips().removeFleetMember(ship);
                return variant;
            }
        }

        return null;
    }

    public void transferWeaponsAndWingsToStorage(FleetMemberAPI fleetMember) {
        if (fleetMember == null || fleetMember.getVariant() == null) {
            return;
        }

        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            return;
        }

        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        CargoAPI storageCargo = storage.getCargo();

        ShipVariantAPI variant = fleetMember.getVariant();
        ShipHullSpecAPI hullSpec = variant.getHullSpec();

        HashMap<String, String> builtInWeapons = hullSpec.getBuiltInWeapons();
        List<String> builtInWings = hullSpec.getBuiltInWings();

        for (String weaponSlot : variant.getFittedWeaponSlots()) {
            WeaponSpecAPI weaponSpec = variant.getWeaponSpec(weaponSlot);
            if (weaponSpec != null && !builtInWeapons.containsKey(weaponSlot)) {
                storageCargo.addWeapons(weaponSpec.getWeaponId(), 1);
            }
        }

        for (String wingId : variant.getFittedWings()) {
            if (!builtInWings.contains(wingId)) {
                storageCargo.addFighters(wingId, 1);
            }
        }

        storage.getCargo().sort();
    }

    @Override
    protected String getName(ShipVariantAPI item) {
        return item.getHullSpec().getNameWithDesignationWithDashClass();
    }

    @Override
    protected String getId(ShipVariantAPI item) {
        return item.getHullSpec().getHullId();
    }

    protected SpecialItemData getSpecialItem(String id) {
        return new SpecialItemData("ship_bp", id);
    }
}

class ReverseEngineeringWeaponIndustry extends AbstractReverseEngineeringIndustry<WeaponSpecAPI> {

    public ReverseEngineeringWeaponIndustry() {
        super(Ids.REVERSE_ENG_SUB, "repaReverseEngHubStorageColour", "weapon", Ids.PRODUCED_WEAPONS_MEMORY);
    }

    @Override
    protected int getItemSlotSize() {
        return ReverseEngSettings.sizeWeapon();
    }

    @Override
    protected WeaponSpecAPI pickNextItem(Set<String> excludeIds) {
        SubmarketAPI sub = market.getSubmarket(Ids.REVERSE_ENG_SUB);

        if (sub == null) {
            debugLog("Error: SubmarketAPI is null.");
            return null;
        }

        CargoAPI storage = sub.getCargo();
        if (storage == null || storage.getWeapons() == null) {
            debugLog("Error: CargoAPI or Weapons list is null.");
            return null;
        }

        List<CargoAPI.CargoItemQuantity<String>> weaponItems = storage.getWeapons();
        if (weaponItems.isEmpty()) {
            return null;
        }

        Set<String> unlockedWeapons = Global.getSector().getPlayerFaction().getKnownWeapons();
        Set<String> produced = getProducedSet(producedSetKey);
        List<String> availableWeapons = new ArrayList<>();
        List<String> allInCargoWeapons = new ArrayList<>();

        for (CargoAPI.CargoItemQuantity<String> weaponItem : weaponItems) {
            String weaponId = weaponItem.getItem();
            // Never re-scan something already produced or already chosen for this batch.
            if (produced.contains(weaponId) || excludeIds.contains(weaponId)) {
                continue;
            }
            if (!unlockedWeapons.contains(weaponId)) {
                availableWeapons.add(weaponId);
            }
            allInCargoWeapons.add(weaponId);
        }

        if (availableWeapons.isEmpty()) {
            availableWeapons.addAll(allInCargoWeapons);
        }

        for (String weaponId : availableWeapons) {
            WeaponSpecAPI weaponSpec = Global.getSettings().getWeaponSpec(weaponId);

            if (weaponSpec != null) {
                storage.removeWeapons(weaponId, 1);
                return weaponSpec;
            }
        }
        return null;
    }

    @Override
    protected String getName(WeaponSpecAPI item) {
        return item.getWeaponName();
    }

    @Override
    protected String getId(WeaponSpecAPI item) {
        return item.getWeaponId();
    }

    protected SpecialItemData getSpecialItem(String id) {
        return new SpecialItemData("weapon_bp", id);
    }
}

class ReverseEngineeringFighterWingIndustry extends AbstractReverseEngineeringIndustry<FighterWingSpecAPI> {

    public ReverseEngineeringFighterWingIndustry() {
        super(Ids.REVERSE_ENG_SUB, "repaReverseEngHubStorageColour", "fighter wing", Ids.PRODUCED_FIGHTERS_MEMORY);
    }

    @Override
    protected int getItemSlotSize() {
        return ReverseEngSettings.sizeFighter();
    }

    @Override
    protected FighterWingSpecAPI pickNextItem(Set<String> excludeIds) {
        SubmarketAPI sub = market.getSubmarket(Ids.REVERSE_ENG_SUB);

        if (sub == null) {
            debugLog("Error: SubmarketAPI is null.");
            return null;
        }

        CargoAPI storage = sub.getCargo();
        if (storage == null || storage.getFighters() == null) {
            debugLog("Error: CargoAPI or FighterWing list is null.");
            return null;
        }

        List<CargoAPI.CargoItemQuantity<String>> fighterWingItems = storage.getFighters();
        if (fighterWingItems.isEmpty()) {
            return null;
        }

        Set<String> unlockedFighter = Global.getSector().getPlayerFaction().getKnownFighters();
        Set<String> produced = getProducedSet(producedSetKey);
        List<String> availableFighter = new ArrayList<>();
        List<String> allInCargoFighter = new ArrayList<>();

        for (CargoAPI.CargoItemQuantity<String> fighterWingItem : fighterWingItems) {
            String fighterWingId = fighterWingItem.getItem();
            // Never re-scan something already produced or already chosen for this batch.
            if (produced.contains(fighterWingId) || excludeIds.contains(fighterWingId)) {
                continue;
            }
            if (!unlockedFighter.contains(fighterWingId)) {
                availableFighter.add(fighterWingId);
            }
            allInCargoFighter.add(fighterWingId);
        }

        if (availableFighter.isEmpty()) {
            availableFighter.addAll(allInCargoFighter);
        }

        for (String fighterWingId : availableFighter) {

            FighterWingSpecAPI fighterWingSpec = Global.getSettings().getFighterWingSpec(fighterWingId);

            if (fighterWingSpec != null) {
                storage.removeFighters(fighterWingId, 1);
                return fighterWingSpec;
            }
        }
        return null;
    }

    @Override
    protected String getName(FighterWingSpecAPI item) {
        return item.getWingName();
    }

    @Override
    protected String getId(FighterWingSpecAPI item) {
        return item.getId();
    }

    protected SpecialItemData getSpecialItem(String id) {
        return new SpecialItemData("fighter_bp", id);
    }
}
