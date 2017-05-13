package ru.l2gw.gameserver.serverpackets;

public class SocialAction extends L2GameServerPacket
{
	private final int _playerId;
	private final int _actionId;

	public static enum SocialType
	{
		GREETING(2),
		VICTORY(3),
		ADVANCE(4),
		NO(5),
		YES(6),
		BOW(7),
		UNAWARE(8),
		WAITING(9),
		LAUGH(10),
		APPLAUD(11),
		DANCE(12),
		SORROW(13),
		CHARM(14),
		SHYNESS(15),
		EXCHANGE_BOWS(16),
		HIGH_FIVE(17),
		COUPLE_HIGH_FIVE(18),
		AWAKENING(20),
		REAWAKENING (21), // - Привзлетает и вспыхивает красным туманом.
		PROPOSE(28),
		PROVOKE(29),
		BEAUTY_SHOP_BUY(30),
		BEAUTY_SHOP_REVERT(31),
		BEAUTY(33),
		GIVE_HERO(20016),
		LEVEL_UP(2122);
		
		private int _id;

		private SocialType(int id)
		{
			_id = id;
		}

		public int getId()
		{
			return _id;
		}
	}

	/**
	 * 0x27 SocialAction         dd
	 */
	public SocialAction(int playerId, SocialType socialType)
	{
		_playerId = playerId;
		_actionId = socialType.getId();

	}

	public SocialAction(int playerId, int actionId)
	{
		_playerId = playerId;
		_actionId = actionId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x27);
		writeD(_playerId);
		writeD(_actionId);
		writeD(0); // ??? 0
	}
}