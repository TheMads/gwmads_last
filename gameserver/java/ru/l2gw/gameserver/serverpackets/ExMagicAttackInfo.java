package ru.l2gw.gameserver.serverpackets;

/**
 *
 * @author monithly
 */
public class ExMagicAttackInfo extends L2GameServerPacket
{
	private final int _targetId, _skillId;

	public ExMagicAttackInfo(int targetId, int skillId)
	{
		_targetId = targetId;
		_skillId = skillId;
	}

	@Override
	protected void writeImpl()
	{
		writeEx(0xFB);
		writeD(_targetId);
		writeD(_skillId);
		writeD(0x01);
	}
}