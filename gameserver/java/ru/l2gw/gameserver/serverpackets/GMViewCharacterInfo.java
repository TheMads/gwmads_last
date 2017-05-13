package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.util.Location;

//dddddSddddQddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhddddddddddddddddddddffffddddSdddcccddhhddddccdccdddddddddd
public class GMViewCharacterInfo extends L2GameServerPacket
{
	private Location _loc;
	private PcInventory _inv;
	private int obj_id, _race, _sex, class_id, pvp_flag, karma, level, mount_type;
	private int _str, _con, _dex, _int, _wit, _men, _sp;
	private int curHp, maxHp, curMp, maxMp, curCp, maxCp, curLoad, maxLoad, rec_left, rec_have;
	private int _patk, _patkspd, _pdef, evasion, accuracy, crit, _matk, _matkspd;
	private int _mdef, hair_style, hair_color, face, gm_commands;
	private int clan_id, clan_crest_id, ally_id, title_color;
	private int noble, hero, private_store, name_color, pk_kills, pvp_kills;
	private int _runSpd, _walkSpd, _swimSpd, DwarvenCraftLevel, running, pledge_class, talismans, cloak;
	private String _name, title;
	private long _exp;
	private float move_speed, attack_speed, col_radius, col_height, _expPercent;
	private int[] attackElement;
	private int DefenceFire, DefenceWater, DefenceWind, DefenceEarth, DefenceHoly, DefenceUnholy, fame, vitality;

	public GMViewCharacterInfo(final L2Player player)
	{
		_inv = player.getInventory();
		_loc = player.getLoc();
		obj_id = player.getObjectId();
		_name = player.getName();
		_race = player.getRace().ordinal();
		_sex = player.getSex();
		class_id = player.getClassId().getId();
		level = player.getLevel();
		_exp = player.getExp();
		_expPercent = Experience.getExpPercent(player.getLevel(), player.getExp());
		_str = player.getSTR();
		_dex = player.getDEX();
		_con = player.getCON();
		_int = player.getINT();
		_wit = player.getWIT();
		_men = player.getMEN();
		curHp = (int) player.getCurrentHp();
		maxHp = player.getMaxHp();
		curMp = (int) player.getCurrentMp();
		maxMp = player.getMaxMp();
		_sp = player.getSp();
		curLoad = player.getCurrentLoad();
		maxLoad = player.getMaxLoad();
		_patk = player.getPAtk(null);
		_patkspd = player.getPAtkSpd();
		_pdef = player.getPDef(null);
		evasion = player.getEvasionRate(null);
		accuracy = player.getAccuracy();
		crit = player.getCriticalHit(null, null);
		_matk = player.getMAtk(null, null);
		_matkspd = player.getMAtkSpd();
		_mdef = player.getMDef(null, null);
		pvp_flag = player.getPvpFlag();
		karma = player.getKarma();
		_runSpd = (int) player.getRunSpeed();
		_walkSpd = (int) player.getWalkSpeed();
		_swimSpd = (int) player.getSwimSpeed();
		move_speed = player.getMovementSpeedMultiplier();
		attack_speed = player.getAttackSpeedMultiplier();
		mount_type = player.getMountEngine().getMountType();
		col_radius = player.getColRadius();
		col_height = player.getColHeight();
		hair_style = player.getHairStyle();
		hair_color = player.getHairColor();
		face = player.getFace();
		gm_commands = player.isGM() ? 1 : 0;
		title = player.getTitle();
		clan_id = player.getClanId();
		clan_crest_id = player.getClanCrestId(); //clan crest
		ally_id = player.getAllyId();
		private_store = player.getPrivateStoreType();
		DwarvenCraftLevel = Math.max(player.getSkillLevel(1320), 0);
		pk_kills = player.getPkKills();
		pvp_kills = player.getPvpKills();
		rec_left = player.getRecSystem().getRecommendsLeft(); //c2 recommendations remaining
		rec_have = player.getRecSystem().getRecommendsHave(); //c2 recommendations received
		talismans = player.getInventory().getAllowedTalismans();
		cloak = player.isStatActive(Stats.CLOAK) ? 1 : 0;
		curCp = (int) player.getCurrentCp();
		maxCp = player.getMaxCp();
		running = player.isRunning() ? 0x01 : 0x00;
		pledge_class = player.getPledgeRank();
		noble = player.isNoble() ? 1 : 0; //0x01: symbol on char menu ctrl+I
		hero = player.isHero() ? 1 : 0; //0x01: Hero Aura and symbol
		name_color = player.getNameColor();
		title_color = player.getTitleColor();
		attackElement = player.getAttackElement();
		DefenceFire = player.getDefenceFire();
		DefenceWater = player.getDefenceWater();
		DefenceWind = player.getDefenceWind();
		DefenceEarth = player.getDefenceEarth();
		DefenceHoly = player.getDefenceHoly();
		DefenceUnholy = player.getDefenceDark();
		fame = player.getFame();
		vitality = player.getVitality().getPoints();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x95);

		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(_loc.getHeading());
		writeD(obj_id);
		writeS(_name);
		writeD(_race);
		writeD(_sex);
		writeD(class_id);
		writeD(level);
		writeQ(_exp);
		writeF(_expPercent);
		writeD(_str);
		writeD(_dex);
		writeD(_con);
		writeD(_int);
		writeD(_wit);
		writeD(_men);
		writeD(maxHp);
		writeD(curHp);
		writeD(maxMp);
		writeD(curMp);
		writeD(_sp);
		writeD(curLoad);
		writeD(maxLoad);
		writeD(pk_kills);

