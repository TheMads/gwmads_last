package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;

/**
 * sample
 * 06 8f19904b 2522d04b 00000000 80 950c0000 4af50000 08f2ffff 0000    - 0 damage (missed 0x80)
 * 06 85071048 bc0e504b 32000000 10 fc41ffff fd240200 a6f5ffff 0100 bc0e504b 33000000 10                                     3....

 * format
 * dddc dddh (ddc)
 */
public class Attack extends L2GameServerPacket
{

	/*
	 * TODO: Aweking
	 * 0x00 >> обычный удар 
	 * 0x02 >> увернулся 
	 * 0x04 >> крит. удар 
	 * 0x06 >> заблокирован удар 
	 * 0x08 >> удар с соской 
	 * 0x0a >> обычный удар с соской 
	 * 0x0b >> промах 
	 * 0x0c >> критический удар с соской 
	 * 0x0d >> большая надпись, удара нет 
	 * 0x0e >> тоже, что и 0x0a, но есть большая надпись 
	 */
	 
	private static final int FLAG = 0x00; // Обычный удар без надписей.
	private static final int FLAG_SHIELD = 0x02; // Заблокировал Крит.
	private static final int FLAG_MISS = 0x01; // Увернулся от удара
	private static final int FLAG_CRIT = 0x04; // Крит.
	private static final int FLAG_SOULSHOT = 0x08; // Удар с соской.
	
	protected class Hit
	{
		public int _targetId;
		public int _damage;
		public int _flags;

		Hit(L2Object target, int damage, boolean miss, boolean crit, boolean shld)
		{
			_targetId = target.getObjectId();
			_damage = damage;
			if(_soulshot)
				_flags |= FLAG_SOULSHOT;
			if(crit)
				_flags = FLAG_CRIT;
			if(shld)
				_flags = FLAG_SHIELD;
			if(miss)
				_flags = FLAG_MISS;

		}
	}

	public final int _attackerId;
	public final boolean _soulshot;
	protected int _grade;
	protected int _x, _y, _z, _tx, _ty, _tz;
	protected Hit[] hits;

	/**
	 * @param attacker the attacker L2Character
	 * @param ss true if useing SoulShots
	 */
	public Attack(L2Character attacker, L2Character target, boolean ss, int grade)
	{
		_attackerId = attacker.getObjectId();
		_soulshot = ss;
		_grade = grade;
		_x = attacker.getX();
		_y = attacker.getY();
		_z = attacker.getZ();
		_tx = target.getX();
		_ty = target.getY();
		_tz = target.getZ();
		hits = new Hit[0];
	}

	/**
	 * Add this hit (target, damage, miss, critical, shield) to the Server-Client packet Attack.<BR><BR>
	 */
	public void addHit(L2Object target, int damage, boolean miss, boolean crit, boolean shld)
	{
		// Get the last position in the hits table
		int pos = hits.length;

		// Create a new Hit object
		Hit[] tmp = new Hit[pos + 1];

		// Add the new Hit object to hits table
		System.arraycopy(hits, 0, tmp, 0, hits.length);
		tmp[pos] = new Hit(target, damage, miss, crit, shld);
		hits = tmp;
	}

	/**
	 * Return True if the Server-Client packet Attack conatins at least 1 hit.<BR><BR>
	 */
	public boolean hasHits()
	{
		return hits.length > 0;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x33);

		writeD(_attackerId);
		writeD(hits[0]._targetId);
		writeC(0x00); // L2WT GOD
		writeD(hits[0]._damage);
		writeD(hits[0]._flags);
		writeD(_grade);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeH(hits.length - 1);
		for(int i = 1; i < hits.length; i++)
		{
			writeD(hits[i]._targetId);
			writeD(hits[i]._damage);
			writeC(hits[i]._flags);
			writeD(_grade);
		}
		writeD(_tx);
		writeD(_ty);
		writeD(_tz);
	}
}
