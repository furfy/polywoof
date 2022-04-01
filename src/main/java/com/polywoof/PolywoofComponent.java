package com.polywoof;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.RenderableEntity;
import net.runelite.client.ui.overlay.components.ComponentConstants;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofComponent implements RenderableEntity
{
	@Setter private static int textWrap;
	@Setter private static boolean textShadow;
	@Setter private static boolean boxOutline;
	@Setter private static Color backgroundColor = ComponentConstants.STANDARD_BACKGROUND_COLOR;
	@Setter @Getter private static Alignment alignment = Alignment.BOTTOM_LEFT;
	@Setter @Getter private static Behaviour behaviour = Behaviour.DEFAULT;

	private final Rectangle rectangle = new Rectangle();
	private final List<List<String>> paragraphs = new ArrayList<>(10);
	private final String header;
	private final String string;
	private Font font;
	private Subject subject;
	private AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	private boolean revalidate;

	public PolywoofComponent(@Nullable String header, String string, Font font, Subject subject)
	{
		if(header != null && header.isEmpty())
			header = null;

		if(header == null && subject == Subject.HEADER)
			subject = Subject.NONE;

		this.header = header;
		this.string = string;
		this.font = font;
		this.subject = subject;

		revalidate();
	}

	public static void setPosition(OverlayPosition position)
	{
		switch(position)
		{
			case TOP_LEFT:
				setAlignment(Alignment.TOP_LEFT);
				break;
			case TOP_CENTER:
				setAlignment(Alignment.TOP_CENTER);
				break;
			case TOP_RIGHT:
			case CANVAS_TOP_RIGHT:
				setAlignment(Alignment.TOP_RIGHT);
				break;
			case BOTTOM_LEFT:
				setAlignment(Alignment.BOTTOM_LEFT);
				break;
			default:
			case ABOVE_CHATBOX_RIGHT:
				setAlignment(Alignment.BOTTOM_CENTER);
				break;
			case BOTTOM_RIGHT:
				setAlignment(Alignment.BOTTOM_RIGHT);
				break;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if(revalidate)
			update(graphics);

		int x = 0, y = 0;

		switch(alignment)
		{
			case TOP_LEFT:
			case BOTTOM_LEFT:
				x = rectangle.x;
				break;
			case TOP_CENTER:
			case BOTTOM_CENTER:
				x = rectangle.x - rectangle.width / 2;
				break;
			case TOP_RIGHT:
			case BOTTOM_RIGHT:
				x = rectangle.x - rectangle.width;
				break;
		}

		switch(alignment)
		{
			case TOP_LEFT:
			case TOP_RIGHT:
			case TOP_CENTER:
				y = rectangle.y;
				break;
			case BOTTOM_LEFT:
			case BOTTOM_CENTER:
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

		int offset = 0;
		boolean plain = subject == Subject.NONE;
		FontMetrics metrics = graphics.getFontMetrics(font);

		for(List<String> paragraph : paragraphs)
		{
			for(String line : paragraph)
			{
				switch(alignment)
				{
					case TOP_LEFT:
					case BOTTOM_LEFT:
						if(plain)
							switch(behaviour)
							{
								default:
									x = rectangle.x + metrics.getDescent() * 2;
									break;
								case FORCE_CENTER:
									x = rectangle.x + rectangle.width / 2 - metrics.stringWidth(line) / 2;
									break;
								case FORCE_RIGHT:
									x = rectangle.x + rectangle.width - metrics.getDescent() * 2 - metrics.stringWidth(line);
									break;
							}
						else
							x = rectangle.x + rectangle.width / 2 - metrics.stringWidth(line) / 2;
						break;
					case TOP_CENTER:
					case BOTTOM_CENTER:
						if(plain)
							switch(behaviour)
							{
								case FORCE_LEFT:
									x = rectangle.x - rectangle.width / 2 + metrics.getDescent() * 2;
									break;
								default:
									x = rectangle.x - metrics.stringWidth(line) / 2;
									break;
								case FORCE_RIGHT:
									x = rectangle.x + rectangle.width / 2 - metrics.getDescent() * 2 - metrics.stringWidth(line);
									break;
							}
						else
							x = rectangle.x - metrics.stringWidth(line) / 2;
						break;
					case TOP_RIGHT:
					case BOTTOM_RIGHT:
						if(plain)
							switch(behaviour)
							{
								case FORCE_LEFT:
									x = rectangle.x - rectangle.width + metrics.getDescent() * 2;
									break;
								case FORCE_CENTER:
									x = rectangle.x - rectangle.width / 2 - metrics.stringWidth(line) / 2;
									break;
								default:
									x = rectangle.x - metrics.getDescent() * 2 - metrics.stringWidth(line);
									break;
							}
						else
							x = rectangle.x - rectangle.width / 2 - metrics.stringWidth(line) / 2;
						break;
				}

				switch(alignment)
				{
					case TOP_LEFT:
					case TOP_RIGHT:
					case TOP_CENTER:
						y = rectangle.y + metrics.getDescent() * 2 + offset + metrics.getAscent();
						break;
					case BOTTOM_LEFT:
					case BOTTOM_CENTER:
					case BOTTOM_RIGHT:
						y = rectangle.y + metrics.getDescent() * 2 + offset + metrics.getAscent() - rectangle.height;
						break;
				}

				if(textShadow)
				{
					graphics.setColor(Color.BLACK);
					graphics.drawString(line, x + 1, y + 1);
				}

				graphics.setColor(plain ? Color.WHITE : JagexColors.MENU_TARGET);
				graphics.drawString(line, x, y);

				offset += metrics.getAscent() + metrics.getDescent();
			}

			offset += metrics.getDescent() * ((subject == Subject.NONE || subject == Subject.HEADER) && plain ? 1f : 0.5f);
			plain = true;
		}

		return rectangle.getSize();
	}

	public Dimension update(Graphics2D graphics)
	{
		if(!revalidate)
			return rectangle.getSize();

		revalidate = false;
		paragraphs.clear();
		rectangle.setSize(0, 0);

		int index = -1;
		FontMetrics metrics = graphics.getFontMetrics(font);

		for(String split : (subject == Subject.HEADER ? String.format("%s\n%s", header, string) : string).split("\n"))
		{
			List<String> paragraph = new ArrayList<>(10);

			if(subject == Subject.NUMBERED && ++index != 0)
				paragraph.add(String.format("%d. %s", index, split));
			else
				paragraph.add(split);

			for(int i = 0; i < paragraph.size(); i++)
			{
				String line = paragraph.get(i);

				if(metrics.stringWidth(line) <= textWrap - metrics.getDescent() * 4)
				{
					rectangle.width = Math.max(rectangle.width, metrics.stringWidth(line));
					continue;
				}

				int wrap = 0, word = 0;

				for(int j = 0; j < line.length(); j++)
				{
					if(metrics.charsWidth(line.toCharArray(), 0, j + 1) > textWrap - metrics.getDescent() * 4)
					{
						int cut = Math.min(wrap, word);

						if(cut == 0 && (cut = Math.max(wrap, word)) == 0)
							break;

						String begin = line.substring(0, cut);
						String end = line.substring(cut);

						paragraph.set(i, begin);

						if(!end.isEmpty())
							paragraph.add(i + 1, end);

						rectangle.width = Math.max(rectangle.width, metrics.stringWidth(begin));
						break;
					}

					switch(line.charAt(j))
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

			rectangle.height += metrics.getAscent() * paragraph.size() + metrics.getDescent() * (paragraph.size() - 1);
			paragraphs.add(paragraph);
		}

		if(paragraphs.size() > 1)
		{
			if(subject == Subject.HEADER)
				rectangle.height -= metrics.getDescent() * 0.5f;

			rectangle.height += metrics.getDescent() * (subject == Subject.NONE || subject == Subject.HEADER ? 2f : 1.5f) * (paragraphs.size() - 1);
		}

		rectangle.width += metrics.getDescent() * 4;
		rectangle.height += metrics.getDescent() * 4;

		log.debug("[{}x{}]", rectangle.width, rectangle.height);

		return rectangle.getSize();
	}

	public void setLocation(int x, int y)
	{
		rectangle.setLocation(x, y);
	}

	public void setFontSize(int size)
	{
		if(size == font.getSize())
			return;

		font = font.deriveFont((float) size);
	}

	public void setHeaderSubject(boolean toggle)
	{
		if(header == null || subject == Subject.OPTIONS || subject == Subject.NUMBERED)
			return;

		subject = toggle ? Subject.HEADER : Subject.NONE;
	}

	public void setNumberedSubject(boolean toggle)
	{
		if(subject == Subject.NONE || subject == Subject.HEADER)
			return;

		subject = toggle ? Subject.NUMBERED : Subject.OPTIONS;
	}

	public void setAlpha(float opacity)
	{
		composite = composite.derive(opacity);
	}

	public void revalidate()
	{
		revalidate = true;
	}

	public enum Alignment
	{
		TOP_LEFT,
		TOP_CENTER,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_CENTER,
		BOTTOM_RIGHT
	}

	public enum Behaviour
	{
		DEFAULT,
		FORCE_LEFT,
		FORCE_CENTER,
		FORCE_RIGHT
	}

	public enum Subject
	{
		NONE,
		HEADER,
		OPTIONS,
		NUMBERED
	}
}
