package com.polywoof;

import com.polywoof.api.API;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.RenderableEntity;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.util.ColorUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j @ParametersAreNonnullByDefault public final class TextBox implements RenderableEntity
{
	public static Alignment alignment = Alignment.DEFAULT;
	public static Behaviour behaviour = Behaviour.DEFAULT;
	public static Color backgroundColor = ComponentConstants.STANDARD_BACKGROUND_COLOR;
	public static Font resourceFont = FontManager.getDefaultFont();
	public static boolean boxOutline;
	public static boolean textShadow;

	private static final Matcher contentMatcher = Pattern.compile("<(?<property>.+?)(?:=(?<data>.+?))?/?>|.+?(?=<.+?>|$)").matcher("");
	private final List<API.GameText> textList;
	private final List<ContentPage> content = new ArrayList<>();
	private final Rectangle bounds = new Rectangle();
	private AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	private Font font;
	private Font fallbackFont;
	private int textWrap;

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

	public TextBox(List<API.GameText> textList, String fontName, int fontSize, int textWrap)
	{
		this.textList = textList;
		update(fontName, fontSize, textWrap);
	}

	@Override public Dimension render(Graphics2D graphics)
	{
		if(bounds.isEmpty())
		{
			return bounds.getSize();
		}

		Point anchor = new Point();
		Point offset = new Point();

		float h;
		float v;
		float a;

		switch(behaviour)
		{
			case TOP_LEFT:
			case LEFT:
			case BOTTOM_LEFT:
				h = 1f;
				break;
			default:
			case DEFAULT:
			case TOP:
			case CENTER:
			case BOTTOM:
				h = 0f;
				break;
			case TOP_RIGHT:
			case RIGHT:
			case BOTTOM_RIGHT:
				h = -1f;
				break;
		}

		switch(behaviour)
		{
			default:
			case DEFAULT:
			case TOP_LEFT:
			case TOP:
			case TOP_RIGHT:
			case LEFT:
			case CENTER:
			case RIGHT:
				v = -1f;
				break;
			case BOTTOM_LEFT:
			case BOTTOM:
			case BOTTOM_RIGHT:
				v = 1f;
				break;
		}

		anchor.x = (int)(bounds.x - bounds.width * (-h / 2f + 0.5f));
		anchor.y = (int)(bounds.y - bounds.height * (v / 2f + 0.5f));

		graphics.setColor(backgroundColor);
		graphics.setComposite(composite);
		graphics.fillRect(anchor.x, anchor.y, bounds.width, bounds.height);

		if(boxOutline)
		{
			graphics.setColor(backgroundColor.darker());
			graphics.drawRect(anchor.x, anchor.y, bounds.width - 1, bounds.height - 1);
			graphics.setColor(backgroundColor.brighter());
			graphics.drawRect(anchor.x + 1, anchor.y + 1, bounds.width - 3, bounds.height - 3);
		}

		Color overrideColor = Color.WHITE;
		Font overrideFont = font;
		FontMetrics metrics = graphics.getFontMetrics(font);

		for(ContentPage contentPage : content)
		{
			switch(contentPage.gameText.type)
			{
				case TITLE:
				case OVERHEAD:
				case OPTION:
					a = 0f;
					break;
				default:
					switch(alignment)
					{
						case FORCE_LEFT:
							a = -1f;
							break;
						default:
						case DEFAULT:
							switch(behaviour)
							{
								case TOP_LEFT:
								case LEFT:
								case BOTTOM_LEFT:
									a = -1f;
									break;
								default:
								case DEFAULT:
								case TOP:
								case CENTER:
								case BOTTOM:
									a = 0f;
									break;
								case TOP_RIGHT:
								case RIGHT:
								case BOTTOM_RIGHT:
									a = 1f;
									break;
							}
							break;
						case FORCE_CENTER:
							a = 0f;
							break;
						case FORCE_RIGHT:
							a = 1f;
							break;
					}
					break;
			}

			for(ElementLine elementLine : contentPage)
			{
				for(Object element : elementLine)
				{
					if(element instanceof String)
					{
						if(overrideFont.canDisplayUpTo((String)element) == -1)
						{
							graphics.setFont(overrideFont);
						}
						else
						{
							graphics.setFont(fallbackFont);
						}

						FontMetrics dynamic = graphics.getFontMetrics();

						anchor.x = bounds.x + offset.x + (int)(bounds.width * ((a / 2f + 0.5f) + (h / 2f - 0.5f)) - elementLine.width * (a / 2f + 0.5f) - metrics.getDescent() * a * 3f);
						anchor.y = bounds.y + offset.y + elementLine.height + metrics.getDescent() - (int)(bounds.height * (v / 2f + 0.5f));

						if(textShadow)
						{
							graphics.setColor(Color.BLACK);
							graphics.drawString((String)element, anchor.x + 1, anchor.y + 1);
						}

						switch(contentPage.gameText.type)
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
								graphics.setColor(overrideColor);
								break;
						}

						graphics.drawString((String)element, anchor.x, anchor.y);
						offset.x += dynamic.stringWidth((String)element);
					}

					if(element instanceof Color)
					{
						if(element.equals(Color.BLACK))
						{
							overrideColor = Color.WHITE;
						}
						else
						{
							overrideColor = (Color)element;
						}
					}

					if(element instanceof Font)
					{
						overrideFont = (Font)element;
					}
				}

				offset.x = 0;
				offset.y += elementLine.height;
			}

			offset.y += metrics.getDescent();
		}

		return bounds.getSize();
	}

	public Dimension update(Graphics2D graphics)
	{
		if(!bounds.isEmpty())
		{
			return bounds.getSize();
		}

		Font overrideFont = font;
		FontMetrics metrics = graphics.getFontMetrics(font);

		content.clear();

		for(API.GameText gameText : textList)
		{
			ContentPage contentPage = new ContentPage(content, gameText);
			ElementLine elementLine = new ElementLine(contentPage);

			contentMatcher.reset(gameText.text);

			while(contentMatcher.find())
			{
				if(contentMatcher.group("property") == null)
				{
					if(overrideFont.canDisplayUpTo(contentMatcher.group()) == -1)
					{
						graphics.setFont(overrideFont);
					}
					else
					{
						graphics.setFont(fallbackFont);
					}

					FontMetrics dynamic = graphics.getFontMetrics();

					int width = dynamic.stringWidth(contentMatcher.group());
					int height = dynamic.getAscent() + metrics.getDescent();

					if(elementLine.width + width > textWrap)
					{
						int[] codes = contentMatcher.group().codePoints().toArray();
						int cursor = 0;
						int widest = width;
						int prefer = 0;
						int offset = 0;

						for(int i = 0; i < codes.length; i++)
						{
							int code = codes[i];

							widest -= dynamic.charWidth(code);

							if(code == ' ')
							{
								prefer = i + 1;
								offset = widest;
							}

							if(elementLine.width + width - widest > textWrap)
							{
								char append = '\n';

								if(prefer > 0 && i - prefer < 10)
								{
									i = prefer;
									prefer = 0;
									widest = offset;
								}
								else if(i > 0)
								{
									append = '↲';
								}

								elementLine.add(new String(codes, cursor, i - cursor) + append);
								elementLine.width += width - widest;
								elementLine.height = Math.max(height, elementLine.height);

								bounds.width = Math.max(elementLine.width, bounds.width);
								bounds.height += elementLine.height;

								elementLine = new ElementLine(contentPage);
								cursor = i;
								widest += width - widest - dynamic.charWidth(code);
							}
						}

						elementLine.add(new String(codes, cursor, codes.length - cursor));
						elementLine.width += width - widest;
					}
					else
					{
						elementLine.add(contentMatcher.group());
						elementLine.width += width;
					}

					elementLine.height = Math.max(height, elementLine.height);

					bounds.width = Math.max(elementLine.width, bounds.width);
				}
				else
				{
					switch(contentMatcher.group("property"))
					{
						case "br":
							int height = elementLine.height;

							elementLine = new ElementLine(contentPage);
							elementLine.height = height;

							bounds.height += elementLine.height;
							break;
						case "col":
							elementLine.add(ColorUtil.fromHex(contentMatcher.group("data")));
							break;
						case "/col":
							elementLine.add(Color.WHITE);
							break;
						default:
							log.warn("{} not implemented", contentMatcher.group());
							break;
					}
				}
			}

			bounds.height += elementLine.height + metrics.getDescent();
		}

		bounds.width += metrics.getDescent() * 6;
		bounds.height += metrics.getDescent() * 3;

		return bounds.getSize();
	}

	public void setLocation(int x, int y)
	{
		switch(behaviour)
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

		switch(behaviour)
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

		bounds.setLocation(x, y);
	}

	public void update(String fontName, int fontSize, int textWrap)
	{
		if(fontName.equals("CozetteVector"))
		{
			font = resourceFont.deriveFont(Font.PLAIN, fontSize);
		}
		else
		{
			font = new Font(fontName, Font.PLAIN, fontSize);
		}

		fallbackFont = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
		this.textWrap = textWrap;

		bounds.setSize(0, 0);
	}

	public void setOpacity(float opacity)
	{
		composite = composite.derive(opacity);
	}

	private static final class ContentPage extends ArrayList<ElementLine>
	{
		public final API.GameText gameText;

		public ContentPage(List<ContentPage> contentList, API.GameText gameText)
		{
			this.gameText = gameText;
			contentList.add(this);
		}
	}

	private static final class ElementLine extends ArrayList<Object>
	{
		public int width = 0;
		public int height = 0;

		public ElementLine(ContentPage contentPage)
		{
			contentPage.add(this);
		}
	}

	public enum Alignment
	{
		DEFAULT,
		FORCE_LEFT,
		FORCE_CENTER,
		FORCE_RIGHT
	}

	public enum Behaviour
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
}
