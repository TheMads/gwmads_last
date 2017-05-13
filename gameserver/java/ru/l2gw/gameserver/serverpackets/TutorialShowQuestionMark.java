package ru.l2gw.gameserver.serverpackets;

public class TutorialShowQuestionMark extends L2GameServerPacket
{
	private int _number;

	public TutorialShowQuestionMark(int number)
	{
		_number = number;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xa7);
		writeD(_number);
	}
}