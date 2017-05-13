package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 20.05.2010 12:46:12
 */
public class ExBRPremiumState extends L2GameServerPacket
{
	private int _objectId;
	private byte _premium;

	public ExBRPremiumState(int objectId, boolean premium)
	{
		_objectId = objectId;
		_premium = (byte) (premium ? 1 : 0);
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xDA);
		writeD(_objectId);
		writeC(_premium);
	}
}
