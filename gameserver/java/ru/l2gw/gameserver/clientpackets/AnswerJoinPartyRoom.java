package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.instancemanager.PartyRoomManager;

public class AnswerJoinPartyRoom extends L2GameClientPacket
{
	private int _response;

	@Override
	public void readImpl()
	{
		if(_buf.hasRemaining())
			_response = readD();
		else
			_response = 0;

	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Player requestor = player.getTransactionRequester();

		player.setTransactionRequester(null);

		if(requestor == null)
			return;

		requestor.setTransactionRequester(null);

		if(player.getTransactionType() != L2Player.TransactionType.PARTY_ROOM || player.getTransactionType() != requestor.getTransactionType())
			return;

		if(_response == 1)
		{
			if(requestor.getPartyRoom() <= 0)
			{
				player.sendActionFailed();
				return;
			}
			if(player.getPartyRoom() > 0)
			{
				player.sendActionFailed();
				return;
			}
			PartyRoomManager.getInstance().joinPartyRoom(player, requestor.getPartyRoom());
		}
		else
			requestor.sendPacket(new SystemMessage(SystemMessage.THE_PLAYER_DECLINED_TO_JOIN_YOUR_PARTY));

		//TODO проверить на наличие пакета ДОБАВЛЕНИЯ в список, в другом случае отсылать весь список всем мемберам
	}
}