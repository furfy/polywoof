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

@Slf4j @ParametersAreNonnullByDefault public final class DeepL extends API
{
	private static final List<Language> trustedLanguageList = new ArrayList<>();
	private static final MediaType mediaType = MediaType.parse("Content-Type: application/json");
	private final OkHttpClient client;
	private String endpoint;
	private String key;

	public DeepL(OkHttpClient client, Reportable reportable)
	{
		super(reportable);
		this.client = client;
	}

	@Override public void fetch(List<GameText> textList, boolean detectSource, Language language, boolean ignoreTags, Runnable runnable)
	{
		if(!(language instanceof TrustedLanguage))
		{
			return;
		}

		List<GameText> bodyText = textList.stream().filter(gameText -> !gameText.cache).collect(Collectors.toList());

		if(bodyText.isEmpty())
		{
			runnable.run();
		}
		else
		{
			JsonArray jsonArray = new JsonArray();

			for(GameText gameText : bodyText)
			{
				jsonArray.add(Utils.Text.filter(gameText.game, ignoreTags));
			}

			JsonObject jsonObject = new JsonObject();
			jsonObject.add("text", jsonArray);
			jsonObject.addProperty("target_lang", language.code);
			jsonObject.addProperty("context", "runescape; dungeons and dragons; medieval fantasy;");
			jsonObject.addProperty("split_sentences", "nonewlines");
			jsonObject.addProperty("preserve_formatting", true);
			jsonObject.addProperty("formality", "prefer_less");
			jsonObject.addProperty("tag_handling", "xml");

			if(!detectSource)
			{
				jsonObject.addProperty("source_lang", "EN");
			}

			fetch("/v2/translate", FormBody.create(mediaType, jsonObject.toString()), body ->
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

					runnable.run();
				}
			});
		}
	}

	@Override public void languageSupport(Supportable supportable)
	{
		synchronized(trustedLanguageList)
		{
			if(trustedLanguageList.isEmpty())
			{
				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("type", "target");

				fetch("/v2/languages", FormBody.create(mediaType, jsonObject.toString()), body ->
				{
					synchronized(trustedLanguageList)
					{
						JsonArray json = parser.parse(body).getAsJsonArray();

						for(JsonElement element : json)
						{
							trustedLanguageList.add(new TrustedLanguage(element.getAsJsonObject()));
						}

						supportable.support(trustedLanguageList);
					}
				});
			}
			else
			{
				supportable.support(trustedLanguageList);
			}
		}
	}

	@Override public Language languageFind(String query)
	{
		synchronized(trustedLanguageList)
		{
			return languageFinder(query, trustedLanguageList, resourceLanguageList);
		}
	}

	public void update(String key)
	{
		if(!key.equals(this.key))
		{
			this.key = key;
			endpoint = key.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com";
		}
	}

	public void usage(Usable usable)
	{
		fetch("/v2/usage", FormBody.create(mediaType, "{}"), body ->
		{
			JsonObject json = parser.parse(body).getAsJsonObject();
			usable.usage(json.get("character_count").getAsLong(), json.get("character_limit").getAsLong());
		});
	}

	private static void handleCode(int code) throws Exception
	{
		switch(code)
		{
			case 200:
				return;
			case 400:
				throw new Exception("Bad Request");
			case 403:
				throw new Exception("Forbidden");
			case 429:
				throw new Exception("Too Many Requests");
			case 456:
				throw new Exception("Quota Exceeded");
			case 503:
				throw new Exception("Service Unavailable");
			default:
				throw new Exception(String.valueOf(code));
		}
	}

	private void fetch(String path, RequestBody requestBody, Receivable receivable)
	{
		if(key.isBlank())
		{
			return;
		}

		try
		{
			Request.Builder request = new Request.Builder()
					.addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
					.addHeader("Authorization", "DeepL-Auth-Key " + key)
					.addHeader("Accept", "application/json")
					.addHeader("Content-Type", "application/json")
					.addHeader("Content-Length", String.valueOf(requestBody.contentLength()))
					.url(endpoint + path)
					.post(requestBody);

			client.newCall(request.build()).enqueue(new Callback()
			{
				@Override public void onFailure(Call call, IOException error)
				{
					handleError(error);
				}

				@Override public void onResponse(Call call, Response response)
				{
					try(ResponseBody responseBody = response.body())
					{
						handleCode(response.code());

						if(responseBody != null)
						{
							receivable.receive(responseBody.string());
						}
					}
					catch(Exception error)
					{
						handleError(error);
					}
				}
			});
		}
		catch(Exception error)
		{
			log.error("Failed to create the API request", error);
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
