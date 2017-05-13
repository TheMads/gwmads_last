package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;

public class RequestExEndScenePlayer extends L2GameClientPacket
{
	private int _movieId;

	/**
	 * format: d
	 */
	@Override
	public void readImpl()
	{
		_movieId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		if(player.getMovieId() != _movieId)
			return;

		player.setMovieId(0);
		L2World.sendObjectsToPlayer(player);
	}
}
