package ru.l2gw.gameserver.serverpackets;

/**
 * sample
 * format
 * d
 */
public class ShowCalc extends L2GameServerPacket
{
	private int _calculatorId;

	public ShowCalc(int calculatorId)
	{
		_calculatorId = calculatorId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xE2);
		writeD(_calculatorId);
	}
}