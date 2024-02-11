package com.polywoof;

import com.polywoof.api.API;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j @ParametersAreNonnullByDefault public class PolywoofUtils
{
	private static final Pattern[] filterPatterns = {
			Pattern.compile("<br>"),
			Pattern.compile("<.*?>"),
			Pattern.compile("  +")
	};

	public static String filter(String string)
	{
		return filterPatterns[2].matcher(filterPatterns[1].matcher(filterPatterns[0].matcher(string).replaceAll(" ")).replaceAll("")).replaceAll(" ").trim();
	}

	public static class Interface
	{
		public static CurrentWidget getCurrentWidget(Point point, @Nullable Widget[] widgetArray, Type type)
		{
			if(widgetArray == null)
			{
				return null;
			}

			CurrentWidget currentWidget = new CurrentWidget(null, null);

			for(Widget widget : widgetArray)
			{
				if(!widget.isHidden() && widget.getType() == WidgetType.TEXT && widget.contains(point))
				{
					currentWidget.widget = widget;
					currentWidget.type = type;
				}

				CurrentWidget current = getCurrentWidget(point, widget.getDynamicChildren(), Type.DYNAMIC);

				if(!current.exists())
				{
					current = getCurrentWidget(point, widget.getNestedChildren(), Type.NESTED);
				}

				if(!current.exists())
				{
					current = getCurrentWidget(point, widget.getStaticChildren(), Type.STATIC);
				}

				if(current.exists())
				{
					currentWidget = current;
				}
			}

			return currentWidget;
		}

		@Nullable @AllArgsConstructor(access = AccessLevel.PRIVATE) public static class CurrentWidget
		{
			Widget widget;
			Type type;

			public boolean exists()
			{
				return widget != null;
			}
		}

		public enum Type
		{
			ROOT,
			DYNAMIC,
			NESTED,
			STATIC
		}
	}

	public static class TextVerifier
	{
		public final List<Pattern> patternList = new ArrayList<>();

		public TextVerifier(String patterns)
		{
			update(patterns);
		}

		public void verify(List<API.GameText> textList)
		{
			for(Pattern pattern : patternList)
			{
				textList.removeIf(gameText ->
				{
					switch(gameText.type)
					{
						case MESSAGE:
						case OVERHEAD:
						case DIALOG:
							return pattern.matcher(gameText.game).find();
					}

					return false;
				});

				for(API.GameText gameText : textList)
				{
					if(gameText.type != API.GameText.Type.TITLE)
					{
						return;
					}
				}

				textList.clear();
			}
		}

		public void update(String patterns)
		{
			patternList.clear();

			for(String pattern : patterns.split("\n"))
			{
				patternList.add(Pattern.compile(pattern, Pattern.DOTALL));
			}
		}
	}
}
