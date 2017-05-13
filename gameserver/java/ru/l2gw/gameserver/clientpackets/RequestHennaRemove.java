package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2HennaInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 * @author rage
 * @date 17.12.10 0:19
 */
public class RequestHennaRemove extends L2GameClientPacket
{
	private int _symbolId;
	// format  cd
	
	/**
	 * packet type id 0xbb
	 * format:		cd
	 * @param decrypt
	 */
	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getPlayer();
		if (activeChar == null)
			return;
		
		for (int i = 1; i <= 3; i++)
		{
			L2HennaInstance henna = activeChar.getHenna(i);
			if (henna != null && henna.getSymbolId() == _symbolId)
			{
				if (activeChar.getAdena() >= (henna.getPrice() / 5))
				{
					activeChar.removeHenna(i, null);
					break;
				}
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_ADENA));
			}
		}
	}
}
