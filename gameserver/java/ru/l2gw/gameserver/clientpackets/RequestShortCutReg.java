package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2ShortCut;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ShortCutRegister;
import ru.l2gw.gameserver.skills.TimeStamp;

public class RequestShortCutReg extends L2GameClientPacket
{
	private int _type;
	private int _id;
	private int _slot;
	private int _page;

	/**
	 * packet type id 0x3D
	 * format:		cddddd
	 */
	@Override
	public void readImpl()
	{
		_type = readD();
		int slot = readD();
		_id = readD();
		readD(); // unknown

		_slot = slot % 12;
		_page = slot / 12;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(_slot < 0 || _slot > 11 || _page < 0 || _page > 11)
		{
			player.sendActionFailed();
			return;
		}

		switch(_type)
		{
			case 0x01: // item
			{
				L2ItemInstance item = player.getInventory().getItemByObjectId(_id);
				if(item != null && item.getItem().getDelayShareGroup() > 0)
				{
					TimeStamp timeStamp = player.getSkillReuseTimeStamp(-item.getItem().getDelayShareGroup());
					int remainingTime = timeStamp != null ? (int) (timeStamp.getEndTime() - System.currentTimeMillis()) / 1000 : -1;
					L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, -1, item.getItem().getDelayShareGroup(), remainingTime, item.getItem().getReuseDelay() / 1000);
					sendPacket(new ShortCutRegister(sc));
					player.registerShortCut(sc);
				}
				else
				{
					L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, -1);
					sendPacket(new ShortCutRegister(sc));
					player.registerShortCut(sc);
				}
				break;
			}
			case 0x03: // action
			case 0x04: // macro
			case 0x05: // recipe
			case 0x06: // My Teleport
			{
				L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, -1);
				sendPacket(new ShortCutRegister(sc));
				player.registerShortCut(sc);
				break;
			}
			case 0x02: // skill
			{
				int level = player.getSkillDisplayLevel(_id);
				if(level > 0)
				{
					L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, level);
					sendPacket(new ShortCutRegister(sc));
					player.registerShortCut(sc);
				}
				break;
			}
		}

	}
}