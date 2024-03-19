package com.polywoof;

import com.polywoof.api.API;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

@Slf4j @ParametersAreNonnullByDefault public final class PolywoofOverlay extends Overlay
{
	public static final BufferedImage resourceIcon = ImageUtil.loadImageResource(PolywoofPlugin.class, "/icon.png");
	public static final Dimension iconSize = new Dimension(30, 30);
	public static final Tooltip[] tooltips = {
			new Tooltip("Polywoof is " + ColorUtil.wrapWithColorTag("On", Color.GREEN)),
			new Tooltip("Polywoof is " + ColorUtil.wrapWithColorTag("Off", Color.RED)),
			new Tooltip("Polywoof is " + ColorUtil.wrapWithColorTag("Offline", Color.ORANGE)),
			new Tooltip("Polywoof is set to " + ColorUtil.wrapWithColorTag("Generic", Color.GRAY))};

	private final AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	private final List<Component> order = new ArrayList<>();
	private final List<Component> temporary = new ArrayList<>();
	private final Map<String, Component> permanent = new HashMap<>();
	@Getter private boolean mouseOver;
	@Getter private boolean revalidate;

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private TooltipManager tooltipManager;

	@Override public Dimension render(Graphics2D graphics)
	{
		if(config.quickActions() && !mouseOver)
		{
			graphics.setComposite(composite.derive(0.2f));
			graphics.drawImage(resourceIcon, 0, 0, iconSize.width, iconSize.height, null);
		}

		if(!permanent.isEmpty() || !temporary.isEmpty())
		{
			OverlayPosition position = Objects.requireNonNullElse(getPreferredPosition(), getPosition());

			int offset = 0;
			int height = -4;

			order.clear();
			order.addAll(permanent.values());
			order.addAll(temporary);

			for(Component component : order)
			{
				if(revalidate)
				{
					component.text.update(config.fontName(), config.fontSize(), config.textWrap());
				}

				height += component.text.update(graphics).height + 4;
			}

			revalidate = false;

			for(Component component : order)
			{
				long opacity = Math.min(300L, System.currentTimeMillis() - component.timestamp);

				if(opacity < 300L)
				{
					component.text.setOpacity(opacity / 300f);
				}
				else
				{
					if(component.timeout == 0L)
					{
						component.text.setOpacity(1f);
					}
					else
					{
						long timeout = Math.min(1000L, component.timestamp + component.timeout - System.currentTimeMillis());

						if(timeout > 0L)
						{
							component.text.setOpacity(timeout / 1000f);
						}
						else
						{
							temporary.remove(component);
						}
					}
				}

				if(config.overlayAlignment() == TextBox.Behaviour.DEFAULT)
				{
					if(getPreferredLocation() == null)
					{
						switch(position)
						{
							case TOP_LEFT:
								TextBox.behaviour = TextBox.Behaviour.TOP_LEFT;
								break;
							case TOP_CENTER:
								TextBox.behaviour = TextBox.Behaviour.TOP;
								break;
							case TOP_RIGHT:
							case CANVAS_TOP_RIGHT:
								TextBox.behaviour = TextBox.Behaviour.TOP_RIGHT;
								break;
							default:
								TextBox.behaviour = TextBox.Behaviour.CENTER;
								break;
							case BOTTOM_LEFT:
								TextBox.behaviour = TextBox.Behaviour.BOTTOM_LEFT;
								break;
							case ABOVE_CHATBOX_RIGHT:
								TextBox.behaviour = TextBox.Behaviour.BOTTOM;
								break;
							case BOTTOM_RIGHT:
								TextBox.behaviour = TextBox.Behaviour.BOTTOM_RIGHT;
								break;
						}
					}
					else
					{
						TextBox.behaviour = TextBox.Behaviour.DEFAULT;
					}
				}
				else
				{
					TextBox.behaviour = config.overlayAlignment();
				}

				switch(TextBox.behaviour)
				{
					case TOP_LEFT:
					case TOP:
					case TOP_RIGHT:
						component.text.setLocation(0, offset);
						break;
					default:
					case DEFAULT:
					case LEFT:
					case CENTER:
					case RIGHT:
						component.text.setLocation(0, offset - height / 2);
						break;
					case BOTTOM_LEFT:
					case BOTTOM:
					case BOTTOM_RIGHT:
						component.text.setLocation(0, -offset);
						break;
				}

				offset += component.text.render(graphics).height + 4;
			}
		}

		if(config.quickActions() && mouseOver)
		{
			mouseOver = false;
			graphics.setComposite(composite.derive(1f));
			graphics.drawImage(resourceIcon, 0, 0, iconSize.width, iconSize.height, null);
		}

		return iconSize;
	}

	@Override public void onMouseOver()
	{
		if(!config.quickActions() || client.isMenuOpen())
		{
			return;
		}

		Tooltip tooltip;

		if(config.toggle())
		{
			if(config.backend() == PolywoofPlugin.TranslationBackend.GENERIC)
			{
				tooltip = tooltips[3];
			}
			else
			{
				tooltip = config.key().isBlank() ? tooltips[2] : tooltips[0];
			}
		}
		else
		{
			tooltip = tooltips[1];
		}

		tooltipManager.add(tooltip);
		mouseOver = true;
	}

	@Override public void revalidate()
	{
		TextBox.alignment = config.textAlignment();
		TextBox.backgroundColor = config.overlayBackgroundColor();
		TextBox.boxOutline = config.overlayOutline();
		TextBox.textShadow = config.textShadow();
		revalidate = true;
	}

	public void set(String key, List<API.GameText> textList)
	{
		if(permanent.containsKey(key))
		{
			temporary.add(0, permanent.get(key));
			permanent.get(key).timeout = System.currentTimeMillis() - permanent.get(key).timestamp + 1500L;
		}

		permanent.put(key, new Component(new TextBox(textList, config.fontName(), config.fontSize(), config.textWrap()), 0L));
	}

	public void put(List<API.GameText> textList, long timestamp)
	{
		temporary.add(0, new Component(new TextBox(textList, config.fontName(), config.fontSize(), config.textWrap()), timestamp));
	}

	public void pop(String key)
	{
		if(!permanent.containsKey(key))
		{
			return;
		}

		temporary.add(0, permanent.get(key));
		permanent.get(key).timeout = System.currentTimeMillis() - permanent.get(key).timestamp + 1500L;
		permanent.remove(key);
	}

	public void clear()
	{
		permanent.clear();
		temporary.clear();
	}

	public long timeout(List<API.GameText> textList)
	{
		long timeout = 0L;

		for(API.GameText gameText : textList)
		{
			timeout += (long)(Utils.Text.filter(Objects.requireNonNullElse(gameText.text, gameText.game), true).length() * 5f / config.readingSpeed());
		}

		return 1500L + timeout * 1000L;
	}

	@AllArgsConstructor(access = AccessLevel.PUBLIC) private static final class Component
	{
		public final TextBox text;
		public final long timestamp = System.currentTimeMillis();
		public long timeout;
	}
}
