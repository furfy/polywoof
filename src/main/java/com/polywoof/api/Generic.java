package com.polywoof.api;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public final class Generic extends API
{
	public Generic()
	{
		super((backend, error) -> log.error("Undefinable", error));
	}

	@Override public void fetch(List<GameText> textList, boolean detectSource, Language language, boolean ignoreTags, Runnable runnable)
	{
		if(!(language instanceof ResourceLanguage))
		{
			return;
		}

		for(GameText gameText : textList)
		{
			gameText.text = gameText.game;
			gameText.cache = true;
		}

		runnable.run();
	}

	@Override public void languageSupport(Supportable supportable)
	{
		synchronized(resourceLanguageList)
		{
			supportable.support(resourceLanguageList);
		}
	}

	@Override public Language languageFind(String query)
	{
		return languageFinder(query, resourceLanguageList);
	}
}
