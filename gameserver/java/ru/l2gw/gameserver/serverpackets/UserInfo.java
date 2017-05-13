package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2Skill.AbnormalVisualEffect;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2CubicInstance;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.util.Location;

public class UserInfo extends L2GameServerPacket
{
	public static final byte[] PAPERDOLL_ORDER = {
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_REAR,
		Inventory.PAPERDOLL_LEAR,
		Inventory.PAPERDOLL_NECK,
		Inventory.PAPERDOLL_RFINGER,
		Inventory.PAPERDOLL_LFINGER,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_BACK,
		Inventory.PAPERDOLL_LRHAND,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_DHAIR,
		Inventory.PAPERDOLL_RBRACELET,
		Inventory.PAPERDOLL_LBRACELET,
		Inventory.PAPERDOLL_DECO1,
		Inventory.PAPERDOLL_DECO2,
		Inventory.PAPERDOLL_DECO3,
		Inventory.PAPERDOLL_DECO4,
		Inventory.PAPERDOLL_DECO5,
		Inventory.PAPERDOLL_DECO6,
		Inventory.PAPERDOLL_BELT
	};
	
	private boolean can_writeImpl = false, partyRoom, isFlying;
	private boolean weaponEquipped;
	private final L2Player _player;
	private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd, _relation;
	private float move_speed, attack_speed, col_radius, col_height, _perExp;
	private PcInventory _inv;
	private Location _loc, _fishLoc;
	private int obj_id, vehicle_obj_id, _race, sex, base_class, level, curCp, maxCp, _enchant;
	private long _exp;
	private int curHp, maxHp, curMp, maxMp, curLoad, maxLoad, rec_left, rec_have;
	private int _str, _con, _dex, _int, _wit, _men, _sp, ClanPrivs, InventoryLimit;
	private int _patk, _patkspd, _pdef, evasion, accuracy, crit, _matk, _matkspd, _mevasion, _maccuracy, _mCritRate;
	private int _mdef, pvp_flag, karma, hair_style, hair_color, face, gm_commands;
	private int clan_id, clan_crest_id, ally_id, ally_crest_id, large_clan_crest_id;
	private int private_store, can_crystalize, pk_kills, pvp_kills, class_id, agathion;
	private int team, abnormalEffect, abnormalEffect2, noble, hero, fishing, mount_id, cw_level;
	private int name_color, running, pledge_class, pledge_type, title_color, transformation;
	private int DefenceFire, DefenceWater, DefenceWind, DefenceEarth, DefenceHoly, DefenceUnholy;
	private byte mount_type;
	private String _name, title;
	private GArray<Integer> _cubics;
	private GArray<AbnormalVisualEffect> _abnormalEffectsArray;
	private int[] attackElement;
	private int fame, vitality, talismans, cloak;
	private int _territoryId, _battlefieldPenalty, _disguised;

	public UserInfo(L2Player player)
	{
		_player = player;
	}

	@Override
	final public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		if(!player.equals(_player))
			return;

		if(_player.isCursedWeaponEquipped())
		{
			_name = _player.getTransformationName();
			clan_crest_id = 0;
			ally_crest_id = 0;
			large_clan_crest_id = 0;
			cw_level = CursedWeaponsManager.getInstance().getLevel(_player.getCursedWeaponEquippedId());
		}
		else
		{
			_name = _player.getName();
			clan_crest_id = _player.getClanCrestId();
			ally_crest_id = _player.getAllyCrestId();
			large_clan_crest_id = _player.getClanCrestLargeId();
			cw_level = 0;
		}

		if(_player.getMountEngine().isMounted())
		{
			_enchant = 0;
			if(_player.getTransformation() != 0)
			{
				mount_id = 0;
				mount_type = 0;
			}
			else
			{
				mount_id = _player.getMountEngine().getMountNpcId() + 1000000;
				mount_type = (byte) _player.getMountEngine().getMountType();
			}
		}
		else
		{
			_enchant = (byte) _player.getEnchantEffect();
			mount_id = 0;
			mount_type = 0;
		}
		weaponEquipped = _player.getActiveWeaponInstance() != null;
		move_speed = _player.getMovementSpeedMultiplier();
		_runSpd = _player.getTemplate().baseRunSpd;
		_walkSpd = _player.getTemplate().baseWalkSpd;
		_flRunSpd = _flyRunSpd = _runSpd;
		_flWalkSpd = _flyWalkSpd = _walkSpd;
		_swimRunSpd = (int) _player.getSwimSpeed();
		_swimWalkSpd = (int) _player.getSwimSpeed();
		
		_inv = _player.getInventory();
		_relation = _player.isClanLeader() ? 0x40 : 0;

