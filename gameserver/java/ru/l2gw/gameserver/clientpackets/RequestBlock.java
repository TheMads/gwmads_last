package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.util.Collection;

public class RequestBlock extends L2GameClientPacket
{
	// format: cd(S)
	private final static int BLOCK = 0;
	private final static int UNBLOCK = 1;
	private final static int BLOCKLIST = 2;
	private final static int ALLBLOCK = 3;
	private final static int ALLUNBLOCK = 4;

	private Integer _type;
	private String targetName = null;

	@Override
	public void readImpl()
	{
		_type = readD(); //0x00 - block, 0x01 - unblock, 0x03 - allblock, 0x04 - allunblock

		if(_type == BLOCK || _type == UNBLOCK)
			targetName = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		switch(_type)
		{
			case BLOCK:
				player.addToBlockList(targetName);
				break;
			case UNBLOCK:
				player.removeFromBlockList(targetName);
				break;
			case BLOCKLIST:
				Collection<String> blockList = player.getBlockList();

				if(blockList != null)
				{
					player.sendPacket(new SystemMessage(SystemMessage._IGNORE_LIST_));

					for(String name : blockList)
						player.sendMessage(name);

					player.sendPacket(new SystemMessage(SystemMessage.__EQUALS__));
				}
				break;
			case ALLBLOCK:
				player.setBlockAll(true);
				player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOW_BLOCKING_EVERYTHING));
				break;
			case ALLUNBLOCK:
				player.setBlockAll(false);
				player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NO_LONGER_BLOCKING_EVERYTHING));
				break;
			default:
				_log.info("Unknown 0x0a block type: " + _type);
		}
	}
}