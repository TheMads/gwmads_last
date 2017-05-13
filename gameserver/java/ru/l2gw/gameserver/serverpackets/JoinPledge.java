package ru.l2gw.gameserver.serverpackets;

public class JoinPledge extends L2GameServerPacket
{
	private int _pledgeId;

	public JoinPledge(int pledgeId)
	{
		_pledgeId = pledgeId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2D);

		writeD(_pledgeId);
	}
}