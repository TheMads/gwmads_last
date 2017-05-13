package ru.l2gw.gameserver.clientpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.ExCursedWeaponList;

import java.util.List;

/**
 * Format: (ch)
 */
public class RequestCursedWeaponList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Character cha = getClient().getPlayer();
		if(cha == null)
			return;

		//send a ExCursedWeaponList :p
		List<Integer> list = new FastList<Integer>();
		for(int id : CursedWeaponsManager.getInstance().getCursedWeaponsIds())
			list.add(id);

		cha.sendPacket(new ExCursedWeaponList(list));
	}
}
