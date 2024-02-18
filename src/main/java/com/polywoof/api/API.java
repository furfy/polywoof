package com.polywoof.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.polywoof.PolywoofPlugin;
import com.polywoof.Dictionary;
import com.polywoof.Utils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ColorUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public abstract class API
{
	public abstract void fetch(List<GameText> textList, Language language, Translatable translatable);
	public abstract void languageList(Supportable supportable);
	public abstract Language languageFind(String language);

	@AllArgsConstructor(access = AccessLevel.PROTECTED) public abstract static class Language
	{
		public final String code;
		public final String name;
	}

	protected static final JsonParser parser = new JsonParser();
	protected static final List<GameText> history = new ArrayList<>(20);
	protected static final List<Language> resourceLanguageList = new ArrayList<>();
	protected static Exception lastError;

	static
	{
		try(InputStream stream = PolywoofPlugin.class.getResourceAsStream("/languages.json"))
		{
			if(stream == null)
			{
				throw new IllegalArgumentException();
			}

			try(InputStreamReader reader = new InputStreamReader(stream))
			{
				synchronized(resourceLanguageList)
				{
					for(JsonElement element : parser.parse(reader).getAsJsonArray())
					{
						resourceLanguageList.add(new ResourceLanguage(element.getAsJsonObject()));
					}
				}
			}
		}
		catch(Exception error)
		{
			log.error("Failed to load languages from the resource", error);
		}
	}

	public final void stored(List<GameText> textList, Language language, Dictionary dictionary, Translatable translatable)
	{
		if(dictionary.status())
		{
			if(language instanceof ResourceLanguage)
			{
				dictionary.select(textList, language, () ->
				{
					textList.removeIf(gameText -> !gameText.cache);

					if(textList.stream().anyMatch(gameText -> gameText.type != GameText.Type.TITLE))
					{
						translatable.translate();
					}
				});
			}
			else
			{
				dictionary.select(textList, language, () -> fetch(textList, language, () -> dictionary.insert(textList, language, translatable::translate)));
			}
		}
		else
		{
			fetch(textList, language, translatable);
		}
	}

	public final void buffered(List<GameText> textList, Language language, Translatable translatable)
	{
		for(GameText previous : history)
		{
			for(GameText gameText : textList)
			{
				if(gameText.game.equals(previous.game))
				{
					gameText.text = previous.text;
					gameText.cache = previous.cache;
				}
			}
		}

		fetch(textList, language, () ->
		{
			for(GameText gameText : textList)
			{
				if(!gameText.cache)
				{
					if(history.size() > 19)
					{
						history.remove(0);
					}

					history.add(gameText);
				}
			}

			translatable.translate();
		});
	}

	public static String statusMessage(Status status)
	{
		StringBuilder builder = new StringBuilder(ColorUtil.wrapWithColorTag(status.text, status.color));

		if(status == Status.ON && lastError != null)
		{
			builder.append(ColorUtil.wrapWithColorTag(String.format(" [%s]", lastError.getMessage()), Color.RED));
		}

		return builder.toString();
	}

	@SafeVarargs protected static Language languageFinder(String query, List<Language>... languageList)
	{
		query = query.trim().toUpperCase();

		synchronized(resourceLanguageList)
		{
			for(List<Language> languages : languageList)
			{
				for(Language language : languages)
				{
					if(language.code.toUpperCase().equals(query))
					{
						return language;
					}
				}
			}

			for(List<Language> languages : languageList)
			{
				for(Language language : languages)
				{
					if(language.name.toUpperCase().startsWith(query))
					{
						return language;
					}
				}
			}

			for(List<Language> languages : languageList)
			{
				for(Language language : languages)
				{
					if(language.name.toUpperCase().contains(query))
					{
						return language;
					}
				}
			}
		}

		return new UnknownLanguage(query);
	}

	public final static class GameText
	{
		public final String game;
		public final Type type;
		public String text;
		public boolean cache;

		public GameText(String game, Type type)
		{
			this.game = game;
			this.type = type;
		}

		public GameText(String game, boolean keepTitle)
		{
			this(game, Type.TITLE);

			if(keepTitle)
			{
				text = Utils.Text.filter(game);
				cache = true;
			}
		}

		@AllArgsConstructor(access = AccessLevel.PRIVATE) public enum Type
		{
			TITLE(256),
			MESSAGE(512),
			EXAMINE(512),
			OVERHEAD(512),
			DIALOG(512),
			OPTION(512),
			SCROLL(2048);

			public final int size;
		}
	}

	public static final class ResourceLanguage extends Language
	{
		private ResourceLanguage(JsonObject object)
		{
			super(object.get("language").getAsString(), object.get("name").getAsString());
		}
	}

	public static final class UnknownLanguage extends Language
	{
		private UnknownLanguage(String code)
		{
			super(code, "Unknown");
		}
	}

	public interface Translatable
	{
		void translate();
	}

	public interface Supportable
	{
		void list(List<Language> languageList);
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE) public enum Status
	{
		ON("On", Color.GREEN),
		OFF("Off", Color.RED),
		OFFLINE("Offline", Color.ORANGE),
		GENERIC("Generic", Color.LIGHT_GRAY);

		public final String text;
		public final Color color;
	}
}
