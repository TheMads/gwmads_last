package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

public class ExOlympiadUserInfo extends L2GameServerPacket
{
	// cdSddddd
	private int _side;
	private int _objectId;
	private String _name;
	private int _classId;
	private int _currentHp;
	private int _maxHp;
	private int _currentCp;
	private int _maxCp;


	/**
	 * @param player
	 * @param side (1 = right, 2 = left)
	 */
	public ExOlympiadUserInfo(L2Player player, int side)
	{
		_objectId = player.getObjectId();
		_name = player.getName();
		_classId = player.getClassId().getId();
		_currentHp = (int)player.getCurrentHp();
		_maxHp = player.getMaxHp();
		_currentCp = (int)player.getCurrentCp();
		_maxCp = player.getMaxCp();
		_side = side;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x7B);
		writeC(_side);
		writeD(_objectId);
		writeS(_name);
		writeD(_classId);
		writeD(_currentHp);
		writeD(_maxHp);
		writeD(_currentCp);
		writeD(_maxCp);
	}

	public int getSide()
	{
		return _side;
	}
}