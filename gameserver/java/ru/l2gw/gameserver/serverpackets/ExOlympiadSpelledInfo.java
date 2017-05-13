package ru.l2gw.gameserver.serverpackets;

public class ExOlympiadSpelledInfo extends AbstractAbnormalStatus
{
	// chdd(dhd)
	private int _objectId = 0;

	public ExOlympiadSpelledInfo(int objectId)
	{
		_objectId = objectId;
	}

	@Override
	protected final void writeImpl()
	{
		if(_objectId == 0)
			return;

		writeC(EXTENDED_PACKET);
		writeH(0x7C);

		writeD(_objectId);
		writeD(_abnormals.size());
		for(AbnormalStatus status : _abnormals)
		{
			writeD(status.skillId);
			writeH(status.skillLvl);
			writeD(status.timeLeft);
		}
	}
}
