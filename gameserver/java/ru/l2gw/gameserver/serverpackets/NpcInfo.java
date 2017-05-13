package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.instances.*;
import ru.l2gw.gameserver.model.L2Skill.AbnormalVisualEffect;
import ru.l2gw.util.Location;

public class NpcInfo extends L2GameServerPacket
{
	//   ddddddddddddddddddffffdddcccccSSddd dddddc
	private boolean can_writeImpl = false;
	private L2Character _cha;
	private L2Summon _summon;
	private int _npcObjId, _npcId, running, incombat, dead, team, _showNameTag = 1;
	private int _currentHP, _currentMP, _currentCP, _maxHP, _maxMP, _maxCP;
	private int _runSpd, _walkSpd, _mAtkSpd, _pAtkSpd, _rhand, _lhand;
	private int karma, pvp_flag, _abnormalEffect, _abnormalEffect2, clan_crest_id, ally_crest_id, clan_id, ally_id, _enchantEffect;
	private float colHeight, colRadius, growHeight, growRadius;
	//private int _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	private boolean _isAttackable;
	private Location _loc, decoy_fishLoc;
	private String _name = "";
	private String _title = "";
	private boolean _isShowSpawnAnimation = false;
	private boolean _invis = false;
	private int _nameStringId, _titleStringId;
	private int _state;
	private GArray<AbnormalVisualEffect> _abnormalEffectsArray;

	private Inventory decoy_inv;
	private int decoy_race, decoy_sex, decoy_base_class, decoy_clan_id, decoy_ally_id, type;
	private int decoy_noble, decoy_hair_style, decoy_hair_color, decoy_face, decoy_sitting;
	private int decoy_invis, decoy_rec_have, decoy_rec_left, decoy_class_id, decoy_large_clan_crest_id;
	private int decoy_enchant, decoy_PledgeClass, decoy_pledge_type;
	private int decoy_NameColor, decoy_TitleColor, decoy_Transformation, decoy_Agathion, decoy_battlefieldPenalty;
	private int decoy_hero, decoy_mount_id, decoy_swimSpd, decoy_cw_level, decoy_clan_rep_score;
	private byte decoy_mount_type, decoy_private_store, decoy_fishing;
	private double decoy_move_speed, decoy_attack_speed;
	private FastList<L2CubicInstance> decoy_cubics;
	private boolean isFlying = false;

	public NpcInfo(L2NpcInstance cha, L2Character attacker)
	{
		if(cha == null)
			return;

		_cha = cha;
		_npcId = cha.getDisplayId() != 0 ? cha.getDisplayId() : cha.getTemplate().npcId;
		_isAttackable = cha instanceof L2FakeTowerInstance || cha.isAttackable(attacker, false, false);
		_rhand = cha.getRightHandItem();
		_lhand = cha.getLeftHandItem();
		_enchantEffect = cha.getWeaponEnchant();
		if(Config.SERVER_SIDE_NPC_NAME || cha.getDisplayId() != 0)
			_name = cha.getName();
		if(Config.SERVER_SIDE_NPC_TITLE || cha.getDisplayId() != 0 || cha.isTrap())// || cha.getTitle() != null && !cha.getTitle().isEmpty())
			_title = cha.getTitle();
		_nameStringId = cha.getNameFStringId();
		_titleStringId = cha.getTitleFStringId();
		_state = cha.getNpcState();
		if(cha.isChampion() > 0)
		{
			_title = "Champion";
			if(Config.ALT_CHAMPION_SHOW_AURA)
				team = cha.isChampion();
		}
		_isShowSpawnAnimation = cha.isShowSpawnAnimation();
		_invis = cha.isHide();
		isFlying = _cha.isFlying();
		_showNameTag = cha.getShowNameTag();
		
		_currentHP = (int) cha.getCurrentHp();
		_currentMP = (int) cha.getCurrentMp();
		_currentCP = (int) cha.getCurrentCp();
		_maxHP = cha.getMaxHp();
		_maxMP = cha.getMaxMp();
		_maxCP = cha.getMaxCp();
		
		can_writeImpl = true;
	}

