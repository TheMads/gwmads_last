package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2HennaInstance;
import ru.l2gw.gameserver.serverpackets.HennaItemRemoveInfo;
import ru.l2gw.gameserver.tables.HennaTable;
import ru.l2gw.gameserver.templates.L2Henna;

/**
 * @author rage
 * @date 17.12.10 0:18
 */
public class RequestHennaItemRemoveInfo extends L2GameClientPacket
{
	private int _symbolId;
	// format  cd
	
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
		
		L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);
		if (template == null)
			return;
		
		L2HennaInstance henna = new L2HennaInstance(template);
		activeChar.sendPacket(new HennaItemRemoveInfo(henna, activeChar));
	}
}
