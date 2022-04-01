package com.polywoof;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
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
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.ColorUtil;
import okhttp3.OkHttpClient;
import org.h2.engine.Constants;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.io.File;

@Slf4j
@ParametersAreNonnullByDefault
@PluginDescriptor(name = "Polywoof", description = "Translation for almost every dialogue and some related text, so you can understand what's going on!", tags = {"helper", "language", "translator", "translation"})
public class PolywoofPlugin extends Plugin
{
	private PolywoofStorage storage;
	private PolywoofTranslator translator;
	private Widget[] widgets;
	private int dialogue;
	private String previous;

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private PolywoofOverlay overlay;
	@Inject private OverlayManager overlayManager;
	@Inject private ChatMessageManager chatMessageManager;
	@Inject private OkHttpClient okHttpClient;

	@Provides
	PolywoofConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PolywoofConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		storage = new PolywoofStorage(new File(RuneLite.CACHE_DIR, "polywoof" + Constants.SUFFIX_MV_FILE));
		storage.open();

		translator = new PolywoofTranslator(okHttpClient, storage, config.key());
		translator.languages("target", languages -> log.info("{} languages loaded!", languages.size()));

		overlay.setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		overlay.setPriority(OverlayPriority.LOW);
		overlay.setLayer(OverlayLayer.ABOVE_WIDGETS);
		overlay.revalidate();
		overlayManager.add(overlay);

		if(config.key().isEmpty())
		{
			String message = new ChatMessageBuilder()
				.append("Polywoof is not ready, the ")
				.append(ChatColorType.HIGHLIGHT)
				.append("API Key")
				.append(ChatColorType.NORMAL)
				.append(" is missing.")
				.build();

			chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
		}

