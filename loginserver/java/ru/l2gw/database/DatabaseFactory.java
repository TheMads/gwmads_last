package ru.l2gw.database;

import ru.l2gw.commons.dbcp.BasicDataSource;
import ru.l2gw.loginserver.Config;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author: rage
 * @date: 03.03.12 15:08
 */
public class DatabaseFactory extends BasicDataSource
{
	private static final DatabaseFactory instance = new DatabaseFactory();

	public static DatabaseFactory getInstance()
	{
		return instance;
	}

	public DatabaseFactory()
	{
		super(Config.DATABASE_URL, Config.DATABASE_LOGIN, Config.DATABASE_PASSWORD, Config.DATABASE_MAX_CONNECTIONS, Config.DATABASE_MAX_CONNECTIONS, Config.DATABASE_MAX_IDLE_TIMEOUT, Config.DATABASE_IDLE_TEST_PERIOD);
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		return getConnection(null);
	}
}