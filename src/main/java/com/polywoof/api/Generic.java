package com.polywoof.api;

import com.polywoof.Utils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public final class Generic extends API
{
	@Override public void fetch(List<GameText> textList, Language language, Translatable translatable)
	{
		for(GameText gameText : textList)
		{
			gameText.text = Utils.Text.filter(gameText.game);
		}

		translatable.translate();
	}

	@Override public void languageList(Supportable supportable)
	{
		synchronized(resourceLanguageList)
		{
			supportable.list(resourceLanguageList);
		}
	}

	@Override public Language languageFind(String language)
	{
		return languageFinder(language, resourceLanguageList);
	}
}