		if(config.showUsage())
			showUsage();
	}

	@Override
	protected void shutDown() throws Exception
	{
		storage.close();
		overlay.reset();
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if(!event.getGroup().equals("polywoof"))
			return;

		switch(event.getKey())
		{
			case "language":
				if(PolywoofTranslator.languageFinder(config.language()) instanceof PolywoofTranslator.UnknownLanguage)
				{
					String language = config.language().trim().replaceAll("\n", " ");

					if(language.length() > 10)
						language = language.substring(0, 10) + "...";

					String message = new ChatMessageBuilder()
						.append("Your chosen language «")
						.append(ChatColorType.HIGHLIGHT)
						.append(language)
						.append(ChatColorType.NORMAL)
						.append("» is not found!")
						.build();

					chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
				}
				break;
			case "key":
				translator.update(config.key());
				translator.languages("target", languages -> log.info("{} languages loaded!", languages.size()));
				break;
			case "showButton":
				config.toggle(true);
				break;
			case "fontName":
			case "fontSize":
			case "textShadow":
			case "overlayColor":
			case "overlayOutline":
			case "textAlignment":
			case "textWrap":
			case "sourceName":
			case "numberedOptions":
				overlay.revalidate();
				break;
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if(!config.showButton() || !overlay.isMouseOver())
			return;

		client.createMenuEntry(1)
			.setOption("Check")
			.setTarget(ColorUtil.wrapWithColorTag("Usage", JagexColors.MENU_TARGET))
			.setType(MenuAction.RUNELITE)
			.onClick(menuEntry -> showUsage());

		client.createMenuEntry(2)
			.setOption(config.toggle() ? "Disable" : "Enable")
			.setTarget(ColorUtil.wrapWithColorTag("Plugin", JagexColors.MENU_TARGET))
			.setType(MenuAction.RUNELITE)
			.onClick(menuEntry -> config.toggle(!config.toggle()));
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if(!config.toggle())
			return;

		String header, string;
		PolywoofStorage.DataType type;

		switch(event.getType())
		{
			case GAMEMESSAGE:
				if(!config.test() && !config.enableMessages())
					return;

				header = event.getName();
				string = event.getMessage();
				type = PolywoofStorage.DataType.CHAT_MESSAGES;
				break;
			case ITEM_EXAMINE:
			case NPC_EXAMINE:
			case OBJECT_EXAMINE:
				if(!config.test() && !config.enableExamine())
					return;

				header = null;
				string = event.getMessage();
				type = PolywoofStorage.DataType.ANY_EXAMINE;
				break;
			default:
				return;
		}

		PolywoofFormatter.parser(string, PolywoofFormatter.messageEntries, replacement ->
		{
			PolywoofComponent.Subject subject = dialogueSubject();

			if(config.test())
				overlay.put(PolywoofFormatter.filter(header), PolywoofFormatter.filter(replacement), subject);
			else
				translator.translate(replacement, PolywoofTranslator.languageFinder(config.language()), type, put -> overlay.put(PolywoofFormatter.filter(header), put, subject));
		});
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if(!config.toggle() || !config.test() && !config.enableOverhead())
			return;

		if(event.getActor() instanceof NPC)
		{
			String header, string;
			PolywoofStorage.DataType type;
			NPC actor = (NPC) event.getActor();

			switch(actor.getId())
			{
				default:
					header = actor.getName();
					string = event.getOverheadText();
					type = PolywoofStorage.DataType.OVERHEAD_TEXT;
					break;
				case NpcID.TOWN_CRIER:
				case NpcID.TOWN_CRIER_277:
				case NpcID.TOWN_CRIER_278:
				case NpcID.TOWN_CRIER_279:
				case NpcID.TOWN_CRIER_280:
				case NpcID.TOWN_CRIER_6823:
				case NpcID.TOWN_CRIER_10887:
					return;
			}

			PolywoofFormatter.parser(string, PolywoofFormatter.overheadEntries, replacement ->
			{
				PolywoofComponent.Subject subject = dialogueSubject();

				if(config.test())
					overlay.put(PolywoofFormatter.filter(header), PolywoofFormatter.filter(replacement), subject);
				else
					translator.translate(replacement, PolywoofTranslator.languageFinder(config.language()), type, put -> overlay.put(PolywoofFormatter.filter(header), put, subject));
			});
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if(!config.toggle())
		{
			previous = null;
			overlay.pop(null);
			return;
		}

		String header, string;
		PolywoofStorage.DataType type;

		switch(dialogue)
		{
			case WidgetID.DIARY_QUEST_GROUP_ID:
				if(!config.test() && !config.enableDiary())
					return;

				header = widgets[0].getText();
				string = PolywoofFormatter.formatScrolls(widgets[1].getStaticChildren());
				type = PolywoofStorage.DataType.QUEST_DIARY;
				break;
			case WidgetID.DIALOG_SPRITE_GROUP_ID:
			case WidgetIDCustom.DIALOG_11:
			case WidgetIDCustom.DIALOG_229:
				header = null;
				string = widgets[0].getText();
				type = PolywoofStorage.DataType.DIALOGUE_TEXT;
				break;
			case WidgetID.CLUE_SCROLL_GROUP_ID:
				if(!config.test() && !config.enableClues())
					return;

				header = null;
				string = widgets[0].getText();
				type = PolywoofStorage.DataType.VARIOUS_SCROLLS;
				break;
			case WidgetID.DIALOG_PLAYER_GROUP_ID:
			case WidgetID.DIALOG_NPC_GROUP_ID:
				header = widgets[0].getText();
				string = widgets[1].getText();
				type = PolywoofStorage.DataType.DIALOGUE_TEXT;
				break;
			case WidgetID.DIALOG_OPTION_GROUP_ID:
				header = null;
				string = PolywoofFormatter.formatOptions(widgets[0].getDynamicChildren());
				type = PolywoofStorage.DataType.DIALOGUE_OPTIONS;
				break;
			case WidgetIDCustom.SCROLL_95:
			case WidgetIDCustom.SCROLL_220:
			case WidgetIDCustom.SCROLL_222:
				if(!config.test() && !config.enableScrolls())
					return;

				header = null;
				string = PolywoofFormatter.formatScrolls(widgets[0].getNestedChildren());
				type = PolywoofStorage.DataType.VARIOUS_SCROLLS;
				break;
			case WidgetIDCustom.BOOK_392:
				if(!config.test() && !config.enableBooks())
					return;

				header = widgets[0].getText();
				string = PolywoofFormatter.formatScrolls(widgets[1].getStaticChildren(), widgets[2].getStaticChildren());
				type = PolywoofStorage.DataType.VARIOUS_BOOKS;
				break;
			default:
				return;
		}

		if(string.equals(previous))
			return;

		previous = string;
		overlay.pop(null);

		PolywoofFormatter.parser(string, PolywoofFormatter.dialogueEntries, replacement ->
		{
			PolywoofComponent.Subject subject = dialogueSubject(dialogue);

			if(config.test())
				overlay.set(null, PolywoofFormatter.filter(header), PolywoofFormatter.filter(replacement), subject);
			else
				translator.translate(replacement, PolywoofTranslator.languageFinder(config.language()), type, set -> overlay.set(null, PolywoofFormatter.filter(header), set, subject));
		});
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		Widget[] loaded;

		switch(event.getGroupId())
		{
			case WidgetID.DIARY_QUEST_GROUP_ID:
				loaded = new Widget[] {client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TITLE), client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TEXT)};
				break;
			case WidgetID.DIALOG_SPRITE_GROUP_ID:
				loaded = new Widget[] {client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT)};
				break;
			case WidgetID.CLUE_SCROLL_GROUP_ID:
				loaded = new Widget[] {client.getWidget(WidgetInfo.CLUE_SCROLL_TEXT)};
				break;
			case WidgetID.DIALOG_PLAYER_GROUP_ID:
				loaded = new Widget[] {client.getWidget(event.getGroupId(), 4), client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT)};
				break;
			case WidgetID.DIALOG_OPTION_GROUP_ID:
				loaded = new Widget[] {client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS)};
				break;
			case WidgetID.DIALOG_NPC_GROUP_ID:
				loaded = new Widget[] {client.getWidget(WidgetInfo.DIALOG_NPC_NAME), client.getWidget(WidgetInfo.DIALOG_NPC_TEXT)};
				break;
			case WidgetIDCustom.DIALOG_11:
				loaded = new Widget[] {client.getWidget(event.getGroupId(), 2)};
				break;
			case WidgetIDCustom.SCROLL_95:
			case WidgetIDCustom.SCROLL_220:
			case WidgetIDCustom.SCROLL_222:
				loaded = new Widget[] {client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 16)};
				break;
			case WidgetIDCustom.DIALOG_229:
				loaded = new Widget[] {client.getWidget(event.getGroupId(), 1)};
				break;
			case WidgetIDCustom.BOOK_392:
				loaded = new Widget[] {client.getWidget(event.getGroupId(), 6), client.getWidget(event.getGroupId(), 43), client.getWidget(event.getGroupId(), 59)};
				break;
			default:
				return;
		}

		for(Widget widget : loaded)
			if(widget == null)
				return;

		widgets = loaded;
		dialogue = event.getGroupId();
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		switch(event.getGroupId())
		{
			case WidgetID.DIARY_QUEST_GROUP_ID:
			case WidgetID.DIALOG_SPRITE_GROUP_ID:
			case WidgetID.CLUE_SCROLL_GROUP_ID:
			case WidgetID.DIALOG_PLAYER_GROUP_ID:
			case WidgetID.DIALOG_OPTION_GROUP_ID:
			case WidgetID.DIALOG_NPC_GROUP_ID:
			case WidgetIDCustom.DIALOG_11:
			case WidgetIDCustom.SCROLL_95:
			case WidgetIDCustom.SCROLL_220:
			case WidgetIDCustom.SCROLL_222:
			case WidgetIDCustom.DIALOG_229:
			case WidgetIDCustom.BOOK_392:
				dialogue = 0;
				previous = null;
				overlay.pop(null);
				break;
		}
	}

	public void showUsage()
	{
		translator.usage((characterCount, characterLimit) ->
		{
			String message = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("Your current DeepL API usage is ")
				.append(ChatColorType.HIGHLIGHT)
				.append(Math.round(100f * ((float) characterCount / characterLimit)) + "%")
				.append(ChatColorType.NORMAL)
				.append(" of the monthly quota!")
				.build();

			chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
		});
	}

	private PolywoofComponent.Subject dialogueSubject(int dialogue)
	{
		if(dialogue == WidgetID.DIALOG_OPTION_GROUP_ID)
			return config.numberedOptions() ? PolywoofComponent.Subject.NUMBERED : PolywoofComponent.Subject.OPTIONS;

		return dialogueSubject();
	}

	private PolywoofComponent.Subject dialogueSubject()
	{
		return config.sourceName() ? PolywoofComponent.Subject.HEADER : PolywoofComponent.Subject.NONE;
	}

	public static class WidgetIDCustom
	{
		public static final int DIALOG_11 = 11;
		public static final int SCROLL_95 = 95;
		public static final int SCROLL_220 = 220;
		public static final int SCROLL_222 = 222;
		public static final int DIALOG_229 = 229;
		public static final int BOOK_392 = 392;
	}
}
