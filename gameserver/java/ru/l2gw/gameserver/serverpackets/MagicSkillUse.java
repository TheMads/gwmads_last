package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;

/**
 * Format:   dddddddddd
 * Пример пакета:
 * 48
 * 86 99 00 4F  86 99 00 4F
 * EF 08 00 00  01 00 00 00
 * 00 00 00 00  00 00 00 00
 * F9 B5 FF FF  7D E0 01 00  68 F3 FF FF
 * 00 00 00 00
 */
public class MagicSkillUse extends L2GameServerPacket
{
	protected int _targetId;
	protected int _skillId;
	protected int _skillLevel;
	protected int _hitTime;
	protected long _reuseDelay;
	protected int _chaId, _x, _y, _z, _tx, _ty, _tz;
	protected boolean _isBuff;
	private int _doubleCast;

	public MagicSkillUse(L2Character cha, L2Character target, int skillId, int skillLevel, int hitTime, long reuseDelay)
	{
		this(cha, target, skillId, skillLevel, hitTime, reuseDelay, false);
	}

	public MagicSkillUse(L2Character cha, L2Character target, int skillId, int skillLevel, int hitTime, long reuseDelay, boolean isBuff)
	{
		_chaId = cha.getObjectId();
		_targetId = target.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_hitTime = hitTime;
		_reuseDelay = reuseDelay;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_tx = target.getX();
		_ty = target.getY();
		_tz = target.getZ();
		_isBuff = isBuff;
		_doubleCast = 0;
	}

	public MagicSkillUse(L2Character cha, int skillId, int skillLevel, int hitTime, long reuseDelay)
	{
		_chaId = cha.getObjectId();
		_targetId = cha.getTargetId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_hitTime = hitTime;
		_reuseDelay = reuseDelay;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_tx = cha.getX();
		_ty = cha.getY();
		_tz = cha.getZ();
		_isBuff = false;
		_doubleCast = 0;
	}

	public boolean isBuffPacket()
	{
		return _isBuff;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x48);
		writeD(_doubleCast);
		writeD(_chaId);
		writeD(_targetId);
		writeC(0x00); // L2WT GOD
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_hitTime);
		writeD(getSkillReplace(_skillId));
		writeD((int) _reuseDelay);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(0x00); // L2WT GOD
		writeD(_tx);
		writeD(_ty);
		writeD(_tz);
	}
	
	public static int getSkillReplace(int _skillId)
	{
		switch (_skillId)
		{
			case 11012:
			case 11013:
			case 11014:
			case 11015:
			case 11016:
				return 11011;
			case 11018:
			case 11019:
			case 11020:
			case 11021:
			case 11022:
				return 11017;
			case 11024:
			case 11025:
			case 11026:
			case 11027:
			case 11028:
				return 11023;
			case 11035:
			case 11036:
			case 11037:
			case 11038:
			case 11039:
				return 11034;
			case 11041:
			case 11042:
			case 11043:
			case 11044:
			case 11045:
				return 11040;
			default:
				return -1;
		}
	}
}