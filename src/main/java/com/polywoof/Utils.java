package com.polywoof;

import com.polywoof.api.API;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j @ParametersAreNonnullByDefault public final class Utils
{
	public static final class Text
	{
		private static final Matcher[] matchers = {
				Pattern.compile("<br>").matcher(""),
				Pattern.compile("<.*?>").matcher(""),
				Pattern.compile("  +").matcher("")
		};

		public static String filter(String string)
		{
			return matchers[2].reset(matchers[1].reset(matchers[0].reset(string).replaceAll(" ")).replaceAll("")).replaceAll(" ").trim();
		}
	}

	public static final class Interface
	{
		public static WidgetData getWidgetData(Point point, Widget[] widgets, Type type)
		{
			WidgetData widgetData = null;

			for(Widget widget : widgets)
			{
				WidgetData data = getWidgetData(point, widget.getDynamicChildren(), Type.DYNAMIC);

				if(data == null)
				{
					data = getWidgetData(point, widget.getNestedChildren(), Type.NESTED);
				}

				if(data == null)
				{
					data = getWidgetData(point, widget.getStaticChildren(), Type.STATIC);
				}

				if(data == null)
				{
					if(!widget.isHidden() && widget.getType() == WidgetType.TEXT && widget.contains(point))
					{
						widgetData = new WidgetData(widget, type);
					}
				}
				else
				{
					widgetData = data;
				}
			}

			return widgetData;
		}

		@AllArgsConstructor(access = AccessLevel.PRIVATE) public static final class WidgetData
		{
			public Widget widget;
			public Type type;
		}

		public enum Type
		{
			ROOT,
			DYNAMIC,
			NESTED,
			STATIC
		}
	}

	public static final class TextVerifier
	{
		private final List<Matcher> matcherList = new ArrayList<>();

		public TextVerifier(String patterns)
		{
			update(patterns);
		}

		public boolean verify(List<API.GameText> textList)
		{
			for(Matcher matcher : matcherList)
			{
				textList.removeIf(gameText ->
				{
					switch(gameText.type)
					{
						case MESSAGE:
						case OVERHEAD:
						case DIALOG:
						case OPTION:
							return matcher.reset(gameText.game).find();
					}

					return false;
				});
			}

			return textList.stream().anyMatch(gameText -> gameText.type != API.GameText.Type.TITLE);
		}

		public void update(String patterns)
		{
			matcherList.clear();

			for(String pattern : patterns.split("\n"))
			{
				matcherList.add(Pattern.compile(pattern, Pattern.DOTALL).matcher(""));
			}
		}
	}
}
