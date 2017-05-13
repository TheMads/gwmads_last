package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.serverpackets.ExChangePostState;
import ru.l2gw.gameserver.serverpackets.ExShowReceivedPostList;

/**
 * Запрос на удаление полученных сообщений. Удалить можно только письмо без вложения. Отсылается при нажатии на "delete" в списке полученных писем.
 * @see ExShowReceivedPostList
 * @see RequestExDeleteSentPost
 */
public class RequestExDeleteReceivedPost extends L2GameClientPacket
{
	private int[] _list;

	/**
	 * format: dx[d]
	 */
	@Override
	public void readImpl()
	{
		_list = new int[readD()]; // количество элементов для удаления

		for(int i = 0; i < _list.length; i++)
			_list[i] = readD(); // уникальный номер письма
	}

	@Override
	public void runImpl()
	{
		if(_list.length == 0)
			return;

		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(!player.isInZonePeace())
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_THE_MAIL_FUNCTION_OUTSIDE_THE_PEACE_ZONE);
			return;
		}

		MailController.getInstance().deleteReceivedLetters(player, _list);
		int[] delmsg = new int[_list.length * 2];
		for(int i = 0; i < _list.length; i++)
		{
			delmsg[i * 2] = _list[i];
			delmsg[i * 2 + 1] = 0;
		}
		player.sendPacket(new ExChangePostState(1, delmsg));
	}
}