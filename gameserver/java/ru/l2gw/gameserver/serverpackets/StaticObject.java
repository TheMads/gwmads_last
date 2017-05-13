package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2StaticObjectInstance;

public class StaticObject extends L2GameServerPacket
{
	private int _id, _objectId;
	private int _type, _isTargetable, _meshIndex, _isClosed, _isEnemy, _currentHp, _maxHp, _showHp, _damageGrade;

	/**
	 * [S]0x9f StaticObjectPacket   dd
	 */
	public StaticObject(L2StaticObjectInstance StaticObject)
	{
		_id = StaticObject.getStaticObjectId();
		_objectId = StaticObject.getObjectId();
		_type = 2; //Kamael (0x02 - throne and map board)
		_isTargetable = 1; //Kamael
		_meshIndex = StaticObject.getType() == 3 && StaticObject.getBuilding(1) != null && !StaticObject.getBuilding(1).getSiege().isInProgress() && StaticObject.getBuilding(1).getOwnerId() > 0 ? 1 : 0;
		_isClosed = 0; //Kamael
		_isEnemy = 0; //Kamael
		_currentHp = 0; //Kamael
		_maxHp = 0; //Kamael
		_showHp = 0; //Kamael
		_damageGrade = 0; //Kamael
	}

	public StaticObject(L2DoorInstance door)
	{
		_id = door.getDoorId();
		_objectId = door.getObjectId();
		_type = 1;
		_isTargetable = 1;
		_meshIndex = 1;
		_isClosed = door.isOpen() ? 0 : 1; //opened 0 /closed 1
		_isEnemy = 0;
		_currentHp = (int) door.getCurrentHp();
		_maxHp = door.getMaxHp();
		_showHp = door.isHPVisible() ? 1 : 0;
		_damageGrade = door.getDamage();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9f);
		writeD(_id);
		writeD(_objectId);
		writeD(_type);
		writeD(_isTargetable);
		writeD(_meshIndex);
		writeD(_isClosed);
		writeD(_isEnemy);
		writeD(_currentHp);
		writeD(_maxHp);
		writeD(_showHp);
		writeD(_damageGrade);
	}
}