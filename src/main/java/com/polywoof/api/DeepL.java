package com.polywoof.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.polywoof.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.*;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public final class DeepL extends API
{
	private static final List<Language> trustedLanguageList = new ArrayList<>();
	private final OkHttpClient client;
	private String key;
	private String endpoint;

	public DeepL(OkHttpClient client, String key)
	{
		this.client = client;
		update(key);
	}

	@Override public void fetch(List<GameText> textList, Language language, Translatable translatable)
	{
		if(language instanceof TrustedLanguage)
		{
			synchronized(textList)
			{
				FormBody.Builder requestBody = new FormBody.Builder().add("target_lang", language.code)
						.add("context", "runescape; dungeons and dragons; medieval fantasy;")
						.add("source_lang", "en")
						.add("preserve_formatting", "1")
						.add("formality", "prefer_less")
						.add("tag_handling", "html")
						.add("non_splitting_tags", "br");

				List<GameText> bodyText = new ArrayList<>(textList.size());

				for(GameText gameText : textList)
				{
					if(!gameText.cache)
					{
						requestBody.add("text", Utils.Text.filter(gameText.game));
						bodyText.add(gameText);
					}
				}

				if(bodyText.isEmpty())
				{
					translatable.translate();
				}
				else
				{
					fetch("/v2/translate", requestBody.build(), body ->
					{
						JsonArray json = parser.parse(body).getAsJsonObject().getAsJsonArray("translations");

						if(bodyText.size() == json.size())
						{
							Iterator<JsonElement> iterator = json.iterator();

							for(GameText gameText : textList)
							{
								if(!gameText.cache)
								{
									gameText.text = StringEscapeUtils.unescapeHtml4(iterator.next().getAsJsonObject().get("text").getAsString());
								}
							}
						}

						translatable.translate();
					});
				}
			}
		}
	}

	@Override public void languageList(Supportable supportable)
	{
		fetch("/v2/languages", new FormBody.Builder().add("type", "target").build(), body ->
		{
			JsonArray json = parser.parse(body).getAsJsonArray();

			synchronized(trustedLanguageList)
			{
				trustedLanguageList.clear();

				for(JsonElement element : json)
				{
					trustedLanguageList.add(new TrustedLanguage(element.getAsJsonObject()));
				}

				supportable.list(trustedLanguageList);
			}
		});
	}

	@Override public Language languageFind(String language)
	{
		synchronized(trustedLanguageList)
		{
			return languageFinder(language, trustedLanguageList, resourceLanguageList);
		}
	}

	private void fetch(String path, RequestBody requestBody, Receivable receivable)
	{
		if(!key.isBlank())
		{
			try
			{
				log.debug("Trying to create the {} request", path);
				Request request = new Request.Builder().addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
						.addHeader("Authorization", "DeepL-Auth-Key " + key)
						.addHeader("Accept", "application/json")
						.addHeader("Content-Type", "application/x-www-form-urlencoded")
						.addHeader("Content-Length", String.valueOf(requestBody.contentLength()))
						.url(endpoint + path)
						.post(requestBody)
						.build();

				client.newCall(request).enqueue(new Callback()
				{
					@Override public void onFailure(Call call, IOException error)
					{
						log.error("Failed to receive the API response", error);
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

	public void usage(Usable usable)
	{
		fetch("/v2/usage", new FormBody.Builder().build(), body ->
		{
			JsonObject json = parser.parse(body).getAsJsonObject();
			usable.usage(json.get("character_count").getAsLong(), json.get("character_limit").getAsLong());
		});
	}

	public void update(String key)
	{
		this.key = key;
		endpoint = key.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com";

		languageList(languageList -> languageList.forEach(language -> log.debug("{} - {}", language.code, language.name)));
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
				throw new Exception("Authorization failed");
			case 404:
				throw new Exception("The requested resource could not be found");
			case 413:
				throw new Exception("The request size exceeds the limit");
			case 414:
				throw new Exception("The request URL is too long");
			case 429:
				throw new Exception("Too many requests");
			case 456:
				throw new Exception("Quota exceeded");
			case 503:
				throw new Exception("Resource currently unavailable");
			default:
				throw new Exception("Internal error");
		}
	}

	public static final class TrustedLanguage extends Language
	{
		private TrustedLanguage(JsonObject object)
		{
			super(object.get("language").getAsString(), object.get("name").getAsString());
		}
	}

	public interface Receivable
	{
		void receive(String body) throws Exception;
	}

	public interface Usable
	{
		void usage(long characterCount, long characterLimit);
	}
}
