package com.polywoof.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.polywoof.PolywoofUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.*;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j @ParametersAreNonnullByDefault public class DeepL extends API
{
	protected static final List<Language> trusted = new ArrayList<>(100);
	private final OkHttpClient client;
	private String URL;
	private String key;

	public DeepL(OkHttpClient client, String key)
	{
		this.client = client;
		update(key);
	}

	public void fetch(List<GameText> textList, Language language, Submittable submittable)
	{
		if(!(language instanceof TrustedLanguage))
		{
			return;
		}

		synchronized(textList)
		{
			FormBody.Builder requestBody = new FormBody.Builder().add("target_lang", language.code)
					.add("source_lang", "en")
					.add("preserve_formatting", "1")
					.add("tag_handling", "html")
					.add("non_splitting_tags", "br");

			AtomicInteger uncached = new AtomicInteger();

			for(GameText gameText : textList)
			{
				if(!gameText.cache)
				{
					requestBody.add("text", PolywoofUtils.filter(gameText.game));
					uncached.incrementAndGet();
				}
			}

			if(uncached.get() == 0)
			{
				submittable.submit();
				return;
			}

			fetch("/v2/translate", requestBody.build(), body ->
			{
				JsonObject json = parser.parse(body).getAsJsonObject();
				List<String> translated = new ArrayList<>(50);

				for(JsonElement element : json.getAsJsonArray("translations"))
				{
					translated.add(StringEscapeUtils.unescapeHtml4(element.getAsJsonObject().get("text").getAsString()));
				}

				if(translated.size() == uncached.get())
				{
					for(GameText gameText : textList)
					{
						if(!gameText.cache)
						{
							gameText.text = translated.remove(0);
						}
					}
				}

				log.debug("DeepL translation is successful");
				submittable.submit();
			});
		}
	}

	public void languageList(Supportable supportable)
	{
		fetch("/v2/languages", new FormBody.Builder().add("type", "target").build(), body ->
		{
			JsonArray json = parser.parse(body).getAsJsonArray();

			synchronized(trusted)
			{
				trusted.clear();

				for(JsonElement element : json)
				{
					trusted.add(new TrustedLanguage(element.getAsJsonObject()));
				}

				supportable.list(trusted);
			}
		});
	}

	public Language languageFind(String language)
	{
		return languageFinder(language, trusted, languages);
	}

	public void update(String key)
	{
		URL = key.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com";
		this.key = key;
	}

	private void fetch(String path, RequestBody requestBody, Receivable receivable)
	{
		if(key.isEmpty())
		{
			return;
		}

		try
		{
			Request request = new Request.Builder().addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
					.addHeader("Authorization", "DeepL-Auth-Key " + key)
					.addHeader("Accept", "application/json")
					.addHeader("Content-Type", "application/x-www-form-urlencoded")
					.addHeader("Content-Length", String.valueOf(requestBody.contentLength()))
					.url(URL + path)
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

						if(responseBody == null)
						{
							return;
						}

						receivable.receive(responseBody.string());
					}
					catch(Exception error)
					{
						log.error("Failed to proceed the API response", error);
					}
				}
			});
		}
		catch(IOException error)
		{
			log.error("Failed to create the API request", error);
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

	public static class TrustedLanguage extends Language
	{
		protected TrustedLanguage(JsonObject object)
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
