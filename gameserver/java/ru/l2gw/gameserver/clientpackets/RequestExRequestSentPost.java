package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.mail.Letter;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.serverpackets.ExReplySentPost;

/**
 * Запрос информации об отправленном письме. Появляется при нажатии на письмо из списка {@link ExShowSentPostList}.
 * В ответ шлется {@link ExReplySentPost}.
 * @see RequestExRequestReceivedPost
 */
public class RequestExRequestSentPost extends L2GameClientPacket
{
	private int postId;

	/**
	 * format: d
	 */
	@Override
	public void readImpl()
	{
		postId = readD(); // id письма
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		Letter letter = MailController.getInstance().getSendLetter(player.getObjectId(), postId);
		if(letter != null)
			player.sendPacket(new ExReplySentPost(letter));
	}
}