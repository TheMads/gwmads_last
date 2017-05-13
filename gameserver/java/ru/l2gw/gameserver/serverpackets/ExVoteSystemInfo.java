package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 16.12.10 18:03
 */
public class ExVoteSystemInfo extends L2GameServerPacket
{
	private int _recomLeft;
	private int _recomHave;
	private int _bonusTime;
	private int _bonusVal;
	private int _bonusType;

	public ExVoteSystemInfo(L2Player player)
	{
		_recomLeft = player.getRecSystem().getRecommendsLeft();
		_recomHave = player.getRecSystem().getRecommendsHave();
		_bonusTime = player.getRecSystem().getBonusTime();
		_bonusVal = player.getRecSystem().getBonusVal();
		_bonusType = (player.isHourglassEffected() || !player.getRecSystem().isActive()) ? 1 : 0;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xCA);
		writeD(_recomLeft);
		writeD(_recomHave);
		writeD(_bonusTime);
		writeD(_bonusVal);
		writeD(_bonusType);
	}
}