		if(_player.getSiegeState() == 1)
			_relation |= 0x180;
		else if(_player.getSiegeState() == 2)
			_relation |= 0x80;
		else if(_player.getSiegeState() == 3)
			_relation |= 0x1000;

		_loc = _player.getLoc();
		obj_id = _player.getObjectId();
		vehicle_obj_id = _player.isInBoat() ? _player.getVehicle().getObjectId() : 0x00;
		_race = _player.getRace().ordinal();
		sex = _player.getSex();
		base_class = _player.getBaseClass();
		level = _player.getLevel();
		_exp = _player.getExp();
		_perExp = Experience.getExpPercent(_player.getLevel(), _player.getExp());
		_str = _player.getSTR();
		_dex = _player.getDEX();
		_con = _player.getCON();
		_int = _player.getINT();
		_wit = _player.getWIT();
		_men = _player.getMEN();
		curHp = (int) _player.getCurrentHp();
		maxHp = _player.getMaxHp();
		curMp = (int) _player.getCurrentMp();
		maxMp = _player.getMaxMp();
		curLoad = _player.getCurrentLoad();
		maxLoad = _player.getMaxLoad();
		_sp = _player.getSp();
		_patk = _player.getPAtk(null);
		_patkspd = _player.getPAtkSpd();
		_pdef = _player.getPDef(null);
		evasion = _player.getEvasionRate(null);
		_mevasion = player.getMEvasionRate(null);
		_maccuracy = player.getMAccuracy();
		_mCritRate = player.getMagicCriticalRate(null, null);
		accuracy = _player.getAccuracy();
		crit = _player.getCriticalHit(null, null);
		_matk = _player.getMAtk(null, null);
		_matkspd = _player.getMAtkSpd();
		_mdef = _player.getMDef(null, null);
		pvp_flag = _player.getPvpFlag(); // 0=white, 1=purple, 2=purpleblink
		karma = _player.getKarma();
		attack_speed = _player.getAttackSpeedMultiplier();
		col_radius = _player.getColRadius();
		col_height = _player.getColHeight();
		hair_style = _player.getHairStyle();
		hair_color = _player.getHairColor();
		face = _player.getFace();
		gm_commands = AdminTemplateManager.checkBoolean("useCommands", player) || Config.ALLOW_SPECIAL_COMMANDS ? 1 : 0;
		// builder level активирует в клиенте админские команды
		title = _player.getTitle();
		if(_player.isInvisible())
			title = "Invisible";
		if(_player.isPolymorphed() && NpcTable.getTemplate(_player.getPolyid()) != null)
			title += " - " + NpcTable.getTemplate(_player.getPolyid()).name;
		clan_id = _player.getClanId();
		ally_id = _player.getAllyId();
		private_store = _player.getPrivateStoreType();
		can_crystalize = _player.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE) > 0 ? 1 : 0;
		pk_kills = _player.getPkKills();
		pvp_kills = _player.getPvpKills();
		_cubics = new GArray<Integer>(3);
		for(L2CubicInstance cub : _player.getCubics())
			if(cub != null)
				_cubics.add(cub.getId());
		abnormalEffect = _player.getAbnormalEffect();
		abnormalEffect2 = _player.getAbnormalEffect2();
		_abnormalEffectsArray = _player.getAbnormalEffects();
		ClanPrivs = _player.getClanPrivileges();
		rec_left = _player.getRecSystem().getRecommendsLeft(); //c2 recommendations remaining
		rec_have = _player.getRecSystem().getRecommendsHave(); //c2 recommendations received
		InventoryLimit = _player.getInventoryLimit();
		class_id = _player.getClassId().getId();
		maxCp = _player.getMaxCp();
		curCp = (int) _player.getCurrentCp();
		team = _player.getTeam(); //team circle around feet 1= Blue, 2 = red
		noble = _player.isNoble() || _player.isGM() && Config.GM_HERO_AURA ? 1 : 0; //0x01: symbol on char menu ctrl+I
		hero = _player.isHero() || _player.isGM() && Config.GM_HERO_AURA ? 1 : 0; //0x01: Hero Aura and symbol
		fishing = _player.isFishing() ? 1 : 0; // Fishing Mode
		_fishLoc = _player.getFishLoc();
		name_color = _player.getNameColor();
		running = _player.isRunning() ? 0x01 : 0x00; //changes the Speed display on Status Window
		pledge_class = _player.getPledgeRank();
		pledge_type = _player.getPledgeType();
		title_color = _player.getTitleColor();
		transformation = _player.getTransformation();
		attackElement = _player.getAttackElement();
		DefenceFire = _player.getDefenceFire();
		DefenceWater = _player.getDefenceWater();
		DefenceWind = _player.getDefenceWind();
		DefenceEarth = _player.getDefenceEarth();
		DefenceHoly = _player.getDefenceHoly();
		DefenceUnholy = _player.getDefenceDark();
		agathion = _player.getAgathionId();
		fame = _player.getFame();
		partyRoom = PartyRoomManager.getInstance().isLeader(_player);
		isFlying = _player.isInFlyingTransform();
		_territoryId = _player.getTerritoryId();
		vitality = _player.getVitality().getPoints();
		//_player.refreshSavedStats();
		talismans = _player.getInventory().getAllowedTalismans();
		cloak = _player.isStatActive(Stats.CLOAK) ? 1 : 0;
		_battlefieldPenalty = _player.getEffectBySkillId(L2Skill.SKILL_BATTLEFIELD_PENALTY) != null ? 0 : 1;
		_disguised = _player.getVarInt("disguised");
		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0x32);
		
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(vehicle_obj_id);
		writeD(obj_id);
		writeS(_name);
		writeD(_race);
		writeD(sex);
		writeD(base_class);
		writeD(level);
		writeQ(_exp);
		writeF(_perExp);
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
		writeD(weaponEquipped ? 40 : 20);

		for(byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollObjectId(PAPERDOLL_ID));

		for(byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollItemId(PAPERDOLL_ID));

		for(byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollAugmentationId(PAPERDOLL_ID));

		writeD(talismans);
		writeD(cloak);

		//writeH(0x00) //Lindvior 526
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
		writeD(0x00); // wtf???
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
		
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
		writeD(_mevasion);
		writeD(_maccuracy);
		writeD(_mCritRate);
		writeD(pvp_flag);
		writeD(karma);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd); // swimspeed
		writeD(_swimWalkSpd); // swimspeed
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
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
		writeD(ally_crest_id);
		// 0x40 leader rights
		// siege flags: attacker - 0x180 sword over name, defender - 0x80 shield, 0xC0 crown (|leader), 0x1C0 flag (|leader)
		// shield, 0xC0 crown (|leader), 0x1C0 flag (|leader)
		writeD(_relation);
		writeC(mount_type); // mount type
		writeC(private_store);
		writeC(can_crystalize);
		writeD(pk_kills);
		writeD(pvp_kills);
		writeH(_cubics.size());
		while(_cubics.size() > 0)
			writeH(_cubics.removeFirst());
		writeC(partyRoom ? 0x01 : 0x00); //1-find party members
		writeC(isFlying ? 0x02 : 0x00);
		writeD(ClanPrivs);
		writeH(rec_left);
		writeH(rec_have);
		writeD(mount_id);
		writeH(InventoryLimit);
		writeD(class_id);
		writeD(0x00); // special effects? circles around player...
		writeD(maxCp);
		writeD(curCp);
		writeC(_enchant);
		writeC(team);
		writeD(large_clan_crest_id);
		writeC(noble);
		writeC(hero);
		writeC(0x00);
		writeD(_fishLoc.getX());
		writeD(_fishLoc.getY());
		writeD(_fishLoc.getZ());
		writeD(name_color);
		writeC(running);
		writeD(pledge_class);
		writeD(pledge_type);
		writeD(title_color);
		writeD(cw_level);
		writeD(transformation); // Transformation id
		// AttackElement (0 - Fire, 1 - Water, 2 - Wind, 3 - Earth, 4 - Holy, 5 - Dark, -2 - None)
		writeH(attackElement == null ? -2 : attackElement[0]);
		writeH(attackElement == null ? 0 : attackElement[1]); // AttackElementValue
		writeH(DefenceFire); // DefAttrFire
		writeH(DefenceWater); // DefAttrWater
		writeH(DefenceWind); // DefAttrWind
		writeH(DefenceEarth); // DefAttrEarth
		writeH(DefenceHoly); // DefAttrHoly
		writeH(DefenceUnholy); // DefAttrUnholy
		writeD(agathion);

		writeD(fame); // Fame points
		writeD(0x01); // Dark Icon Battle Field Penalty

		writeD(vitality); // Vitality Points

		writeD(0x00);// Unknown GOD
		writeD(0x00);// Unknown GOD (1 - Party searching?)
		writeC(0x00);// Unknown GOD
		writeD(0x00);// Unknown GOD
		
		writeD(_abnormalEffectsArray.size());
		for (AbnormalVisualEffect ae : _abnormalEffectsArray)
			writeD(ae.getVisualMask(ae));
		
		writeD(0x00);

		writeC(0); // Tauti
	}
}