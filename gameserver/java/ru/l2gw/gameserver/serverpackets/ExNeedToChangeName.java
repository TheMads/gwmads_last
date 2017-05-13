package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 17:00
 */
public class ExNeedToChangeName extends L2GameServerPacket
{
	private int type, subType;
	private String name;

	public ExNeedToChangeName(int type, int subType, String name)
	{
		this.type = type;
		this.subType = subType;
		this.name = name;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x6A);
		writeD(type);
		writeD(subType);
		writeS(name);
	}
}
