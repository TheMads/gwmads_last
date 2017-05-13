package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.playerSubOrders.TeleportBook;
import ru.l2gw.gameserver.tables.SkillTable;

public class RequestTeleportBookMark extends L2GameClientPacket
{
	private final static L2Skill myTeleportSkill = SkillTable.getInstance().getInfo(2588, 1);
	private int slot;

	@Override
	public void readImpl()
	{
		slot = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player != null && !player.isCastingNow() && TeleportBook.checkUseCondition(player))
		{
			if(player.getItemCountByItemId(TeleportBook.TELEPORT_SCROLL_ID) < 1)
			{
				player.sendPacket(Msg.YOU_CANNOT_TELEPORT_BECAUSE_YOU_DO_NOT_HAVE_A_TELEPORT_ITEM);
				return;
			}
			player.setSessionVar("tele_slot", String.valueOf(slot));
			player.getAI().Cast(myTeleportSkill, player, null, false, false);
		}
	}
}