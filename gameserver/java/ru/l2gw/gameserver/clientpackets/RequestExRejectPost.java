package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.serverpackets.ExChangePostState;

public class RequestExRejectPost extends L2GameClientPacket
{
	private int postId;

	/**
	 * format: d
	 */
	@Override
	public void readImpl()
	{
		postId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(!player.isInZonePeace())
		{
			player.sendPacket(Msg.YOU_CANNOT_CANCEL_IN_A_NON_PEACE_ZONE_LOCATION);
			return;
		}

		if(MailController.getInstance().returnLetter(player.getObjectId(), postId, false))
		{
			player.sendPacket(new ExChangePostState(1, new int[]{ postId, 0}));
			player.sendPacket(Msg.MAIL_SUCCESSFULLY_RETURNED);
		}
	}
}