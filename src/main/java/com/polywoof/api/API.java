package com.polywoof.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.polywoof.PolywoofPlugin;
import com.polywoof.PolywoofStorage;
import com.polywoof.PolywoofUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public abstract class API
{
	protected static final JsonParser parser = new JsonParser();
	protected static final List<Language> languages = new ArrayList<>();

	public abstract void fetch(List<GameText> textList, Language language, Submittable submittable);
	public abstract void languageList(Supportable supportable);
	public abstract Language languageFind(String language);

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
				JsonArray json = parser.parse(reader).getAsJsonArray();
				languages.clear();

				for(JsonElement element : json)
				{
					languages.add(new Language(element.getAsJsonObject()
							.get("language")
							.getAsString(), element.getAsJsonObject().get("name").getAsString()));
				}
			}
		}
		catch(IOException error)
		{
			log.error("Failed to load languages from the resource", error);
		}
	}

	@SafeVarargs protected static Language languageFinder(String search, List<Language>... languageList)
	{
		search = search.trim().toUpperCase();

		for(List<Language> languages : languageList)
		{
			for(Language language : languages)
			{
				if(language.code.toUpperCase().equals(search))
				{
					return language;
				}
			}
		}

		for(List<Language> languages : languageList)
		{
			for(Language language : languages)
			{
				if(language.name.toUpperCase().startsWith(search))
				{
					return language;
				}
			}
		}

		for(List<Language> languages : languageList)
		{
			for(Language language : languages)
			{
				if(language.name.toUpperCase().contains(search))
				{
					return language;
				}
			}
		}

		return new UnknownLanguage(search);
	}

	public void submit(List<GameText> textList, Language language, PolywoofStorage storage, Submittable submittable)
	{
		if(!storage.status())
		{
			fetch(textList, language, submittable);
			return;
		}

		storage.select(textList, language, () ->
		{
			log.debug("Language select {}", language.name);
			fetch(textList, language, () ->
			{
				log.debug("Language fetch {}", language.name);
				storage.insert(textList, language, () ->
				{
					log.debug("Language insert {}", language.name);
					submittable.submit();
				});
			});
		});
	}

	public static class GameText
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
				text = PolywoofUtils.filter(game);
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

	@AllArgsConstructor(access = AccessLevel.PROTECTED) public static class Language
	{
		public final String code;
		public final String name;
	}

	public static class UnknownLanguage extends Language
	{
		protected UnknownLanguage(String code)
		{
			super(code, "Unknown");
		}
	}

	public interface Submittable
	{
		void submit();
	}

	public interface Supportable
	{
		void list(List<Language> languages);
	}
}
