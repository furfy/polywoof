package com.polywoof;

import com.google.inject.Provides;
import com.polywoof.api.API;
import com.polywoof.api.DeepL;
import com.polywoof.api.Generic;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;
import okhttp3.OkHttpClient;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j @ParametersAreNonnullByDefault @PluginDescriptor(
		name = "Polywoof",
		description = "Enjoy Quests in any Language!",
		tags = {
				"helper",
				"language",
				"translation"})
public class PolywoofPlugin extends Plugin
{
	public static Font resourceFont;
	public static BufferedImage resourceIcon;
	public static final Executor executor = Executors.newSingleThreadExecutor();

	private PolywoofStorage storage;
	private API backend;
	private PolywoofUtils.TextVerifier verifierChatMessage;
	private PolywoofUtils.TextVerifier verifierOverheadText;
	private PolywoofUtils.TextVerifier verifierDialogText;
	private boolean selectWidget = false;
	private final List<API.GameText> previousList = new ArrayList<>(50);
	private final List<String> examineList = new ArrayList<>();

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private PolywoofOverlay overlay;
	@Inject private OverlayManager overlayManager;
	@Inject private ChatMessageManager chatMessageManager;
	@Inject private OkHttpClient okHttpClient;

	@Provides PolywoofConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PolywoofConfig.class);
	}

	@Override protected void startUp()
	{
		try(InputStream stream = PolywoofPlugin.class.getResourceAsStream("/font.ttf"))
		{
			if(stream == null)
			{
				throw new IllegalArgumentException();
			}

			resourceFont = Font.createFont(Font.TRUETYPE_FONT, stream);
		}
		catch(Exception error)
		{
			log.error("Failed to load the default font", error);
		}

		try(InputStream stream = PolywoofPlugin.class.getResourceAsStream("/icon.png"))
		{
			if(stream == null)
			{
				throw new IllegalArgumentException();
			}

			resourceIcon = ImageIO.read(stream);
		}
		catch(Exception error)
		{
			log.error("Failed to load the button icon", error);
		}

		overlay.setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		overlay.setPriority(Overlay.PRIORITY_LOW);
		overlay.setLayer(OverlayLayer.ABOVE_WIDGETS);
		overlay.revalidate();
		overlayManager.add(overlay);

		storage = new PolywoofStorage("polywoof");
		verifierChatMessage = new PolywoofUtils.TextVerifier(config.filterChatMessage());
		verifierOverheadText = new PolywoofUtils.TextVerifier(config.filterOverheadText());
		verifierDialogText = new PolywoofUtils.TextVerifier(config.filterDialogText());

		setBackend(config.backend());

		if(config.key().isBlank() && !(backend instanceof Generic))
		{
			String message = new ChatMessageBuilder().append(ChatColorType.NORMAL)
					.append("Polywoof is not ready, the ")
					.append(ChatColorType.HIGHLIGHT)
					.append("API Key")
					.append(ChatColorType.NORMAL)
					.append(" is missing.")
					.build();

			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(message)
					.build());
		}

		if(config.showUsage())
		{
			getUsage();
		}
	}

	@Override protected void shutDown()
	{
		storage.close();
		overlay.clear();
		overlayManager.remove(overlay);
	}

	@Subscribe public void onConfigChanged(ConfigChanged configChanged)
	{
		if(!configChanged.getGroup().equals("polywoof"))
		{
			return;
		}

		switch(configChanged.getKey())
		{
			case "toggle":
				examineList.clear();
				break;
			case "backend":
				setBackend(config.backend());
			case "language":
				if(backend.languageFind(config.language()) instanceof API.UnknownLanguage)
				{
					String language = config.language().trim().replaceAll("\n", " ");

					if(language.length() > 10)
					{
						language = String.format("%s...", language.substring(0, 10));
					}

					String message = new ChatMessageBuilder().append(ChatColorType.NORMAL)
							.append("Your chosen language «")
							.append(ChatColorType.HIGHLIGHT)
							.append(language)
							.append(ChatColorType.NORMAL)
							.append("» is not found!")
							.build();

					chatMessageManager.queue(QueuedMessage.builder()
							.type(ChatMessageType.CONSOLE)
							.runeLiteFormattedMessage(message)
							.build());
				}
				break;
			case "key":
				if(backend instanceof DeepL)
				{
					((DeepL)backend).update(config.key());
					backend.languageList(languages -> log.info("{} languages loaded!", languages.size()));
				}
				break;
			case "showButton":
				config.toggle(true);
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
				verifierChatMessage.update(config.filterDialogText());
				break;
			case "filterOverheadText":
				verifierOverheadText.update(config.filterDialogText());
				break;
			case "filterDialogText":
				verifierDialogText.update(config.filterDialogText());
				break;
		}
	}

	@Subscribe public void onMenuOpened(MenuOpened menuOpened)
	{
		if(!config.showButton() || !overlay.isMouseOver())
		{
			return;
		}

		client.createMenuEntry(1)
				.setOption("Show")
				.setTarget(ColorUtil.wrapWithColorTag("Usage", JagexColors.MENU_TARGET))
				.setType(MenuAction.RUNELITE)
				.onClick(menuEntry -> getUsage());

		client.createMenuEntry(2)
				.setOption(config.toggle() ? "Disable" : "Enable")
				.setTarget(ColorUtil.wrapWithColorTag("Translation", JagexColors.MENU_TARGET))
				.setType(MenuAction.RUNELITE)
				.onClick(menuEntry -> config.toggle(!config.toggle()));

		if(backend instanceof Generic)
		{
			client.createMenuEntry(3)
					.setOption("Select")
					.setTarget(ColorUtil.wrapWithColorTag("Widget", JagexColors.MENU_TARGET))
					.setType(MenuAction.RUNELITE)
					.onClick(menuEntry -> selectWidget = true);
		}
	}

	@Subscribe public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked)
	{
		if(selectWidget)
		{
			selectWidget = false;
			getWidget();
		}

		if(!config.toggle())
		{
			return;
		}

		switch(menuOptionClicked.getMenuAction())
		{
			case EXAMINE_ITEM_GROUND:
			case EXAMINE_NPC:
			case EXAMINE_OBJECT:
			case CC_OP_LOW_PRIORITY:
				if(config.showTitle() && config.translateExamine())
				{
					if(menuOptionClicked.getMenuEntry().getNpc() == null)
					{
						examineList.add(menuOptionClicked.getMenuTarget());
					}
					else
					{
						examineList.add(menuOptionClicked.getMenuEntry().getNpc().getName());
					}
				}
				break;
		}
	}

	@Subscribe public void onChatMessage(ChatMessage chatMessage)
	{
		if(!config.toggle())
		{
			return;
		}

		List<API.GameText> textList = new ArrayList<>(50);

		switch(chatMessage.getType())
		{
			case GAMEMESSAGE:
				if(!config.translateChatMessage())
				{
					return;
				}

				if(config.showTitle() && !chatMessage.getName().isBlank())
				{
					textList.add(new API.GameText(chatMessage.getName(), config.keepTitle()));
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

				if(config.showTitle() && !examineList.isEmpty())
				{
					textList.add(new API.GameText(examineList.remove(0), config.keepTitle()));
				}

				textList.add(new API.GameText(chatMessage.getMessage(), API.GameText.Type.EXAMINE));
				break;
			default:
				return;
		}

		verifierChatMessage.verify(textList);

		if(!textList.isEmpty())
		{
			backend.submit(textList, backend.languageFind(config.language()), storage, () -> overlay.put(textList));
		}
	}

	@Subscribe public void onOverheadTextChanged(OverheadTextChanged overheadTextChanged)
	{
		if(!config.toggle())
		{
			return;
		}

		if(config.translateOverheadText() && overheadTextChanged.getActor() instanceof NPC)
		{
			List<API.GameText> textList = new ArrayList<>(50);
			NPC npc = (NPC)overheadTextChanged.getActor();

			switch(npc.getId())
			{
				default:
					if(config.showTitle() && npc.getName() != null)
					{
						textList.add(new API.GameText(npc.getName(), config.keepTitle()));
					}

					textList.add(new API.GameText(overheadTextChanged.getOverheadText(), API.GameText.Type.OVERHEAD));
					break;
				case NpcID.TOWN_CRIER:
				case NpcID.ELITE_VOID_KNIGHT:
					return;
			}

			verifierOverheadText.verify(textList);

			if(!textList.isEmpty())
			{
				backend.submit(textList, backend.languageFind(config.language()), storage, () -> overlay.put(textList));

			}
		}
	}

	@Subscribe public void onGameTick(GameTick gameTick)
	{
		if(!config.toggle())
		{
			previousList.clear();
			overlay.pop("default");
			return;
		}

		List<API.GameText> textList = new ArrayList<>(50);
		Widget dialogPlayerName = client.getWidget(InterfaceID.DIALOG_PLAYER, 4);
		Widget dialogPlayerText = client.getWidget(ComponentID.DIALOG_PLAYER_TEXT);
		Widget dialogNpcName = client.getWidget(ComponentID.DIALOG_NPC_NAME);
		Widget dialogNpcText = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
		Widget dialogSpriteText = client.getWidget(ComponentID.DIALOG_SPRITE_TEXT);
		Widget dialogDoubleSprite = client.getWidget(ComponentID.DIALOG_DOUBLE_SPRITE_TEXT);
		Widget dialogOptionOptions = client.getWidget(ComponentID.DIALOG_OPTION_OPTIONS);
		Widget genericScrollText = client.getWidget(ComponentID.GENERIC_SCROLL_TEXT);
		Widget clueScrollText = client.getWidget(ComponentID.CLUESCROLL_TEXT);

		if(dialogPlayerName != null && dialogPlayerText != null)
		{
			if(config.showTitle())
			{
				textList.add(new API.GameText(dialogPlayerName.getText(), config.keepTitle()));
			}

			textList.add(new API.GameText(dialogPlayerText.getText(), API.GameText.Type.DIALOG));
		}

		if(dialogNpcName != null && dialogNpcText != null)
		{
			if(config.showTitle())
			{
				textList.add(new API.GameText(dialogNpcName.getText(), config.keepTitle()));
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
						textList.add(new API.GameText(widget.getText(), config.keepTitle()));
					}
				}
			}
		}

		if(config.translateScroll() && genericScrollText != null)
		{
			StringBuilder builder = new StringBuilder(API.GameText.Type.SCROLL.size);

			for(Widget widget : genericScrollText.getNestedChildren())
			{
				if(widget.getType() == WidgetType.TEXT)
				{
					if(widget.getText().isEmpty() && builder.length() > 0)
					{
						textList.add(new API.GameText(builder.toString(), API.GameText.Type.SCROLL));
						builder = new StringBuilder(API.GameText.Type.SCROLL.size);
					}
					else
					{
						builder.append(widget.getText());
					}
				}
			}
		}

		if(config.translateTreasureClue() && clueScrollText != null)
		{
			textList.add(new API.GameText(clueScrollText.getText(), API.GameText.Type.SCROLL));
		}

		verifierDialogText.verify(textList);

		if(textList.isEmpty())
		{
			if(!previousList.isEmpty())
			{
				previousList.clear();
				overlay.pop("default");
			}

			return;
		}

		if(!previousList.isEmpty() && textList.size() == previousList.size())
		{
			Iterator<API.GameText> iterator = previousList.iterator();

			for(API.GameText gameText : textList)
			{
				if(!gameText.game.equals(iterator.next().game))
				{
					break;
				}

				if(!iterator.hasNext())
				{
					return;
				}
			}
		}

		previousList.clear();
		previousList.addAll(textList);
		overlay.pop("default");
		backend.submit(textList, backend.languageFind(config.language()), storage, () -> overlay.set("default", textList));
	}

	private void setBackend(PolywoofConfig.TranslationBackend backend)
	{
		switch(backend)
		{
			case GENERIC:
				this.backend = new Generic();
				storage.close();
				break;
			case DEEPL:
				this.backend = new DeepL(okHttpClient, config.key());
				this.backend.languageList(languages -> log.info("{} languages loaded!", languages.size()));
				storage.open();
				break;
		}
	}

	private void getUsage()
	{
		if(backend instanceof Generic)
		{
			String message = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT)
					.append("Generic")
					.append(ChatColorType.NORMAL)
					.append(" backend does not have any quota.")
					.build();

			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(message)
					.build());
		}

		if(backend instanceof DeepL)
		{
			((DeepL)backend).usage((characterCount, characterLimit) ->
			{
				String message = new ChatMessageBuilder().append(ChatColorType.NORMAL)
						.append("Your current API usage is ")
						.append(ChatColorType.HIGHLIGHT)
						.append(Math.round(100f * ((float)characterCount / characterLimit)) + "%")
						.append(ChatColorType.NORMAL)
						.append(" of the monthly quota!")
						.build();

				chatMessageManager.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(message)
						.build());
			});
		}
	}

	private void getWidget()
	{
		PolywoofUtils.Interface.CurrentWidget currentWidget = PolywoofUtils.Interface.getCurrentWidget(
				client.getMouseCanvasPosition(),
				client.getWidgetRoots(),
				PolywoofUtils.Interface.Type.ROOT);

		if(currentWidget.exists())
		{
			String message = new ChatMessageBuilder().append(ChatColorType.NORMAL)
					.append("[")
					.append(ChatColorType.HIGHLIGHT)
					.append(String.format(
							"%s %d.%d",
							currentWidget.type,
							WidgetUtil.componentToInterface(currentWidget.widget.getId()),
							WidgetUtil.componentToId(currentWidget.widget.getId())))
					.append(ChatColorType.NORMAL)
					.append(String.format("] «%s»", currentWidget.widget.getText()))
					.build();

			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(message)
					.build());
		}
	}
}