	public NpcInfo(L2Summon summon, L2Character attacker)
	{
		if(summon == null)
			return;
		if(summon.getPlayer() != null && summon.getPlayer().isInvisible())
			return;

		_cha = summon;
		_summon = summon;
		_npcId = summon.getTemplate().npcId;
		_isAttackable = summon.isAttackable(attacker, false, false); //(summon.getKarma() > 0);
		_rhand = 0;
		_lhand = 0;
		if(Config.SERVER_SIDE_NPC_NAME || summon.isPet())
			_name = _cha.getName();
		_title = summon.getTitle();
		_nameStringId = summon.getNameFStringId();
		_titleStringId = summon.getTitleFStringId();

		//TODO: нужен дамп пакета с оффа NpcInfo словленный при трансформированном пете.
		//TODO: writeD(type);  // maybe show great wolf type ?  -  ничего не дает, хотя type корректно шлется.
		if(_summon.getTemplate().getNpcId() == 16025 || // Black Wolf
		_summon.getTemplate().getNpcId() == 16037 // Snow Great Wolf
		)
		{
			if(_summon.getLevel() >= 60 && _summon.getLevel() < 65)
			{
				type = 1;
			}
			if(_summon.getLevel() >= 65 && _summon.getLevel() < 70)
			{
				type = 2;
			}
			if(_summon.getLevel() > 70)
			{
				type = 3;
			}
		}
		if(_summon.getTemplate().getNpcId() == 16041 || // Fenrir
		_summon.getTemplate().getNpcId() == 16042 // Snow Fenrir
		)
		{
			if(_summon.getLevel() >= 70 && _summon.getLevel() < 75)
			{
				type = 1;
			}
			if(_summon.getLevel() >= 75 && _summon.getLevel() < 80)
			{
				type = 2;
			}
			if(_summon.getLevel() >= 85)
			{
				type = 3;
			}
		}

		isFlying = _cha.isFlying();
		can_writeImpl = true;
	}

	public NpcInfo(L2Summon summon, L2Character attacker, boolean isShowSpawnAnimation)
	{
		this(summon, attacker);
		_isShowSpawnAnimation = isShowSpawnAnimation;
	}

	@Override
	final public void runImpl()
	{
		if(!can_writeImpl)
			return;

		if((_cha.getAbnormalEffect() & L2Skill.AbnormalVisualEffect.big_body.mask) == L2Skill.AbnormalVisualEffect.big_body.mask)
		{
			colHeight = _cha.getGrowColHeight();
			colRadius = _cha.getGrowColRadius();
			growHeight = _cha.getColHeight();
			growRadius = _cha.getColRadius();
		}
		else
		{
			colHeight = _cha.getColHeight();
			colRadius = _cha.getColRadius();
			growHeight = _cha.getGrowColHeight();
			growRadius = _cha.getGrowColRadius();
		}

		_npcObjId = _cha.getObjectId();
		_loc = _cha.getLoc();
		_mAtkSpd = _cha.getMAtkSpd();
		clan_id = _cha.getClanId();
		if(clan_id > 0)
		{
			ally_id = _cha.getAllyId();
			clan_crest_id = _cha.getClanCrestId();
			ally_crest_id = _cha.getAllyCrestId();
		}
		isFlying = _cha.isFlying();

		if(_cha instanceof L2DecoyInstance)
			runImpl_Decoy();
		else
		{
			_runSpd = (int) _cha.getRunSpeed();
			_walkSpd = (int) _cha.getWalkSpeed();
			karma = _cha.getKarma();
			pvp_flag = _cha.getPvpFlag();
			_pAtkSpd = _cha.getPAtkSpd();
			running = _cha.isRunning() ? 1 : 0;
			incombat = _cha.isInCombat() ? 1 : 0;
			dead = _cha.isAlikeDead() ? 1 : 0;
			_abnormalEffectsArray = _cha.getAbnormalEffects();
			if(_cha.isChampion() == 0)
				team = _cha.getTeam();
		}
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		if(_cha instanceof L2DecoyInstance)
		{
			writeImpl_Decoy();
			return;
		}

		writeC(0x0c);
		//ddddddddddddddddddffffdddcccccSSddddddddccffdddd
		writeD(_npcObjId);
		writeD(_npcId + 1000000); // npctype id c4
		writeD(_isAttackable ? 1 : 0);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(_loc.getHeading());
		writeD(0x00);
		writeD(_mAtkSpd);
		writeD(_pAtkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_runSpd /*_swimRunSpd*//*0x32*/); // swimspeed
		writeD(_walkSpd/*_swimWalkSpd*//*0x32*/); // swimspeed
		writeD(_runSpd/*_flRunSpd*/);
		writeD(_walkSpd/*_flWalkSpd*/);
		writeD(_runSpd/*_flyRunSpd*/);
		writeD(_walkSpd/*_flyWalkSpd*/);
		writeF(1.1); // взято из клиента
		writeF(_pAtkSpd / 277.47834071f);
		writeF(colRadius);
		writeF(colHeight);
		writeD(_rhand); // right hand weapon
		writeD(0); //TODO chest
		writeD(_lhand); // left hand weapon
		writeC(1); // 2.2: name above char 1=true ... ??; 2.3: 1 - normal, 2 - dead
		writeC(running);
		writeC(incombat);
		writeC(dead);
		writeC(_isShowSpawnAnimation ? 2 : _invis ? 1 : 0); // invisible ?? 0=false  1=true   2=summoned (only works if model has a summon animation)
		writeD(_nameStringId);
		writeS(_name);
		writeD(_titleStringId);
		writeS(_title);
		writeD(_cha.isSummon() || _cha.isPet() || _cha instanceof L2XmassTreeInstance ? 1 : 0); // 0 - Зеленый титл, 1 синий (у саммонов)
		writeD(pvp_flag);
		writeD(karma); // hmm karma ??
		//writeD(_abnormalEffect); // C2
		writeD(clan_id); // clan id (клиентом не используется, но требуется для показа значка)
		writeD(clan_crest_id); // clan crest id
		writeD(ally_id); // ally id (клиентом не используется, но требуется для показа значка)
		writeD(ally_crest_id); // ally crest id
		writeD(0x00);
		writeC(isFlying ? 1 : 0); // C2
		writeC(team); // team aura 1-blue, 2-red
		writeF(growRadius);
		writeF(growHeight);
		writeD(Math.min(_enchantEffect, 127)); // C4
		writeD(isFlying ? 1 : 0); // C2
		writeD(0x00);
		writeD(type);
		writeC(_showNameTag); // влияет на возможность примененя к цели /nexttarget и /assist
		writeC(_showNameTag); // name above char 1=true ... ??
		writeD(_state);
		
		writeD(0x00);
		writeD(_currentHP);
		writeD(_maxHP);
		writeD(_currentMP);
		writeD(_maxMP);
		writeD(_currentCP);
		writeD(_maxCP);
		writeH(0x00);
		writeH(0x00);
		writeC(0x00);
		
		writeD(0x00); // TAUTI
		writeD(0x00); // TAUTI
		
		writeD(_abnormalEffectsArray.size());
		for (AbnormalVisualEffect ae : _abnormalEffectsArray)
			writeD(ae.getVisualMask(ae));
	}

