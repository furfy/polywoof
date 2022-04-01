package com.polywoof;

import net.runelite.client.config.*;
import net.runelite.client.ui.overlay.components.ComponentConstants;

import java.awt.*;

@ConfigGroup("polywoof")
public interface PolywoofConfig extends Config
{
	@ConfigSection(name = "Primary", description = "Most important", position = 0)
	String primarySection = "primarySection";

	@ConfigSection(name = "Translation", description = "What will be translated", position = 1)
	String translationSection = "translationSection";

	@ConfigSection(name = "Visual", description = "Font and visual appearance", position = 2)
	String visualSection = "visualSection";

	@ConfigSection(name = "Formatting", description = "Text formatting", position = 3)
	String formattingSection = "formattingSection";

	@ConfigItem(keyName = "toggle", name = "", description = "", hidden = true)
	default boolean toggle()
	{
		return true;
	}

	@ConfigItem(keyName = "toggle", name = "", description = "")
	void toggle(boolean toggle);

	/*
		Primary
	 */

	@ConfigItem(keyName = "language", name = "Target Language", description = "Type your desired one, «ru» for russian, «fr» french, etc", section = primarySection, position = 0)
	default String language()
	{
		return "ru";
	}

	@ConfigItem(keyName = "key", name = "DeepL API Key", description = "This is REQUIRED, see www.DeepL.com", secret = true, section = primarySection, position = 1)
	default String key()
	{
		return "";
	}

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "readingSpeed", name = "Reading Skill", description = "How quickly do you read", section = primarySection, position = 2)
	default int readingSpeed()
	{
		return 10;
	}

	@ConfigItem(keyName = "showUsage", name = "Show API Usage", description = "See your monthly usage on logon", section = primarySection, position = 3)
	default boolean showUsage()
	{
		return true;
	}

	@ConfigItem(keyName = "showButton", name = "Show Button", description = "Quality of life feature", section = primarySection, position = 4)
	default boolean showButton()
	{
		return true;
	}

	@ConfigItem(keyName = "test", name = "Test Mode", description = "It makes everything useless", warning = "Are you sure you want to toggle the translation?", section = primarySection, position = 5)
	default boolean test()
	{
		return false;
	}

	/*
		Translation
	 */

	@ConfigItem(keyName = "enableMessages", name = "Chat Messages", description = "Translate chat messages", section = translationSection, position = 0)
	default boolean enableMessages()
	{
		return true;
	}

	@ConfigItem(keyName = "enableExamine", name = "Examine", description = "Translate any examine", section = translationSection, position = 1)
	default boolean enableExamine()
	{
		return true;
	}

	@ConfigItem(keyName = "enableOverhead", name = "Overhead Text", description = "Translate overhead text", section = translationSection, position = 2)
	default boolean enableOverhead()
	{
		return false;
	}

	@ConfigItem(keyName = "enableScrolls", name = "Scrolls", description = "Translate various scrolls", section = translationSection, position = 3)
	default boolean enableScrolls()
	{
		return false;
	}

	@ConfigItem(keyName = "enableBooks", name = "Books", description = "Translate various books", warning = "It uses a huge amount of resources to translate! Are you sure?", section = translationSection, position = 4)
	default boolean enableBooks()
	{
		return false;
	}

	@ConfigItem(keyName = "enableClues", name = "Treasure Clues", description = "Translate treasure clues", section = translationSection, position = 5)
	default boolean enableClues()
	{
		return false;
	}

	@ConfigItem(keyName = "enableDiary", name = "Quest Diary", description = "Translate quest diary", warning = "It uses a lot of resources to translate! Are you sure?", section = translationSection, position = 6)
	default boolean enableDiary()
	{
		return false;
	}

	/*
		Visual
	 */

	@ConfigItem(keyName = "fontName", name = "Font Name", description = "Check a fonts viewer", section = visualSection, position = 0)
	default String fontName()
	{
		return "Consolas";
	}

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "fontSize", name = "Font Size", description = "Why it does matter", section = visualSection, position = 1)
	default int fontSize()
	{
		return 12;
	}

	@ConfigItem(keyName = "textShadow", name = "Text Shadow", description = "Suspicious shadowy text", section = visualSection, position = 2)
	default boolean textShadow()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "overlayColor", name = "Background Color", description = "Any color is acceptable", section = visualSection, position = 3)
	default Color overlayColor()
	{
		return ComponentConstants.STANDARD_BACKGROUND_COLOR;
	}

	@ConfigItem(keyName = "overlayOutline", name = "Show Outline", description = "Make it.. pretty", section = visualSection, position = 4)
	default boolean overlayOutline()
	{
		return true;
	}

	/*
		Formatting
	 */

	@ConfigItem(keyName = "textAlignment", name = "Text Alignment", description = "Move it to the right place", section = formattingSection, position = 0)
	default PolywoofComponent.Behaviour textAlignment()
	{
		return PolywoofComponent.Behaviour.DEFAULT;
	}

	@Range(min = 32, max = 2277)
	@ConfigItem(keyName = "textWrap", name = "Text Wrap Width", description = "Longest text ever", section = formattingSection, position = 1)
	default int textWrap()
	{
		return 420;
	}

	@ConfigItem(keyName = "sourceName", name = "Source Name", description = "Tell me who said that", section = formattingSection, position = 2)
	default boolean sourceName()
	{
		return true;
	}

	@ConfigItem(keyName = "numberedOptions", name = "Numbered Options", description = "Let's count up to ten", section = formattingSection, position = 3)
	default boolean numberedOptions()
	{
		return true;
	}

	/*
		Ignore
	*/
}
