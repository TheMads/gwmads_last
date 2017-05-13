package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.*;

public class RequestGMCommand extends L2GameClientPacket
{
	// format: cSdd
	private String _targetName;
	private int _command;
	@SuppressWarnings("unused")
	private int _unknown;

	@Override
	public void readImpl()
	{
		_targetName = readS();
		_command = readD();
		_unknown = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		L2Player target = L2ObjectsStorage.getPlayer(_targetName);
		if(player == null || target == null)
			return;

		switch(_command)
		{
			case 1:
				if(AdminTemplateManager.checkBoolean("viewPlayerInfo", player))
				{
					sendPacket(new GMViewCharacterInfo(target));
					sendPacket(new GMHennaInfo(target));
				}
				break;
			case 2:
				if(AdminTemplateManager.checkBoolean("viewClanInfo", player) && target.getClanId() != 0)
					sendPacket(new GMViewPledgeInfo(target.getClan(), target));
				break;
			case 3:
				if(AdminTemplateManager.checkBoolean("viewSkillInfo", player))
					sendPacket(new GMViewSkillInfo(target));
				break;
			case 4:
				if(AdminTemplateManager.checkBoolean("viewItemInfo", player))
					sendPacket(new GMViewQuestInfo(target));
				break;
			case 5:
				if(AdminTemplateManager.checkBoolean("viewItemInfo", player))
					sendPacket(new GMViewItemList(target));
				break;
			case 6:
				if(AdminTemplateManager.checkBoolean("viewItemInfo", player))
					sendPacket(new GMViewWarehouseWithdrawList(target));
				break;
		}
	}
}
