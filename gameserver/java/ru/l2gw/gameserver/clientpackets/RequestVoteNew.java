package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExBrExtraUserInfo;
import ru.l2gw.gameserver.serverpackets.ExVoteSystemInfo;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.serverpackets.UserInfo;

/**
 * @author rage
 * @date 17.12.10 0:31
 */
public class RequestVoteNew extends L2GameClientPacket
{
	private int _targetId;
	
	@Override
	protected void readImpl()
	{
		_targetId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getPlayer();
		if(activeChar == null)
			return;
		
		L2Object object = activeChar.getTarget();
		
		if(!(object instanceof L2Player))
		{
			if(object == null)
				activeChar.sendPacket(new SystemMessage(SystemMessage.SELECT_TARGET));
			else
				activeChar.sendPacket(new SystemMessage(SystemMessage.INVALID_TARGET));
			return;
		}
		
		L2Player target = (L2Player) object;
		
		if(target.getObjectId() != _targetId)
			return;
		
		if(target == activeChar)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_RECOMMEND_YOURSELF));
			return;
		}
		
		if(activeChar.getRecSystem().getRecommendsLeft() <= 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.NO_MORE_RECOMMENDATIONS_TO_HAVE));
			return;
		}
		
		if(target.getRecSystem().getRecommendsHave() >= 255)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_NO_LONGER_RECIVE_A_RECOMMENDATION));
			return;
		}

		if(target.getRecSystem().getRecommendsHave() >= 255)
		{
			activeChar.sendPacket(Msg.YOU_NO_LONGER_RECIVE_A_RECOMMENDATION);
			return;
		}

		activeChar.getRecSystem().giveRecommend(target);
		SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_RECOMMENDED);
		sm.addString(target.getName());
		sm.addNumber(activeChar.getRecSystem().getRecommendsLeft());
		activeChar.sendPacket(sm);

		sm = new SystemMessage(SystemMessage.YOU_HAVE_BEEN_RECOMMENDED);
		sm.addString(activeChar.getName());
		target.sendPacket(sm);
	}
}
