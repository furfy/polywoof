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
			description = "Settings for overall Experience",
			position = 0)
	String generalSection = "generalSection";

	@ConfigItem(
			keyName = "backend",
			name = "Translation Service",
			description = "Choose the Backend for translation",
			section = generalSection,
			position = 0)
	default PolywoofPlugin.TranslationBackend backend()
	{
		return PolywoofPlugin.TranslationBackend.DEEPL;
	}

	@ConfigItem(
			keyName = "key",
			name = "API Key",
			description = "See the Guide how to get the API Key",
			secret = true,
			section = generalSection,
			position = 1)
	default String key()
	{
		return "";
	}

	@ConfigItem(
			keyName = "targetLanguage",
			name = "Target Language",
			description = "Type your desired Language",
			section = generalSection,
			position = 2)
	default String language()
	{
		return "ru";
	}

	@ConfigItem(
			keyName = "quickActions",
			name = "Quick Actions",
			description = "Translucent Quick Actions icon near Text Boxes",
			section = generalSection,
			position = 3)
	default boolean quickActions()
	{
		return true;
	}

	@ConfigItem(
			keyName = "backendAlternative",
			name = "Alternative Service",
			description = "Select the Backend for the Quick Actions",
			section = generalSection,
			position = 4)
	default PolywoofPlugin.TranslationBackend backendAlternative()
	{
		return PolywoofPlugin.TranslationBackend.DEEPL;
	}

	@ConfigItem(
			keyName = "keyAlternative",
			name = "API Key",
			description = "Same as the previous API Key, but for the Alternative Backend",
			secret = true,
			section = generalSection,
			position = 5)
	default String keyAlternative()
	{
		return "";
	}

	@Range(min = 1, max = 99) @ConfigItem(
			keyName = "readingSpeed",
			name = "Reading Skill",
			description = "Affects how quickly temporary Text Boxes disappear",
			section = generalSection,
			position = 6)
	default int readingSpeed()
	{
		return 10;
	}

	@ConfigItem(
			keyName = "showUsage",
			name = "Show API Usage",
			description = "Shows your API Usage when you logon",
			section = generalSection,
			position = 7)
	default boolean showUsage()
	{
		return true;
	}

	@ConfigSection(
			name = "Translation",
			description = "Things to be Translated",
			position = 1)
	String translationSection = "translationSection";

	@ConfigItem(
			keyName = "keepTitle",
			name = "Keep Titles",
			description = "Context Titles will not be translated",
			section = translationSection,
			position = 0)
	default boolean keepTitle()
	{
		return false;
	}

	@ConfigItem(
			keyName = "translateChatMessage",
			name = "Chat Messages",
			description = "Translation for Chat Messages excluding players",
			section = translationSection,
			position = 1)
	default boolean translateChatMessage()
	{
		return true;
	}

	@ConfigItem(
			keyName = "translateExamine",
			name = "Examines",
			description = "Translation for Object Examines",
			section = translationSection,
			position = 2)
	default boolean translateExamine()
	{
		return true;
	}

	@ConfigItem(
			keyName = "translateOverheadText",
			name = "Overhead Text",
			description = "Translation for Overhead Text excluding players",
			section = translationSection,
			position = 3)
	default boolean translateOverheadText()
	{
		return true;
	}

	@ConfigItem(
			keyName = "translateDialog",
			name = "Dialogs",
			description = "Translation for Dialogs and Options",
			section = translationSection,
			position = 4)
	default boolean translateDialog()
	{
		return true;
	}

	@ConfigItem(
			keyName = "translateScroll",
			name = "Scrolls",
			description = "Translation for Scrolls",
			section = translationSection,
			position = 5)
	default boolean translateScroll()
	{
		return false;
	}

	@ConfigItem(
			keyName = "translateTreasureClue",
			name = "Treasure Clues",
			description = "Translation for Treasure Clues",
			section = translationSection,
			position = 6)
	default boolean translateTreasureClue()
	{
		return false;
	}

	@ConfigSection(
			name = "Appearance",
			description = "Font and Visual Tweaks",
			closedByDefault = true,
			position = 2)
	String appearanceSection = "visualSection";

	@ConfigItem(
			keyName = "fontName",
			name = "Font Name",
			description = "Choose your favorite Font",
			section = appearanceSection,
			position = 0)
	default String fontName()
	{
		return "CozetteVector";
	}

	@Range(min = 1, max = 99) @ConfigItem(
			keyName = "fontSize",
			name = "Font Size",
			description = "Adjust the Font Size for more comfort",
			section = appearanceSection,
			position = 1)
	default int fontSize()
	{
		return 12;
	}

	@ConfigItem(
			keyName = "textShadow",
			name = "Text Shadow",
			description = "Text Shadow for better visibility",
			section = appearanceSection,
			position = 2)
	default boolean textShadow()
	{
		return true;
	}

	@Alpha @ConfigItem(
			keyName = "overlayBackgroundColor",
			name = "Background Color",
			description = "Color of Text Boxes background",
			section = appearanceSection,
			position = 3)
	default Color overlayBackgroundColor()
	{
		return ComponentConstants.STANDARD_BACKGROUND_COLOR;
	}

	@ConfigItem(
			keyName = "overlayOutline",
			name = "Show Outline",
			description = "Outline around Text Boxes",
			section = appearanceSection,
			position = 4)
	default boolean overlayOutline()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showTitle",
			name = "Show Titles",
			description = "Context Titles where text comes from",
			section = appearanceSection,
			position = 5)
	default boolean showTitle()
	{
		return true;
	}

	@ConfigSection(
			name = "Formatting",
			description = "Text Formatting inside of Text Boxes",
			closedByDefault = true,
			position = 3)
	String formattingSection = "formattingSection";

	@ConfigItem(
			keyName = "overlayAlignment",
			name = "Text Box Alignment",
			description = "Override default Alignment Behaviour",
			section = formattingSection,
			position = 0)
	default TextBox.Alignment overlayAlignment()
	{
		return TextBox.Alignment.DEFAULT;
	}

	@ConfigItem(
			keyName = "textAlignment",
			name = "Text Alignment",
			description = "Customize Text Alignment for unusual positions",
			section = formattingSection,
			position = 1)
	default TextBox.Behaviour textAlignment()
	{
		return TextBox.Behaviour.DEFAULT;
	}

	@Range(min = 32, max = 2277) @ConfigItem(
			keyName = "textWrap",
			name = "Text Wrap Width",
			description = "Set maximum Text Length to fit horizontally",
			section = formattingSection,
			position = 2)
	default int textWrap()
	{
		return 420;
	}

	@ConfigItem(
			keyName = "filterChatMessage",
			name = "Chat Messages Filter",
			description = "Filter for Chat Messages",
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
			description = "Filter for Overhead Text",
			section = formattingSection,
			position = 4)
	default String filterOverheadText()
	{
		return "^[0-9,\\.]+$";
	}

	@ConfigItem(
			keyName = "filterDialog",
			name = "Dialog Text Filter",
			description = "Filter for Dialogs and Options",
			section = formattingSection,
			position = 5)
	default String filterDialog()
	{
		return "^Congratulations, you've just advanced your .+ level\\." +
				"\n^Exchange '.+': 5 coins" +
				"\n^Status: [0-9,]+ damage points left\\." +
				"\n^Your new task is to kill" +
				"\n^You're currently assigned to kill";
	}
}
