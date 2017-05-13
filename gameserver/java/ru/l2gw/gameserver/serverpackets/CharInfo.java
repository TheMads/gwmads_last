package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.AbnormalEffect;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2Skill.AbnormalVisualEffect;
import ru.l2gw.gameserver.model.instances.L2CubicInstance;
import ru.l2gw.util.Location;
import ru.l2gw.commons.arrays.GArray;

public class CharInfo extends L2GameServerPacket
{
	private L2Player _player;
	private int[][] _inv;
	private int _mAtkSpd, _pAtkSpd;
	private int _runSpd, _walkSpd, _swimSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	private Location _loc, _fishLoc;
	private String _name, _title;
	private int _objId, _race, _sex, base_class, pvp_flag, karma, rec_have, _vehicelId;
	private float speed_move, speed_atack, col_radius, col_height;
	private int hair_style, hair_color, face, abnormalEffect, abnormalEffect2;
	private int clan_id, clan_crest_id, large_clan_crest_id, ally_id, ally_crest_id, class_id;
	private byte _sit, _run, _combat, _dead, _invis, private_store, _enchant;
	private byte _team, _noble, _hero, _fishing, mount_type;
	private int plg_class, pledge_type, clan_rep_score, cw_level, mount_id;
	private int _nameColor, title_color, _transform, _agathion;
	private FastList<Integer> cubics;
	private boolean can_writeImpl = false;
	private boolean partyRoom = false;
	private int curHP, maxHP, curMP, maxMP, curCP;
	private boolean isFlying;
	private int _battlefieldPenalty;
	//private GArray<AbnormalEffect> _abnormalEffects;
	private GArray<AbnormalVisualEffect> _abnormalEffectsArray;

	protected boolean logHandled()
	{
		return true;
	}

	public CharInfo(L2Player player)
	{
		if(player == null)
			return;
		_player = player;
	}

	@Override
	final public void runImpl()
	{
		if(_player == null || _player.isInvisible() || _player.isDeleting())
			return;

		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.equals(_player))
		{
			_log.warn("You cant send CharInfo about his character to active user!!!");
			return;
		}

		if(_player.isPolymorphed())
		{
			player.sendPacket(new NpcInfoPoly(_player, player));
			return;
		}

