package com.polywoof;

import com.google.inject.Provides;
import com.polywoof.api.API;
import com.polywoof.api.DeepL;
import com.polywoof.api.Generic;
import com.polywoof.api.Google;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault @PluginDescriptor(
		name = "Polywoof",
		description = "Enjoy Quests in any Language!",
		tags = {
				"helper",
				"language",
				"translation"})
public class PolywoofPlugin extends Plugin implements MouseListener
{
	private final List<API.GameText> previousList = new ArrayList<>();
	private final List<String> examineList = new ArrayList<>();
	private API backend;
	private Dictionary dictionary;
	private Utils.TextVerifier verifierChatMessage;
	private Utils.TextVerifier verifierOverheadText;
	private Utils.TextVerifier verifierDialogText;
	private boolean getWidgetData;

	@Inject private ChatMessageManager chatMessageManager;
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
		mouseManager.registerMouseListener(this);
		overlay.setPosition(OverlayPosition.BOTTOM_LEFT);
		overlay.setPriority(Overlay.PRIORITY_LOW);
		overlay.setLayer(OverlayLayer.ABOVE_WIDGETS);
		overlay.revalidate();
		overlayManager.add(overlay);
		dictionary = new Dictionary("polywoof");
		verifierChatMessage = new Utils.TextVerifier(config.filterChatMessage());
		verifierOverheadText = new Utils.TextVerifier(config.filterOverheadText());
		verifierDialogText = new Utils.TextVerifier(config.filterDialogText());
		getWidgetData = false;

		changeBackend(config.backend());

