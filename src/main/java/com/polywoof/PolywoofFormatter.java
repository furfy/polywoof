package com.polywoof;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofFormatter
{
	public static final BlockEntry[] messageEntries =
	{
		new BlockEntry("^[0-9,]+ x (Coins\\.)$", "%s"),
		new BlockEntry("^You gain [0-9,]+ .+ XP\\.$", null),
		new BlockEntry("^Your .+ lap count is:", null),
		new BlockEntry("^You can inflict [0-9,]+ more points of damage before a ring will shatter\\.$", null),
		new BlockEntry("^You can smelt [0-9,]+ more pieces of iron ore before a ring melts\\.$", null),
		new BlockEntry("^Your reward is:", null),
		new BlockEntry("^You have opened the Brimstone chest", null),
		new BlockEntry("^Congratulations, you've just advanced your .+ level\\.", null),
		new BlockEntry("^Congratulations, you've reached a total level of [0-9,]+\\.$", null),
		new BlockEntry("^Congratulations, you've completed .+ combat task:", null),
		new BlockEntry("^<col.+?>Well done! You have completed .+ task in the .+ area\\.", null),
		new BlockEntry("^<col.+?>You're assigned to kill </col>.+<col.+?>;", null),
		new BlockEntry("^<col.+?>You have completed your task! You killed</col> [0-9,]+ .+<col.+?>\\.", null),
		new BlockEntry("^<col.+?>You've completed </col>[0-9,]+ tasks <col.+?>", null),
		new BlockEntry("^You've been awarded [0-9,]+ bonus Runecraft XP for closing the rift\\.$", null),
		new BlockEntry("^Amount of rifts you have closed:", null),
		new BlockEntry("^Total elemental energy:", null),
		new BlockEntry("^Elemental energy attuned:", null),
		new BlockEntry("^.+: currently costs [0-9,]+ coins\\.$", null),
		new BlockEntry("^<col.+?>Valuable drop:", null),
		new BlockEntry("^<col.+?>.+ received a drop:", null)
	};

	public static final BlockEntry[] overheadEntries =
	{
		new BlockEntry("^[0-9,]+$", null)
	};

	public static final BlockEntry[] dialogueEntries =
	{
		new BlockEntry("^(Congratulations, you've just advanced your .+ level\\.)", "%s"),
		new BlockEntry("^(Your wish has been granted!<br>You have been awarded) [0-9,]+ (.+ experience!)$", "%s %s"),
		new BlockEntry("^(Your brain power serves you well!<br>You have been awarded) [0-9,]+ (.+ experience!)$", "%s %s"),
		new BlockEntry("^(Your new task is to kill) [0-9,]+ (.+\\.)$", "%s %s"),
		new BlockEntry("^(You're currently assigned to kill .+);", "%s."),
		new BlockEntry("^Select an Option\nExchange '.+': 5 coins\n", null),
		new BlockEntry("^Phials converts your banknotes?\\.$", null),
		new BlockEntry("^Status: [0-9,]+ damage points left\\.\nBreak the ring\\.", null),
		new BlockEntry("^The ring is fully charged\\.<br>There would be no point in breaking it\\.$", null),
		new BlockEntry("^The ring shatters\\. Your next ring of recoil will start<br>afresh from [0-9,]+ damage points\\.$", null)
	};

	private static final Pattern[] filterPatterns =
	{
		Pattern.compile("<br>"),
		Pattern.compile("<.*?>"),
		Pattern.compile("  +")
	};

	public static void parser(String string, BlockEntry[] entries, Parsable callback)
	{
		for(BlockEntry entry : entries)
		{
			Matcher matcher = entry.pattern.matcher(string);

			if(matcher.find())
			{
				if(entry.replacement == null)
					return;

				Object[] groups = new Object[matcher.groupCount()];

				for(int i = 0; i < matcher.groupCount(); i++)
					groups[i] = matcher.group(i + 1);

				string = String.format(entry.replacement, groups);
				break;
			}
		}

		callback.parse(string);
	}

	public static String filter(@Nullable String string)
	{
		if(string == null)
			return null;

		string = filterPatterns[0].matcher(string).replaceAll(" ");
		string = filterPatterns[1].matcher(string).replaceAll("");
		string = filterPatterns[2].matcher(string).replaceAll(" ");

		return string.trim();
	}

	public static String formatOptions(Widget ... options)
	{
		int inserter = 0;
		StringBuilder builder = new StringBuilder(128);

		for(Widget option : options)
		{
			if(option.getType() == WidgetType.TEXT && !option.getText().isEmpty())
			{
				switch(inserter)
				{
					case 0:
						inserter = 1;
						break;
					case 1:
						builder.append("\n");
						break;
				}

				builder.append(option.getText());
			}
		}

		return builder.toString();
	}

	public static String formatScrolls(Widget[] ... scrolls)
	{
		int inserter = -1;
		StringBuilder builder = new StringBuilder(128);

		for(Widget[] scroll : scrolls)
		{
			for(Widget line : scroll)
			{
				if(line.getType() == WidgetType.TEXT)
				{
					if(line.getText().isEmpty())
					{
						switch(inserter)
						{
							case 0:
								inserter = 1;
								break;
						}
					}
					else
					{
						switch(inserter)
						{
							case 0:
								builder.append(" ");
								break;
							case 1:
								builder.append("\n");
								break;
						}

						inserter = 0;
						builder.append(line.getText());
					}
				}
			}
		}

		return builder.toString();
	}

	interface Parsable
	{
		void parse(String replacement);
	}

	public static class BlockEntry
	{
		public final Pattern pattern;
		public final String replacement;

		public BlockEntry(String pattern, @Nullable String replacement)
		{
			this.pattern = Pattern.compile(pattern, Pattern.DOTALL);
			this.replacement = replacement;
		}
	}
}