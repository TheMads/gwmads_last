package ru.l2gw.gameserver.clientpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.model.CursedWeapon;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.ExCursedWeaponLocation;
import ru.l2gw.gameserver.serverpackets.ExCursedWeaponLocation.CursedWeaponInfo;
import ru.l2gw.util.Location;

import java.util.List;

/**
 * Format: (ch)
 * @author  -Wooden-
 */
public class RequestCursedWeaponLocation extends L2GameClientPacket
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

		List<CursedWeaponInfo> list = new FastList<CursedWeaponInfo>();
		for(CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
		{
			Location pos = cw.getWorldPosition();
			if(pos != null)
				list.add(new CursedWeaponInfo(pos, cw.getItemId(), cw.isActivated() ? 1 : 0));
		}

		//send the ExCursedWeaponLocation
		cha.sendPacket(new ExCursedWeaponLocation(list));
	}
}