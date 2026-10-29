package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Spell;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;
import com.zrp200.scrollofdebug.ScrollOfDebug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Personal-use tap menu: pick a category, then an item, then amount/level, then Get.
 */
public class WndCheat {

	private static final int MARGIN = 2;

	private static int btnH() {
		return PixelScene.landscape() ? 14 : 16;
	}

	private static int width() {
		return PixelScene.landscape() ? 144 : 120;
	}

	private static final String[] CAT_NAMES = {
			"Weapons", "Armor", "Rings", "Wands", "Artifacts",
			"Potions", "Scrolls", "Spells", "Trinkets", "Food", "Other"
	};
	private static final Class[] CAT_CLASSES = {
			Weapon.class, Armor.class, Ring.class, Wand.class, Artifact.class,
			Potion.class, Scroll.class, Spell.class, Trinket.class, Food.class, null
	};

	private static ArrayList<Class<? extends Item>> allItems;

	private static ArrayList<Class<? extends Item>> allItems() {
		if (allItems == null) {
			allItems = new ArrayList<>();
			if (ScrollOfDebug.trie != null) {
				for (Class c : ScrollOfDebug.trie.getAllClasses()) {
					if (c != Item.class
							&& Item.class.isAssignableFrom(c)
							&& ScrollOfDebug.canInstantiate(c)) {
						allItems.add((Class<? extends Item>) c);
					}
				}
			}
		}
		return allItems;
	}

	private static String display(Class c) {
		return c.getSimpleName().replaceAll("([a-z0-9])([A-Z])", "$1 $2");
	}

	private static List<Class<? extends Item>> itemsFor(int category) {
		ArrayList<Class<? extends Item>> result = new ArrayList<>();
		for (Class<? extends Item> c : allItems()) {
			if (CAT_CLASSES[category] != null) {
				if (CAT_CLASSES[category].isAssignableFrom(c)) result.add(c);
			} else {
				boolean matched = false;
				for (Class parent : CAT_CLASSES) {
					if (parent != null && parent.isAssignableFrom(c)) {
						matched = true;
						break;
					}
				}
				if (!matched) result.add(c);
			}
		}
		Collections.sort(result, new Comparator<Class<? extends Item>>() {
			@Override
			public int compare(Class<? extends Item> a, Class<? extends Item> b) {
				return display(a).compareToIgnoreCase(display(b));
			}
		});
		return result;
	}

	public static void open() {
		GameScene.show(new Categories());
	}
	
	// ---------------------------------------------------------------- step 1
	
	public static class Categories extends Window {
	
		public Categories() {
			super();
			int width = width();
	
			RenderedTextBlock title = PixelScene.renderTextBlock("Give item: pick a category", 9);
			title.hardlight(TITLE_COLOR);
			title.maxWidth(width - MARGIN * 2);
			title.setPos(MARGIN, MARGIN);
			add(title);
	
			float pos = title.bottom() + MARGIN * 2;
	
			for (int i = 0; i < CAT_NAMES.length; i++) {
				final int index = i;
				RedButton btn = new RedButton(CAT_NAMES[i]) {
					@Override
					protected void onClick() {
						hide();
						GameScene.show(new ItemList(CAT_NAMES[index], itemsFor(index)));
					}
				};
				add(btn);
				btn.setRect(0, pos, width, btnH());
				pos += btnH() + 1;
			}
	
			RedButton close = new RedButton("Close") {
				@Override
				protected void onClick() {
					hide();
				}
			};
			add(close);
			close.setRect(0, pos + MARGIN, width, btnH());
	
			resize(width, (int) close.bottom() + MARGIN);
		}
	}
	
	// ---------------------------------------------------------------- step 2
	
	public static class ItemList extends Window {
	
		private static final int ITEMS_PER_PAGE = 14; // 7 rows × 2 columns
	
		private final String categoryName;
		private final List<Class<? extends Item>> items;
		private int page = 0;
	
		public ItemList(String categoryName, List<Class<? extends Item>> items) {
			this(categoryName, items, 0);
		}
	