	private void runImpl_Decoy()
	{
		L2Player cha_owner = _cha.getPlayer();
		_runSpd = (int) cha_owner.getRunSpeed();
		_walkSpd = (int) cha_owner.getWalkSpeed();
		karma = cha_owner.getKarma();
		pvp_flag = cha_owner.getPvpFlag();
		_pAtkSpd = cha_owner.getPAtkSpd();
		running = cha_owner.isRunning() ? 1 : 0;
		incombat = cha_owner.isInCombat() ? 1 : 0;
		dead = cha_owner.isAlikeDead() ? 1 : 0;
		_abnormalEffect = cha_owner.getAbnormalEffect();
		team = cha_owner.getTeam();

		if(cha_owner.isCursedWeaponEquipped())
		{
			_name = cha_owner.getTransformationName();
			_title = "";
			clan_crest_id = 0;
			ally_crest_id = 0;
			decoy_clan_id = 0;
			decoy_ally_id = 0;
			decoy_large_clan_crest_id = 0;
			decoy_cw_level = CursedWeaponsManager.getInstance().getLevel(cha_owner.getCursedWeaponEquippedId());
		}
		else
		{
			_name = cha_owner.getName();
			_title = cha_owner.getTitle();
			clan_crest_id = cha_owner.getClanCrestId();
			ally_crest_id = cha_owner.getAllyCrestId();
			decoy_clan_id = cha_owner.getClanId();
			decoy_ally_id = cha_owner.getAllyId();
			decoy_large_clan_crest_id = cha_owner.getClanCrestLargeId();
			decoy_cw_level = 0;
		}

		if(cha_owner.getMountEngine().isMounted())
		{
			decoy_enchant = 0;
			decoy_mount_id = cha_owner.getMountEngine().getMountNpcId() + 1000000;
			decoy_mount_type = (byte) cha_owner.getMountEngine().getMountType();
		}
		else
		{
			decoy_enchant = (byte) cha_owner.getEnchantEffect();
			decoy_mount_id = 0;
			decoy_mount_type = 0;
		}

		if(decoy_clan_id > 0 && cha_owner.getClanId() != 0)
			decoy_clan_rep_score = cha_owner.getClan().getReputationScore();
		else
			decoy_clan_rep_score = 0;

		decoy_fishing = cha_owner.isFishing() ? (byte) 1 : (byte) 0;
		decoy_fishLoc = cha_owner.getFishLoc();
		decoy_swimSpd = (int) cha_owner.getSwimSpeed();
		decoy_private_store = (byte) cha_owner.getPrivateStoreType(); // 1 - sellshop
		decoy_inv = cha_owner.getInventory();
		decoy_race = cha_owner.getBaseTemplate().race.ordinal();
		decoy_sex = cha_owner.getSex();
		decoy_base_class = cha_owner.getBaseClass();
		decoy_move_speed = cha_owner.getMovementSpeedMultiplier();
		decoy_attack_speed = cha_owner.getAttackSpeedMultiplier();
		decoy_hair_style = cha_owner.getHairStyle();
		decoy_hair_color = cha_owner.getHairColor();
		decoy_face = cha_owner.getFace();
		decoy_sitting = cha_owner.isSitting() ? 0 : 1;
		decoy_invis = cha_owner.isInvisible() ? 1 : 0;
		decoy_cubics = cha_owner.getCubics();
		decoy_rec_left = cha_owner.getRecSystem().getRecommendsLeft();
		decoy_rec_have = cha_owner.getRecSystem().getRecommendsHave();
		decoy_class_id = cha_owner.getClassId().getId();
		decoy_noble = cha_owner.isNoble() ? 1 : 0;
		decoy_hero = cha_owner.isHero() || cha_owner.isGM() && Config.GM_HERO_AURA ? 1 : 0; // 0x01: Hero Aura
		decoy_NameColor = cha_owner.getNameColor();
		decoy_PledgeClass = cha_owner.getPledgeRank();
		decoy_pledge_type = cha_owner.getPledgeType();
		decoy_TitleColor = cha_owner.getTitleColor();
		decoy_Transformation = cha_owner.getTransformation();
		decoy_Agathion = cha_owner.getAgathionId();
		decoy_battlefieldPenalty = cha_owner.getEffectBySkillId(L2Skill.SKILL_BATTLEFIELD_PENALTY) != null ? 0 : 1;
	}