		if(_player.isCursedWeaponEquipped())
		{
			_name = _player.getTransformationName();
			_title = "";
			clan_id = 0;
			clan_crest_id = 0;
			ally_id = 0;
			ally_crest_id = 0;
			large_clan_crest_id = 0;
			cw_level = CursedWeaponsManager.getInstance().getLevel(_player.getCursedWeaponEquippedId());
		}
		else
		{
			_name = _player.getName();
			_title = _player.getTitle();
			clan_id = _player.getClanId();
			clan_crest_id = _player.getClanCrestId();
			ally_id = _player.getAllyId();
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

		_inv = new int[Inventory.PAPERDOLL_MAX][2];
		for(int PAPERDOLL_ID : PAPERDOLL_ORDER)
		{
			_inv[PAPERDOLL_ID][0] = player.getInventory().getPaperdollItemId(PAPERDOLL_ID);
			_inv[PAPERDOLL_ID][1] = player.getInventory().getPaperdollAugmentationId(PAPERDOLL_ID);
		}
		
		_mAtkSpd = _player.getMAtkSpd();
		_pAtkSpd = _player.getPAtkSpd();
		speed_move = 0; // GoD
		_runSpd = _player.getTemplate().baseRunSpd; //int) (_player.getRunSpeed() / _moveMultiplier);
		_walkSpd = _player.getTemplate().baseWalkSpd; //int) (_player.getWalkSpeed() / _moveMultiplier);

		_flRunSpd = 0; // TODO
		_flWalkSpd = 0; // TODO

		if(_player.isFlying())
		{
			_flyRunSpd = _runSpd;
			_flyWalkSpd = _walkSpd;
		}
		else
		{
			_flyRunSpd = 0;
			_flyWalkSpd = 0;
		}

		_swimSpd = (int) _player.getSwimSpeed();
		_vehicelId = _player.isInBoat() ? _player.getVehicle().getObjectId() : 0;
		_loc = _vehicelId == 0 ? _player.getLoc() : _player.getLocInVehicle().clone().changeZ(15);
		_objId = _player.getObjectId();
		_race = _player.getBaseTemplate().race.ordinal();
		_sex = _player.getSex();
		base_class = _player.getBaseClass();
		pvp_flag = _player.getPvpFlag();
		karma = _player.getKarma();
		speed_move = _player.getMovementSpeedMultiplier();
		speed_atack = _player.getAttackSpeedMultiplier();
		col_radius = _player.getColRadius();
		col_height = _player.getColHeight();
		hair_style = _player.getHairStyle();
		hair_color = _player.getHairColor();
		face = _player.getFace();
		if(clan_id > 0 && _player.getClanId() != 0)
			clan_rep_score = _player.getClan().getReputationScore();
		else
			clan_rep_score = 0;
		_sit = _player.isSitting() ? (byte) 0 : (byte) 1; // standing = 1 sitting = 0
		_run = _player.isRunning() ? (byte) 1 : (byte) 0; // running = 1 walking = 0
		_combat = _player.isInCombat() ? (byte) 1 : (byte) 0;
		_dead = _player.isAlikeDead() ? (byte) 1 : (byte) 0;
		_invis = _player.isInvisible() || _player.isHide() ? (byte) 1 : (byte) 0; // invisible = 1 visible = 0
		private_store = (byte) _player.getPrivateStoreType(); // 1 - sellshop
		cubics = new FastList<Integer>();
		if(_player.getCubics() != null)
			for(L2CubicInstance cub : _player.getCubics())
				if(cub != null)
					cubics.add(cub.getId());
		_abnormalEffectsArray = player.getAbnormalEffects();
		//abnormalEffect = _player.getAbnormalEffect();
		//abnormalEffect2 = _player.getAbnormalEffect2();
		rec_have = _player.getRecSystem().getRecommendsHave();
		class_id = _player.getClassId().getId();
		_team = (byte) _player.getTeam(); // team circle around feet 1 = Blue, 2 = red
		_noble = _player.isNoble() ? (byte) 1 : (byte) 0; // 0x01: symbol on char menu ctrl+I
		_hero = _player.isHero() || _player.isGM() && Config.GM_HERO_AURA ? (byte) 1 : (byte) 0; // 0x01: Hero Aura
		_fishing = _player.isFishing() ? (byte) 1 : (byte) 0;
		_fishLoc = _player.getFishLoc();
		_nameColor = _player.getNameColor(); // New C5
		plg_class = _player.getPledgeRank();
		pledge_type = _player.getPledgeType();
		title_color = _player.getTitleColor();
		_transform = _player.getTransformation();
		_agathion = _player.getAgathionId();

		curCP = (int) player.getCurrentCp();
		curHP = (int) player.getCurrentHp();
		maxHP = player.getMaxHp();
		curMP = (int) player.getCurrentMp();
		maxMP = player.getMaxMp();
		
		partyRoom = PartyRoomManager.getInstance().isLeader(_player);
		isFlying = _player.isInFlyingTransform();
		_battlefieldPenalty = _player.getEffectBySkillId(L2Skill.SKILL_BATTLEFIELD_PENALTY) != null ? 0 : 1;
		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0x31);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(_vehicelId); // vehicle id ?
		writeD(_objId);
		writeS(_name);
		writeD(_race);
		writeD(_sex);
		writeD(base_class);

