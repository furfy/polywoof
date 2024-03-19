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
				Pattern.compile("^ +| +$|( ) +").matcher("")};

		public static String filter(String text, boolean removeTags)
		{
			text = matchers[0].reset(text).replaceAll(" ");

			if(removeTags)
			{
				text = matchers[1].reset(text).replaceAll("");
			}

			return matchers[2].reset(text).replaceAll("$1");
		}

		public static List<String> format(List<Widget> widgetList)
		{
			List<String> textList = new ArrayList<>();
			StringBuilder text = new StringBuilder();

			for(Widget widget : widgetList)
			{
				if(widget.getType() == WidgetType.TEXT && !widget.getText().isBlank())
				{
					if(text.length() > 0)
					{
						text.append("<br>");
					}

					text.append(widget.getText());
				}
				else if(text.length() > 0)
				{
					textList.add(text.toString());
					text.setLength(0);
				}
			}

			if(text.length() > 0)
			{
				textList.add(text.toString());
			}

			return textList;
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

	public static final class TextVerifier extends ArrayList<Matcher>
	{
		public TextVerifier(String patterns)
		{
			update(patterns);
		}

		public void verify(List<API.GameText> textList)
		{
			for(Matcher matcher : this)
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

			if(textList.stream().allMatch(gameText -> gameText.type.equals(API.GameText.Type.TITLE)))
			{
				textList.clear();
			}
		}

		public void update(String patterns)
		{
			clear();

			for(String pattern : patterns.split("\n"))
			{
				add(Pattern.compile(pattern, Pattern.DOTALL).matcher(""));
			}
		}
	}
}
