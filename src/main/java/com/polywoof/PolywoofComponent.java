package com.polywoof;

import com.polywoof.api.API;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.RenderableEntity;
import net.runelite.client.ui.overlay.components.ComponentConstants;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public class PolywoofComponent implements RenderableEntity
{
	public static int textWrap;
	public static boolean textShadow;
	public static boolean boxOutline;
	public static Color backgroundColor = ComponentConstants.STANDARD_BACKGROUND_COLOR;
	public static Behaviour behaviour = Behaviour.DEFAULT;
	public static OverlayPosition position = OverlayPosition.BOTTOM_LEFT;

	private final Rectangle rectangle = new Rectangle();
	private final List<API.GameText> textList;
	private final List<TextWrapped> textWrappedList;
	private Font font;
	private AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	private boolean refresh = true;

	public PolywoofComponent(List<API.GameText> textList, Font font)
	{
		this.textList = textList;
		this.font = font;
		textWrappedList = new ArrayList<>(textList.size());
	}

	@Override public Dimension render(Graphics2D graphics)
	{
		update(graphics);

		int x = 0, y = 0, offset = 0;

		switch(position)
		{
			case TOP_LEFT:
			case BOTTOM_LEFT:
				x = rectangle.x;
				break;
			case TOP_CENTER:
			case ABOVE_CHATBOX_RIGHT:
				x = rectangle.x - rectangle.width / 2;
				break;
			case TOP_RIGHT:
			case CANVAS_TOP_RIGHT:
			case BOTTOM_RIGHT:
				x = rectangle.x - rectangle.width;
				break;
		}

		switch(position)
		{
			case TOP_LEFT:
			case TOP_RIGHT:
			case CANVAS_TOP_RIGHT:
			case TOP_CENTER:
				y = rectangle.y;
				break;
			case BOTTOM_LEFT:
			case ABOVE_CHATBOX_RIGHT:
			case BOTTOM_RIGHT:
				y = rectangle.y - rectangle.height;
				break;
		}

		graphics.setFont(font);
		graphics.setComposite(composite);
		graphics.setColor(backgroundColor);
		graphics.fillRect(x, y, rectangle.width, rectangle.height);

		if(boxOutline)
		{
			graphics.setColor(backgroundColor.darker());
			graphics.drawRect(x, y, rectangle.width - 1, rectangle.height - 1);
			graphics.setColor(backgroundColor.brighter());
			graphics.drawRect(x + 1, y + 1, rectangle.width - 3, rectangle.height - 3);
		}

		FontMetrics metrics = graphics.getFontMetrics(font);

		for(TextWrapped textWrapped : textWrappedList)
		{
			for(String text : textWrapped.wrapped)
			{
				switch(position)
				{
					case TOP_LEFT:
					case BOTTOM_LEFT:
						switch(textWrapped.text.type)
						{
							case TITLE:
							case OVERHEAD:
							case OPTION:
								x = rectangle.x + rectangle.width / 2 - metrics.stringWidth(text) / 2;
								break;
							default:
								switch(behaviour)
								{
									default:
										x = rectangle.x + metrics.getDescent() * 2;
										break;
									case FORCE_CENTER:
										x = rectangle.x + rectangle.width / 2 - metrics.stringWidth(text) / 2;
										break;
									case FORCE_RIGHT:
										x = rectangle.x + rectangle.width - metrics.getDescent() * 2 - metrics.stringWidth(text);
										break;
								}
						}
						break;
					case TOP_CENTER:
					case ABOVE_CHATBOX_RIGHT:
						switch(textWrapped.text.type)
						{
							case TITLE:
							case OVERHEAD:
							case OPTION:
								x = rectangle.x - metrics.stringWidth(text) / 2;
								break;
							default:
								switch(behaviour)
								{
									case FORCE_LEFT:
										x = rectangle.x - rectangle.width / 2 + metrics.getDescent() * 2;
										break;
									default:
										x = rectangle.x - metrics.stringWidth(text) / 2;
										break;
									case FORCE_RIGHT:
										x = rectangle.x + rectangle.width / 2 - metrics.getDescent() * 2 - metrics.stringWidth(text);
										break;
								}
						}
						break;
					case TOP_RIGHT:
					case CANVAS_TOP_RIGHT:
					case BOTTOM_RIGHT:
						switch(textWrapped.text.type)
						{
							case TITLE:
							case OVERHEAD:
							case OPTION:
								x = rectangle.x - rectangle.width / 2 - metrics.stringWidth(text) / 2;
								break;
							default:
								switch(behaviour)
								{
									case FORCE_LEFT:
										x = rectangle.x - rectangle.width + metrics.getDescent() * 2;
										break;
									case FORCE_CENTER:
										x = rectangle.x - rectangle.width / 2 - metrics.stringWidth(text) / 2;
										break;
									default:
										x = rectangle.x - metrics.getDescent() * 2 - metrics.stringWidth(text);
										break;
								}
						}
						break;
				}

				switch(position)
				{
					case TOP_LEFT:
					case TOP_RIGHT:
					case CANVAS_TOP_RIGHT:
					case TOP_CENTER:
						y = rectangle.y + metrics.getDescent() * 2 + offset + metrics.getAscent();
						break;
					case BOTTOM_LEFT:
					case ABOVE_CHATBOX_RIGHT:
					case BOTTOM_RIGHT:
						y = rectangle.y + metrics.getDescent() * 2 + offset + metrics.getAscent() - rectangle.height;
						break;
				}

				if(textShadow)
				{
					graphics.setColor(Color.BLACK);
					graphics.drawString(text, x + 1, y + 1);
				}

				switch(textWrapped.text.type)
				{
					case TITLE:
						graphics.setColor(JagexColors.DARK_ORANGE_INTERFACE_TEXT);
						break;
					case EXAMINE:
						graphics.setColor(Color.LIGHT_GRAY);
						break;
					case OVERHEAD:
						graphics.setColor(JagexColors.YELLOW_INTERFACE_TEXT);
						break;
					default:
						graphics.setColor(Color.WHITE);
						break;
				}

				graphics.drawString(text, x, y);

				offset += metrics.getAscent() + metrics.getDescent();
			}

			offset += metrics.getDescent();
		}

		return rectangle.getSize();
	}

	private void update(Graphics2D graphics)
	{
		if(refresh)
		{
			refresh = false;
		}
		else
		{
			return;
		}

		rectangle.setSize(0, 0);
		textWrappedList.clear();

		FontMetrics metrics = graphics.getFontMetrics(font);

		for(API.GameText gameText : textList)
		{
			List<String> test = new ArrayList<>();
			test.add(gameText.text);

			for(int i = 0; i < test.size(); i++)
			{
				String text = test.get(i);

				if(metrics.stringWidth(text) <= textWrap - metrics.getDescent() * 4)
				{
					rectangle.width = Math.max(rectangle.width, metrics.stringWidth(text));
					continue;
				}

				int wrap = 0, word = 0;

				for(int j = 0; j < text.length(); j++)
				{
					if(metrics.charsWidth(text.toCharArray(), 0, j + 1) > textWrap - metrics.getDescent() * 4)
					{
						int cut = Math.min(wrap, word);

						if(cut == 0 && (cut = Math.max(wrap, word)) == 0)
						{
							break;
						}

						String begin = text.substring(0, cut);
						String end = text.substring(cut);
						test.set(i, begin);

						if(!end.isEmpty())
						{
							test.add(i + 1, end);
						}

						rectangle.width = Math.max(rectangle.width, metrics.stringWidth(begin));
						break;
					}

					switch(text.charAt(j))
					{
						case ' ':
						case '.':
						case ',':
						case ':':
						case ';':
						case '-':
							wrap = j + 1;
							word = 0;
							break;
						default:
							word = j;
							break;
					}
				}
			}

			rectangle.height += metrics.getAscent() * test.size() + metrics.getDescent() * (test.size() - 1);
			textWrappedList.add(new TextWrapped(gameText, test));
		}

		if(textWrappedList.size() > 1)
		{
			rectangle.height += metrics.getDescent() * 2 * (textWrappedList.size() - 1);
		}

		rectangle.width += metrics.getDescent() * 4;
		rectangle.height += metrics.getDescent() * 4;

		log.debug("Component is set to {}x{}", rectangle.width, rectangle.height);
	}

	public void setLocation(int x, int y)
	{
		rectangle.setLocation(x, y);
	}

	public void setFontSize(int size)
	{
		if(font.getSize() != size)
		{
			font = font.deriveFont((float)size);
			refresh = true;
		}
	}

	public void setOpacity(float opacity)
	{
		composite = composite.derive(opacity);
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE) private static class TextWrapped
	{
		public final API.GameText text;
		public final List<String> wrapped;
	}

	public enum Behaviour
	{
		DEFAULT,
		FORCE_LEFT,
		FORCE_CENTER,
		FORCE_RIGHT
	}
}
