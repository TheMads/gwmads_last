package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.mail.Letter;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.util.Util;

/**
 * @author rage
 * @date 17.06.2010 16:32:57
 */
public class ExShowReceivedPostList extends L2GameServerPacket
{
	private final GArray<Letter> _received;

	public ExShowReceivedPostList(L2Player player)
	{
		_received = MailController.getInstance().getReceivedMailList(player.getObjectId());
	}

	// d dx[dSSddddddd]
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xAB);
		writeD((int) (System.currentTimeMillis() / 1000L)); // unknown: каждый раз разное
		writeD(_received.size()); // количество писем
		for(Letter letter : _received)
		{
			writeD(letter.message_id); // уникальный id письма
			writeS(letter.subject); // топик
			writeS(letter.senderName); // отправитель
			writeD(letter.price > 0 ? 1 : 0); // если тут 1 то письмо требует оплаты
			writeD(letter.expire); // время действительности письма в секундах
			writeD(letter.unread); // письмо не прочитано - его нельзя удалить и оно выделяется ярким цветом
			writeD(letter.system == 0 ? 0 : 1); // ?
			writeD(letter.attach_id > 0 ? 1 : 0); // 1 - письмо с приложением, 0 - просто письмо
			writeD(letter.returned); // если тут 1 и следующий параметр 1 то отправителем будет "****", если тут 2 то следующий параметр игнорируется
			writeD(letter.system); // 1 - отправителем значится "**News Informer**"
			writeD(0x00);
		}
	}
}
