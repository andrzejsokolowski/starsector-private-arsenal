package privatearsenal.items;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemPlugin.SpecialItemRendererAPI;
import com.fs.starfarer.api.campaign.impl.items.FighterBlueprintItemPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Drop-in replacement for vanilla {@link FighterBlueprintItemPlugin} that does not
 * crash when rendering a blueprint of a fighter whose <b>wing id does not end in
 * "_wing"</b>.
 * <p>
 * Vanilla's icon renderer
 * ({@code com.fs.starfarer.campaign.ui.trade.Object.renderShipWithCorners}) decides
 * whether the id it is handed is a fighter wing purely by the string check
 * {@code id.endsWith("_wing")}. If it does, the wing is resolved to its variant and
 * rendered. If it does not, the id is looked up as a <i>ship hull spec</i> instead,
 * which throws {@code "Ship hull spec [...] not found!"} and takes down the whole
 * campaign UI the moment such a blueprint becomes visible (e.g. sitting in colony
 * storage).
 * <p>
 * Every vanilla fighter wing ends in "_wing", so vanilla never hits this. Some mods
 * (e.g. Arthr's Ships n Shit: {@code ass_gunbed_wing_LRM}, {@code ass_gunbed_wing_flak},
 * {@code ass_gunbed_wing_HESC}) put the suffix in the middle of the id. Reverse Engineered Private Arsenal's
 * reverse-engineering hub can now produce {@code fighter_bp} items for those wings,
 * which is what exposes the vanilla limitation.
 * <p>
 * For "_wing" ids we defer entirely to vanilla. For everything else we feed the
 * renderer the underlying <i>fighter hull id</i> instead, which the renderer's hull
 * branch resolves correctly (it renders the hull's auto-generated "{hullId}_Hull"
 * variant). The ship draw is additionally wrapped in a try/catch so that, even if a
 * particular hull cannot be resolved, the campaign UI keeps running and the blueprint
 * still shows its background, overlay and tooltip.
 */
public class SafeFighterBlueprintItemPlugin extends FighterBlueprintItemPlugin {

    @Override
    public void render(float x, float y, float w, float h, float alphaMult,
                       float glowMult, SpecialItemRendererAPI renderer) {

        String wingId = getWingId();

        // Vanilla path is correct (and the safest) for any normally-named wing.
        if (wingId == null || wingId.endsWith("_wing")) {
            super.render(x, y, w, h, alphaMult, glowMult, renderer);
            return;
        }

        // --- Geometry copied verbatim from vanilla FighterBlueprintItemPlugin.render ---
        float cx = x + w / 2f;
        float cy = y + h / 2f;

        float blX = cx - 24f;
        float blY = cy - 17f;
        float tlX = cx - 14f;
        float tlY = cy + 26f;
        float trX = cx + 28f;
        float trY = cy + 25f;
        float brX = cx + 20f;
        float brY = cy - 18f;

        boolean known = Global.getSector().getPlayerFaction().knowsFighter(wingId);
        float mult = 1f;

        Color bgColor = Global.getSector().getPlayerFaction().getDarkUIColor();
        bgColor = Misc.setAlpha(bgColor, 255);
        renderer.renderBGWithCorners(bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY,
                alphaMult * mult, glowMult * 0.5f * mult, false);

        // The one change vs. vanilla: hand the renderer a hull id (resolved via the wing's
        // variant) so its "{hullId}_Hull" branch is taken instead of the wing branch that
        // requires the id to end in "_wing".
        String renderId = resolveHullId(wingId);
        if (renderId != null) {
            try {
                renderer.renderShipWithCorners(renderId, null, blX, blY, tlX, tlY, trX, trY, brX, brY,
                        alphaMult * mult, glowMult * 0.5f * mult, !known);
            } catch (Throwable t) {
                // Never let one un-renderable icon crash the campaign UI.
            }
        }

        SpriteAPI overlay = Global.getSettings().getSprite("ui", "bpOverlayFighter");
        overlay.setColor(Color.green);
        overlay.setColor(Global.getSector().getPlayerFaction().getBrightUIColor());
        overlay.setAlphaMult(alphaMult);
        overlay.setNormalBlend();
        renderer.renderScanlinesWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, false);

        if (known) {
            renderer.renderBGWithCorners(Color.black, blX, blY, tlX, tlY, trX, trY, brX, brY,
                    alphaMult * 0.5f, 0f, false);
        }

        overlay.renderWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY);
    }

    /** The wing id stored as this blueprint's special data, or null if unavailable. */
    private String getWingId() {
        if (stack == null) return null;
        SpecialItemData data = stack.getSpecialDataIfSpecial();
        return data != null ? data.getData() : null;
    }

    /** Resolve a fighter wing id to its underlying fighter hull id, or null. */
    private String resolveHullId(String wingId) {
        try {
            FighterWingSpecAPI w = (wing != null) ? wing : Global.getSettings().getFighterWingSpec(wingId);
            if (w != null && w.getVariant() != null && w.getVariant().getHullSpec() != null) {
                return w.getVariant().getHullSpec().getHullId();
            }
        } catch (Throwable t) {
            // fall through
        }
        return null;
    }
}
