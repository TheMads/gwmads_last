package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Player;

import ru.l2gw.gameserver.serverpackets.MyTargetSelected;
import ru.l2gw.gameserver.serverpackets.ValidateLocation;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2NpcNotSayInstance extends L2NpcInstance
{
	public L2NpcNotSayInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		if(this != player.getTarget())
		{
			if(player.setTarget(this))
			{
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));
				player.sendPacket(new ValidateLocation(this));
			}
		}
		//else
		//	player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));
	}

}