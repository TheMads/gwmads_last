package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.serverpackets.ExChangePostState;
import ru.l2gw.commons.arrays.GArray;

/**
 * Запрос на удаление отправленных сообщений. Удалить можно только письмо без вложения. Отсылается при нажатии на "delete" в списке отправленных писем.
 * @see ExShowSentPostList
 * @see RequestExDeleteReceivedPost
 */
public class RequestExDeleteSentPost extends L2GameClientPacket
{
	private GArray<Integer> _list;

	/**
	 * format: dx[d]
	 */
	@Override
	public void readImpl()
	{
		int size = readD();
		_list = new GArray<Integer>(size); // количество элементов для удаления

		for(int i = 0; i < size; i++)
			_list.add(readD()); // уникальный номер письма
	}

	@Override
	public void runImpl()
	{
		if(_list.size() == 0)
			return;

		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(!player.isInZonePeace())
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_THE_MAIL_FUNCTION_OUTSIDE_THE_PEACE_ZONE);
			return;
		}

		MailController.getInstance().deleteSendLetters(player, _list);

		int[] delmsg = new int[_list.size() * 2];
		for(int i = 0; i < _list.size(); i++)
		{
			delmsg[i * 2] = _list.get(i);
			delmsg[i * 2 + 1] = 0;
		}

		player.sendPacket(new ExChangePostState(0, delmsg));
	}
}