		for(int PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(_inv[PAPERDOLL_ID][0]);

		for(int PAPERDOLL_ID : PAPERDOLL_ORDER)
		{
			writeH(_inv[PAPERDOLL_ID][1]);
			writeH(_inv[PAPERDOLL_ID][2]);
		}

		writeD(0x01); // TODO talisman count(VISTALL)
		writeD(0x00); // TODO cloak status(VISTALL)
		
		/*TODO: [Bonux] Правильно рассортировать
		writeD(_inv[Inventory.PAPERDOLL_LHAND][3]); //Внешний вид щита (ИД Итема).
		writeD(_inv[Inventory.PAPERDOLL_GLOVES][3]); //Внешний вид перчаток (ИД Итема). !!!ПРАВИЛЬНО
		writeD(_inv[Inventory.PAPERDOLL_CHEST][3]); //Внешний вид верха (ИД Итема). !!!ПРАВИЛЬНО
		writeD(_inv[Inventory.PAPERDOLL_LEGS][3]); //Внешний вид низа (ИД Итема). !!!ПРАВИЛЬНО
		writeD(_inv[Inventory.PAPERDOLL_FEET][3]); //Внешний вид ботинок (ИД Итема). !!!ПРАВИЛЬНО
		writeD(_inv[Inventory.PAPERDOLL_HAIR][3]); //Внешний вид шляпы (ИД итема).
		writeD(_inv[Inventory.PAPERDOLL_DHAIR][3]); //Внешний вид маски (ИД итема).
		writeD(0x00); //???
		writeD(_inv[Inventory.PAPERDOLL_RHAND][3]); //Внешний вид оружия (ИД Итема).
		*/
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		
		writeD(pvp_flag);
		writeD(karma);
		
		writeD(_mAtkSpd);
		writeD(_pAtkSpd);
		
		writeD(0x00);
		
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimSpd/* 0x32 */); // swimspeed
		writeD(_swimSpd/* 0x32 */); // swimspeed
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		
		writeF(speed_move); // _player.getProperMultiplier()
		writeF(speed_atack); // _player.getAttackSpeedMultiplier()
		writeF(col_radius);
		writeF(col_height);
		writeD(hair_style);
		writeD(hair_color);
		writeD(face);
		writeS(_title);
		writeD(clan_id);
		writeD(clan_crest_id);
		writeD(ally_id);
		writeD(ally_crest_id);
		
		writeC(_sit);
		writeC(_run);
		writeC(_combat);
		writeC(_dead);
		writeC(_invis);
		writeC(mount_type); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
		writeC(private_store);

		if(cubics == null)
			writeH(0);
		else
		{
			writeH(cubics.size());
			for(Integer cubId : cubics)
				writeH(cubId);
		}

		writeC(partyRoom ? 0x01 : 0x00); // find party members
		writeC(isFlying ? 0x02 : 0x00);
		writeH(rec_have);
		writeD(mount_id);
		writeD(class_id);
		writeD(0x00); //?
		writeC(_enchant);
		
		writeC(_team);
		
		writeD(large_clan_crest_id);
		writeC(_noble);
		writeC(_hero);
		writeC(_fishing);
		writeD(_fishLoc.getX());
		writeD(_fishLoc.getY());
		writeD(_fishLoc.getZ());
		writeD(_nameColor);
		writeD(_loc.getHeading());
		writeD(plg_class);
		writeD(pledge_type);
		writeD(title_color);
		writeD(cw_level);
		writeD(clan_rep_score);
		writeD(_transform);
		writeD(_agathion);
		
		writeD(0x01); // T2
		
		writeD(0x00);// Unknown1 (GOD)
		writeD(0x00);// Unknown2 (GOD)
		writeD(0x00);// Unknown3 (GOD)
		

		writeD(curCP);
		writeD(maxHP);
		writeD(maxHP);
		writeD(curMP);
		writeD(maxMP);
		
		writeD(0x00);// Unknown9 (GOD)
		writeC(0x00);// Unknown10 (GOD)
		writeD(0x00); // TAUTI
		
		writeD(_abnormalEffectsArray.size());
		for (AbnormalVisualEffect ae : _abnormalEffectsArray)
			writeD(ae.getVisualMask(ae));
		
		writeC(0x00); // TAUTI
	}

	public static final int[] PAPERDOLL_ORDER = {
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
			Inventory.PAPERDOLL_BELT };
}