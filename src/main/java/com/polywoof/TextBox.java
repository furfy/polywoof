package com.polywoof;

import com.polywoof.api.API;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.RenderableEntity;
import net.runelite.client.ui.overlay.components.ComponentConstants;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public final class TextBox implements RenderableEntity
{
	public static Alignment alignment = Alignment.BOTTOM_LEFT;
	public static Behaviour behaviour = Behaviour.DEFAULT;
	public static Color backgroundColor = ComponentConstants.STANDARD_BACKGROUND_COLOR;
	public static Font resourceFont = FontManager.getDefaultFont();
	public static boolean boxOutline;
	public static boolean textShadow;
	public static int textWrap;

	private static Font font;
	private final List<API.GameText> textList;
	private final List<TextWrapped> textWrappedList = new ArrayList<>();
	private final Rectangle rectangle = new Rectangle();
	private AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

	static
	{
		try(InputStream stream = PolywoofPlugin.class.getResourceAsStream("/font.ttf"))
		{
			if(stream == null)
			{
				throw new IllegalArgumentException();
			}

			resourceFont = Font.createFont(Font.TRUETYPE_FONT, stream);
		}
		catch(Exception error)
		{
			log.error("Failed to load the default font", error);
		}
	}

	public TextBox(List<API.GameText> textList, String fontName, int fontSize)
	{
		this.textList = textList;
		setFont(fontName, fontSize);
	}

	@Override public Dimension render(Graphics2D graphics)
	{
		int x = 0, y = 0, offset = 0;

		switch(alignment)
		{
			case TOP_LEFT:
			case LEFT:
			case BOTTOM_LEFT:
				x = rectangle.x;
				break;
			case DEFAULT:
			case TOP:
			case CENTER:
			case BOTTOM:
				x = rectangle.x - rectangle.width / 2;
				break;
			case TOP_RIGHT:
			case RIGHT:
			case BOTTOM_RIGHT:
				x = rectangle.x - rectangle.width;
				break;
		}

		switch(alignment)
		{
			case DEFAULT:
			case TOP_LEFT:
			case TOP:
			case TOP_RIGHT:
			case LEFT:
			case CENTER:
			case RIGHT:
				y = rectangle.y;
				break;
			case BOTTOM_LEFT:
			case BOTTOM:
			case BOTTOM_RIGHT:
				y = rectangle.y - rectangle.height;
				break;
		}

		graphics.setColor(backgroundColor);
		graphics.setComposite(composite);
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
			for(String text : textWrapped.wrapList)
			{
				switch(alignment)
				{
					case TOP_LEFT:
					case LEFT:
					case BOTTOM_LEFT:
						switch(textWrapped.gameText.type)
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
					case DEFAULT:
					case TOP:
					case CENTER:
					case BOTTOM:
						switch(textWrapped.gameText.type)
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
					case RIGHT:
					case BOTTOM_RIGHT:
						switch(textWrapped.gameText.type)
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

				switch(alignment)
				{
					case DEFAULT:
					case TOP_LEFT:
					case TOP:
					case TOP_RIGHT:
					case LEFT:
					case CENTER:
					case RIGHT:
						y = rectangle.y + metrics.getDescent() * 2 + offset + metrics.getAscent();
						break;
					case BOTTOM_LEFT:
					case BOTTOM:
					case BOTTOM_RIGHT:
						y = rectangle.y + metrics.getDescent() * 2 + offset + metrics.getAscent() - rectangle.height;
						break;
				}

				graphics.setFont(font);

				if(textShadow)
				{
					graphics.setColor(Color.BLACK);
					graphics.drawString(text, x + 1, y + 1);
				}

				switch(textWrapped.gameText.type)
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

	public Dimension update(Graphics2D graphics)
	{
		if(rectangle.isEmpty())
		{
			FontMetrics metrics = graphics.getFontMetrics(font);

			textWrappedList.clear();

			for(API.GameText gameText : textList)
			{
				List<String> wrapList = new ArrayList<>();
				wrapList.add(gameText.text);

				for(int i = 0; i < wrapList.size(); i++)
				{
					String text = wrapList.get(i);

					if(metrics.stringWidth(text) > textWrap - metrics.getDescent() * 4)
					{
						int wrap = 0, word = 0;

						for(int j = 0; j < text.length(); j++)
						{
							if(metrics.charsWidth(text.toCharArray(), 0, j + 1) > textWrap - metrics.getDescent() * 4)
							{
								int cut = Math.min(wrap, word);

								if(cut != 0 || (cut = Math.max(wrap, word)) != 0)
								{
									String begin = text.substring(0, cut);
									String end = text.substring(cut);
									wrapList.set(i, begin);

									if(!end.isEmpty())
									{
										wrapList.add(i + 1, end);
									}

									rectangle.width = Math.max(rectangle.width, metrics.stringWidth(begin));
								}

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
					else
					{
						rectangle.width = Math.max(rectangle.width, metrics.stringWidth(text));
					}
				}

				textWrappedList.add(new TextWrapped(gameText, wrapList));
				rectangle.height += metrics.getAscent() * wrapList.size() + metrics.getDescent() * (wrapList.size() - 1);
			}

			if(textWrappedList.size() > 1)
			{
				rectangle.height += metrics.getDescent() * 2 * (textWrappedList.size() - 1);
			}

			rectangle.width += metrics.getDescent() * 4;
			rectangle.height += metrics.getDescent() * 4;
		}

		return rectangle.getSize();
	}

	public void setLocation(int x, int y)
	{
		switch(alignment)
		{
			case TOP_RIGHT:
			case RIGHT:
			case BOTTOM_RIGHT:
				x += PolywoofOverlay.iconSize.width;
				break;
			case DEFAULT:
			case TOP:
			case CENTER:
			case BOTTOM:
				x += PolywoofOverlay.iconSize.width / 2;
				break;
		}

		switch(alignment)
		{
			case BOTTOM_LEFT:
			case BOTTOM:
			case BOTTOM_RIGHT:
				y += PolywoofOverlay.iconSize.height;
				break;
			case DEFAULT:
			case LEFT:
			case CENTER:
			case RIGHT:
				y += PolywoofOverlay.iconSize.height / 2;
				break;
		}

		rectangle.setLocation(x, y);
	}

	public void setFont(String font, int size)
	{
		if(font.equals("CozetteVector"))
		{
			TextBox.font = resourceFont.deriveFont(Font.PLAIN, size);
		}
		else
		{
			TextBox.font = new Font(font, Font.PLAIN, size);
		}

		rectangle.setSize(0, 0);
	}

	public void setOpacity(float opacity)
	{
		composite = composite.derive(opacity);
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE) private static final class TextWrapped
	{
		public final API.GameText gameText;
		public final List<String> wrapList;
	}

	public enum Alignment
	{
		DEFAULT,
		TOP_LEFT,
		TOP,
		TOP_RIGHT,
		LEFT,
		CENTER,
		RIGHT,
		BOTTOM_LEFT,
		BOTTOM,
		BOTTOM_RIGHT
	}

	public enum Behaviour
	{
		DEFAULT,
		FORCE_LEFT,
		FORCE_CENTER,
		FORCE_RIGHT
	}
}
