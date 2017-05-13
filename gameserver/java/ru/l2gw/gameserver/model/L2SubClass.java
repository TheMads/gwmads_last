package ru.l2gw.gameserver.model;

import gnu.trove.map.hash.TIntIntHashMap;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.base.OccupationGroup;

/**
 * Character Sub-Class Definition
 * <BR>
 * Used to store key information about a character's sub-class.
 *
 * @author Tempy
 */
public class L2SubClass
{
	private short _class = 0;
	private long _exp = Experience.LEVEL[40];
	private int _sp = 0;
	private byte _level = 40;
	private double _Hp = 1;
	private double _Mp = 1;
	private double _Cp = 1;
	private boolean _active = false;
	private boolean _isBase = false;
	private byte _slot = 0;
	private L2Player _player;
	private DeathPenalty _dp;

	private static TIntIntHashMap _certificateClass = new TIntIntHashMap();
	private static TIntIntHashMap _certificateItems = new TIntIntHashMap();
	private static TIntIntHashMap _certificateTransforms = new TIntIntHashMap();

	static
	{
		_certificateClass.put(2, 1); // 10281 Certificate - Warrior Ability
		_certificateClass.put(3, 1);
		_certificateClass.put(46, 1);
		_certificateClass.put(48, 1);
		_certificateClass.put(55, 1);
		_certificateClass.put(57, 1);
		_certificateClass.put(89, 1);
		_certificateClass.put(88, 1);
		_certificateClass.put(113, 1);
		_certificateClass.put(114, 1);
		_certificateClass.put(117, 1);
		_certificateClass.put(118, 1);
		_certificateClass.put(127, 1);
		_certificateClass.put(128, 1);
		_certificateClass.put(129, 1);
		_certificateClass.put(131, 1);
		_certificateClass.put(132, 1);
		_certificateClass.put(133, 1);
		_certificateClass.put(5, 2); // 10282 Certificate - Knight Ability
		_certificateClass.put(90, 2);
		_certificateClass.put(6, 2);
		_certificateClass.put(91, 2);
		_certificateClass.put(20, 2);
		_certificateClass.put(99, 2);
		_certificateClass.put(33, 2);
		_certificateClass.put(106, 2);
		_certificateClass.put(9, 3); // 10283 Certificate - Rogue Ability
		_certificateClass.put(92, 3);
		_certificateClass.put(24, 3);
		_certificateClass.put(102, 3);
		_certificateClass.put(37, 3);
		_certificateClass.put(109, 3);
		_certificateClass.put(130, 3);
		_certificateClass.put(134, 3);
		_certificateClass.put(8, 3);
		_certificateClass.put(93, 3);
		_certificateClass.put(23, 3);
		_certificateClass.put(101, 3);
		_certificateClass.put(36, 3);
		_certificateClass.put(108, 3);
		_certificateClass.put(12, 4); // 10284 Certificate - Wizard Ability
		_certificateClass.put(94, 4);
		_certificateClass.put(13, 4);
		_certificateClass.put(95, 4);
		_certificateClass.put(27, 4);
		_certificateClass.put(103, 4);
		_certificateClass.put(40, 4);
		_certificateClass.put(110, 4);
		_certificateClass.put(16, 5); // 10285 Certificate - Healer Ability
		_certificateClass.put(97, 5);
		_certificateClass.put(30, 5);
		_certificateClass.put(105, 5);
		_certificateClass.put(43, 5);
		_certificateClass.put(112, 5);
		_certificateClass.put(14, 6); // 10286 Certificate - Summoner Ability
		_certificateClass.put(96, 6);
		_certificateClass.put(28, 6);
		_certificateClass.put(104, 6);
		_certificateClass.put(41, 6);
		_certificateClass.put(111, 6);
		_certificateClass.put(17, 7); // 10287 Certificate - Enchanter Ability
		_certificateClass.put(98, 7);
		_certificateClass.put(21, 7);
		_certificateClass.put(100, 7);
		_certificateClass.put(34, 7);
		_certificateClass.put(107, 7);
		_certificateClass.put(52, 7);
		_certificateClass.put(116, 7);
		_certificateClass.put(51, 7);
		_certificateClass.put(115, 7);
		_certificateClass.put(135, 7);
		_certificateClass.put(136, 7);

		_certificateItems.put(1, 10281);
		_certificateItems.put(2, 10282);
		_certificateItems.put(3, 10283);
		_certificateItems.put(4, 10284);
		_certificateItems.put(5, 10285);
		_certificateItems.put(6, 10286);
		_certificateItems.put(7, 10287);

		_certificateTransforms.put(1, 10289);
		_certificateTransforms.put(2, 10288);
		_certificateTransforms.put(3, 10290);
		_certificateTransforms.put(4, 10292);
		_certificateTransforms.put(5, 10291);
		_certificateTransforms.put(6, 10294);
		_certificateTransforms.put(7, 10293);
	}

