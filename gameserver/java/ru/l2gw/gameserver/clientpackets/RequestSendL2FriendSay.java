package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.L2FriendSay;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 * Recieve Private (Friend) Message - 0xCC
 *
 * Format: c SS
 *
 * S: Message
 * S: Receiving Player
 */
public class RequestSendL2FriendSay extends L2GameClientPacket
{
	private static final Log logChat = LogFactory.getLog("chat");
	private String _message;
	private String _reciever;

	@Override
	public void readImpl()
	{
		_message = readS();
		_reciever = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.getNoChannel() != 0)
		{
			if(player.getNoChannelRemained() > 0 || player.getNoChannel() < 0)
			{
				player.sendPacket(new SystemMessage(SystemMessage.CHATTING_IS_CURRENTLY_PROHIBITED_IF_YOU_TRY_TO_CHAT_BEFORE_THE_PROHIBITION_IS_REMOVED_THE_PROHIBITION_TIME_WILL_BECOME_EVEN_LONGER));
				return;
			}
			player.updateNoChannel(0);
		}

		L2Player targetPlayer = L2ObjectsStorage.getPlayer(_reciever);
		if(targetPlayer == null)
		{
			player.sendPacket(Msg.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			return;
		}

		if(!player.getFriendList().getList().containsKey(targetPlayer.getObjectId()))
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_ON_YOUR_FRIEND_LIST).addCharName(targetPlayer));
			return;
		}

		if(Config.LOG_CHAT)
		{
			logChat.info("PRIV_MSG " + "[" + player.getName() + " to " + _reciever + "] " + _message);
		}

		L2FriendSay frm = new L2FriendSay(player.getName(), _reciever, _message);
		targetPlayer.sendPacket(frm);
	}
}