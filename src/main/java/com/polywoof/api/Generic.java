package com.polywoof.api;

import com.polywoof.PolywoofUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public class Generic extends API
{
	public void fetch(List<GameText> textList, Language language, Submittable submittable)
	{
		for(GameText gameText : textList)
		{
			gameText.text = PolywoofUtils.filter(gameText.game);
		}

		submittable.submit();
	}

	public void languageList(Supportable supportable)
	{
		supportable.list(languages);
	}

	public Language languageFind(String language)
	{
		return languageFinder(language, languages);
	}
}
