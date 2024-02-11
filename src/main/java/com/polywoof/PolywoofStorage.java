package com.polywoof;

import com.polywoof.api.API;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import org.h2.engine.Constants;
import org.h2.jdbcx.JdbcDataSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j @ParametersAreNonnullByDefault public class PolywoofStorage implements AutoCloseable
{
	private static final int revision = 2;
	private final JdbcDataSource data = new JdbcDataSource();
	private Connection db;

	public PolywoofStorage(String name)
	{
		this.data.setURL(Constants.START_URL + new File(RuneLite.CACHE_DIR, name).getPath());
	}

	public void open()
	{
		if(status())
		{
			return;
		}

		PolywoofPlugin.executor.execute(() ->
		{
			try
			{
				Connection db = data.getConnection();

				try
				{
					try(PreparedStatement statement = db.prepareStatement("create table if not exists" +
							"\nINFORMATION_SCHEMA.PROPERTIES (PROPERTY_NAME varchar(256) primary key, PROPERTY_VALUE varchar(256) not null)"))
					{
						statement.executeUpdate();
					}

					try(PreparedStatement preparedStatement = db.prepareStatement("select * from" +
							"\nINFORMATION_SCHEMA.PROPERTIES where PROPERTY_NAME = ? and PROPERTY_VALUE >= ?"))
					{
						preparedStatement.setString(1, "dictionary.revision");
						preparedStatement.setString(2, String.valueOf(revision));

						if(!preparedStatement.executeQuery().next())
						{
							try(PreparedStatement statement = db.prepareStatement("drop schema if exists" +
									"\nDICTIONARY cascade"))
							{
								statement.executeUpdate();
							}

							try(PreparedStatement statement = db.prepareStatement("merge into" +
									"\nINFORMATION_SCHEMA.PROPERTIES values(?, ?)"))
							{
								statement.setString(1, "dictionary.revision");
								statement.setString(2, String.valueOf(revision));
								statement.executeUpdate();
							}
						}
					}

					try(PreparedStatement statement = db.prepareStatement("create schema if not exists" +
							"\nDICTIONARY"))
					{
						statement.executeUpdate();
					}

					for(API.GameText.Type type : API.GameText.Type.values())
					{
						try(PreparedStatement statement = db.prepareStatement(String.format("create table if not exists" +
								"\nDICTIONARY.%1$s (GAME varchar(%2$s) primary key)", type, type.size)))
						{
							statement.executeUpdate();
						}
					}

					this.db = db;
					log.info("Storage is opened successfully");
				}
				catch(SQLException error)
				{
					db.close();
					throw error;
				}
			}
			catch(SQLException error)
			{
				log.error("Failed to open storage", error);
			}
		});
	}

	public void close()
	{
		if(!status())
		{
			return;
		}

		PolywoofPlugin.executor.execute(() ->
		{
			try
			{
				db.close();
				log.info("Storage is closed successfully");
			}
			catch(SQLException error)
			{
				log.error("Failed to close storage", error);
			}
		});
	}

	public void select(List<API.GameText> textList, API.Language language, Queryable queryable)
	{
		if(!status())
		{
			return;
		}

		PolywoofPlugin.executor.execute(() ->
		{
			for(API.GameText gameText : textList)
			{
				if(gameText.cache)
				{
					continue;
				}

				try(PreparedStatement preparedStatement = db.prepareStatement("select * from" +
						"\nINFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = ? and TABLE_NAME = ? and COLUMN_NAME = ?"))
				{
					preparedStatement.setString(1, "DICTIONARY");
					preparedStatement.setString(2, gameText.type.toString());
					preparedStatement.setString(3, language.code);

					if(preparedStatement.executeQuery().next())
					{
						try(PreparedStatement statement = db.prepareStatement(String.format("select `%2$s` from" +
								"\nDICTIONARY.%1$s where GAME = ? and `%2$s` is not null", gameText.type, language.code)))
						{
							log.debug("Selecting {} from storage", language.name);

							statement.setString(1, gameText.game);
							ResultSet resultSet = statement.executeQuery();

							if(resultSet.next())
							{
								gameText.text = resultSet.getString(language.code);
								gameText.cache = true;
							}
						}
					}
				}
				catch(SQLException error)
				{
					log.error("Failed to select from storage", error);
				}
			}

			queryable.query();
		});
	}

	public void insert(List<API.GameText> textList, API.Language language, Queryable queryable)
	{
		if(!status())
		{
			return;
		}

		PolywoofPlugin.executor.execute(() ->
		{
			for(API.GameText gameText : textList)
			{
				if(!gameText.cache)
				{
					try
					{
						try(PreparedStatement statement = db.prepareStatement(String.format("alter table" +
								"\nDICTIONARY.%1$s add if not exists `%3$s` varchar(%2$s)", gameText.type, gameText.type.size, language.code)))
						{
							statement.executeUpdate();
						}

						try(PreparedStatement statement = db.prepareStatement(String.format("merge into" +
								"\nDICTIONARY.%1$s (GAME, `%2$s`) values(?, ?)", gameText.type, language.code)))
						{
							log.debug("Inserting {} into storage", language.name);

							statement.setString(1, gameText.game);
							statement.setString(2, gameText.text);
							statement.executeUpdate();
						}
					}
					catch(SQLException error)
					{
						log.error("Failed to insert into storage", error);
					}
				}
			}

			queryable.query();
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
			log.error("Failed to check storage", error);
		}

		return false;
	}

	public interface Queryable
	{
		void query();
	}
}
