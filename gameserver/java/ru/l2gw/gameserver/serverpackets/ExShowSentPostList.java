package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.mail.Letter;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Util;

/**
 * @author rage
 * @date 17.06.2010 16:37:26
 */
public class ExShowSentPostList extends L2GameServerPacket
{
	private final GArray<Letter> _sended;

	public ExShowSentPostList(L2Player player)
	{
		_sended = MailController.getInstance().getSendMailList(player.getObjectId());
	}

	// d dx[dSSddddd]
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xAD);
		writeD(Util.getCurrentTime()); // ?
		writeD(_sended.size()); // количество писем
		for(Letter letter : _sended)
		{
			writeD(letter.message_id); // уникальный id письма
			writeS(letter.subject); // топик
			writeS(letter.receiverName); // кому
			writeD(letter.price > 0 ? 1 : 0); // если тут 1 то письмо требует оплаты
			writeD(letter.expire); // время действительности письма в секундах
			writeD(0);
			writeD(letter.system == 0 ? 0 : 1); // returnable
			writeD(letter.attach_id > 0 ? 1 : 0); // 1 - письмо с приложением, 0 - просто письмо
		}
	}
}
