package ru.l2gw.gameserver.model.entity.siege.castle;

import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CastleSiegeDatabase extends SiegeDatabase
{
	public CastleSiegeDatabase(Siege siege)
	{
		super(siege);
	}

	@Override
	public void saveSiegeDate()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE residence SET siegeDate = ?, changeTimeEnd = ?, changeTimeOver = ? WHERE id = ?");
			statement.setLong(1, _siege.getSiegeDate().getTimeInMillis() / 1000);
			statement.setLong(2, _siege.getChangeTimeEnd().getTimeInMillis() / 1000);
			statement.setString(3, String.valueOf(_siege.isChangeTimeOver()));
			statement.setInt(4, _siege.getSiegeUnit().getId());
			statement.execute();
		}
		catch(Exception e)
		{
			System.out.println("Exception: saveSiegeDate(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}