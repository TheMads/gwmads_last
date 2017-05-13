package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.mail.Letter;
import ru.l2gw.gameserver.model.mail.LetterAttach;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * @author кфпу
 * @date 18.06.2010 15:43:12
 */
public class ExReplySentPost extends AbstractItemPacket
{
	private Letter _letter;
	private LetterAttach _attach = null;

	public ExReplySentPost(Letter letter)
	{
		_letter = letter;
		if(_letter.attach_id > 0)
			_attach = MailController.getInstance().getAttach(_letter.attach_id);
	}

	// ddSSS dx[hddQdddhhhhhhhhhh] Qd
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xAE);

		writeD(_letter.message_id); // id письма
		writeD(_letter.price > 0 ? 1 : 0); // 1 - письмо с запросом оплаты, 0 - просто письмо

		writeS(_letter.receiverName); // кому
		writeS(_letter.subject); // топик
		writeS(_letter.message); // тело

		if(_attach != null)
		{
			writeD(_attach.getItems().size()); // количество приложенных вещей
			for(L2ItemInstance temp : _attach.getItems())
			{
				writeItemInfo(temp);
				writeD(temp.getObjectId());
			}
		}
		else
			writeD(0x00);

		writeQ(_letter.price); // для писем с оплатой - цена
		writeD(0); // ?
	}
}