		if(backend instanceof Generic)
		{
			ChatMessageBuilder builder = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT)
					.append("Generic")
					.append(ChatColorType.NORMAL)
					.append(" backend is set, intended for testing.");

			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(builder.build())
					.build());
		}

		if(config.showUsage())
		{
			verifyUsage();
		}
	}

	@Override protected void shutDown()
	{
		clientUI.setCursor(clientUI.getDefaultCursor());
		mouseManager.unregisterMouseListener(this);
		overlay.clear();
		overlayManager.remove(overlay);
		dictionary.close();
		previousList.clear();
		examineList.clear();
	}

	@Override public MouseEvent mouseClicked(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override public MouseEvent mousePressed(MouseEvent mouseEvent)
	{
		if(getWidgetData)
		{
			getWidgetData = false;
			clientThread.invoke(() ->
			{
				Utils.Interface.WidgetData widgetData = Utils.Interface.getWidgetData(
						client.getMouseCanvasPosition(),
						client.getWidgetRoots(),
						Utils.Interface.Type.ROOT);

				if(widgetData != null)
				{
					int group = WidgetUtil.componentToInterface(widgetData.widget.getId());
					int id = WidgetUtil.componentToId(widgetData.widget.getId());

					if(backend instanceof Generic)
					{
						ChatMessageBuilder builder = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT)
								.append(String.format("[%s %d.%d]", Text.titleCase(widgetData.type), group, id))
								.append(ChatColorType.NORMAL);

						if(!widgetData.widget.getText().isBlank())
						{
							builder.append(String.format(" «%s»", widgetData.widget.getText()));
						}

						chatMessageManager.queue(QueuedMessage.builder()
								.type(ChatMessageType.CONSOLE)
								.runeLiteFormattedMessage(builder.build())
								.build());
					}
					else if(!widgetData.widget.getText().isBlank())
					{
						API.GameText gameText;

						if(!widgetData.widget.hasListener() && widgetData.type != Utils.Interface.Type.DYNAMIC)
						{
							StringBuilder builder = new StringBuilder(API.GameText.Type.SCROLL.size);

							for(int i = 1;; i++)
							{
								Widget widget = client.getWidget(group, id - i);

								if(widget == null || widget.hasListener() || widget.getType() != WidgetType.TEXT || widget.getText().isBlank())
								{
									break;
								}

								builder.insert(0, ' ').insert(0, widget.getText());
							}

							builder.append(widgetData.widget.getText());

							for(int i = 1;; i++)
							{
								Widget widget = client.getWidget(group, id + i);

								if(widget == null || widget.hasListener() || widget.getType() != WidgetType.TEXT || widget.getText().isBlank())
								{
									break;
								}

								builder.append(' ').append(widget.getText());
							}

							gameText = new API.GameText(builder.toString(), API.GameText.Type.SCROLL);
						}
						else
						{
							gameText = new API.GameText(widgetData.widget.getText(), API.GameText.Type.DIALOG);
						}

						backend.buffered(List.of(gameText), backend.languageFind(config.language()), () -> overlay.put(List.of(gameText)));
					}
				}
			});

			clientUI.setCursor(clientUI.getDefaultCursor());
			mouseEvent.consume();
		}

		return mouseEvent;
	}

	@Override public MouseEvent mouseReleased(MouseEvent mouseEvent)
	{
		if(getWidgetData)
		{
			clientUI.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			mouseEvent.consume();
		}

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
		if(configChanged.getGroup().equals("polywoof"))
		{
			log.debug("Trying to change {} in the config", configChanged.getKey());
			switch(configChanged.getKey())
			{
				case "backend":
					changeBackend(config.backend());
					break;
				case "language":
					if(backend.languageFind(config.language()) instanceof API.UnknownLanguage)
					{
						ChatMessageBuilder builder = new ChatMessageBuilder().append(ChatColorType.NORMAL)
								.append("Current language is ")
								.append(ChatColorType.HIGHLIGHT)
								.append("not supported")
								.append(ChatColorType.NORMAL)
								.append("!");

						chatMessageManager.queue(QueuedMessage.builder()
								.type(ChatMessageType.CONSOLE)
								.runeLiteFormattedMessage(builder.build())
								.build());
					}
					break;
				case "key":
					if(config.key().equals("pesik"))
					{
						API.GameText title = new API.GameText("Секретка..", true);
						API.GameText message = new API.GameText("Ну тяф и что? Да-да, я пёсик!", API.GameText.Type.OVERHEAD);
						message.text = message.game;
						message.cache = true;

						overlay.put(List.of(title, message));
					}

					if(backend instanceof DeepL)
					{
						((DeepL)backend).update(config.key());
					}

					if(backend instanceof Google)
					{
						((Google)backend).update(config.key());
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
					verifierChatMessage.update(config.filterChatMessage());
					break;
				case "filterOverheadText":
					verifierOverheadText.update(config.filterOverheadText());
					break;
				case "filterDialogText":
					verifierDialogText.update(config.filterDialogText());
					break;
			}
		}
	}

	@Subscribe public void onChatMessage(ChatMessage chatMessage)
	{
		if(config.toggle())
		{
			List<API.GameText> textList = new ArrayList<>();

			switch(chatMessage.getType())
			{
				case GAMEMESSAGE:
					if(config.translateChatMessage())
					{
						if(config.showTitle() && !chatMessage.getName().isBlank())
						{
							textList.add(new API.GameText(chatMessage.getName(), config.keepTitle()));
						}

						textList.add(new API.GameText(chatMessage.getMessage(), API.GameText.Type.MESSAGE));
					}
					break;
				case ITEM_EXAMINE:
				case NPC_EXAMINE:
				case OBJECT_EXAMINE:
					if(config.translateExamine())
					{
						if(config.showTitle() && !examineList.isEmpty())
						{
							textList.add(new API.GameText(examineList.remove(0), config.keepTitle()));
						}

						textList.add(new API.GameText(chatMessage.getMessage(), API.GameText.Type.EXAMINE));
					}
					break;
				default:
					return;
			}

			if(verifierChatMessage.verify(textList))
			{
				backend.stored(textList, backend.languageFind(config.language()), dictionary, () -> overlay.put(textList));
			}
		}
	}

	@Subscribe public void onGameTick(GameTick gameTick)
	{
		if(config.toggle())
		{
			List<API.GameText> textList = new ArrayList<>();
			Widget dialogPlayerName = client.getWidget(InterfaceID.DIALOG_PLAYER, 4);
			Widget dialogPlayerText = client.getWidget(ComponentID.DIALOG_PLAYER_TEXT);
			Widget dialogNpcName = client.getWidget(ComponentID.DIALOG_NPC_NAME);
			Widget dialogNpcText = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
			Widget dialogSpriteText = client.getWidget(ComponentID.DIALOG_SPRITE_TEXT);
			Widget dialogDoubleSprite = client.getWidget(ComponentID.DIALOG_DOUBLE_SPRITE_TEXT);
			Widget dialogUnknown229 = client.getWidget(229, 1);
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
						if(widget.getText().isBlank() && builder.length() > 0)
						{
							textList.add(new API.GameText(builder.toString(), API.GameText.Type.SCROLL));
							builder = new StringBuilder(API.GameText.Type.SCROLL.size);
						}
						else
						{
							if(builder.length() > 0)
							{
								builder.append(' ');
							}

							builder.append(widget.getText());
						}
					}
				}
			}

			if(config.translateTreasureClue() && clueScrollText != null)
			{
				textList.add(new API.GameText(clueScrollText.getText(), API.GameText.Type.SCROLL));
			}

			if(verifierDialogText.verify(textList))
			{
				if(textList.size() == previousList.size())
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
				backend.stored(textList, backend.languageFind(config.language()), dictionary, () -> overlay.set("default", textList));
			}
			else if(!previousList.isEmpty())
			{
				previousList.clear();
				overlay.pop("default");
			}
		}
		else
		{
			previousList.clear();
			overlay.pop("default");
		}
	}

	@Subscribe public void onMenuOpened(MenuOpened menuOpened)
	{
		if(config.showButton() && overlay.isMouseOver())
		{
			client.createMenuEntry(1)
					.setOption(backend instanceof Generic ? "Get" : "Select")
					.setTarget(ColorUtil.wrapWithColorTag(backend instanceof Generic ? "Widget ID" : "Text", JagexColors.MENU_TARGET))
					.setType(MenuAction.RUNELITE)
					.onClick(menuEntry -> getWidgetData = true);

			client.createMenuEntry(2)
					.setOption("Get")
					.setTarget(ColorUtil.wrapWithColorTag("API Usage", JagexColors.MENU_TARGET))
					.setType(MenuAction.RUNELITE)
					.onClick(menuEntry -> verifyUsage());

			client.createMenuEntry(3)
					.setOption(config.toggle() ? "Suspend" : "Resume")
					.setTarget(ColorUtil.wrapWithColorTag("Translation", JagexColors.MENU_TARGET))
					.setType(MenuAction.RUNELITE)
					.onClick(menuEntry -> config.toggle(!config.toggle()));
		}
	}

	@Subscribe public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked)
	{
		if(config.toggle() && config.translateExamine() && config.showTitle() && menuOptionClicked.getMenuOption().equals("Examine"))
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
	}

	@Subscribe public void onOverheadTextChanged(OverheadTextChanged overheadTextChanged)
	{
		if(config.toggle() && config.translateOverheadText() && overheadTextChanged.getActor() instanceof NPC)
		{
			List<API.GameText> textList = new ArrayList<>();
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

			if(verifierOverheadText.verify(textList))
			{
				backend.stored(textList, backend.languageFind(config.language()), dictionary, () -> overlay.put(textList));
			}
		}
	}

	private void changeBackend(TranslationBackend backend)
	{
		log.info("Trying to set the {} backend", Text.titleCase(backend));
		switch(backend)
		{
			case GENERIC:
				this.backend = new Generic();
				dictionary.close();
				break;
			case DEEPL:
				this.backend = new DeepL(okHttpClient, config.key());
				dictionary.open();
				break;
			case GOOGLE:
				this.backend = new Google(okHttpClient, config.key());
				dictionary.open();
		}
	}

	private void verifyUsage()
	{
		if(backend instanceof DeepL)
		{
			((DeepL)backend).usage((characterCount, characterLimit) ->
			{
				ChatMessageBuilder builder = new ChatMessageBuilder().append(ChatColorType.NORMAL)
						.append("Your current API usage is ")
						.append(ChatColorType.HIGHLIGHT)
						.append(Math.round(100f * ((float)characterCount / characterLimit)) + "%")
						.append(ChatColorType.NORMAL)
						.append(" of the monthly quota!");

				chatMessageManager.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(builder.build())
						.build());
			});
		}
		else
		{
			ChatMessageBuilder builder = new ChatMessageBuilder().append(ChatColorType.NORMAL)
					.append("Not implemented for ")
					.append(ChatColorType.HIGHLIGHT)
					.append(Text.titleCase(config.backend()))
					.append(ChatColorType.NORMAL)
					.append(" backend.");

			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(builder.build())
					.build());
		}
	}

	public enum TranslationBackend
	{
		GENERIC,
		DEEPL,
		GOOGLE
	}
}
