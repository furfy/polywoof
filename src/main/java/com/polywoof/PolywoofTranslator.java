package com.polywoof;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.*;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofTranslator
{
	private static final JsonParser parser = new JsonParser();
	private static final List<PolywoofStorage.Language> trusted = new ArrayList<>(100);
	private static final List<PolywoofStorage.Language> offline = languageLoader(PolywoofPlugin.class, "/languages.json");

	private final OkHttpClient client;
	private final PolywoofStorage storage;
	private String URL;
	private String key;

	public PolywoofTranslator(OkHttpClient client, PolywoofStorage storage, String auth)
	{
		this.client = client;
		this.storage = storage;
		this.update(auth);
	}

	public static synchronized List<PolywoofStorage.Language> languageLoader(Class<?> reflection, String resource)
	{
		try(InputStream stream = reflection.getResourceAsStream(resource))
		{
			if(stream == null)
				throw new IllegalArgumentException();

			try(InputStreamReader reader = new InputStreamReader(stream))
			{
				JsonArray json = parser.parse(reader).getAsJsonArray();
				List<PolywoofStorage.Language> output = new ArrayList<>(json.size());

				for(JsonElement element : json)
					output.add(new OfflineLanguage(element.getAsJsonObject().get("language").getAsString(), element.getAsJsonObject().get("name").getAsString()));

				return output;
			}
		}
		catch(IOException error)
		{
			log.error("Failed to load languages from the resource", error);
			return new ArrayList<>(0);
		}
	}

	public static PolywoofStorage.Language languageFinder(String search)
	{
		search = search.trim().toUpperCase();

		for(PolywoofStorage.Language language : trusted)
			if(language.toString().toUpperCase().equals(search))
				return language;

		for(PolywoofStorage.Language language : offline)
			if(language.toString().toUpperCase().equals(search))
				return language;

		for(PolywoofStorage.Language language : trusted)
			if(language.name.toUpperCase().startsWith(search))
				return language;

		for(PolywoofStorage.Language language : offline)
			if(language.name.toUpperCase().startsWith(search))
				return language;

		for(PolywoofStorage.Language language : trusted)
			if(language.name.toUpperCase().contains(search))
				return language;

		for(PolywoofStorage.Language language : offline)
			if(language.name.toUpperCase().contains(search))
				return language;

		return new UnknownLanguage(search);
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

	private void post(String path, RequestBody request, Receivable callback)
	{
		if(key.isEmpty())
			return;

		try
		{
			Request headers = new Request.Builder()
				.addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
				.addHeader("Authorization", "DeepL-Auth-Key " + key)
				.addHeader("Accept", "application/json")
				.addHeader("Content-Type", "application/x-www-form-urlencoded")
				.addHeader("Content-Length", String.valueOf(request.contentLength()))
				.url(URL + path)
				.post(request)
				.build();

			client.newCall(headers).enqueue(new Callback()
			{
				@Override
				public void onFailure(Call call, IOException error)
				{
					log.error("Failed to receive the API response", error);
				}

				@Override
				public void onResponse(Call call, Response response)
				{
					try(ResponseBody body = response.body())
					{
						handleCode(response.code());

						if(body == null)
							return;

						callback.receive(body.string());
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

	public void translate(String string, PolywoofStorage.Language language, PolywoofStorage.DataType type, Translatable callback)
	{
		if(string.isEmpty() || language instanceof UnknownLanguage)
			return;

		if(!storage.status())
		{
			translate(string, language, callback);
			return;
		}

		storage.select(string, language, type, select ->
		{
			if(select == null)
			{
				translate(string, language, insert ->
				{
					storage.insert(insert, string, language, type, () -> log.debug("[{}] INSERT", language));
					callback.translate(insert);
				});
			}
			else
			{
				log.debug("[{}] SELECT", language);
				callback.translate(select);
			}
		});
	}

	public void translate(String string, PolywoofStorage.Language language, Translatable callback)
	{
		if((string = PolywoofFormatter.filter(string)).isEmpty() || !(language instanceof TrustedLanguage))
			return;

		FormBody.Builder request = new FormBody.Builder()
			.add("target_lang", language.toString())
			.add("source_lang", "en")
			.add("preserve_formatting", "1")
			.add("tag_handling", "html")
			.add("non_splitting_tags", "br");

		for(String split : string.split("\n"))
			request.add("text", split);

		post("/v2/translate", request.build(), body ->
		{
			JsonObject json = parser.parse(body).getAsJsonObject();
			StringBuilder output = new StringBuilder(128);

			for(JsonElement element : json.getAsJsonArray("translations"))
				output.append(StringEscapeUtils.unescapeHtml4(element.getAsJsonObject().get("text").getAsString())).append("\n");

			callback.translate(output.toString());
		});
	}

	public void usage(Usable callback)
	{
		post("/v2/usage", new FormBody.Builder().build(), body ->
		{
			JsonObject json = parser.parse(body).getAsJsonObject();
			callback.usage(json.get("character_count").getAsLong(), json.get("character_limit").getAsLong());
		});
	}

	public void languages(String type, Supportable callback)
	{
		post("/v2/languages", new FormBody.Builder().add("type", type).build(), body ->
		{
			JsonArray json = parser.parse(body).getAsJsonArray();

			synchronized(trusted)
			{
				trusted.clear();

				for(JsonElement element : json)
					trusted.add(new TrustedLanguage(element.getAsJsonObject()));

				callback.list(trusted);
			}
		});
	}

	public void update(String auth)
	{
		URL = auth.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com";
		key = auth;
	}

	public interface Receivable
	{
		void receive(String body) throws Exception;
	}

	public interface Translatable
	{
		void translate(String text);
	}

	public interface Usable
	{
		void usage(long characterCount, long characterLimit);
	}

	public interface Supportable
	{
		void list(List<PolywoofStorage.Language> languages);
	}

	public static class TrustedLanguage extends PolywoofStorage.Language
	{
		public TrustedLanguage(JsonObject object)
		{
			super(object.get("language").getAsString(), object.get("name").getAsString());
		}
	}

	public static class OfflineLanguage extends PolywoofStorage.Language
	{
		public OfflineLanguage(String code, String name)
		{
			super(code, name);
		}
	}

	public static class UnknownLanguage extends PolywoofStorage.Language
	{
		public UnknownLanguage(String code)
		{
			super(code, "Unknown");
		}
	}
}
