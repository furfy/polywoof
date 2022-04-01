package com.polywoof;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofOverlay extends Overlay
{
	private static final BufferedImage buttonImage = ImageUtil.loadImageResource(PolywoofPlugin.class, "/button.png");
	private static final Tooltip[] tooltips =
	{
		new Tooltip("Polywoof is " + ColorUtil.wrapWithColorTag("Off", Color.RED)),
		new Tooltip("Polywoof is " + ColorUtil.wrapWithColorTag("On", Color.GREEN))
	};

	private final Rectangle rectangle = new Rectangle(0, 0, 30, 30);
	private final Map<String, Subtitle> permanent = new HashMap<>(1);
	private final List<Subtitle> temporary = new ArrayList<>(9);
	private Font font;
	private AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private TooltipManager tooltipManager;
	@Getter private boolean mouseOver;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if(config.showButton() && !mouseOver)
		{
			graphics.setComposite(composite = composite.derive(0.1f));
			graphics.drawImage(buttonImage, 0, 0, rectangle.width, rectangle.height, null);
		}

		List<Subtitle> copy = new ArrayList<>(permanent.size() + temporary.size());
		copy.addAll(permanent.values());
		copy.addAll(temporary);

		if(!copy.isEmpty())
			PolywoofComponent.setPosition(getPreferredPosition() == null ? getPosition() : getPreferredPosition());

		int offset = 0;

		for(Subtitle subtitle : copy)
		{
			if(subtitle.timestamp != 0)
			{
				long difference = Math.max(0, subtitle.timestamp - System.currentTimeMillis());

				if(difference == 0)
				{
					temporary.remove(subtitle);
					continue;
				}

				subtitle.component.setAlpha(Math.min(1000f, difference) / 1000f);
			}

			switch(PolywoofComponent.getAlignment())
			{
				case TOP_LEFT:
					subtitle.component.setLocation(rectangle.x, rectangle.y + offset);
					break;
				case TOP_CENTER:
					subtitle.component.setLocation(rectangle.width / 2, rectangle.y + offset);
					break;
				case TOP_RIGHT:
					subtitle.component.setLocation(rectangle.width - rectangle.x, rectangle.y + offset);
					break;
				case BOTTOM_LEFT:
					subtitle.component.setLocation(rectangle.x, rectangle.height - rectangle.y - offset);
					break;
				case BOTTOM_CENTER:
					subtitle.component.setLocation(rectangle.width / 2, rectangle.height - rectangle.y - offset);
					break;
				case BOTTOM_RIGHT:
					subtitle.component.setLocation(rectangle.width - rectangle.x, rectangle.height - rectangle.y - offset);
					break;
			}

			offset += subtitle.component.render(graphics).height + 4;
		}

		if(mouseOver)
		{
			if(config.showButton())
			{
				graphics.setComposite(composite = composite.derive(1f));
				graphics.drawImage(buttonImage, 0, 0, rectangle.width, rectangle.height, null);

				if(!client.isMenuOpen())
					tooltipManager.add(tooltips[config.toggle() ? 1 : 0]);
			}

			mouseOver = false;
		}

		return rectangle.getSize();
	}

	@Override
	public void onMouseOver()
	{
		mouseOver = true;
	}

	@Override
	public void revalidate()
	{
		font = new Font(config.fontName(), Font.PLAIN, config.fontSize());

		PolywoofComponent.setTextWrap(config.textWrap());
		PolywoofComponent.setTextShadow(config.textShadow());
		PolywoofComponent.setBoxOutline(config.overlayOutline());
		PolywoofComponent.setBackgroundColor(config.overlayColor());
		PolywoofComponent.setBehaviour(config.textAlignment());

		for(Subtitle subtitle : permanent.values())
		{
			subtitle.component.setFontSize(config.fontSize());
			subtitle.component.setHeaderSubject(config.sourceName());
			subtitle.component.setNumberedSubject(config.numberedOptions());
			subtitle.component.revalidate();
		}

		for(Subtitle subtitle : temporary)
		{
			subtitle.component.setFontSize(config.fontSize());
			subtitle.component.setHeaderSubject(config.sourceName());
			subtitle.component.setNumberedSubject(config.numberedOptions());
			subtitle.component.revalidate();
		}
	}

	public void put(@Nullable String header, String string, PolywoofComponent.Subject subject)
	{
		if(string.isEmpty())
			return;

		if(temporary.size() >= 9)
			temporary.remove(temporary.size() - 1);

		temporary.add(0, Subtitle.temporary(new PolywoofComponent(header, string, font, subject), System.currentTimeMillis() + 1500L + (long) (string.length() * 1000f * (1f / config.readingSpeed()))));
	}

	public void set(@Nullable String key, @Nullable String header, String string, PolywoofComponent.Subject subject)
	{
		if(string.isEmpty())
			return;

		if(permanent.containsKey(key))
			pop(key);

		permanent.put(key, Subtitle.permanent(new PolywoofComponent(header, string, font, subject)));
	}

	public void pop(@Nullable String key)
	{
		if(!permanent.containsKey(key))
			return;

		if(temporary.size() >= 9)
			temporary.remove(temporary.size() - 1);

		temporary.add(0, Subtitle.temporary(permanent.get(key).component, System.currentTimeMillis() + 1500L));
		permanent.remove(key);
	}

	public void reset()
	{
		permanent.clear();
		temporary.clear();
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Subtitle
	{
		public final PolywoofComponent component;
		public final long timestamp;

		public static Subtitle permanent(PolywoofComponent component)
		{
			return new Subtitle(component, 0);
		}

		public static Subtitle temporary(PolywoofComponent component, long timestamp)
		{
			return new Subtitle(component, timestamp);
		}
	}
}
