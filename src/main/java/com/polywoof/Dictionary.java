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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j @ParametersAreNonnullByDefault public final class Dictionary implements AutoCloseable
{
	private static final Executor executor = Executors.newSingleThreadExecutor();
	private static final int revision = 2;
	private final JdbcDataSource data = new JdbcDataSource();
	private Connection db;

	public Dictionary(String name)
	{
		this.data.setURL(Constants.START_URL + new File(RuneLite.CACHE_DIR, name).getPath());
	}

	public void open()
	{
		if(!status())
		{
			executor.execute(() ->
			{
				try
				{
					log.info("Trying to open the database");
					Connection db = data.getConnection();

					try
					{
						log.debug("Trying to create the properties");
						try(PreparedStatement statement = db.prepareStatement("create table if not exists" +
								"\nINFORMATION_SCHEMA.PROPERTIES (PROPERTY_NAME varchar(256) primary key, PROPERTY_VALUE varchar(256) not null)"))
						{
							statement.executeUpdate();
						}

						log.debug("Trying to select from the properties");
						try(PreparedStatement preparedStatement = db.prepareStatement("select * from" +
								"\nINFORMATION_SCHEMA.PROPERTIES where PROPERTY_NAME = ? and PROPERTY_VALUE >= ?"))
						{
							preparedStatement.setString(1, "dictionary.revision");
							preparedStatement.setString(2, String.valueOf(revision));

							if(!preparedStatement.executeQuery().next())
							{
								log.info("Trying to drop the schema");
								try(PreparedStatement statement = db.prepareStatement("drop schema if exists" +
										"\nDICTIONARY cascade"))
								{
									statement.executeUpdate();
								}

								log.debug("Trying to insert into the properties");
								try(PreparedStatement statement = db.prepareStatement("merge into" +
										"\nINFORMATION_SCHEMA.PROPERTIES values(?, ?)"))
								{
									statement.setString(1, "dictionary.revision");
									statement.setString(2, String.valueOf(revision));
									statement.executeUpdate();
								}
							}
						}

						log.debug("Trying to create the schema");
						try(PreparedStatement statement = db.prepareStatement("create schema if not exists" +
								"\nDICTIONARY"))
						{
							statement.executeUpdate();
						}

						for(API.GameText.Type type : API.GameText.Type.values())
						{
							log.debug("Trying to create {} in the database", type);
							try(PreparedStatement statement = db.prepareStatement(String.format("create table if not exists" +
									"\nDICTIONARY.%1$s (GAME varchar(%2$s) primary key)", type, type.size)))
							{
								statement.executeUpdate();
							}
						}

						this.db = db;
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
	}

	public void close()
	{
		if(status())
		{
			executor.execute(() ->
			{
				try
				{
					log.info("Trying to close the database");
					db.close();
				}
				catch(SQLException error)
				{
					log.error("Failed to close the database", error);
				}
			});
		}
	}

	public boolean status()
	{
		try
		{
			return db != null && !db.isClosed();
		}
		catch(SQLException error)
		{
			log.error("Failed to get the status of the database", error);
		}

		return false;
	}

	public void select(List<API.GameText> textList, API.Language language, Queryable queryable)
	{
		if(status() && !textList.isEmpty())
		{
			executor.execute(() ->
			{
				for(API.GameText gameText : textList)
				{
					if(!gameText.cache)
					{
						log.debug("Trying to select {} from the database", gameText.type);
						try(PreparedStatement preparedStatement = db.prepareStatement("select * from" +
								"\nINFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = ? and TABLE_NAME = ? and COLUMN_NAME = ?"))
						{
							preparedStatement.setString(1, "DICTIONARY");
							preparedStatement.setString(2, gameText.type.toString());
							preparedStatement.setString(3, language.code);

							if(preparedStatement.executeQuery().next())
							{
								log.debug("Trying to select {} from the database", language.name);
								try(PreparedStatement statement = db.prepareStatement(String.format("select `%2$s` from" +
										"\nDICTIONARY.%1$s where GAME = ? and `%2$s` is not null", gameText.type, language.code)))
								{
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
							log.error("Failed to select from the database", error);
						}
					}
				}

				queryable.query();
			});
		}
	}

	public void insert(List<API.GameText> textList, API.Language language, Queryable queryable)
	{
		if(status() && !textList.isEmpty())
		{
			executor.execute(() ->
			{
				for(API.GameText gameText : textList)
				{
					if(!gameText.cache)
					{
						try
						{
							log.debug("Trying to create {} in the database", gameText.type);
							try(PreparedStatement statement = db.prepareStatement(String.format("alter table" +
									"\nDICTIONARY.%1$s add if not exists `%3$s` varchar(%2$s)", gameText.type, gameText.type.size, language.code)))
							{
								statement.executeUpdate();
							}

							log.debug("Trying to insert {} into the database", language.name);
							try(PreparedStatement statement = db.prepareStatement(String.format("merge into" +
									"\nDICTIONARY.%1$s (GAME, `%2$s`) values(?, ?)", gameText.type, language.code)))
							{
								statement.setString(1, gameText.game);
								statement.setString(2, gameText.text);
								statement.executeUpdate();
							}
						}
						catch(SQLException error)
						{
							log.error("Failed to insert into the database", error);
						}
					}
				}

				queryable.query();
			});
		}
	}

	public interface Queryable
	{
		void query();
	}
}
