package com.polywoof.api;

import com.polywoof.Utils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public final class Generic extends API
{
	@Override public void fetch(List<GameText> textList, Language language, boolean detectSource, Translatable translatable)
	{
		if(language instanceof ResourceLanguage)
		{
			for(GameText gameText : textList)
			{
				gameText.text = Utils.Text.filter(gameText.game);
				gameText.cache = true;
			}

			translatable.translate();
		}
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
