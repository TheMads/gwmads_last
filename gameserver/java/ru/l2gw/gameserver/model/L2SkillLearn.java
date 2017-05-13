package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.tables.SkillTreeTable;

public final class L2SkillLearn
{
	private final short _id;
	private final short _skillLevel;
	private final int _spCost;
	private final int _repCost;
	private final byte _minLevel;
	private final short _itemId;
	private final int _itemCount;
	private final int _skillGroupId;
	private final byte _raceId;

	// not needed, just for easier debug
	public final String _name;

	public L2SkillLearn(short id, short lvl, byte minLvl, String name, int cost, short itemId, int itemCount, byte raceId, int groupId)
	{
		_id = id;
		_skillLevel = lvl;
		_minLevel = minLvl;
		_raceId = raceId;
		_name = name.intern();

		_skillGroupId = groupId;
		if(_skillGroupId == SkillTreeTable.SKILL_TYPE_CLAN || _skillGroupId == SkillTreeTable.SKILL_TYPE_CLAN_SUB_PLEDGE)
		{
			_spCost = 0;
			_repCost = cost;
		}
		else
		{
			_spCost = cost;
			_repCost = 0;
		}

		_itemId = itemId;
		_itemCount = itemCount;
	}

	public short getId()
	{
		return _id;
	}

	public short getLevel()
	{
		return _skillLevel;
	}

	public byte getMinLevel()
	{
		return _minLevel;
	}

	public String getName()
	{
		return _name;
	}

	public int getSpCost()
	{
		return _spCost;
	}

	public short getItemId()
	{
		return _itemId;
	}

	public int getItemCount()
	{
		return _itemCount;
	}

	public int getRepCost()
	{
		return _repCost;
	}

	public int getSkillGroup()
	{
		return _skillGroupId;
	}

	public boolean isNormal()
	{
		return _skillGroupId == SkillTreeTable.SKILL_TYPE_NORMAL;
	}

	public boolean isCommon()
	{
		return _skillGroupId == SkillTreeTable.SKILL_TYPE_FISHING;
	}

	public boolean isClan()
	{
		return _skillGroupId == SkillTreeTable.SKILL_TYPE_CLAN;
	}

	public boolean isSubclass()
	{
		return _skillGroupId == SkillTreeTable.SKILL_TYPE_SUBCLASS;
	}

	public boolean isTransformation()
	{
		return _skillGroupId == SkillTreeTable.SKILL_TYPE_TRANSFORM;
	}

	public boolean isTransferSkill()
	{
		return _skillGroupId == SkillTreeTable.SKILL_TYPE_TRANSFER;
	}

	public byte getRaceId()
	{
		return _raceId;
	}

	@Override
	public String toString()
	{
		return "SkillLearn for " + getName() + " id: " + getId() + " level: " + getLevel() + " type: " + _skillGroupId + " cost: " + _spCost + " rep: " + _repCost + " itemId: " + _itemId + " itemCount: " + _itemCount;
	}
}