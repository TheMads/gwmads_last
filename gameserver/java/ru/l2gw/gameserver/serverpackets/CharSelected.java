package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

public class CharSelected extends L2GameServerPacket
{
	//   SdSddddddddddffddddddddddddddddddddddddddddddddddddddddd d
	private int _sessionId, char_id, clan_id, sex, race, class_id;
	private String _name, _title;
	private Location _loc;
	private double curHp, curMp;
	private int _sp, level, karma, _int, _str, _con, _men, _dex, _wit, _pk;
	private long _exp;
	private boolean _isActive = false;

	public CharSelected(final L2Player player, final int sessionId)
	{
		_sessionId = sessionId;

		_name = player.getName();
		char_id = player.getCharId(); // ??
		_title = player.getTitle();
		clan_id = player.getClanId();
		sex = player.getSex();
		race = player.getRace().ordinal();
		class_id = player.getClassId().getId();
		_loc = player.getLoc();
		curHp = player.getCurrentHp();
		curMp = player.getCurrentMp();
		_sp = player.getSp();
		_exp = player.getExp();
		level = player.getLevel();
		karma = player.getKarma();
		_pk = player.getPkKills();
		_int = player.getINT();
		_str = player.getSTR();
		_con = player.getCON();
		_men = player.getMEN();
		_dex = player.getDEX();
		_wit = player.getWIT();
		_isActive = true;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x0B);
		
		writeS(_name);
		writeD(char_id);
		writeS(_title);
		writeD(_sessionId);
		writeD(clan_id);
		writeD(0x00); // ??
		writeD(sex);
		writeD(race);
		writeD(class_id);
		writeD(_isActive ? 0x01 : 0x00);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());

		writeF(curHp);
		writeF(curMp);
		writeD(_sp);
		writeQ(_exp);
		writeD(level);
		writeD(karma); // ?
		writeD(_pk);
		writeD(_int);
		writeD(_str);
		writeD(_con);
		writeD(_men);
		writeD(_dex);
		writeD(_wit);
		for(int i = 0; i < 30; i++)
			writeD(0x00);

		writeF(0x00); //c3  work
		writeF(0x00); //c3  work
		// extra info
		writeD(GameTimeController.getInstance().getGameTime() % (24 * 60)); // in-game time
		writeD(0x00); //
		writeD(0x00); //c3
		writeC(0x00); //c3 InspectorBin
		writeH(0x00); //c3
		writeH(0x00); //c3
		writeD(0x00); //c3
		//writeD(0x00); //c3 InspectorBin for 528 client
		//writeD(0x00); //c3
		//writeD(0x00); //c3
		//writeD(0x00); //c3
		//writeD(0x00); //c3
		//writeD(0x00); //c3
		//writeD(0x00); //c3
		//writeD(0x00); //c3
		//writeD(0x00); //c3

		//writeD(0x00); //c5
		//writeD(0x00); //c5
		//writeD(0x00); //c5
	}
}