package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.mail.Letter;
import ru.l2gw.gameserver.model.mail.LetterAttach;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * @author rage
 * @date 18.06.2010 11:50:40
 */
public class ExReplyReceivedPost extends AbstractItemPacket
{
	private Letter _letter;
	private LetterAttach _letterAttach = null;

	public ExReplyReceivedPost(Letter letter)
	{
		_letter = letter;
		if(letter.attach_id > 0)
			_letterAttach = MailController.getInstance().getAttach(_letter.attach_id);
	}

	// dddSSS dx[hddQdddhhhhhhhhhh] Qdd
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xAC);

		writeD(_letter.message_id); // id письма
		writeD(_letter.price > 0 && _letter.returned == 0 ? 1 : 0); // 1 - письмо с запросом оплаты, 0 - просто письмо
		writeD(_letter.returned); // для писем с флагом "от news informer" в отправителе значится "****", всегда оверрайдит тип на просто письмо

		writeS(_letter.senderName); // от кого
		writeS(_letter.subject); // топик
		writeS(_letter.message); // тело

		if(_letterAttach != null)
		{
			writeD(_letterAttach.getItems().size()); // количество приложенных вещей
			for(L2ItemInstance temp : _letterAttach.getItems())
			{
				writeItemInfo(temp);
				writeD(temp.getObjectId());
			}
		}
		else
			writeD(0);

		writeQ(_letter.price); // для писем с оплатой - цена
		writeD(_letterAttach != null ? 1 : 0); // 1 - письмо можно вернуть
		writeD(_letter.system); // 1 - на письмо нельзя отвечать, его нельзя вернуть, в отправителе значится news informer (или "****" если установлен флаг в начале пакета)
	}
}
