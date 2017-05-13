package ru.l2gw.gameserver.serverpackets;

/**
 *
 * sample
 *
 * 0000: 85 02 00 10 04 00 00 01 00 4b 02 00 00 2c 04 00    .........K...,..
 * 0010: 00 01 00 58 02 00 00                               ...X...
 *
 *
 * format   h (dhd)
 */
public class AbnormalStatusUpdate extends AbstractAbnormalStatus
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x85);
		writeH(_abnormals.size());

		for(AbnormalStatus status : _abnormals)
		{
			writeD(status.skillId);
			writeH(status.skillLvl);
			writeD(status.timeLeft);
		}
	}
}