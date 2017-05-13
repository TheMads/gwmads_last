package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.serverpackets.ExChangePostState;

/**
 * Запрос на удаление письма с приложениями. Возвращает приложения отправителю на личный склад и удаляет письмо. Ответ на кнопку Cancel в {@link ExReplySentPost}.
 */
public class RequestExCancelSentPost extends L2GameClientPacket
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

		if(player.isTradeInProgress())
		{
			player.sendPacket(Msg.YOU_CANNOT_CANCEL_DURING_AN_EXCHANGE);
			return;
		}

		if(player.isInStoreMode())
		{
			player.sendPacket(Msg.YOU_CANNOT_CANCEL_BECAUSE_THE_PRIVATE_SHOP_OR_WORKSHOP_IS_IN_PROGRESS);
			return;
		}

		if(player.getEnchantScroll() != null)
		{
			player.sendPacket(Msg.YOU_CANNOT_CANCEL_DURING_AN_ITEM_ENHANCEMENT_OR_ATTRIBUTE_ENHANCEMENT);
			return;
		}

		if(MailController.getInstance().cancelLetter(player, postId))
		{
			player.sendPacket(new ExChangePostState(0, new int[]{ postId, 0}));
			player.sendPacket(Msg.MAIL_SUCCESSFULLY_CANCELLED);
		}
	}
}