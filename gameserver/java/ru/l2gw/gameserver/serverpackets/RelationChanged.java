package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

public class RelationChanged extends L2GameServerPacket
{
	public static final int RELATION_PARTY1       = 0x00001; // party member
	public static final int RELATION_PARTY2       = 0x00002; // party member
	public static final int RELATION_PARTY3       = 0x00004; // party member
	public static final int RELATION_PARTY4       = 0x00008; // party member
	public static final int RELATION_PARTYLEADER  = 0x00010; // true if is party leader
	public static final int RELATION_HAS_PARTY    = 0x00020; // true if is in party
	public static final int RELATION_CLAN_MEMBER  = 0x00040; // true if is in clan
	public static final int RELATION_LEADER 	  = 0x00080; // true if is clan leader
	public static final int RELATION_CLAN_MATE    = 0x00100; // true if is in same clan
	public static final int RELATION_INSIEGE   	  = 0x00200; // true if in siege
	public static final int RELATION_ATTACKER     = 0x00400; // true when attacker
	public static final int RELATION_ALLY         = 0x00800; // blue siege icon, cannot have if red
	public static final int RELATION_ENEMY        = 0x01000; // true when red icon, doesn't matter with blue
	public static final int RELATION_MUTUAL_WAR   = 0x04000; // double fist
	public static final int RELATION_1SIDED_WAR   = 0x08000; // single fist
	public static final int RELATION_ALLY_MEMBER  = 0x10000; // clan is in alliance
	public static final int RELATION_TERRITORY_WAR= 0x80000; // show Territory War icon
	public static final int RELATION_ATTACKABLE   = 0x100000; // Custom for internal use


	private boolean _isAutoAttackable;
	private int _relation, _karma, _pvpFlag;
	private int _objectId = 0;

	public RelationChanged(L2Player player, int relation)
	{
		_isAutoAttackable = (relation & RELATION_ATTACKABLE) == RELATION_ATTACKABLE;
		_relation = relation & 0xFFFFF; // Discard custom attackable flag
		_objectId = player.getObjectId();
		_karma = player.getKarma();
		_pvpFlag = player.getPvpFlag();
	}

	@Override
	protected final void writeImpl()
	{
		if(_objectId == 0)
			return;

		writeC(0xCE);
		writeD(0x01); // count
		writeD(_objectId);
		writeD(_relation);
		writeD(_isAutoAttackable ? 1 : 0);
		writeD(_karma);
		writeD(_pvpFlag);
	}
}