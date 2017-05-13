package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 07.11.2009 15:33:28
 */
public class ExSubPledgetSkillAdd extends L2GameServerPacket
{
	private int _skillId;
	private int _skillLevel;
	private int _subPledge;

	public ExSubPledgetSkillAdd(int skillId, int skillLevel, int subPledge)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
		_subPledge = subPledge;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x77);
		writeD(_subPledge);
		writeD(_skillId);
		writeD(_skillLevel);
	}
}
