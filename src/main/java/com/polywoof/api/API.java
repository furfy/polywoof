package com.polywoof.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.polywoof.Dictionary;
import com.polywoof.PolywoofPlugin;
import com.polywoof.Utils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault @AllArgsConstructor(access = AccessLevel.PUBLIC) public abstract class API
{
	protected static final JsonParser parser = new JsonParser();
	protected static final List<Language> resourceLanguageList = new ArrayList<>();

	private static final HashMap<GameText, Language> memory = new HashMap<>();
	private final Reportable reportable;

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

	public abstract void fetch(List<GameText> textList, boolean detectSource, Language language, boolean ignoreTags, Runnable runnable);
	public abstract void languageSupport(Supportable supportable);
	public abstract Language languageFind(String query);

	public final void dictionaryGet(List<GameText> textList, String targetLanguage, boolean ignoreTags, Dictionary dictionary, Runnable runnable)
	{
		Language language = languageFind(targetLanguage);

		if(textList.isEmpty() || language instanceof UnknownLanguage)
		{
			return;
		}

		if(this instanceof Generic || !dictionary.status())
		{
			fetch(textList, false, language, ignoreTags, runnable);
		}
		else
		{
			if(language instanceof ResourceLanguage)
			{
				dictionary.select(textList, language, () ->
				{
					textList.removeIf(gameText -> !gameText.cache);

					if(textList.stream().anyMatch(gameText -> gameText.type != GameText.Type.TITLE))
					{
						runnable.run();
					}
				});
			}
			else
			{
				dictionary.select(textList, language, () -> fetch(textList, false, language, ignoreTags, () -> dictionary.insert(textList, language, runnable)));
			}
		}
	}

	public final void memoryGet(List<GameText> textList, String targetLanguage, boolean ignoreTags, Runnable runnable)
	{
		Language language = languageFind(targetLanguage);

		if(textList.isEmpty() || language instanceof UnknownLanguage)
		{
			return;
		}

		if(this instanceof Generic)
		{
			fetch(textList, true, language, ignoreTags, runnable);
		}
		else
		{
			for(GameText memoryText : memory.keySet())
			{
				for(GameText gameText : textList)
				{
					if(gameText.game.equals(memoryText.game) && memory.get(memoryText).equals(language))
					{
						gameText.text = memoryText.text;
						gameText.cache = true;
					}
				}
			}

			fetch(textList, true, language, ignoreTags, () ->
			{
				for(GameText gameText : textList)
				{
					if(!gameText.cache)
					{
						memory.put(gameText, language);
					}
				}

				runnable.run();
			});
		}
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

				for(Language language : languages)
				{
					if(language.name.toUpperCase().equals(query))
					{
						return language;
					}
				}

				for(Language language : languages)
				{
					if(language.code.toUpperCase().startsWith(query))
					{
						return language;
					}
				}

				for(Language language : languages)
				{
					if(language.name.toUpperCase().startsWith(query))
					{
						return language;
					}
				}

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

	protected void handleError(Exception error)
	{
		reportable.report(this, error);
	}

	@AllArgsConstructor(access = AccessLevel.PROTECTED) public abstract static class Language
	{
		public final String code;
		public final String name;
	}

	public static final class GameText
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

		public static GameText create(String game, Type type, boolean translate, boolean removeTags)
		{
			GameText gameText = new GameText(game, type);

			if(!translate)
			{
				gameText.text = Utils.Text.filter(game, removeTags);
				gameText.cache = true;
			}

			return gameText;
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
			super(object.get("code").getAsString(), object.get("name").getAsString());
		}
	}

	public static final class UnknownLanguage extends Language
	{
		private UnknownLanguage(String code)
		{
			super(code, "Unknown");
		}
	}

	public interface Supportable
	{
		void support(List<Language> languageList);
	}

	public interface Reportable
	{
		void report(API backend, Exception error);
	}
}