	private void writeImpl_Decoy()
	{
		writeC(0x0C);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(0x00);
		writeD(_npcObjId);
		writeS(_name);
		writeD(decoy_race);
		writeD(decoy_sex);
		writeD(decoy_base_class);

		for(byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(decoy_inv.getPaperdollItemId(PAPERDOLL_ID));

		for(byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(decoy_inv.getPaperdollAugmentationId(PAPERDOLL_ID));

		writeD(0x01); // ?GraciaFinal
		writeD(0x00); // ?GraciaFinal

		writeD(pvp_flag);
		writeD(karma);
		writeD(_mAtkSpd);
		writeD(_pAtkSpd);
		writeD(0x00);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(decoy_swimSpd); // swimspeed
		writeD(decoy_swimSpd); // swimspeed
		writeD(_runSpd/*_flRunSpd*/);
		writeD(_walkSpd/*_flWalkSpd*/);
		writeD(_runSpd/*_flyRunSpd*/);
		writeD(_walkSpd/*_flyWalkSpd*/);
		writeF(decoy_move_speed);
		writeF(decoy_attack_speed);
		writeF(colRadius);
		writeF(colHeight);
		writeD(decoy_hair_style);
		writeD(decoy_hair_color);
		writeD(decoy_face);
		writeS(_title);
		writeD(decoy_clan_id);
		writeD(clan_crest_id);
		writeD(decoy_ally_id);
		writeD(ally_crest_id);
		writeC(decoy_sitting);
		writeC(running);
		writeC(incombat);
		writeC(dead);
		writeC(decoy_invis);
		writeC(decoy_mount_type);
		writeC(decoy_private_store);

		if(decoy_cubics == null)
			writeH(0);
		else
		{
			writeH(decoy_cubics.size());
			for(L2CubicInstance cub : decoy_cubics)
				writeH(cub.getId());
		}

		writeC(0x00); // find party members
		writeD(_abnormalEffect);
		writeC(0x00);
		writeH(decoy_rec_have);
		writeD(decoy_mount_id);
		writeD(decoy_class_id);
		writeD(0); // ?
		writeC(decoy_enchant);
		writeC(team);
		writeD(decoy_large_clan_crest_id);
		writeC(decoy_noble);
		writeC(decoy_hero);
		writeC(decoy_fishing);
		writeD(decoy_fishLoc.getX());
		writeD(decoy_fishLoc.getY());
		writeD(decoy_fishLoc.getZ());
		writeD(decoy_NameColor);
		writeD(_loc.getHeading());
		writeD(decoy_PledgeClass);
		writeD(decoy_pledge_type);
		writeD(decoy_TitleColor);
		writeD(decoy_cw_level);
		writeD(decoy_clan_rep_score);
		writeD(decoy_Transformation);
		writeD(decoy_Agathion);
		writeD(decoy_battlefieldPenalty); // Dark Icon Battle Filed Penalty
		writeD(_abnormalEffect2);
	}

	public static final byte[] PAPERDOLL_ORDER = {
			Inventory.PAPERDOLL_UNDER,
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
			Inventory.PAPERDOLL_BELT // Пояс
	};
}