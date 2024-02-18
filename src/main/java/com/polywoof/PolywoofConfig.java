package com.polywoof;

import net.runelite.client.config.*;
import net.runelite.client.ui.overlay.components.ComponentConstants;

import java.awt.*;

@ConfigGroup("polywoof") public interface PolywoofConfig extends Config
{
	@ConfigItem(
			keyName = "toggle",
			name = "",
			description = "")
	void toggle(boolean toggle);

	@ConfigItem(
			keyName = "toggle",
			name = "",
			description = "",
			hidden = true)
	default boolean toggle()
	{
		return true;
	}

	@ConfigSection(
			name = "General",
			description = "Settings for overall experience",
			position = 0)
	String generalSection = "generalSection";

	@ConfigItem(
			keyName = "backend",
			name = "Service",
			description = "Choose the backend for translation",
			section = generalSection,
			position = 0)
	default PolywoofPlugin.TranslationBackend backend()
	{
		return PolywoofPlugin.TranslationBackend.DEEPL;
	}

	@ConfigItem(
			keyName = "language",
			name = "Language",
			description = "Type your desired language",
			section = generalSection,
			position = 1)
	default String language()
	{
		return "ru";
	}

	@ConfigItem(
			keyName = "key",
			name = "API Key",
			description = "This is REQUIRED for the DeepL",
			secret = true,
			section = generalSection,
			position = 2)
	default String key()
	{
		return "";
	}

	@Range(min = 1, max = 99) @ConfigItem(
			keyName = "readingSpeed",
			name = "Reading Skill",
			description = "Your reading skill level",
			section = generalSection,
			position = 3)
	default int readingSpeed()
	{
		return 10;
	}

	@ConfigItem(
			keyName = "showUsage",
			name = "Show API Usage",
			description = "Show your monthly usage in the chat on logon",
			section = generalSection,
			position = 4)
	default boolean showUsage()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showTitle",
			name = "Show Title",
			description = "Title with the context where text came from",
			section = generalSection,
			position = 5)
	default boolean showTitle()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showButton",
			name = "Quick Actions",
			description = "Translucent button near text boxes for quick actions",
			section = generalSection,
			position = 6)
	default boolean showButton()
	{
		return true;
	}

	@ConfigSection(
			name = "Translation",
			description = "Things to be translated",
			position = 1)
	String translationSection = "translationSection";

	@ConfigItem(
			keyName = "keepTitle",
			name = "Keep Titles",
			description = "Keep context titles as it is",
			section = translationSection,
			position = 0)
	default boolean keepTitle()
	{
		return false;
	}

	@ConfigItem(
			keyName = "translateChatMessage",
			name = "Chat Messages",
			description = "Translation for chat messages excluding players",
			section = translationSection,
			position = 1)
	default boolean translateChatMessage()
	{
		return true;
	}

	@ConfigItem(
			keyName = "translateExamine",
			name = "Examines",
			description = "Translation for object examines",
			section = translationSection,
			position = 2)
	default boolean translateExamine()
	{
		return true;
	}

	@ConfigItem(
			keyName = "translateOverheadText",
			name = "Overhead Text",
			description = "Translation for overhead text excluding players",
			section = translationSection,
			position = 3)
	default boolean translateOverheadText()
	{
		return true;
	}

	@ConfigItem(
			keyName = "translateScroll",
			name = "Scrolls",
			description = "Translation for scrolls",
			section = translationSection,
			position = 4)
	default boolean translateScroll()
	{
		return false;
	}

	@ConfigItem(
			keyName = "translateTreasureClue",
			name = "Treasure Clues",
			description = "Translation for treasure clues",
			section = translationSection,
			position = 5)
	default boolean translateTreasureClue()
	{
		return false;
	}

	@ConfigSection(
			name = "Visual",
			description = "Font and visual tweaks",
			position = 2)
	String visualSection = "visualSection";

	@ConfigItem(
			keyName = "fontName",
			name = "Font Name",
			description = "Choose your favorite font",
			section = visualSection,
			position = 0)
	default String fontName()
	{
		return "CozetteVector";
	}

	@Range(min = 1, max = 99) @ConfigItem(
			keyName = "fontSize",
			name = "Font Size",
			description = "Adjust the font size for more comfort",
			section = visualSection,
			position = 1)
	default int fontSize()
	{
		return 12;
	}

	@ConfigItem(
			keyName = "textShadow",
			name = "Text Shadow",
			description = "Text shadow for better visibility",
			section = visualSection,
			position = 2)
	default boolean textShadow()
	{
		return true;
	}

	@Alpha @ConfigItem(
			keyName = "overlayBackgroundColor",
			name = "Background Color",
			description = "Color of text boxes background",
			section = visualSection,
			position = 3)
	default Color overlayBackgroundColor()
	{
		return ComponentConstants.STANDARD_BACKGROUND_COLOR;
	}

	@ConfigItem(
			keyName = "overlayOutline",
			name = "Show Outline",
			description = "Outline around text boxes",
			section = visualSection,
			position = 4)
	default boolean overlayOutline()
	{
		return true;
	}

	@ConfigSection(
			name = "Formatting",
			description = "Text formatting inside of text boxes",
			closedByDefault = true,
			position = 3)
	String formattingSection = "formattingSection";

	@ConfigItem(
			keyName = "overlayAlignment",
			name = "Text Box Alignment",
			description = "Override default alignment behaviour",
			section = formattingSection,
			position = 0)
	default TextBox.Alignment overlayAlignment()
	{
		return TextBox.Alignment.DEFAULT;
	}

	@ConfigItem(
			keyName = "textAlignment",
			name = "Text Alignment",
			description = "Customize text alignment for unusual positions",
			section = formattingSection,
			position = 1)
	default TextBox.Behaviour textAlignment()
	{
		return TextBox.Behaviour.DEFAULT;
	}

	@Range(min = 32, max = 2277) @ConfigItem(
			keyName = "textWrap",
			name = "Text Wrap Width",
			description = "Set maximum text length to fit horizontally",
			section = formattingSection,
			position = 2)
	default int textWrap()
	{
		return 420;
	}

	@ConfigItem(
			keyName = "filterChatMessage",
			name = "Chat Messages Filter",
			description = "Which chat messages should not be translated",
			section = formattingSection,
			position = 3)
	default String filterChatMessage()
	{
		return "^[0-9,]+ x Coins\\.$" +
				"\n^You gain [0-9,]+ .+ XP\\.$" +
				"\n^Your .+ lap count is:" +
				"\n^Your reward is:" +
				"\n^<col.+?>Well done! You have completed" +
				"\n^<col.+?>You're assigned to kill" +
				"\n^<col.+?>You have completed your task!" +
				"\n^<col.+?>You've completed </col>[0-9,]+ tasks <col.+?>";
	}

	@ConfigItem(
			keyName = "filterOverheadText",
			name = "Overhead Text Filter",
			description = "Which overhead text should be ignored",
			section = formattingSection,
			position = 4)
	default String filterOverheadText()
	{
		return "^[0-9,\\.]+$";
	}

	@ConfigItem(
			keyName = "filterDialogText",
			name = "Dialog Text Filter",
			description = "Which dialog text should be skipped",
			section = formattingSection,
			position = 5)
	default String filterDialogText()
	{
		return "^Congratulations, you've just advanced your .+ level\\." +
				"\n^Exchange '.+': 5 coins" +
				"\n^Status: [0-9,]+ damage points left\\." +
				"\n^Your new task is to kill" +
				"\n^You're currently assigned to kill";
	}
}
