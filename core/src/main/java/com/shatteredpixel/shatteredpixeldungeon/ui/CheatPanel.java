package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

/**
 * Personal-use floating panel: a translucent bar that you tap to open/minimize,
 * holding an immortality toggle and an EXP multiplier button.
 */
public class CheatPanel extends Component {

	private static final String KEY_IMMORTAL = "cheat_immortal";
	private static final String KEY_EXP_MULT = "cheat_exp_mult";
	private static final int[] MULTS = {1, 2, 5, 10};

	private static final float BAR_ALPHA = 0.35f;
	private static final float PANEL_ALPHA = 0.85f;

	public static final int PANEL_W = 96;
	public static final int BAR_W = 56;
	public static final int BAR_H = 10;

	//persisted state, read by Hero
	public static boolean immortal() {
		return SPDSettings.getBoolean(KEY_IMMORTAL, false);
	}

	public static int expMultiplier() {
		int idx = SPDSettings.getInt(KEY_EXP_MULT, 0, 0, MULTS.length - 1);
		return MULTS[idx];
	}

	private boolean expanded = false;

	private NinePatch barBg;
	private RenderedTextBlock barLabel;
	private PointerArea barArea;

	private NinePatch panelBg;
	private CheckBox immortalBox;
	private RedButton expButton;

	@Override
	protected void createChildren() {
		super.createChildren();

		panelBg = Chrome.get(Chrome.Type.TOAST_TR);
		add(panelBg);

		immortalBox = new CheckBox("Immortal") {
			@Override
			protected void onClick() {
				super.onClick();
				SPDSettings.put(KEY_IMMORTAL, checked());
			}
		};
		immortalBox.checked(immortal());
		add(immortalBox);

		expButton = new RedButton("") {
			@Override
			protected void onClick() {
				int idx = SPDSettings.getInt(KEY_EXP_MULT, 0, 0, MULTS.length - 1);
				idx = (idx + 1) % MULTS.length;
				SPDSettings.put(KEY_EXP_MULT, idx);
				refreshExpLabel();
			}
		};
		add(expButton);
		refreshExpLabel();

		//the always-visible translucent bar
		barBg = Chrome.get(Chrome.Type.TOAST_TR);
		add(barBg);

		barLabel = PixelScene.renderTextBlock(6);
		add(barLabel);

		barArea = new PointerArea(barBg) {
			@Override
			protected void onClick(com.watabou.input.PointerEvent event) {
				expanded = !expanded;
				layout();
			}
		};
		add(barArea);
	}

	private void refreshExpLabel() {
		expButton.text("EXP multiplier: x" + expMultiplier());
		layout();
	}

	@Override
	protected void layout() {
		super.layout();
		if (barBg == null) return;

		//bar sits at the top of this component
		barBg.x = x + (PANEL_W - BAR_W) / 2f;
		barBg.y = y;
		barBg.size(BAR_W, BAR_H);
		barBg.alpha(BAR_ALPHA);

		barLabel.text(expanded ? "v hide" : "^ cheats");
		barLabel.alpha(BAR_ALPHA + 0.35f);
		barLabel.setPos(barBg.x + (BAR_W - barLabel.width()) / 2f,
				barBg.y + (BAR_H - barLabel.height()) / 2f);
		PixelScene.align(barLabel);

		//the bar itself is the only tappable area when minimized
		barArea.x = barBg.x;
		barArea.y = barBg.y;
		barArea.width = BAR_W;
		barArea.height = BAR_H;

		panelBg.visible = panelBg.active = expanded;
		immortalBox.visible = immortalBox.active = expanded;
		expButton.visible = expButton.active = expanded;

		if (expanded) {
			float py = y + BAR_H + 1;
			float rowH = 16;
			panelBg.x = x;
			panelBg.y = py;
			panelBg.size(PANEL_W, rowH * 2 + 8);
			panelBg.alpha(PANEL_ALPHA);

			immortalBox.setRect(x + 4, py + 4, PANEL_W - 8, rowH);
			expButton.setRect(x + 4, py + 4 + rowH, PANEL_W - 8, rowH);
			height = BAR_H + 1 + rowH * 2 + 8;
		} else {
			height = BAR_H;
		}
		width = PANEL_W;
	}
}
