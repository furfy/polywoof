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
	private String email;

	static
	{
		synchronized(trustedLanguageList)
		{
			synchronized(resourceLanguageList)
			{
				for(Language language : resourceLanguageList)
				{
					trustedLanguageList.add(new TrustedLanguage(language));
				}
			}
		}
	}

	public MyMemory(OkHttpClient client, Reportable reportable)
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
			AtomicInteger size = new AtomicInteger();

			for(GameText gameText : bodyText)
			{
				FormBody.Builder request = new FormBody.Builder()
						.add("q", Utils.Text.filter(gameText.game, true))
						.add("langpair", String.format("%s|%s", detectSource ? "Autodetect" : "en-GB", language.code));

				if(!email.equals("demo"))
				{
					request.add("de", email);
				}

				fetch(request.build(), body ->
				{
					try
					{
						JsonObject json = parser.parse(body).getAsJsonObject();
						handleCode(json.get("responseStatus").getAsInt());

						gameText.text = StringEscapeUtils.unescapeHtml4(json.getAsJsonObject("responseData").get("translatedText").getAsString());

						if(bodyText.size() == size.incrementAndGet())
						{
							runnable.run();
						}
					}
					catch(Exception error)
					{
						handleError(error);
					}
				});
			}
		}
	}

	@Override public void languageSupport(Supportable supportable)
	{
		synchronized(trustedLanguageList)
		{
			supportable.support(trustedLanguageList);
		}
	}

	@Override public Language languageFind(String query)
	{
		synchronized(trustedLanguageList)
		{
			return languageFinder(query, trustedLanguageList, resourceLanguageList);
		}
	}

	public void update(String email)
	{
		if(!email.equals(this.email))
		{
			if(!email.isBlank() && !email.matches("^[^@]+@[^@.]+\\.[^@.]+$"))
			{
				email = "demo";
			}

			this.email = email;
		}
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
			case 503:
				throw new Exception("Service Unavailable");
			default:
				throw new Exception(String.valueOf(code));
		}
	}

	private void fetch(RequestBody requestBody, Receivable receivable)
	{
		if(email.isBlank())
		{
			return;
		}

		try
		{
			Request.Builder request = new Request.Builder()
					.addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
					.addHeader("Accept", "application/json")
					.addHeader("Content-Type", "application/x-www-form-urlencoded")
					.addHeader("Content-Length", String.valueOf(requestBody.contentLength()))
					.url("https://api.mymemory.translated.net/get")
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
