package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.mail.Letter;
import ru.l2gw.gameserver.model.mail.LetterAttach;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.serverpackets.ExNoticePostArrived;
import ru.l2gw.gameserver.serverpackets.ExReplyWritePost;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.util.Util;

/**
 * Запрос на отсылку нового письма. В ответ шлется {@link ExReplyWritePost}.
 * @see RequestExPostItemList
 * @see RequestExRequestReceivedPostList
 */
public class RequestExSendPost extends L2GameClientPacket
{
	private int _messageType;
	private String _targetName, _subject, _message;
	private int[] _attItems;
	private long[] _attItemsQ;
	private long _price;

	/**
	 * format: SdSS dx[dQ] Q
	 */
	@Override
	public void readImpl()
	{
		_targetName = readS(35); // имя адресата
		_messageType = readD(); // тип письма, 0 простое 1 с запросом оплаты
		_subject = readS(30); // subject
		_message = readS(30000); // body

		_attItems = new int[readD()]; // число прикрепленных вещей
		_attItemsQ = new long[_attItems.length];
		for(int i = 0; i < _attItems.length; i++)
		{
			_attItems[i] = readD(); // objectId
			_attItemsQ[i] = readQ(); // количество
		}

		_price = _messageType == 1 ? readQ() : 0; // цена для писем с запросом оплаты
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isCastingNow())
		{
			player.sendActionFailed();
			return;
		}

		if(player.getName().equalsIgnoreCase(_targetName))
		{
			player.sendPacket(Msg.YOU_CANNOT_SEND_A_MAIL_TO_YOURSELF);
			return;
		}

		if(_attItems.length > 0 && !player.isInZonePeace())
		{
			player.sendPacket(Msg.YOU_CANNOT_FORWARD_IN_A_NON_PEACE_ZONE_LOCATION);
			return;
		}

		if(player.isTradeInProgress())
		{
			player.sendPacket(Msg.YOU_CANNOT_FORWARD_DURING_AN_EXCHANGE);
			return;
		}

		if(player.isInStoreMode())
		{
			player.sendPacket(Msg.YOU_CANNOT_FORWARD_BECAUSE_THE_PRIVATE_SHOP_OR_WORKSHOP_IS_IN_PROGRESS);
			return;
		}

		if(player.getEnchantScroll() != null)
		{
			player.sendPacket(Msg.YOU_CANNOT_FORWARD_DURING_AN_ITEM_ENHANCEMENT_OR_ATTRIBUTE_ENHANCEMENT);
			return;
		}

		int targetId;
		L2Player target = L2ObjectsStorage.getPlayer(_targetName);
		if(target != null)
		{
			targetId = target.getObjectId();
			_targetName = target.getName();
			if(target.isInBlockList(player)) // цель заблокировала отправителя
			{
				player.sendPacket(Msg.YOU_CANNOT_SEND_MAILS_TO_ANY_CHARACTER_THAT_HAS_BLOCKED_YOU);
				return;
			}
		}
		else
		{
			String[] name = { _targetName };
			targetId = Util.getCharIdByNameAndName(name);
			_targetName = name[0];
			if(targetId > 0 && Util.checkBlockList(targetId, player.getObjectId())) // цель заблокировала отправителя
			{
				player.sendPacket(Msg.YOU_CANNOT_SEND_MAILS_TO_ANY_CHARACTER_THAT_HAS_BLOCKED_YOU);
				return;
			}
		}

		if(targetId == 0) // не нашли цель?
		{
			player.sendPacket(Msg.WHEN_THE_RECIPIENT_DOESN_T_EXIST_OR_THE_CHARACTER_HAS_BEEN_DELETED_SENDING_MAIL_IS_NOT_POSSIBLE);
			return;
		}

		if(MailController.getInstance().getReceivedCount(targetId) >= 240)
		{
			player.sendPacket(Msg.THE_MAIL_LIMIT_240_OF_THE_OPPONENT_S_CHARACTER_HAS_BEEN_EXCEEDED_AND_THIS_CANNOT_BE_FORWARDED);
			return;
		}

		if(MailController.getInstance().getSendCount(player.getObjectId()) >= 240)
		{
			player.sendPacket(Msg.THE_MAIL_LIMIT_240_HAS_BEEN_EXCEEDED_AND_THIS_CANNOT_BE_FORWARDED);
			return;
		}

		int expireTime = (int) (System.currentTimeMillis() / 1000) + (_messageType == 1 ? (12 * 60 * 60) : (15 * 24 * 60 * 60));

		long serviceCost = 100 + _attItems.length * 1000;

		if(player.getAdena() < serviceCost)
		{
			player.sendPacket(Msg.YOU_CANNOT_FORWARD_BECAUSE_YOU_DON_T_HAVE_ENOUGH_ADENA);
			return;
		}

		for(int i = 0; i < _attItems.length; i++)
		{
			L2ItemInstance item = player.getInventory().getItemByObjectId(_attItems[i]);
			if(item == null || item.getCount() < _attItemsQ[i] || _attItemsQ[i] <= 0 ||item.getItemId() == 57 && item.getCount() < _attItemsQ[i] + serviceCost || item.isEquipped() || item.getItem().getType2() == L2Item.TYPE2_QUEST || !item.canBeTraded(player) || !item.getItem().isKeepType(L2Item.KEEP_TYPE_MAIL))
			{
				player.sendPacket(Msg.THE_ITEM_THAT_YOU_RE_TRYING_TO_SEND_CANNOT_BE_FORWARDED_BECAUSE_IT_ISN_T_PROPER);
				return;
			}
		}

		if(_price > 0 && _attItems.length < 1)
		{
			player.sendPacket(Msg.IT_S_A_PAYMENT_REQUEST_TRANSACTION_PLEASE_ATTACH_THE_ITEM);
			return;
		}

		player.reduceAdena("SendMail", serviceCost, null, true);

		Letter letter = new Letter();
		letter.receiverId = targetId;
		letter.receiverName = _targetName;
		letter.senderId = player.getObjectId();
		letter.senderName = player.getName();
		letter.subject = _subject;
		letter.message = _message;
		letter.price = _price;
		letter.unread = 1;
		letter.expire = expireTime;

		LetterAttach attach = null;
		if(_attItems.length > 0)
			attach = new LetterAttach(player, _attItems, _attItemsQ);

		MailController.getInstance().sendMail(letter, attach);

		player.sendPacket(new ExReplyWritePost(1));
		player.sendPacket(Msg.MAIL_SUCCESSFULLY_SENT);
		if(target != null)
			target.sendPacket(new ExNoticePostArrived(1));
	}
}