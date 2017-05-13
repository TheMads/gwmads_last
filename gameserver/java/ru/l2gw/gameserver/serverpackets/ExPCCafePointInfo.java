package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * Format: ch ddcdc
 */
public class ExPCCafePointInfo extends L2GameServerPacket
{
	private int pcBangPoints, m_AddPoint, m_PeriodType, RemainTime, PointType;

	public ExPCCafePointInfo(L2Player player)
	{
		pcBangPoints = player.getPcBangPoints();
	}

	public ExPCCafePointInfo(L2Player player, int modify, boolean add, boolean _double)
	{
		m_AddPoint = modify;
		pcBangPoints = player.getPcBangPoints();
		if(add)
		{
			m_PeriodType = 1;
			PointType = _double ? 2 : 1;
		}
		else
		{
			m_PeriodType = 2;
			PointType = 0;
		}
		RemainTime = 0; // Нужно сделать переменную у чара, на время использования поинтов.(НО ОНО НАМ НЕ НАДО...)
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x32);
		writeD(pcBangPoints);
		writeD(m_AddPoint);
		writeC(m_PeriodType);
		writeD(RemainTime);
		writeC(PointType);
		writeD(0); // TODO: online time
	}
}