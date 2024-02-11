package com.polywoof;

import com.polywoof.api.API;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j @ParametersAreNonnullByDefault public class PolywoofOverlay extends Overlay
{
	private static final Tooltip[] tooltips = {
			new Tooltip(String.format("Translation is %s", ColorUtil.wrapWithColorTag("Off", Color.RED))),
			new Tooltip(String.format("Translation is %s", ColorUtil.wrapWithColorTag("On", Color.GREEN)))};

	private final Rectangle rectangle = new Rectangle(0, 0, 30, 30);
	private final Map<String, Subtitle> permanent = new HashMap<>();
	private final List<Subtitle> temporary = new ArrayList<>();
	private Font font;
	private AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private TooltipManager tooltipManager;
	@Getter private boolean mouseOver;

	@Override public Dimension render(Graphics2D graphics)
	{
		if(config.showButton() && !mouseOver)
		{
			graphics.setComposite(composite = composite.derive(0.1f));
			graphics.drawImage(PolywoofPlugin.resourceIcon, 0, 0, rectangle.width, rectangle.height, null);
		}

		List<Subtitle> copy = new ArrayList<>(permanent.size() + temporary.size());
		copy.addAll(permanent.values());
		copy.addAll(temporary);

		if(!copy.isEmpty())
		{
			PolywoofComponent.position = getPreferredPosition() == null ? getPosition() : getPreferredPosition();
		}

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

				subtitle.component.setOpacity(Math.min(1000f, difference) / 1000f);
			}

			switch(PolywoofComponent.position)
			{
				case TOP_LEFT:
					subtitle.component.setLocation(rectangle.x, rectangle.y + offset);
					break;
				case TOP_CENTER:
					subtitle.component.setLocation(rectangle.width / 2, rectangle.y + offset);
					break;
				case TOP_RIGHT:
				case CANVAS_TOP_RIGHT:
					subtitle.component.setLocation(rectangle.width - rectangle.x, rectangle.y + offset);
					break;
				case BOTTOM_LEFT:
					subtitle.component.setLocation(rectangle.x, rectangle.height - rectangle.y - offset);
					break;
				case ABOVE_CHATBOX_RIGHT:
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
			mouseOver = false;

			if(config.showButton())
			{
				graphics.setComposite(composite = composite.derive(1f));
				graphics.drawImage(PolywoofPlugin.resourceIcon, 0, 0, rectangle.width, rectangle.height, null);

				if(!client.isMenuOpen())
				{
					tooltipManager.add(tooltips[config.toggle() ? 1 : 0]);
				}
			}
		}

		return rectangle.getSize();
	}

	@Override public void onMouseOver()
	{
		mouseOver = true;
	}

	@Override public void revalidate()
	{
		if(PolywoofPlugin.resourceFont == null || !config.fontName().equals("CozetteVector"))
		{
			font = new Font(config.fontName(), Font.PLAIN, config.fontSize());
		}
		else
		{
			font = PolywoofPlugin.resourceFont.deriveFont(Font.PLAIN, config.fontSize());
		}

		PolywoofComponent.textWrap = config.textWrap();
		PolywoofComponent.textShadow = config.textShadow();
		PolywoofComponent.boxOutline = config.overlayOutline();
		PolywoofComponent.backgroundColor = config.overlayBackgroundColor();
		PolywoofComponent.behaviour = config.textAlignment();

		for(Subtitle subtitle : permanent.values())
		{
			subtitle.component.setFontSize(config.fontSize());
		}

		for(Subtitle subtitle : temporary)
		{
			subtitle.component.setFontSize(config.fontSize());
		}
	}

	public void put(List<API.GameText> textList)
	{
		if(temporary.size() >= 9)
		{
			temporary.remove(temporary.size() - 1);
		}

		int length = 0;

		for(API.GameText gameText : textList)
		{
			length += gameText.text.length();
		}

		temporary.add(0, new Subtitle(
				new PolywoofComponent(textList, font),
				System.currentTimeMillis() + 1500L + (long)(length * 1000f * (1f / config.readingSpeed()))));
	}

	public void set(String key, List<API.GameText> textList)
	{
		if(permanent.containsKey(key))
		{
			pop(key);
		}

		permanent.put(key, new Subtitle(new PolywoofComponent(textList, font), 0));
	}

	public void pop(String key)
	{
		if(!permanent.containsKey(key))
		{
			return;
		}

		if(temporary.size() >= 9)
		{
			temporary.remove(temporary.size() - 1);
		}

		temporary.add(0, new Subtitle(permanent.get(key).component, System.currentTimeMillis() + 1500L));
		permanent.remove(key);
	}

	public void clear()
	{
		permanent.clear();
		temporary.clear();
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE) private static class Subtitle
	{
		public final PolywoofComponent component;
		public final long timestamp;
	}
}
