package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.DoorTable;

/**
 * @author Dezmond_snz
 * Format: cddd
 */
public class DlgAnswer extends L2GameClientPacket
{
	static Log _log = LogFactory.getLog(DlgAnswer.class.getName());

	private int _messageId;
	private int _answer;
	private int _requestId;

	@Override
	public void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_requestId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		if(Config.DEBUG)
			_log.debug(getType() + ": Answer acepted. Message ID " + _messageId + ", asnwer " + _answer + ", request Id " + _requestId);

		switch(_requestId)
		{
			case 1:
				player.teleportAnswer(_answer);
				break;
			case 2:
				player.reviveAnswer(_answer);
				break;
			case 3:
				player.scriptAnswer(_answer);
				break;
			case 4:
				if(Config.ALLOW_WEDDING && player.isEngageRequest())
					player.engageAnswer(_answer);
				break;
			case 65:
				if(_answer == 1)
				{
					L2NpcInstance broker = player.getLastNpc();
					if(broker == null || !player.isInRange(broker, player.getInteractDistance(broker)))
					{
						player.sendPacket(Msg.IT_S_TOO_FAR_FROM_THE_NPC_TO_WORK);
						return;
					}

					String var = player.getVar("bid-" + broker.getNpcId());
					long bid = Long.parseLong(var != null && !var.isEmpty() ? var : "0");
					if(bid > 0)
					{
						player.unsetVar("bid-" + broker.getNpcId());
						player.addAdena("ItemAuctionOutBid", bid, broker, true);
					}
				}
				break;
		}

		if(_answer == 1 && _requestId > 1000 && DoorTable.getInstance().getDoor(_requestId) != null)
			if(_messageId == SystemMessage.WOULD_YOU_LIKE_TO_OPEN_THE_GATE)
				DoorTable.getInstance().getDoor(_requestId).openMe();
			else if(_messageId == SystemMessage.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE)
				DoorTable.getInstance().getDoor(_requestId).closeMe();
	}
}
