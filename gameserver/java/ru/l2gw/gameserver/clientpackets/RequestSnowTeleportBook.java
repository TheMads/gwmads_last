package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

//import ru.l2gw.gameserver.serverpackets.ExGetBookMarkInfoPacket;;

public class RequestSnowTeleportBook extends L2GameClientPacket
{
	private int _unk1;
	private String _locName;
	private String _shortName;
	private int _icon;

	@Override
	public void readImpl()
	{
		_unk1 = readD();
		if(_unk1 == 1)
		{
			_locName = readS();
			_icon = readD();
			_shortName = readS();
		}
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(_unk1 == 1)
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_BOOKMARK_THIS_LOCATION_BECAUSE_YOU_DO_NOT_HAVE_A_MY_TELEPORT_FLAG));
		//player.sendPacket(new ExGetBookMarkInfoPacket(q,w,e,r,t,y));
	}
}
