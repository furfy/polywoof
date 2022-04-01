package com.polywoof;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.h2.engine.Constants;
import org.h2.jdbcx.JdbcDataSource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofStorage implements AutoCloseable
{
	private final Executor executor = Executors.newSingleThreadExecutor();
	private final JdbcDataSource data = new JdbcDataSource();
	private Connection db;

	public PolywoofStorage(File file)
	{
		String path = file.getPath();

		if(path.endsWith(Constants.SUFFIX_MV_FILE))
			path = path.substring(0, path.length() - Constants.SUFFIX_MV_FILE.length());

		this.data.setURL(Constants.START_URL + path);
	}

	public void open()
	{
		if(status())
			return;

		executor.execute(() ->
		{
			try
			{
				Connection connection = data.getConnection();

				for(DataType type : DataType.values())
				{
					try(PreparedStatement create = connection.prepareStatement(String.format("CREATE TABLE IF NOT EXISTS `%1$s` (OSRS VARCHAR(%2$s) PRIMARY KEY)", type, type.size)))
					{
						create.executeUpdate();
					}
					catch(SQLException error)
					{
						log.error("Failed to prepare the database", error);
						connection.close();
						return;
					}
				}

				db = connection;
			}
			catch(SQLException error)
			{
				log.error("Failed to open the database", error);
			}
		});
	}

	public void close()
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			try
			{
				db.close();
			}
			catch(SQLException error)
			{
				log.error("Failed to close the database", error);
			}
		});
	}

	public void select(String key, Language column, DataType table, @Nullable Selectable callback)
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			try(PreparedStatement schema = db.prepareStatement("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=? AND COLUMN_NAME=?"))
			{
				schema.setString(1, table.toString());
				schema.setString(2, column.toString());

				if(!schema.executeQuery().next())
				{
					if(callback != null)
						callback.select(null);
					return;
				}

				try(PreparedStatement select = db.prepareStatement(String.format("SELECT `%2$s` FROM `%1$s` WHERE OSRS=? AND `%2$s` IS NOT NULL", table, column)))
				{
					select.setString(1, key);

					ResultSet result = select.executeQuery();
					String string = null;

					if(result.next())
						string = result.getString(column.toString());

					if(callback != null)
						callback.select(string);
				}
			}
			catch(SQLException error)
			{
				log.error("Failed to select from the database", error);
			}
		});
	}

	public void insert(String string, String key, Language column, DataType table, @Nullable Insertable callback)
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			try(PreparedStatement update = db.prepareStatement(String.format("ALTER TABLE `%1$s` ADD IF NOT EXISTS `%3$s` VARCHAR(%2$s)", table, table.size, column)))
			{
				update.executeUpdate();

				try(PreparedStatement insert = db.prepareStatement(String.format("MERGE INTO `%1$s` (OSRS, `%2$s`) VALUES(?, ?)", table, column)))
				{
					insert.setString(1, key);
					insert.setString(2, string);
					insert.executeUpdate();

					if(callback != null)
						callback.insert();
				}
			}
			catch(SQLException error)
			{
				log.error("Failed to insert into the database", error);
			}
		});
	}

	public boolean status()
	{
		try
		{
			return db != null && !db.isClosed();
		}
		catch(SQLException error)
		{
			log.error("Failed to check the database", error);
		}

		return false;
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public enum DataType
	{
		CHAT_MESSAGES(256),
		ANY_EXAMINE(256),
		OVERHEAD_TEXT(256),
		DIALOGUE_TEXT(512),
		DIALOGUE_OPTIONS(512),
		VARIOUS_SCROLLS(1024),
		VARIOUS_BOOKS(2048),
		QUEST_DIARY(2048);

		public final int size;

		@Override
		public String toString()
		{
			return name();
		}
	}

	interface Selectable
	{
		void select(@Nullable String string);
	}

	interface Insertable
	{
		void insert();
	}

	@AllArgsConstructor(access = AccessLevel.PROTECTED)
	public static class Language
	{
		public final String code;
		public final String name;

		@Override
		public String toString()
		{
			return code;
		}
	}
}
