package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.serverpackets.NpcSay;
import ru.l2gw.gameserver.serverpackets.ExCallToChangeClass;
import ru.l2gw.gameserver.serverpackets.ExShowScreenMessage;

public class RequestCallToChangeClass extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
		//Trigger
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		
		if (player == null)
			return;
		if (player.getVarB("GermunkusUSM"))
			return;
		
		int _cId = 0;
		for (ClassId Cl : ClassId.VALUES)
		{
			if (Cl.level() == 5 && player.getClassId().childOf(Cl))
			{
				_cId = Cl.getId();
				break;
			}
		}

		if (player.isDead())
		{
			sendPacket(new ExCallToChangeClass(_cId, false));
			return;
		}
	}
}