		private ItemList(String categoryName, List<Class<? extends Item>> items, int page) {
			super();
			this.categoryName = categoryName;
			this.items = items;
			this.page = page;
	
			final int width = width();
			final int colW = (width - MARGIN) / 2;
	
			int totalPages = Math.max(1, (items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
			if (page >= totalPages) page = totalPages - 1;
			if (page < 0) page = 0;
	
			// Title with page info
			String titleText = categoryName + " (" + items.size() + ")";
			if (totalPages > 1) {
				titleText += "  " + (page + 1) + "/" + totalPages;
			}
			RenderedTextBlock title = PixelScene.renderTextBlock(titleText, 9);
			title.hardlight(TITLE_COLOR);
			title.maxWidth(width - MARGIN * 2);
			title.setPos(MARGIN, MARGIN);
			add(title);
	
			float pos = title.bottom() + MARGIN * 2;
			float leftX  = 0;
			float rightX = colW + MARGIN;
	
			int start = page * ITEMS_PER_PAGE;
			int end   = Math.min(start + ITEMS_PER_PAGE, items.size());
	
			for (int i = start; i < end; i++) {
				final Class<? extends Item> cls = items.get(i);
				RedButton btn = new RedButton(display(cls), 6) {
					@Override
					protected void onClick() {
						hide();
						GameScene.show(new Give(cls, categoryName, items));
					}
				};
				add(btn);
	
				int local = i - start;
				if (local % 2 == 0) {
					btn.setRect(leftX, pos, colW, btnH());
				} else {
					btn.setRect(rightX, pos, colW, btnH());
					pos += btnH() + 1;
				}
			}
	
			// If the last item on the page was in the left column, still advance
			if ((end - start) % 2 != 0) {
				pos += btnH() + 1;
			}
	
			pos += MARGIN;
	
			// Navigation row
			if (totalPages > 1) {
				RedButton prev = new RedButton("◀ Prev") {
					@Override
					protected void onClick() {
						hide();
						GameScene.show(new ItemList(categoryName, items, page - 1));
					}
				};
				prev.enable(page > 0);
				add(prev);
				prev.setRect(0, pos, colW, btnH());
	
				RedButton next = new RedButton("Next ▶") {
					@Override
					protected void onClick() {
						hide();
						GameScene.show(new ItemList(categoryName, items, page + 1));
					}
				};
				next.enable(page < totalPages - 1);
				add(next);
				next.setRect(rightX, pos, colW, btnH());
	
				pos += btnH() + 1;
			}
	
			// Back button
			RedButton back = new RedButton("Back") {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new Categories());
				}
			};
			add(back);
			back.setRect(0, pos + MARGIN, width, btnH());
	
			resize(width, (int) back.bottom() + MARGIN);
		}
	}
	
	// ---------------------------------------------------------------- step 3

	public static class Give extends Window {
		private final Class<? extends Item> cls;
		private final String categoryName;
		private final List<Class<? extends Item>> list;
		private final boolean upgradable;
		private final boolean stackable;

		private int amount = 1;
		private int level = 0;

		private RenderedTextBlock amountText;
		private RenderedTextBlock levelText;

		public Give(Class<? extends Item> cls, String categoryName, List<Class<? extends Item>> list) {
			super();
			this.cls = cls;
			this.categoryName = categoryName;
			this.list = list;

			Item sample = Reflection.newInstance(cls);
			upgradable = sample != null && sample.isUpgradable();
			stackable = sample != null && sample.stackable;

			int width = width();

			RenderedTextBlock title = PixelScene.renderTextBlock(display(cls), 9);
			title.hardlight(TITLE_COLOR);
			title.maxWidth(width - MARGIN * 2);
			title.setPos(MARGIN, MARGIN);
			add(title);

			float pos = title.bottom() + MARGIN * 3;

			amountText = PixelScene.renderTextBlock(7);
			add(amountText);
			amountText.setPos(MARGIN, pos);
			pos = amountText.bottom() + MARGIN;
			pos = stepperRow(pos, width, new int[]{-10, -1, 1, 10}, true);

			if (upgradable) {
				pos += MARGIN;
				levelText = PixelScene.renderTextBlock(7);
				add(levelText);
				levelText.setPos(MARGIN, pos);
				pos = levelText.bottom() + MARGIN;
				pos = stepperRow(pos, width, new int[]{-5, -1, 1, 5}, false);
			}

			refreshTexts();

			pos += MARGIN * 2;
			RedButton get = new RedButton("GET") {
				@Override
				protected void onClick() {
					give();
				}
			};
			add(get);
			get.setRect(0, pos, width, btnH() + 2);

			RedButton back = new RedButton("Back") {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new ItemList(Give.this.categoryName, Give.this.list));
				}
			};
			add(back);
			back.setRect(0, get.bottom() + 1, width, btnH());

			resize(width, (int) back.bottom() + MARGIN);
		}

		private float stepperRow(float pos, int width, int[] deltas, final boolean isAmount) {
			float w = (width - (deltas.length - 1)) / (float) deltas.length;
			for (int i = 0; i < deltas.length; i++) {
				final int d = deltas[i];
				RedButton btn = new RedButton((d > 0 ? "+" : "") + d, 7) {
					@Override
					protected void onClick() {
						if (isAmount) {
							int max = stackable ? 9999 : 20;
							amount = Math.max(1, Math.min(max, amount + d));
						} else {
							level = Math.max(0, Math.min(50, level + d));
						}
						refreshTexts();
					}
				};
				add(btn);
				btn.setRect(i * (w + 1), pos, w, btnH());
			}
			return pos + btnH();
		}

		private void refreshTexts() {
			amountText.text("Amount: " + amount + (stackable ? "" : " (separate copies)"));
			if (levelText != null) levelText.text("Level: +" + level);
		}

		private void give() {
			Hero hero = Dungeon.hero;
			if (hero == null) return;

			int copies = stackable ? 1 : amount;
			for (int i = 0; i < copies; i++) {
				Item item = Reflection.newInstance(cls);
				if (item == null) return;
				item.identify();
				if (upgradable && level != 0) item.level(level);
				if (stackable) item.quantity(amount);

				String name = item.name();
				if (item.doPickUp(hero)) {
					GLog.i(Messages.get(hero, "you_now_have", name));
					hero.spend(-hero.cooldown());
				} else {
					Dungeon.level.drop(item, hero.pos).sprite.drop();
					GLog.n("Backpack full, dropped " + name + " on the floor.");
				}
			}
		}
	}
}
