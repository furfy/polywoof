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
import java.util.stream.Collectors;

@Slf4j @ParametersAreNonnullByDefault public final class Google extends API
{
	private static final List<Language> trustedLanguageList = new ArrayList<>();
	private static final MediaType mediaType = MediaType.parse("Content-Type: application/json");
	private final OkHttpClient client;
	private String key;

	public Google(OkHttpClient client)
	{
		this.client = client;
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
				JsonArray jsonArray = new JsonArray();

				for(GameText gameText : bodyText)
				{
					jsonArray.add(Utils.Text.filter(gameText.game));
				}

				JsonObject jsonObject = new JsonObject();
				jsonObject.add("q", jsonArray);
				jsonObject.addProperty("target", language.code);
				jsonObject.addProperty("format", "html");

				if(!detectSource)
				{
					jsonObject.addProperty("source", "en-GB");
				}

				fetch("/language/translate/v2", FormBody.create(mediaType, jsonObject.toString()), body ->
				{
					JsonArray json = parser.parse(body).getAsJsonObject().getAsJsonObject("data").getAsJsonArray("translations");

					if(bodyText.size() == json.size())
					{
						Iterator<JsonElement> iterator = json.iterator();

						for(GameText gameText : textList)
						{
							if(!gameText.cache)
							{
								gameText.text = StringEscapeUtils.unescapeHtml4(iterator.next().getAsJsonObject().get("translatedText").getAsString());
							}
						}
					}

					translatable.translate();
				});
			}
		}
	}

	@Override public void languageList(Supportable supportable)
	{
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("target", "en");

		fetch("/language/translate/v2/languages", FormBody.create(mediaType, jsonObject.toString()), body ->
		{
			synchronized(trustedLanguageList)
			{
				JsonArray json = parser.parse(body).getAsJsonObject().getAsJsonObject("data").getAsJsonArray("languages");
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
				Request.Builder request = new Request.Builder()
						.addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
						.addHeader("X-goog-api-key", key)
						.addHeader("Accept", "application/json")
						.addHeader("Content-Type", "application/json")
						.addHeader("Content-Length", String.valueOf(requestBody.contentLength()))
						.url("https://translation.googleapis.com" + path)
						.post(requestBody);

				client.newCall(request.build()).enqueue(new Callback()
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
			this.key = key;
		}

		languageList(languageList -> log.info("Google {} languages loaded", languageList.size()));
	}

	private static void handleCode(int code) throws Exception
	{
		switch(code)
		{
			case 200:
				return;
			case 400:
				throw new Exception("Invalid argument");
			case 401:
				throw new Exception("Unauthenticated");
			case 429:
				throw new Exception("Resource exhausted");
			case 503:
				throw new Exception("Unavailable");
			default:
				throw new Exception(String.valueOf(code));
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
}