	public L2SubClass()
	{
	}

	public short getClassId()
	{
		return _class;
	}

	public long getExp()
	{
		return _exp;
	}

	public int getSp()
	{
		return _sp;
	}

	public byte getLevel()
	{
		return _level;
	}

	public void setClassId(final short classId)
	{
		_class = classId;
	}

	public void setExp(final long expValue)
	{
		//if(expValue > Experience.LEVEL[_level + 1])
		//	_exp = Experience.LEVEL[_level + 1];
		//else
		_exp = expValue;
	}

	public void setSp(final int spValue)
	{
		_sp = spValue;
	}

	public void setHp(final double hpValue)
	{
		_Hp = hpValue;
	}

	public double getHp()
	{
		return _Hp;
	}

	public void setMp(final double mpValue)
	{
		_Mp = mpValue;
	}

	public double getMp()
	{
		return _Mp;
	}

	public void setCp(final double cpValue)
	{
		_Cp = cpValue;
	}

	public double getCp()
	{
		return _Cp;
	}

	public void setLevel(byte levelValue)
	{
		if(levelValue > (isBase() ? Experience.getMaxLevel() : Experience.getMaxSubLevel()))
			levelValue = (byte) (isBase() ? Experience.getMaxLevel() : Experience.getMaxSubLevel());
		else if(levelValue < 40 && !_isBase)
			levelValue = 40;

		_level = levelValue;
	}

	public void incLevel()
	{
		if(_level > (isBase() ? Experience.getMaxLevel() : Experience.getMaxSubLevel()))
			return;

		_level++;
	}

	public void decLevel()
	{
		if(_level == 40 && !_isBase)
			return;

		_level--;
	}

	public void setActive(final boolean active)
	{
		_active = active;
	}

	public boolean isActive()
	{
		return _active;
	}

	public void setBase(final boolean base)
	{
		_isBase = base;
	}

	public boolean isBase()
	{
		return _isBase;
	}

	public DeathPenalty getDeathPenalty()
	{
		if(_dp == null)
			_dp = new DeathPenalty(_player, (byte) 0);
		return _dp;
	}

	public void setDeathPenalty(DeathPenalty dp)
	{
		_dp = dp;
	}

	public void setPlayer(L2Player player)
	{
		_player = player;
	}

	public void setSlot(byte slot)
	{
		_slot = slot;
	}

	public byte getSlot()
	{
		return _slot;
	}

	public int getCertificateItemId()
	{
		return _certificateClass.containsKey((int) _class) ? _certificateItems.get(_certificateClass.get((int) _class)) : 0;
	}

	public int getCertificateTransform()
	{
		return _certificateClass.containsKey((int) _class) ? _certificateTransforms.get(_certificateClass.get((int) _class)) : 0;
	}

	public static boolean isInOccupationGroup(OccupationGroup group, L2Player player)
	{
		int classId = player.getActiveClass();
		return _certificateClass.containsKey(classId) && _certificateClass.get(classId) == group.ordinal();
	}
}