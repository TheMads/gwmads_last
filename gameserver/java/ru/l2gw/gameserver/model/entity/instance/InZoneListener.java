package ru.l2gw.gameserver.model.entity.instance;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author: rage
 * @date: 10.09.12 22:44
 */
public interface InZoneListener
{
	public void onStartInstance(GArray<L2Player> party);
	public void onSuccessEnd(GArray<L2Player> party);
	public void onStopInstance(GArray<L2Player> party);
	public void onPlayerEnter(L2Player player);
	public void onPlayerExit(L2Player player);
}