		for(int PAPERDOLL_ID : Inventory.PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollObjectId(PAPERDOLL_ID));

		for(int PAPERDOLL_ID : Inventory.PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollItemId(PAPERDOLL_ID));

		for(int PAPERDOLL_ID : Inventory.PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollAugmentationId(PAPERDOLL_ID));

		writeD(talismans);
		writeD(cloak);
		writeD(_patk);
		writeD(_patkspd);
		writeD(_pdef);
		writeD(evasion);
		writeD(accuracy);
		writeD(crit);
		writeD(_matk);
		writeD(_matkspd);
		writeD(_patkspd);
		writeD(_mdef);
		writeD(pvp_flag);
		writeD(karma);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimSpd); // swimspeed
		writeD(_swimSpd); // swimspeed
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeF(move_speed);
		writeF(attack_speed);
		writeF(col_radius);
		writeF(col_height);
		writeD(hair_style);
		writeD(hair_color);
		writeD(face);
		writeD(gm_commands);
		writeS(title);
		writeD(clan_id);
		writeD(clan_crest_id);
		writeD(ally_id);
		writeC(mount_type);
		writeC(private_store);
		writeC(DwarvenCraftLevel); //_player.getDwarvenCraftLevel() > 0 ? 1 : 0
		writeD(pk_kills);
		writeD(pvp_kills);
		writeH(rec_left);
		writeH(rec_have); //Blue value for name (0 = white, 255 = pure blue)
		writeD(class_id);
		writeD(0x00); // special effects? circles around player...
		writeD(maxCp);
		writeD(curCp);
		writeC(running); //changes the Speed display on Status Window
		writeC(321);
		writeD(pledge_class); //changes the text above CP on Status Window
		writeC(noble);
		writeC(hero);
		writeD(name_color);
		writeD(title_color);

		writeH(attackElement == null ? -2 : attackElement[0]);
		writeH(attackElement == null ? 0 : attackElement[1]);
		writeH(DefenceFire);
		writeH(DefenceWater);
		writeH(DefenceWind);
		writeH(DefenceEarth);
		writeH(DefenceHoly);
		writeH(DefenceUnholy);

		writeD(fame);
		writeD(vitality);
	}
}