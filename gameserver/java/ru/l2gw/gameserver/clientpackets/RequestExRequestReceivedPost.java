package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.mail.Letter;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.serverpackets.ExChangePostState;
import ru.l2gw.gameserver.serverpackets.ExReplyReceivedPost;
import ru.l2gw.gameserver.serverpackets.ExShowReceivedPostList;

/**
 * Запрос информации об полученном письме. Появляется при нажатии на письмо из списка {@link ExShowReceivedPostList}.
 *
 * @see RequestExRequestSentPost
 */
public class RequestExRequestReceivedPost extends L2GameClientPacket
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

		Letter letter = MailController.getInstance().getReceivedLetter(player.getObjectId(), postId);
		if(letter != null)
		{
			player.sendPacket(new ExReplyReceivedPost(letter));
			player.sendPacket(new ExChangePostState(1, new int[] {postId, 1}));
		}
	}
}