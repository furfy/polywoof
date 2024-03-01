package com.polywoof.api;

import com.google.gson.JsonObject;
import com.polywoof.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.*;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j @ParametersAreNonnullByDefault public final class MyMemory extends API
{
	private static final List<Language> trustedLanguageList = new ArrayList<>();
	private final OkHttpClient client;
	private String key;

	public MyMemory(OkHttpClient client)
	{
		this.client = client;

		synchronized(trustedLanguageList)
		{
			trustedLanguageList.clear();

			for(Language language : resourceLanguageList)
			{
				trustedLanguageList.add(new TrustedLanguage(language));
			}
		}
	}

	@Override public void fetch(List<GameText> textList, Language language, boolean detectSource, Translatable translatable)
	{
		if(language instanceof TrustedLanguage)
		{
			List<GameText> bodyText = textList.stream().filter(gameText -> !gameText.cache).collect(Collectors.toList());

			if(bodyText.isEmpty())
			{
				translatable.translate();
			}
			else
			{
				AtomicInteger size = new AtomicInteger();

				for(GameText gameText : bodyText)
				{
					FormBody.Builder builder = new FormBody.Builder()
							.add("q", Utils.Text.filter(gameText.game))
							.add("langpair", String.format("%s|%s", detectSource ? "Autodetect" : "en-GB", language.code));

					if(!key.equals("demo"))
					{
						builder.add("de", key);
					}

					fetch(builder.build(), body ->
					{
						try
						{
							JsonObject json = parser.parse(body).getAsJsonObject();

							handleCode(json.get("responseStatus").getAsInt());
							gameText.text = StringEscapeUtils.unescapeHtml4(json.getAsJsonObject("responseData").get("translatedText").getAsString());

							if(bodyText.size() == size.incrementAndGet())
							{
								translatable.translate();
							}
						}
						catch(Exception error)
						{
							lastError = error;
						}
					});
				}
			}
		}
	}

	@Override public void languageList(Supportable supportable)
	{
		synchronized(trustedLanguageList)
		{
			supportable.list(trustedLanguageList);
		}
	}

	@Override public Language languageFind(String language)
	{
		synchronized(trustedLanguageList)
		{
			return languageFinder(language, trustedLanguageList, resourceLanguageList);
		}
	}

	private void fetch(RequestBody requestBody, Receivable receivable)
	{
		if(!key.isBlank())
		{
			try
			{
				log.debug("Trying to create the {} request", "/get");
				Request request = new Request.Builder()
						.addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
						.addHeader("Accept", "application/json")
						.addHeader("Content-Type", "application/x-www-form-urlencoded")
						.addHeader("Content-Length", String.valueOf(requestBody.contentLength()))
						.url("https://api.mymemory.translated.net/get")
						.post(requestBody)
						.build();

				client.newCall(request).enqueue(new Callback()
				{
					@Override public void onFailure(Call call, IOException error)
					{
						lastError = error;
					}

					@Override public void onResponse(Call call, Response response)
					{
						try(ResponseBody responseBody = response.body())
						{
							handleCode(response.code());
							lastError = null;

							if(responseBody != null)
							{
								receivable.receive(responseBody.string());
							}
						}
						catch(Exception error)
						{
							lastError = error;
						}
					}
				});
			}
			catch(Exception error)
			{
				log.error("Failed to create the API request", error);
			}
		}
	}

	public void update(String key)
	{
		if(!key.equals(this.key))
		{
			if(!key.isBlank() && !key.matches("^[^@]+@[^@.]+\\.[^@.]+$"))
			{
				key = "demo";
			}

			this.key = key;
		}
	}

	private static void handleCode(int code) throws Exception
	{
		switch(code)
		{
			case 200:
				return;
			case 400:
				throw new Exception("Bad request");
			case 403:
				throw new Exception("Forbidden");
			case 429:
				throw new Exception("Too many requests");
			case 503:
				throw new Exception("Service unavailable");
			default:
				throw new Exception(String.valueOf(code));
		}
	}

	public static final class TrustedLanguage extends Language
	{
		TrustedLanguage(Language language)
		{
			super(language.code, language.name);
		}
	}

	public interface Receivable
	{
		void receive(String body) throws Exception;
	}
}
