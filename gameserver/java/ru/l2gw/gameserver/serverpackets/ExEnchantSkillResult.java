package ru.l2gw.gameserver.serverpackets;

/**
 * @author: rage
 * @date: 13.06.2010 20:46:48
 */
public class ExEnchantSkillResult extends L2GameServerPacket
{
	private final int _result;

	public ExEnchantSkillResult(int result)
	{
		_result = result;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xA8);
		writeD(_result);
	}
}