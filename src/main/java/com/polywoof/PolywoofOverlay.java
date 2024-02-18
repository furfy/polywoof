package com.polywoof;

import com.polywoof.api.API;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.*;

@Slf4j @ParametersAreNonnullByDefault public class PolywoofOverlay extends Overlay
{
	public static final Dimension iconSize = new Dimension(30, 30);
	public static BufferedImage resourceIcon;

	private final List<TextBoxData> textBoxList = new ArrayList<>();
	private final Map<String, TextBoxData> textBoxMap = new HashMap<>();
	private final Tooltip tooltip = new Tooltip("");
	private AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	@Getter private boolean mouseOver;

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private TooltipManager tooltipManager;

	static
	{
		try(InputStream stream = PolywoofPlugin.class.getResourceAsStream("/icon.png"))
		{
			if(stream == null)
			{
				throw new IllegalArgumentException();
			}

			resourceIcon = ImageIO.read(stream);
		}
		catch(Exception error)
		{
			log.error("Failed to load the icon image", error);
		}
	}

	@Override public Dimension render(Graphics2D graphics)
	{
		if(config.showButton() && !mouseOver)
		{
			graphics.setComposite(composite = composite.derive(0.2f));
			graphics.drawImage(resourceIcon, 0, 0, iconSize.width, iconSize.height, null);
		}

		List<TextBoxData> dataList = new ArrayList<>(textBoxMap.values());
		dataList.addAll(textBoxList);

		if(!dataList.isEmpty())
		{
			int offset = 0, size = dataList.stream().mapToInt(textBoxData -> textBoxData.component.update(graphics).height + 4).sum() - 4;
			OverlayPosition position = Objects.requireNonNullElse(getPreferredPosition(), getPosition());

			for(TextBoxData textBoxData : dataList)
			{
				if(textBoxList.contains(textBoxData))
				{
					long delta = textBoxData.timestamp - System.currentTimeMillis();

					if(delta > 0)
					{
						textBoxData.component.setOpacity(Math.min(1000f, delta) / 1000f);
					}
					else
					{
						textBoxList.remove(textBoxData);
					}
				}

				if(config.overlayAlignment() == TextBox.Alignment.DEFAULT)
				{
					if(getPreferredLocation() == null)
					{
						switch(position)
						{
							case TOP_LEFT:
								TextBox.alignment = TextBox.Alignment.TOP_LEFT;
								break;
							case TOP_CENTER:
								TextBox.alignment = TextBox.Alignment.TOP;
								break;
							case TOP_RIGHT:
							case CANVAS_TOP_RIGHT:
								TextBox.alignment = TextBox.Alignment.TOP_RIGHT;
								break;
							default:
								TextBox.alignment = TextBox.Alignment.CENTER;
								break;
							case BOTTOM_LEFT:
								TextBox.alignment = TextBox.Alignment.BOTTOM_LEFT;
								break;
							case ABOVE_CHATBOX_RIGHT:
								TextBox.alignment = TextBox.Alignment.BOTTOM;
								break;
							case BOTTOM_RIGHT:
								TextBox.alignment = TextBox.Alignment.BOTTOM_RIGHT;
								break;
						}
					}
					else
					{
						TextBox.alignment = TextBox.Alignment.DEFAULT;
					}
				}
				else
				{
					TextBox.alignment = config.overlayAlignment();
				}

				switch(TextBox.alignment)
				{
					case TOP_LEFT:
					case TOP:
					case TOP_RIGHT:
						textBoxData.component.setLocation(0, offset);
						break;
					case DEFAULT:
					case LEFT:
					case CENTER:
					case RIGHT:
						textBoxData.component.setLocation(0, offset - size / 2);
						break;
					case BOTTOM_LEFT:
					case BOTTOM:
					case BOTTOM_RIGHT:
						textBoxData.component.setLocation(0, -offset);
						break;
				}

				offset += textBoxData.component.render(graphics).height + 4;
			}
		}

		if(config.showButton() && mouseOver)
		{
			mouseOver = false;
			graphics.setComposite(composite = composite.derive(1f));
			graphics.drawImage(resourceIcon, 0, 0, iconSize.width, iconSize.height, null);
		}

		return iconSize;
	}

	@Override public void onMouseOver()
	{
		if(config.showButton() && !client.isMenuOpen())
		{
			API.Status status;

			if(config.toggle())
			{
				if(config.backend() == PolywoofPlugin.TranslationBackend.GENERIC)
				{
					status = API.Status.GENERIC;
				}
				else
				{
					status = config.key().isBlank() ? API.Status.OFFLINE : API.Status.ON;
				}
			}
			else
			{
				status = API.Status.OFF;
			}

			tooltip.setText("Polywoof is " + API.statusMessage(status));
			tooltipManager.add(tooltip);
			mouseOver = true;
		}
	}

	@Override public void revalidate()
	{
		TextBox.behaviour = config.textAlignment();
		TextBox.backgroundColor = config.overlayBackgroundColor();
		TextBox.boxOutline = config.overlayOutline();
		TextBox.textShadow = config.textShadow();
		TextBox.textWrap = config.textWrap();

		for(TextBoxData textBoxData : textBoxMap.values())
		{
			textBoxData.component.setFont(config.fontName(), config.fontSize());
		}

		for(TextBoxData textBoxData : textBoxList)
		{
			textBoxData.component.setFont(config.fontName(), config.fontSize());
		}
	}

	public final void set(String key, List<API.GameText> textList)
	{
		if(textBoxMap.containsKey(key))
		{
			pop(key);
		}

		textBoxMap.put(key, new TextBoxData(new TextBox(textList, config.fontName(), config.fontSize()), 0));
	}

	public final void put(List<API.GameText> textList)
	{
		if(textBoxList.size() >= 9)
		{
			textBoxList.remove(textBoxList.size() - 1);
		}

		textBoxList.add(0, new TextBoxData(
				new TextBox(textList, config.fontName(), config.fontSize()),
				1500L + (long)(textList.stream().mapToInt(gameText -> gameText.text.length()).sum() * 1000f * (1f / config.readingSpeed()))));
	}

	public final void pop(String key)
	{
		if(textBoxMap.containsKey(key))
		{
			if(textBoxList.size() >= 9)
			{
				textBoxList.remove(textBoxList.size() - 1);
			}

			textBoxList.add(0, new TextBoxData(textBoxMap.get(key).component, 1500L));
			textBoxMap.remove(key);
		}
	}

	public final void clear()
	{
		textBoxList.clear();
		textBoxMap.clear();
	}

	private static class TextBoxData
	{
		private final TextBox component;
		private final long timestamp;

		private TextBoxData(TextBox component, long timestamp)
		{
			this.component = component;
			this.timestamp = System.currentTimeMillis() + timestamp;
		}
	}
}
