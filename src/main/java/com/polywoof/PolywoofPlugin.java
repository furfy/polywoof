package com.polywoof;

import com.google.inject.Provides;
import com.polywoof.api.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

@Slf4j @ParametersAreNonnullByDefault @PluginDescriptor(
		name = "Polywoof",
		description = "Enjoy Quests in any Language!",
		tags = {
				"helper",
				"language",
				"translation"})
public final class PolywoofPlugin extends Plugin implements MouseListener
{
	private final Dictionary dictionary = new Dictionary("polywoof");
	private final HashMap<TranslationBackend, API> backend = new HashMap<>();
	private final List<API.GameText> previousList = new ArrayList<>();
	private final List<String> examineList = new ArrayList<>();
	private Utils.TextVerifier verifierChatMessage;
	private Utils.TextVerifier verifierOverheadText;
	private Utils.TextVerifier verifierDialog;
	private boolean selectText;
	private int examineTick;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientUI clientUI;
	@Inject private MouseManager mouseManager;
	@Inject private OkHttpClient okHttpClient;
	@Inject private OverlayManager overlayManager;
	@Inject private PolywoofConfig config;
	@Inject private PolywoofOverlay overlay;

	@Provides PolywoofConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PolywoofConfig.class);
	}

	@Override protected void startUp()
	{
		selectText = false;
		mouseManager.registerMouseListener(this);
		overlay.setPosition(OverlayPosition.BOTTOM_LEFT);
		overlay.setPriority(Overlay.PRIORITY_LOW);
		overlay.setLayer(OverlayLayer.ABOVE_WIDGETS);
		overlay.revalidate();
		overlayManager.add(overlay);

		verifierChatMessage = new Utils.TextVerifier(config.filterChatMessage());
		verifierOverheadText = new Utils.TextVerifier(config.filterOverheadText());
		verifierDialog = new Utils.TextVerifier(config.filterDialog());

		dictionary.open();
		backend.put(TranslationBackend.GENERIC, new Generic());
		backend.put(TranslationBackend.DEEPL, new DeepL(okHttpClient, new ErrorHandler(TranslationBackend.DEEPL)));
		backend.put(TranslationBackend.GOOGLE, new Google(okHttpClient, new ErrorHandler(TranslationBackend.GOOGLE)));
		backend.put(TranslationBackend.MYMEMORY, new MyMemory(okHttpClient, new ErrorHandler(TranslationBackend.MYMEMORY)));

		update(config.backend());
		update(TranslationBackend.MYMEMORY);
	}

	@Override protected void shutDown()
	{
		clientUI.setCursor(clientUI.getDefaultCursor());
		mouseManager.unregisterMouseListener(this);
		overlay.clear();
		overlayManager.remove(overlay);

		verifierChatMessage.clear();
		verifierOverheadText.clear();
		verifierDialog.clear();

		dictionary.close();
		backend.clear();
		previousList.clear();
		examineList.clear();
	}

	@Override public MouseEvent mouseClicked(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override public MouseEvent mousePressed(MouseEvent mouseEvent)
	{
		if(!selectText)
		{
			return mouseEvent;
		}

		selectText = false;
		clientThread.invoke(() ->
		{
			Utils.Interface.WidgetData widgetData = Utils.Interface.getWidgetData(client.getMouseCanvasPosition(), client.getWidgetRoots(),	Utils.Interface.Type.ROOT);

			if(widgetData == null)
			{
				return;
			}

			int group = WidgetUtil.componentToInterface(widgetData.widget.getId());
			int id = WidgetUtil.componentToId(widgetData.widget.getId());

			if(config.backend() == TranslationBackend.GENERIC)
			{
				String message = String.format(
						ColorUtil.wrapWithColorTag("[%s %d.%d]", JagexColors.DARK_ORANGE_INTERFACE_TEXT),
						Text.titleCase(widgetData.type), group, id);

				if(!widgetData.widget.getText().isBlank())
				{
					message = String.format("%s «%s»", message, ColorUtil.wrapWithColorTag(widgetData.widget.getText(), Color.WHITE));
				}

				overlay.put(List.of(API.GameText.create(message, API.GameText.Type.MESSAGE, false, false)), 10000L);
			}
			else if(!widgetData.widget.getText().isBlank())
			{
				List<API.GameText> textList = new ArrayList<>();

				if(widgetData.type == Utils.Interface.Type.DYNAMIC)
				{
					textList.add(new API.GameText(widgetData.widget.getText(), API.GameText.Type.MESSAGE));
				}
				else
				{
					List<Widget> widgetList = new ArrayList<>();

					for(int i = 1; i < Byte.MAX_VALUE; i++)
					{
						Widget widget = client.getWidget(group, id - i);

						if(widget == null || widget.hasListener() || widget.getType() != WidgetType.TEXT)
						{
							break;
						}

						widgetList.add(0, widget);
					}

					widgetList.add(widgetData.widget);

					for(int i = 1; i < Byte.MAX_VALUE; i++)
					{
						Widget widget = client.getWidget(group, id + i);

						if(widget == null || widget.hasListener() || widget.getType() != WidgetType.TEXT)
						{
							break;
						}

						widgetList.add(widget);
					}

					for(String text : Utils.Text.format(widgetList))
					{
						textList.add(new API.GameText(text, API.GameText.Type.SCROLL));
					}
				}

				backend.get(config.forceMyMemory() ? TranslationBackend.MYMEMORY : config.backend()).memoryGet(textList, config.language(), config.ignoreTags(), () -> overlay.put(textList, overlay.timeout(textList)));
			}
		});

		clientUI.setCursor(clientUI.getDefaultCursor());
		mouseEvent.consume();

		return mouseEvent;
	}

	@Override public MouseEvent mouseReleased(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override public MouseEvent mouseEntered(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override public MouseEvent mouseExited(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override public MouseEvent mouseDragged(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override public MouseEvent mouseMoved(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Subscribe public void onConfigChanged(ConfigChanged configChanged)
	{
		if(!configChanged.getGroup().equals("polywoof"))
		{
			return;
		}

		switch(configChanged.getKey())
		{
			case "backend":
			case "key":
			case "language":
				if(config.key().equals("pesik"))
				{
					String message = String.format(
							"%s %s",
							ColorUtil.wrapWithColorTag("Секретка..", Color.ORANGE),
							ColorUtil.wrapWithColorTag("Ну тяф и что?", new Color(Color.HSBtoRGB((float)(Math.random() * 255d), 1f, 1f))));

					overlay.put(List.of(API.GameText.create(message, API.GameText.Type.OPTION, false, false)), 10000L);
				}

				update(config.backend());
				break;
			case "quickActions":
				config.toggle(true);
				break;
			case "forceMyMemory":
			case "emailAddress":
				update(TranslationBackend.MYMEMORY);
				break;
			case "fontName":
			case "fontSize":
			case "textShadow":
			case "overlayBackgroundColor":
			case "overlayOutline":
			case "textAlignment":
			case "textWrap":
				overlay.revalidate();
				break;
			case "filterChatMessage":
				verifierChatMessage.update(config.filterChatMessage());
				break;
			case "filterOverheadText":
				verifierOverheadText.update(config.filterOverheadText());
				break;
			case "filterDialog":
				verifierDialog.update(config.filterDialog());
				break;
		}
	}

	@Subscribe public void onChatMessage(ChatMessage chatMessage)
	{
		if(!config.toggle())
		{
			return;
		}

		List<API.GameText> textList = new ArrayList<>();

		switch(chatMessage.getType())
		{
			case GAMEMESSAGE:
				if(!config.translateChatMessage())
				{
					return;
				}

				if(config.showTitle() && !chatMessage.getName().isBlank())
				{
					textList.add(API.GameText.create(chatMessage.getName(), API.GameText.Type.TITLE, config.translateTitle(), config.ignoreTags()));
				}

				textList.add(new API.GameText(chatMessage.getMessage(), API.GameText.Type.MESSAGE));
				break;
			case ITEM_EXAMINE:
			case NPC_EXAMINE:
			case OBJECT_EXAMINE:
				if(!config.translateExamine())
				{
					return;
				}

				if(!examineList.isEmpty())
				{
					String examineTitle = examineList.remove(0);

					if(config.showTitle())
					{
						textList.add(API.GameText.create(examineTitle, API.GameText.Type.TITLE, config.translateTitle(), config.ignoreTags()));
					}
				}

				textList.add(new API.GameText(chatMessage.getMessage(), API.GameText.Type.EXAMINE));
				break;
			default:
				return;
		}

		verifierChatMessage.verify(textList);
		backend.get(config.backend()).dictionaryGet(textList, config.language(), config.ignoreTags(), dictionary, () -> overlay.put(textList, overlay.timeout(textList)));
	}

	@Subscribe public void onGameTick(GameTick gameTick)
	{
		if(config.toggle())
		{
			List<API.GameText> textList = new ArrayList<>();

			if(config.translateDialog())
			{
				Widget dialogPlayerName = client.getWidget(InterfaceID.DIALOG_PLAYER, 4);
				Widget dialogPlayerText = client.getWidget(ComponentID.DIALOG_PLAYER_TEXT);
				Widget dialogNpcName = client.getWidget(ComponentID.DIALOG_NPC_NAME);
				Widget dialogNpcText = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
				Widget dialogSpriteText = client.getWidget(ComponentID.DIALOG_SPRITE_TEXT);
				Widget dialogDoubleSprite = client.getWidget(ComponentID.DIALOG_DOUBLE_SPRITE_TEXT);
				Widget dialogUnknown229 = client.getWidget(229, 1);
				Widget dialogOptionOptions = client.getWidget(ComponentID.DIALOG_OPTION_OPTIONS);

				if(dialogPlayerName != null && dialogPlayerText != null)
				{
					if(config.showTitle())
					{
						textList.add(API.GameText.create(dialogPlayerName.getText(), API.GameText.Type.TITLE, config.translateTitle(), config.ignoreTags()));
					}

					textList.add(new API.GameText(dialogPlayerText.getText(), API.GameText.Type.DIALOG));
				}

				if(dialogNpcName != null && dialogNpcText != null)
				{
					if(config.showTitle())
					{
						textList.add(API.GameText.create(dialogNpcName.getText(), API.GameText.Type.TITLE, config.translateTitle(), config.ignoreTags()));
					}

					textList.add(new API.GameText(dialogNpcText.getText(), API.GameText.Type.DIALOG));
				}

				if(dialogSpriteText != null)
				{
					textList.add(new API.GameText(dialogSpriteText.getText(), API.GameText.Type.DIALOG));
				}

				if(dialogDoubleSprite != null)
				{
					textList.add(new API.GameText(dialogDoubleSprite.getText(), API.GameText.Type.DIALOG));
				}

				if(dialogUnknown229 != null)
				{
					textList.add(new API.GameText(dialogUnknown229.getText(), API.GameText.Type.DIALOG));
				}

				if(dialogOptionOptions != null)
				{
					for(Widget widget : dialogOptionOptions.getDynamicChildren())
					{
						if(widget.getType() == WidgetType.TEXT)
						{
							if(widget.hasListener())
							{
								textList.add(new API.GameText(widget.getText(), API.GameText.Type.OPTION));
							}
							else if(config.showTitle())
							{
								textList.add(API.GameText.create(widget.getText(), API.GameText.Type.TITLE, config.translateTitle(), config.ignoreTags()));
							}
						}
					}
				}
			}

			if(config.translateScroll())
			{
				Widget genericScrollText = client.getWidget(ComponentID.GENERIC_SCROLL_TEXT);

				if(genericScrollText != null)
				{
					for(String text : Utils.Text.format(Arrays.asList(genericScrollText.getNestedChildren())))
					{
						textList.add(new API.GameText(text, API.GameText.Type.SCROLL));
					}
				}
			}

			if(config.translateTreasureClue())
			{
				Widget clueScrollText = client.getWidget(ComponentID.CLUESCROLL_TEXT);

				if(clueScrollText != null)
				{
					textList.add(new API.GameText(clueScrollText.getText(), API.GameText.Type.SCROLL));
				}
			}

			verifierDialog.verify(textList);

			if(textList.isEmpty())
			{
				previousList.clear();
				overlay.pop("default");
			}
			else
			{
				Iterator<API.GameText> iterator = previousList.iterator();

				if(textList.stream().anyMatch(gameText -> !iterator.hasNext() || !iterator.next().game.equals(gameText.game)))
				{
					previousList.clear();
					previousList.addAll(textList);
					overlay.pop("default");
					backend.get(config.backend()).dictionaryGet(textList, config.language(), config.ignoreTags(), dictionary, () -> overlay.set("default", textList));
				}
			}
		}
		else
		{
			previousList.clear();
			overlay.pop("default");
		}

		if(examineTick > 0 && --examineTick < 1)
		{
			examineList.clear();
		}
	}

	@Subscribe public void onMenuOpened(MenuOpened menuOpened)
	{
		if(!config.quickActions() || !overlay.isMouseOver())
		{
			return;
		}

		client.createMenuEntry(1)
				.setOption(config.backend() == TranslationBackend.GENERIC ? "Get" : "Select")
				.setTarget(ColorUtil.wrapWithColorTag(config.backend() == TranslationBackend.GENERIC ? "Widget ID" : "Text", JagexColors.MENU_TARGET))
				.setType(MenuAction.RUNELITE)
				.onClick(menuEntry ->
				{
					clientUI.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					selectText = true;
				});

		client.createMenuEntry(2)
				.setOption("Get")
				.setTarget(ColorUtil.wrapWithColorTag("API Usage", JagexColors.MENU_TARGET))
				.setType(MenuAction.RUNELITE)
				.onClick(menuEntry ->
				{
					if(config.backend() == TranslationBackend.DEEPL)
					{
						((DeepL)backend.get(TranslationBackend.DEEPL)).usage((characterCount, characterLimit) ->
						{
							double usage = ((double)characterCount / characterLimit);
							String message = String.format(
									"Your current %s usage is %s of the monthly quota!",
									ColorUtil.wrapWithColorTag(config.backend().product, JagexColors.DARK_ORANGE_INTERFACE_TEXT),
									ColorUtil.wrapWithColorTag(Math.round(100d * usage) + "%", ColorUtil.colorLerp(Color.GREEN, Color.RED, usage)));

							overlay.put(List.of(API.GameText.create(message, API.GameText.Type.MESSAGE, false, false)), 10000L);
						});
					}
					else
					{
						String message = String.format(
								"Not implemented for the %s backend.",
								ColorUtil.wrapWithColorTag(config.backend().product, JagexColors.DARK_ORANGE_INTERFACE_TEXT));

						overlay.put(List.of(API.GameText.create(message, API.GameText.Type.MESSAGE, false, false)), 10000L);
					}
				});

		client.createMenuEntry(3)
				.setOption(config.toggle() ? "Suspend" : "Resume")
				.setTarget(ColorUtil.wrapWithColorTag("Translation", JagexColors.MENU_TARGET))
				.setType(MenuAction.RUNELITE)
				.onClick(menuEntry ->
				{
					String message = "Translation has been %s!";

					if(config.toggle())
					{
						message = String.format(message, ColorUtil.wrapWithColorTag("suspended", Color.RED));
					}
					else
					{
						message = String.format(message, ColorUtil.wrapWithColorTag("resumed", Color.GREEN));
					}

					overlay.put(List.of(API.GameText.create(message, API.GameText.Type.MESSAGE, false, false)), 10000L);
					config.toggle(!config.toggle());
				});
	}

	@Subscribe public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked)
	{
		if(!config.toggle() || !config.translateExamine() || !menuOptionClicked.getMenuOption().equals("Examine"))
		{
			return;
		}

		if(menuOptionClicked.getMenuEntry().getNpc() == null)
		{
			examineList.add(menuOptionClicked.getMenuTarget());
		}
		else
		{
			examineList.add(menuOptionClicked.getMenuEntry().getNpc().getName());
		}

		examineTick = 10;
	}

	@Subscribe public void onOverheadTextChanged(OverheadTextChanged overheadTextChanged)
	{
		if(!config.toggle() || !config.translateOverheadText() || !(overheadTextChanged.getActor() instanceof NPC))
		{
			return;
		}

		List<API.GameText> textList = new ArrayList<>();
		NPC npc = (NPC)overheadTextChanged.getActor();

		switch(npc.getId())
		{
			default:
				if(config.showTitle() && npc.getName() != null)
				{
					textList.add(API.GameText.create(npc.getName(), API.GameText.Type.TITLE, config.translateTitle(), config.ignoreTags()));
				}

				textList.add(new API.GameText(overheadTextChanged.getOverheadText(), API.GameText.Type.OVERHEAD));
				break;
			case NpcID.BEE_KEEPER:
			case NpcID.CAPT_ARNAV:
			case NpcID.NILES:
			case NpcID.MILES:
			case NpcID.GILES:
			case NpcID.COUNT_CHECK:
			case NpcID.SERGEANT_DAMIEN:
			case NpcID.DRUNKEN_DWARF:
			case NpcID.EVIL_BOB:
			case NpcID.POSTIE_PETE:
			case NpcID.FREAKY_FORESTER:
			case NpcID.GENIE:
			case NpcID.LEO:
			case NpcID.DR_JEKYLL:
			case NpcID.FROG_PRINCE:
			case NpcID.FROG_PRINCESS:
			case NpcID.MYSTERIOUS_OLD_MAN:
			case NpcID.PILLORY_GUARD:
			case NpcID.FLIPPA:
			case NpcID.TILT:
			case NpcID.QUIZ_MASTER:
			case NpcID.RICK_TURPENTINE:
			case NpcID.SANDWICH_LADY:
			case NpcID.DUNCE:
			case NpcID.TOWN_CRIER:
			case NpcID.ELITE_VOID_KNIGHT:
				return;
		}

		verifierOverheadText.verify(textList);
		backend.get(config.backend()).dictionaryGet(textList, config.language(), config.ignoreTags(), dictionary, () -> overlay.put(textList, overlay.timeout(textList)));
	}

	private void update(TranslationBackend translationBackend)
	{
		switch(translationBackend)
		{
			case DEEPL:
				((DeepL)backend.get(translationBackend)).update(config.key());
				break;
			case GOOGLE:
				((Google)backend.get(translationBackend)).update(config.key());
				break;
			case MYMEMORY:
				((MyMemory)backend.get(translationBackend)).update(config.emailAddress());
				break;
		}

		backend.get(config.key().isBlank() ? TranslationBackend.GENERIC : translationBackend).languageSupport(languageList ->
		{
			API.Language language = backend.get(translationBackend).languageFind(config.language());

			if(language instanceof API.UnknownLanguage)
			{
				String message = String.format(
						"Specified language was %s.",
						ColorUtil.wrapWithColorTag("not found", Color.RED));

				overlay.put(List.of(API.GameText.create(message, API.GameText.Type.MESSAGE, false, false)), 10000L);
			}
			else if(language instanceof API.ResourceLanguage)
			{
				String message = String.format(
						"%s language was found in the resources.",
						ColorUtil.wrapWithColorTag(language.name, Color.GRAY));

				overlay.put(List.of(API.GameText.create(message, API.GameText.Type.MESSAGE, false, false)), 10000L);
			}
			else
			{
				String message = String.format(
						"%s language is set for the %s backend!",
						ColorUtil.wrapWithColorTag(language.name, Color.GREEN),
						ColorUtil.wrapWithColorTag(translationBackend.product, JagexColors.DARK_ORANGE_INTERFACE_TEXT));

				overlay.put(List.of(API.GameText.create(message, API.GameText.Type.MESSAGE, false, false)), 10000L);
				log.info("Got {} trusted languages", languageList.size());
			}
		});
	}

	@AllArgsConstructor(access = AccessLevel.PUBLIC) private final class ErrorHandler implements API.Reportable
	{
		TranslationBackend translationBackend;

		@Override public void report(API backend, Exception error)
		{
			String message = String.format(
					"%s has encountered %s error!",
					ColorUtil.wrapWithColorTag(translationBackend.product, JagexColors.DARK_ORANGE_INTERFACE_TEXT),
					ColorUtil.wrapWithColorTag(error.getMessage(), Color.RED));

			overlay.put(List.of(API.GameText.create(message, API.GameText.Type.MESSAGE, false, false)), 10000L);
		}
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE) public enum TranslationBackend
	{
		GENERIC("Generic"),
		DEEPL("DeepL API"),
		GOOGLE("Google Translate"),
		MYMEMORY("Translated MyMemory");

		public final String product;
	}
}
