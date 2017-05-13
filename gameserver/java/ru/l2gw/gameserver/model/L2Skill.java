package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2SiegeHeadquarterInstance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.serverpackets.FlyToLocation.FlyType;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.conditions.Condition;
import ru.l2gw.gameserver.skills.conditions.ConditionChance;
import ru.l2gw.gameserver.skills.effects.EffectTemplate;
import ru.l2gw.gameserver.skills.funcs.Func;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;
import ru.l2gw.gameserver.skills.skillclasses.Default;
import ru.l2gw.gameserver.skills.AbnormalEffect;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.*;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.List;

public class L2Skill
{
	protected static final Log _log = LogFactory.getLog(L2Skill.class.getName());

	public static final Integer SKILL_CUBIC_MASTERY = 143;
	public static final Integer SKILL_CRAFTING = 172;
	public static final Integer SKILL_CRYSTALLIZE = 248;
	public static final Integer SKILL_FISHING_MASTERY = 1315;
	public static final Integer BLINDING_BLOW = 321;
	public static final int SKILL_STRIDER_ASSAULT = 325;
	public static final int SKILL_WYVERN_BREATH = 4289;
	public static final int SKILL_HINDER_STRIDER = 4258;
	public static final int SKILL_RAID_CURSE = 4515;
	public static final int SKILL_RAID_SILENS = 4215;
	public static final int SKILL_BATTLEFIELD_PENALTY = 5660;

	private static final Func[] _emptyFunctionSet = new Func[0];

	// these two build the primary key
	protected final int _id;
	protected final int _level;
	protected final int _skillIndex;

	/**
	 * Identifier for a skill that client can't display
	 */
	protected final int _displayId;
	protected int _displayLevel;
	private final int _increaseLevel;

	// not needed, just for easier debug
	private final String _name;

	private final SkillOpType _operateType;
	private final ChanceOpType _chanceOpType;
	private final boolean _isMagic;
	private final boolean _isPhysic;
	private final int _magicType;
	protected final int _mpConsume1;
	protected final int _mpConsume2;
	protected final int _mpUsage;
	protected final int _hpConsume;
	protected final int[] _itemConsume;
	protected final int[] _itemConsumeId;
	protected final int _soulsConsume;
	private final int _maxSoulsConsume;
	protected final boolean _isItemHandler;
	protected final boolean _isPotion;
	private final boolean _isCommon;
	private final boolean _isSaveable;
	private final int _castRange;
	private final boolean _isTriggered;
	protected final int _triggerSkillId;
	protected final int _triggerSkillLvl;
	protected final int _targetConsume;
	protected final int _targetConsumeId;
	protected final int _afterEffectSkillId;
	protected final int _afterEffectSkillLvl;
	protected final boolean _afterEffectFinished;
	protected final boolean _allowPetUse;
	protected final boolean _hidden;
	protected final boolean _shieldHit;
	protected final boolean _isHerb;
	protected final boolean _isDanceSong;
	protected final boolean _olympiadUse;
	protected final int _abnormalLevel;
	protected final AbnormalVisualEffect _abnormalVe;
	protected final long _abnormalTime;
	protected final GArray<String> _abnormalTypes;
	protected final CastType _castType;
	private int _chainIndex;
	private int _chainSkillId;

	private final int _mAtk;
	protected boolean _useSS;

	private AddedSkill[] _addedSkills;

	// all times in milliseconds
	private final int _hitTime;
	private final int _coolTime;
	private final int _hitCancelTime;
	private final long _reuseDelay;
	private final int _reuseSkillId;

	protected final SkillType _skillType;
	protected final double _power;
	protected final double _pvpPower;
	protected final int _effectPoint;
	protected final int _skillRadius;

	protected final boolean _altUse;
	private final Element _element;
	private final int _elementPower;
	private BaseStats _baseStat;
	private final int _activateRate;
	private final byte _magicLevel;
	private final double _levelMod;
	private final boolean _cancelable;
	private final boolean _shieldignore;
	private final double _critRate;
	private final boolean _overhit;
	private final int _weaponsAllowed;
	protected final boolean _isOffensive;
	private final boolean _isForgotten;
	private final boolean _flyingTransformUse;

	private final int _forceId;

	private final FlyType _flyType;
	private final int _flyRadius;

	private final ArrayList<ClassId> _canLearn; // which classes can learn

	private Condition preCondition;
	protected FuncTemplate[] _funcTemplates;
	protected EffectTemplate[] _effectTemplates;

	private final int _minPledgeClass;

	private final boolean _staticReuse;
	private final boolean _staticHitTime;
	private NextAction _nextAction;

	private final int _chance;

	private final int _effectNpcId;

	protected final int _numCharges;
	protected final int _condCharges;

	protected final boolean _isForCubic;
	private final int _npcId;
	private final int _trapLifeTime;

	private final boolean _isDebuff;
	private final boolean _isCastTimeEffect;
	private ResistType _resistType;

	private final String _toString;

	public final TargetType _targetType;
	private final AffectScope _affectScope;
	private final AffectObject _affectObject;
	private int _sector_a1;
	private int _sector_a2;
	private int _sector_r1;
	private int _sector_r2;
	private int _affect_min;
	private int _affect_max;
	private boolean debug = false;
	private int _rideState;
	private final int _buffProtectLevel;

	/**
	 * If the reuse delay left for skill is < this value, this reuse delay will be set as item
	 * cooltime. Value is in Seconds.
	 */
	private final int _minReuseDelayOnSkillAdd;

	public static enum BaseStats
	{
		INT,
		WIT,
		MEN,
		CON,
		DEX,
		STR,
		NONE
	}

	public static enum SkillOpType
	{
		OP_PASSIVE,
		OP_ACTIVE,
		OP_TOGGLE
	}

	public static enum ChanceOpType
	{
		ON_ATTACK,
		ON_CRIT,
		ON_MAGIC_ATTACK,
		ON_ATTACKED,
		ON_MAGIC_ATTACKED,
		ON_EVADED,
		ON_SHIELD,
		ON_DAMAGE_RECEIVED,
		NONE
	}

	public static enum NextAction
	{
		DEFAULT,
		NONE,
		ATTACK,
		CAST,
		SIT
	}

	//elements

	public static enum Element
	{
		NONE,
		FIRE,
		WATER,
		WIND,
		EARTH,
		HOLY,
		DARK
	}

	public enum TargetType
	{
		self,
		holything,
		flagpole,
		npc_body,
		pc_body,
		door_treasure,
		summon,
		master,

		enemy,
		enemy_only,
		enemy_not,
		target,
		door,
		treasure,

		ground,
		none,
		siege_ally,
		at_war,

		invalid
	}

	public enum AffectScope
	{
		single,
		clan,
		dead_clan,
		sector,
		square,
		line,
		party,
		party_clan,
		around,
		dead_union,
		range_sort_by_hp,
		none
	}

	public enum AffectObject
	{
		friend,
		not_friend,
		undead_real_enemy,
		object_dead_npc_body
	}

	public static enum AbnormalVisualEffect
	{
		bleeding(1),
		poison(2),
		dot_fire_area(3),
		ice(4),

		fear(5),
		confusion(6),
		stun(7),
		sleep(8),

		silence(9),
		root(10),
		paralyze(11),
		stone(12),

		unk_13(13),
		bighead(14),
		dot_fire(15),
		change_texture(16),

		big_body(17),
		float_root(18),
		danceStun(19),
		fire_stun(20),

		stealth(21),
		dot_mp(22),
		dot_soil(23),
		magic_circle(24),

		dot_water(25),
		test_1_2(26),
		test_1_4(27),
		ud(28),

		vitality(29),
		red_mark(30),
		death_mark(31),
		blue_mark(32),

		// special effects
		av2_invul(33), // целестиал
		av2_stun(34), // непонятное красное облако
		av2_root(35), // непонятное красное облако
		av2_baguette_sword(36), // пусто

		av2_yellow_affro(37), // Большая круглая желтая прическа с воткнутой в волосы расческой
		av2_pink_affro(38), // Большая круглая розовая прическа с воткнутой в волосы расческой
		av2_black_affro(39), // Большая круглая черная прическа с воткнутой в волосы расческой
		av2_unk80(8), // пусто

		av2_stigma(41), // Stigma of Shillen
		av2_unk200(42), // эффект чем то похожий на рут
		ave_frozen_pillar(43), // превращает в сасульку
		av2_unk800(44), // Пухи желтым начинаю светится

		av2_unk1000(45), // пусто
		av2_unk2000(46), // пусто
		av2_unk4000(47), // Над головой какая то зеленая хрень
		ave_mp_shield(48), // ave_mp_shield

		av2_unk10000(17), // пусто
		av2_unk20000(18), // пусто
		av2_unk40000(19), // пусто
		av2_nevit(47), // Nevit Blessing

		av2_unk100000(21), // пусто
		av2_unk200000(22), // пусто
		av2_unk400000(23), // пусто
		av2_unk800000(24), // пусто

		av2_unk1000000(25), // пусто
		av2_unk2000000(26), // пусто
		av2_unk4000000(27), // пусто
		av2_unk8000000(28), // пусто

		av2_unk10000000(29), // пусто
		av2_unk20000000(30), // пусто
		av2_unk40000000(31), // пусто
		av2_unk80000000(32), // пусто

		none(0);

		public final long mask;

		private AbnormalVisualEffect(long _mask)
		{
			mask = _mask;
		}
		
		public long getVisualMask(AbnormalVisualEffect ve) {
			return ve.mask;
		}

		public static AbnormalVisualEffect getAbnormalByMask(long mask)
		{
			for(AbnormalVisualEffect ve : values())
				if(ve.mask == mask)
					return ve;
			return none;
		}
	}

	public static enum CastType
	{
		none,
		rush_front,
		rush_back,
		rush_behind,
		instant
	}

	public static enum ResistType
	{
		BLEED,
		CANCEL,
		DEBUFF,
		PARALYZE,
		MUTE,
		POISON,
		ROOT,
		SLEEP,
		STUN,
		DEATH,
		FEAR,
		SLOW,
		NONE
	}

	public static enum SkillType
	{
		SKILL(L2Skill.class),

		// unimplemented
		NOTDONE;

		private final Class<? extends L2Skill> clazz;

		public L2Skill makeSkill(StatsSet set)
		{
			try
			{
				Constructor<? extends L2Skill> c = clazz.getConstructor(StatsSet.class);
				return c.newInstance(set);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		private SkillType()
		{
			clazz = Default.class;
		}

		private SkillType(Class<? extends L2Skill> clazz)
		{
			this.clazz = clazz;
		}
	}

	public static enum RideState
	{
		none(0),
		ride_none(1),
		ride_strider(2),
		ride_wolf(4),
		ride_wyvern(8);

		public final int mask;

		private RideState(int m)
		{
			mask = m;
		}
	}

	/**
	 * Внимание!!! У наследников вручную надо поменять тип на public
	 *
	 * @param set парамерты скилла
	 */
	public L2Skill(StatsSet set)
	{
		_id = set.getInteger("skill_id");
		_level = set.getInteger("level");
		_skillIndex = (_id << 16) | _level;
		_displayId = set.getInteger("displayId", _id);
		_displayLevel = set.getInteger("displayLevel", _level);
		_increaseLevel = set.getInteger("increaseLevel", (short) 0);
		_name = set.getString("name");
		_operateType = set.getEnum("operateType", SkillOpType.class);
		_chanceOpType = set.getEnum("chanceOpType", ChanceOpType.class, ChanceOpType.NONE);
		_magicType = set.getInteger("isMagic", 0);
		_isMagic = _magicType == 1;
		_isPhysic = _magicType == 0 || _magicType == 3;
		_isDanceSong = _magicType == 3;
		_altUse = set.getBool("altUse", false);
		_mpConsume1 = set.getInteger("mpConsume1", 0);
		_mpConsume2 = set.getInteger("mpConsume2", 0);
		_mpUsage = set.getInteger("mpUsage", 0);
		_hpConsume = set.getInteger("hpConsume", 0);
		_soulsConsume = set.getInteger("soulsConsume", 0);
		_maxSoulsConsume = set.getInteger("maxSoulsConsume", 0);
		_mAtk = set.getInteger("mAtk", 0);
		_useSS = set.getBool("useSS", true);
		_isDebuff = set.getBool("isDebuff", false);
		_resistType = ResistType.valueOf(set.getString("resistType", "NONE").toUpperCase());

		_isForCubic = set.getBool("isForCubic", false);
		_forceId = set.getInteger("forceId", 0);
		_isTriggered = (set.getBool("isTriggered", false));
		_triggerSkillId = set.getInteger("triggerEffectId", 0);
		_triggerSkillLvl = set.getInteger("triggerEffectLvl", set.getInteger("level"));
		_isCastTimeEffect = set.getBool("castTimeEffect", false);
		_targetConsume = set.getInteger("targetConsumeCount", 0);
		_targetConsumeId = set.getInteger("targetConsumeId", 0);
		_afterEffectSkillId = set.getInteger("afterEffectSkillId", 0);
		_afterEffectSkillLvl = set.getInteger("afterEffectSkillLvl", _afterEffectSkillId > 0 ? _level : 0);
		_afterEffectFinished = set.getBool("afterEffectFinished", false);
		_olympiadUse = set.getBool("olympiadUse", false);
		_abnormalLevel = set.getInteger("abnormalLevel", _level);
		AbnormalVisualEffect ave = AbnormalVisualEffect.none;
		try
		{
			ave = AbnormalVisualEffect.valueOf(set.getString("abnormal_ve", "none"));
		}
		catch(IllegalArgumentException e)
		{
			_log.info("Skill[id=" + _id + ";level=" + _level + "] unknown abnormal_ve: " + set.getString("abnormal_ve"));
		}

		CastType ct = CastType.none;
		try
		{
			ct = CastType.valueOf(set.getString("cast_type", "none"));
		}
		catch(IllegalArgumentException e)
		{
			_log.info("Skill[id=" + _id + ";level=" + _level + "] unknown cast_type: " + set.getString("cast_type"));
		}
		_castType = ct;

		_abnormalVe = ave;
		_abnormalTime = set.getInteger("abnormal_time", 0) * 1000L;
		_abnormalTypes = new GArray<String>(1);
		String at = set.getString("abnormal_type", null);
		if(at != null)
			_abnormalTypes.addAll(Arrays.asList(at.toLowerCase().split(";")));


		//ItemConsume quick hack
		String s1 = set.getString("itemConsumeCount", "");
		String s2 = set.getString("itemConsumeId", "");

		if(s1.length() == 0)
			_itemConsume = new int[]{0};
		else
		{
			String[] s = s1.split(" ");
			_itemConsume = new int[s.length];
			for(int i = 0; i < s.length; i++)
				_itemConsume[i] = Integer.parseInt(s[i]);
		}

		if(s2.length() == 0)
			_itemConsumeId = new int[]{0};
		else
		{
			String[] s = s2.split(" ");
			_itemConsumeId = new int[s.length];
			for(int i = 0; i < s.length; i++)
				_itemConsumeId[i] = Integer.parseInt(s[i]);
		}

		_isItemHandler = set.getBool("isHandler", false);
		_isPotion = set.getBool("isPotion", false);
		_isCommon = set.getBool("isCommon", false);
		_isSaveable = set.getBool("isSaveable", true);
		_castRange = set.getInteger("castRange", -1);
		_hitTime = set.getInteger("hitTime", 0);
		_coolTime = set.getInteger("coolTime", 0);
		_hitCancelTime = set.getInteger("hitCancelTime", 500);
		_reuseDelay = set.getLong("reuseDelay", 0);
		_reuseSkillId = set.getInteger("reuse_skill_id", _id);
		_skillRadius = set.getInteger("skillRadius", 80);
		_power = set.getDouble("power", 0.);
		_pvpPower = set.getDouble("pvpPower", _power);
		_effectPoint = set.getInteger("effectPoint", 0);
		_nextAction = NextAction.valueOf(set.getString("nextAction", "DEFAULT").toUpperCase());
		_chainIndex = set.getInteger("chainIndex",-1);
		_chainSkillId = set.getInteger("chainSkillId",-1);
		_skillType = SkillType.valueOf(set.getString("skillType", "SKILL"));
		_staticReuse = set.getBool("staticReuse", false);
		_staticHitTime = set.getBool("staticHitTime", false);
		_isHerb = set.getBool("isHerb", false);
		int et = -1;

		try
		{
			et = set.getInteger("element", 0);
		}
		catch(IllegalArgumentException e)
		{
		}
		if(et > -1)
			_element = Element.values()[et];
		else
			_element = Element.valueOf(set.getString("element", "NONE").toUpperCase());

		_elementPower = set.getInteger("elementPower", 0);
		_baseStat = BaseStats.valueOf(set.getString("baseStat", "NONE").toUpperCase());
		_activateRate = set.getInteger("activateRate", -1);
		_magicLevel = set.getByte("magicLevel", (byte) 0);
		_levelMod = set.getDouble("levelMod", 1);
		_cancelable = set.getBool("cancelable", true);
		_shieldignore = set.getBool("shieldignore", false);
		_critRate = set.getDouble("critRate", 0);
		_overhit = set.getBool("overHit", false);
		_weaponsAllowed = set.getInteger("weaponsAllowed", 0);
		_minPledgeClass = set.getInteger("minPledgeClass", 0);
		_isOffensive = set.getBool("isOffensive", false);
		_isForgotten = set.getBool("isForgotten", false);
		_flyingTransformUse = set.getBool("flyingTransformUse", false);
		_chance = set.getInteger("chance", 100);
		_effectNpcId = set.getInteger("effectNpcId", 0);
		_npcId = set.getInteger("npcId", 0);
		_trapLifeTime = set.getInteger("trapLifeTime", 0);

		_flyType = FlyType.valueOf(set.getString("flyType", "NONE").toUpperCase());
		_flyRadius = set.getInteger("flyRadius", 0);

		_numCharges = set.getInteger("num_charges", 0);
		_condCharges = set.getInteger("cond_charges", 0);
		_minReuseDelayOnSkillAdd = set.getInteger("minReuseDelayOnSkillAdd", 0);
		_allowPetUse = set.getBool("allowPetUse", false);
		_hidden = set.getBool("hidden", false);
		_shieldHit = set.getBool("shieldHit", false);
		_buffProtectLevel = set.getInteger("buff_protect_level", 0);

		_targetType = TargetType.valueOf(set.getString("target_type", "none"));
		_affectScope = AffectScope.valueOf(set.getString("affect_scope", "single"));
		_affectObject = AffectObject.valueOf(set.getString("affect_object", "not_friend"));
		_useSS = set.getBool("useSS", false);
		String ride_state = set.getString("ride_state", null);
		if(ride_state != null)
		{
			for(String state : ride_state.split(";"))
				if(state != null && !state.isEmpty())
				{
					RideState rs = RideState.valueOf(state);
					_rideState |= rs.mask;
				}
		}
		else
			_rideState = 0;

		if(_affectScope == AffectScope.sector || _affectScope == AffectScope.square || _affectScope == AffectScope.line)
		{
			String[] sr = set.getString("sector_range", "0;0;0;0").split(";");

			if(sr.length < 4)
				_log.warn("Warning: sector_range defined error for skill: " + this);
			try
			{
				_sector_r1 = Integer.parseInt(sr[0]);
				_sector_a1 = Integer.parseInt(sr[1]);
				_sector_r2 = Integer.parseInt(sr[2]);
				_sector_a2 = Integer.parseInt(sr[3]);
			}
			catch(NumberFormatException e)
			{
				_log.warn("Warning: sector_range parse error for skill: " + this);
			}

			if(_sector_a1 == 0 && _sector_a2 == 0 && _sector_r1 == 0 && _sector_r2 == 0)
				_log.warn("Warning: sector_range has zero size for skill: " + this);
		}

		String[] afl = set.getString("affect_limit", "0;0").split(";");

		if(afl.length != 2)
			_log.warn("Warning: affect_limit parse error for skill: " + this);
		else
		{
			try
			{
				_affect_min = Integer.parseInt(afl[0]);
				_affect_max = Integer.parseInt(afl[1]);
			}
			catch(NumberFormatException e)
			{
				_log.warn("Warning: affect_limit parse error for skill: " + this);
			}
		}

		StringTokenizer st = new StringTokenizer(set.getString("addSkills", ""), ";");
		if(st.hasMoreTokens())
		{
			_addedSkills = new AddedSkill[st.countTokens() / 2];
			int i = 0;
			while(st.hasMoreTokens())
			{
				int skillId = Integer.valueOf(st.nextToken());
				String lvl = st.nextToken();
				int skillLvl;
				int minLvl = 0;
				int maxLvl = 0;

				if(lvl.matches("\\d+\\[\\d+\\-\\d+\\]"))
				{
					skillLvl = Integer.valueOf(lvl.substring(0, lvl.indexOf("[")));
					lvl = lvl.substring(lvl.indexOf("[") + 1);
					lvl = lvl.substring(0, lvl.length() - 1);
					StringTokenizer sl = new StringTokenizer(lvl, "-");
					minLvl = Integer.valueOf(sl.nextToken());
					maxLvl = Integer.valueOf(sl.nextToken());
				}
				else
					skillLvl = Integer.valueOf(lvl);

				_addedSkills[i] = new AddedSkill(skillId, skillLvl, minLvl, maxLvl);
				i++;
			}
		}

		_toString = _name + "[id=" + _id + ",lvl=" + _level + "]";

		if(_nextAction == NextAction.DEFAULT)
			_nextAction = NextAction.NONE;

		if(_baseStat == BaseStats.NONE && _isDebuff)
			_baseStat = BaseStats.MEN;

		if(_resistType == ResistType.NONE && _isDebuff)
			_resistType = ResistType.DEBUFF;

		String canLearn = set.getString("canLearn", null);
		if(canLearn == null)
			_canLearn = null;
		else
		{
			_canLearn = new ArrayList<ClassId>();
			st = new StringTokenizer(canLearn, " \r\n\t,;");
			while(st.hasMoreTokens())
			{
				String cls = st.nextToken();
				try
				{
					_canLearn.add(ClassId.valueOf(cls));
				}
				catch(Throwable t)
				{
					_log.error("Bad class " + cls + " to learn skill", t);
				}
			}
		}

		if(!isToggle() && _abnormalTime > 0 && _abnormalTypes.isEmpty() || (!_abnormalTypes.isEmpty() && _abnormalTime == 0))
			_log.info(this + " has no stack type or abnormal level == 0 or abnormal_time == 0");
	}

	public void useSkill(L2Character cha, List<L2Character> targets)
	{
		useSkill(cha, targets, null, false);
	}

	public void useSkill(L2Character cha, List<L2Character> targets, L2ItemInstance usedItem, boolean counter)
	{
		int ss = 0;

		if(!counter && !isHandler())
			if(isMagic())
				ss = cha.getChargedSpiritShot();
			else if(isSSPossible() && cha.getChargedSoulShot())
				ss = 1;

		L2Weapon weapon = cha.getActiveWeaponItem();
		if(_effectTemplates != null && _effectTemplates.length > 0)
		{
			GArray<Env> chances = new GArray<>(targets.size());
			for(L2Character target : targets)
			{
				if(target.isSkillIgnored(getId()))
					continue;
				Env env = new Env(cha, target, this);
				env.value = getActivateRate();
				env.item = usedItem;
				if(env.value > 0)
					Formulas.calcSkillSuccess(env, getResistType());
				chances.add(env);
			}

			for(EffectTemplate et : _effectTemplates)
			{
				if(et.isInstant())
				{
					if(et._applyOnCaster)
					{
						Env e = new Env(cha, cha, this);
						e.success = true;
						GArray<Env> self = new GArray<>(1);
						self.add(e);
						et.getInstantEffect().doEffect(cha, self, ss, counter);
					}
					else
						et.getInstantEffect().doEffect(cha, chances, ss, counter);
				}
				else if(et._applyOnCaster)
				{
					Env e = new Env(cha, cha, this);
					e.success = true;
					applyEffect(e, et);
				}
				else
					for(Env env : chances)
					{
						if(!(env.target instanceof L2NpcInstance || env.target instanceof L2Playable || env.target instanceof L2DoorInstance) || et._excludeCaster && env.target == cha)
							continue;

						if(cha != env.target && env.target.checkReflectDebuffSkill(this))
							env.target = cha.isCubic() ? cha.getPlayer() : cha;
						applyEffect(env, et);
					}
			}
		}

		if(weapon != null && _magicType != 2)
			for(L2Character target : targets)
				weapon.getEffect(false, cha, target, isOffensive());

		//getEffectsSelf(cha);

		if(ss != 0)
			if(isMagic())
				cha.unChargeShots(true);
			else
				cha.unChargeShots(false);
	}

	public void useChanceSkill(Env env)
	{
		if(debug)
			_log.info("Chance: skill useChanceSkill " + _triggerSkillId);

		if(env.character.isAlikeDead() || env.character.isParalyzed() || env.target == null || env.target.isDead())
			return;

		if(!getWeaponDependency(env.character, false) || preCondition instanceof ConditionChance && !preCondition.test(env))
			return;

		if(Rnd.chance(getChance()))
		{
			L2Skill triggerSkill = SkillTable.getInstance().getInfo(_triggerSkillId, _triggerSkillLvl);

			if(triggerSkill != null && !env.target.isAlikeDead())
			{
				if(triggerSkill.getIncreaseLevel() > 0 && env.target.getEffectBySkillId(_triggerSkillId) != null)
				{
					L2Effect ef = env.target.getEffectBySkillId(_triggerSkillId);
					if(ef != null && ef.getSkillLevel() <= triggerSkill.getIncreaseLevel())
						triggerSkill = SkillTable.getInstance().getInfo(_triggerSkillId, ef.getSkillLevel() == triggerSkill.getIncreaseLevel() ? triggerSkill.getIncreaseLevel() : ef.getSkillLevel() + 1);
				}

				if(triggerSkill == null || env.character.isSkillDisabled(triggerSkill.getId()))
					return;

				if(triggerSkill.getCastRange() > 0 && !env.character.isInRange(env.target, triggerSkill.getCastRange() + (int) env.character.getColRadius()) || triggerSkill.getPreCondition() != null && !triggerSkill.getPreCondition().test(env))
					return;

				if(debug)
					_log.info("Chance: apply effects " + _triggerSkillId + " --> " + env.target.getName());

				env.target = triggerSkill.getAimingTarget(env.character, env.target);
				List<L2Character> targets = triggerSkill.getTargets(env.character, env.target, false);

				try
				{
					if(isHidden() && env.character != env.target)
						env.character.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ACTIVATED).addSkillName(getId()));

					triggerSkill.useSkill(env.character, targets);
					long reuse = Formulas.calcSkillReuseDelay(env.character, triggerSkill);
					if(reuse > 0)
					{
						env.character.disableSkill(triggerSkill.getId(), reuse);
					}

					if(targets.size() > 1)
						for(L2Character cha : targets)
						{
							cha.broadcastPacket(cha.isInAirShip() ? new ExMagicSkillUseInAirShip(cha, cha, triggerSkill.getId(), triggerSkill.getLevel(), 0, 0, triggerSkill.isBuff()) : new MagicSkillUse(cha, cha, triggerSkill.getId(), triggerSkill.getLevel(), 0, 0, triggerSkill.isBuff()));
							cha.broadcastPacket(new MagicSkillLaunched(cha.getObjectId(), triggerSkill.getId(), triggerSkill.getLevel(), cha, triggerSkill.isBuff()));
							if(cha.isMoving)
								cha.broadcastMove();
						}
					else
					{
						env.character.broadcastPacket(env.character.isInAirShip() ? new ExMagicSkillUseInAirShip(env.character, env.target, triggerSkill.getId(), triggerSkill.getLevel(), 0, 0, triggerSkill.isBuff()) : new MagicSkillUse(env.character, env.target, triggerSkill.getId(), triggerSkill.getLevel(), 0, 0, triggerSkill.isBuff()));
						env.character.broadcastPacket(new MagicSkillLaunched(env.character.getObjectId(), triggerSkill.getId(), triggerSkill.getLevel(), env.target, triggerSkill.isBuff()));
						if(env.character.isMoving)
							env.character.broadcastMove();
					}
				}
				catch(Exception e)
				{
				}
			}
		}
	}

	public void applyEffect(Env env, EffectTemplate et)
	{
		if(et.isInstant() || env.character instanceof L2DoorInstance)
			return;

		L2Effect e = et.getEffect(env);

		if(e != null)
		{
			if(e.isSuccess(env.success))
			{
				env.target.fireMethodInvoked(MethodCollection.onEffectAdd, new Object[]{this, env.character, env.target, env.item});
				env.target.addEffect(e);
			}
			else if(isOffensive())
				env.character.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addCharName(env.target).addSkillName(_displayId));
		}
	}

	public void applyEffects(L2Character cha, L2Character target, boolean calcChance)
	{
		applyEffects(cha, target, calcChance, 1);
	}

	public void applyEffects(L2Character cha, L2Character target, boolean calcChance, int effectTimeModifier)
	{
		Env env = new Env(cha, target, this);
		env.value = getActivateRate();
		applyEffects(env, calcChance, effectTimeModifier);
	}

	public void applyEffects(Env env, boolean calcChance, int effectTimeModifier)
	{
		if(isPassive() || _effectTemplates == null || _effectTemplates.length == 0)
			return;

		// No effect on doors/walls
		if(env.character instanceof L2DoorInstance)
			return;

		boolean success = true;

		if(calcChance)
			success = Formulas.calcSkillSuccess(env, getResistType());

		for(EffectTemplate et : _effectTemplates)
		{
			if(et.isInstant())
				continue;

			L2Effect e = et.getEffect(env);

			if(e != null)
			{
				if(e.isSuccess(success))
				{
					env.target.fireMethodInvoked(MethodCollection.onEffectAdd, new Object[]{this, env.character, env.target, env.item});
					env.target.addEffect(e, effectTimeModifier);
				}
				else if(isOffensive())
					env.character.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addCharName(env.target).addSkillName(_displayId));
			}
		}
	}

	public L2Character getAimingTarget(L2Character cha)
	{
		return getAimingTarget(cha, cha.getTarget());
	}

	public L2Character getAimingTarget(L2Character cha, L2Object target)
	{
		if(debug)
			_log.info(this + " getAimingTarget: " + _targetType + " ");
		switch(_targetType)
		{
			case self:
			case ground:
				return cha;
			case summon:
				return cha.getPet();
			case enemy:
			case enemy_only:
				if(target == cha)
					return null;
				if(target instanceof L2Character)
					return (L2Character) target;
				break;
			case master:
				return cha.getPlayer();
			default:
				if(target instanceof L2Character)
					return (L2Character) target;
		}

		return null;
	}

	public List<L2Character> getTargets(L2Character cha, L2Character aimingTarget, boolean forceUse)
	{
		List<L2Character> targets = new ArrayList<>();

		switch(_affectScope)
		{
			case single:
			case none:
				targets.add(aimingTarget);
				break;
			case clan:
			case dead_clan:
				targets.add(aimingTarget);
				for(L2Character target : aimingTarget.getKnownCharacters(getSkillRadius(), getSkillRadius()))
					if(target != null && target.isPlayer() && aimingTarget.isClanMember(target) && (target.isDead() == (_affectScope == AffectScope.dead_clan)) && !target.isCursedWeaponEquipped() && (!aimingTarget.isPlayer() || ((L2Player) aimingTarget).getSessionVar("event_team_pvp") == null || aimingTarget.getTeam() == target.getTeam()))
						targets.add(target);
				targets = applyAffectLimit(targets);
				break;
			case dead_union:
				targets.add(aimingTarget);
				for(L2Character target : aimingTarget.getKnownCharacters(getSkillRadius(), getSkillRadius()))
					if(target != null && target.isPlayer() && (aimingTarget.isCommandChanelMember(target) || aimingTarget.isPartyMember(target)) && target.isDead() && !target.isCursedWeaponEquipped())
						targets.add(target);
				targets = applyAffectLimit(targets);
				break;
			case party:
				if(getSkillRadius() < 0 && aimingTarget.getPlayer() != null && aimingTarget.getPlayer().getParty() != null)
					targets.addAll(aimingTarget.getPlayer().getParty().getPartyMembers());
				else
				{
					targets.add(aimingTarget);
					for(L2Character target : aimingTarget.getKnownCharacters(getSkillRadius(), getSkillRadius()))
						if(target != null && aimingTarget.isPartyMember(target))
							targets.add(target);
				}
				targets = checkFriends(cha, aimingTarget, targets, forceUse);
				targets = applyAffectLimit(targets);
				break;
			case party_clan:
				if(getSkillRadius() < 0 && aimingTarget.getPlayer() != null && aimingTarget.getPlayer().getParty() != null)
					targets.addAll(aimingTarget.getPlayer().getParty().getPartyMembers());
				else
				{
					targets.add(aimingTarget);
					for(L2Character target : aimingTarget.getKnownCharacters(getSkillRadius(), getSkillRadius()))
						if(target != null && (aimingTarget.isPartyMember(target) || aimingTarget.isClanMember(target)) && !target.isCursedWeaponEquipped())
							targets.add(target);
				}
				targets = checkFriends(cha, aimingTarget, targets, forceUse);
				targets = applyAffectLimit(targets);
				break;
			case around:
				targets.addAll(aimingTarget.getKnownCharacters(getSkillRadius(), getSkillRadius()));
				targets.add(aimingTarget);
				targets = checkFriends(cha, aimingTarget, targets, forceUse);
				if(isOffensive())
					targets = canSeeCheck(aimingTarget, targets);
				targets = applyAffectLimit(targets);
				break;
			case range_sort_by_hp:
				TreeSet<L2Character> sorted = new TreeSet<>(SortByHpComparator.getInstance());
				for(L2Character target : aimingTarget.getKnownCharacters(getSkillRadius(), getSkillRadius()))
				{
					if(target != cha && !target.isDead() && !(target instanceof L2DoorInstance ) && !(target instanceof L2SiegeHeadquarterInstance))
						sorted.add(target);
				}
				targets.addAll(sorted);
				targets.add(aimingTarget);
				targets = checkFriends(cha, aimingTarget, targets, forceUse);
				targets = applyAffectLimit(targets);
				break;
			case line:
			case square:
				Location sp = cha.getLoc();
				if(cha.getPrevLoc() != null)
					sp = cha.getPrevLoc();

				int angle = cha == aimingTarget ? (int) Util.convertHeadingToDegree(aimingTarget.getHeading()) : (int) Util.calculateAngleFrom(aimingTarget.getX(), aimingTarget.getY(), sp.getX(), sp.getY());

				angle += _sector_a1;

				if(angle >= 360)
					angle -= 360;

				Location ap = _sector_r1 != 0 ? Util.getPointInRadius(sp, _sector_r1, angle) : sp;

				int r2 = _affectScope == AffectScope.line ? (int) aimingTarget.getDistance(sp.getX(), sp.getY()) + _sector_r2 : _sector_r2;
				Location p1 = Util.getPointInRadius(ap, _sector_a2 / 2, angle + 90);
				Location p2 = Util.getPointInRadius(ap, _sector_a2 / 2, angle - 90);
				Location p3 = Util.getPointInRadius(p2, r2, angle);
				Location p4 = Util.getPointInRadius(p1, r2, angle);

				if(debug)
				{
					ExShowTrace trace = new ExShowTrace();
					trace.addTrace(p1.getX(), p1.getY(), p1.getZ(), 15000);
					trace.addTrace(p2.getX(), p2.getY(), p2.getZ(), 15000);
					trace.addTrace(p3.getX(), p3.getY(), p3.getZ(), 15000);
					trace.addTrace(p4.getX(), p4.getY(), p4.getZ(), 15000);
					cha.broadcastPacket(trace);
				}

				Polygon poly = new Polygon();
				poly.addPoint(p1.getX(), p1.getY());
				poly.addPoint(p2.getX(), p2.getY());
				poly.addPoint(p3.getX(), p3.getY());
				poly.addPoint(p4.getX(), p4.getY());

				List<L2Character> tmpTargets = new ArrayList<>();

				for(L2Character target : cha.getKnownCharacters(_sector_r1 + r2, _sector_r1 + r2))
					if(target != null && poly.contains(target.getX(), target.getY()))
						tmpTargets.add(target);
				targets = checkFriends(cha, aimingTarget, tmpTargets, forceUse);
				if(isOffensive())
					targets = canSeeCheck(aimingTarget, targets);
				targets = applyAffectLimit(targets);
				break;
			case sector:
				sp = cha.getLoc();
				if(cha.getPrevLoc() != null)
					sp = cha.getPrevLoc();

				angle = cha == aimingTarget ? (int) Util.convertHeadingToDegree(aimingTarget.getHeading()) : (int) Util.calculateAngleFrom(aimingTarget.getX(), aimingTarget.getY(), sp.getX(), sp.getY());

				angle += _sector_a1;

				if(angle >= 360)
					angle -= 360;

				ap = _sector_r1 != 0 ? Util.getPointInRadius(sp, _sector_r1, angle) : sp;

				int aMin = angle - _sector_a2 / 2;
				int aMax = angle + _sector_a2 / 2;

				if(aMin < 0)
					aMin += 360;

				if(aMax >= 360)
					aMax -= 360;

				if(debug)
				{
					L2ItemInstance item = ItemTable.getInstance().createItem("skill_sector", 57, 1, aimingTarget.getPlayer(), null);
					item.dropMe(aimingTarget, ap);
					item = ItemTable.getInstance().createItem("skill_sector", 57, 2, aimingTarget.getPlayer(), null);
					item.dropMe(aimingTarget, Util.getPointInRadius(ap, (int) (_sector_r2 * 0.33), aMin));
					item = ItemTable.getInstance().createItem("skill_sector", 57, 3, aimingTarget.getPlayer(), null);
					item.dropMe(aimingTarget, Util.getPointInRadius(ap, (int) (_sector_r2 * 0.66), aMin));
					item = ItemTable.getInstance().createItem("skill_sector", 57, 4, aimingTarget.getPlayer(), null);
					item.dropMe(aimingTarget, Util.getPointInRadius(ap, _sector_r2, aMin));
					item = ItemTable.getInstance().createItem("skill_sector", 57, 5, aimingTarget.getPlayer(), null);
					item.dropMe(aimingTarget, Util.getPointInRadius(ap, (int) (_sector_r2 * 0.33), aMax));
					item = ItemTable.getInstance().createItem("skill_sector", 57, 6, aimingTarget.getPlayer(), null);
					item.dropMe(aimingTarget, Util.getPointInRadius(ap, (int) (_sector_r2 * 0.66), aMax));
					item = ItemTable.getInstance().createItem("skill_sector", 57, 7, aimingTarget.getPlayer(), null);
					item.dropMe(aimingTarget, Util.getPointInRadius(ap, _sector_r2, aMax));
					int b = aMax;
					if(aMin > b)
						b += 360;
					int c = aMin + 5;
					int j = 8;
					while(c <= b)
					{
						item = ItemTable.getInstance().createItem("skill_sector", 57, j, aimingTarget.getPlayer(), null);
						item.dropMe(aimingTarget, Util.getPointInRadius(ap, _sector_r2, c));
						c += 5;
						j++;
					}
				}

				tmpTargets = new ArrayList<>();

				for(L2Character target : cha.getKnownCharacters(_sector_r1 + _sector_r2, _sector_r1 + _sector_r2))
					if(target != null)
					{
						if(debug)
							_log.info("knownCharacters in range " + (_sector_r1 + _sector_r2) + " has: " + target);

						int xa = (int) Util.calculateAngleFrom(target.getX(), target.getY(), ap.getX(), ap.getY());
						if(xa == 360)
							xa = 0;
						if(debug)
							_log.info("sector: " + aMin + ";" + aMax + " xa: " + xa);
						if(aMin > aMax)
						{
							if((aMin < xa && xa < 360) || (xa >= 0 && xa < aMax))
								tmpTargets.add(target);
						}
						else if(aMin < xa && xa < aMax)
							tmpTargets.add(target);
					}
				if(debug)
					for(L2Character t : tmpTargets)
						_log.info("tmpTargets has: " + t);
				targets = checkFriends(cha, aimingTarget, tmpTargets, forceUse);
				if(isOffensive())
					targets = canSeeCheck(aimingTarget, targets);
				targets = applyAffectLimit(targets);
				break;
		}

		return targets;
	}

	public boolean checkCondition(L2Character cha, L2Character target, L2ItemInstance usedItem, boolean forceUse, boolean first)
	{
		if(cha.isDead() || target == null)
			return false;

		L2Player player = cha.getPlayer();

		if(player != null && player.isInBoat() && _isOffensive)
		{
			player.sendActionFailed();
			return false;
		}

		if(!getWeaponDependency(cha))
			return false;

		if(first && (cha.isSkillDisabled(_id) || usedItem != null && usedItem.getItem().getDelayShareGroup() > 0 && cha.isSkillDisabled(-usedItem.getItem().getDelayShareGroup())))
		{
			if(Config.ALT_SHOW_REUSE_MSG)
			{
				if(cha.isPlayer())
					cha.sendSkillReuseMessage(_id, _displayLevel, usedItem);
				else if(cha instanceof L2Summon)
					cha.getPlayer().sendPacket(Msg.THAT_PET_SERVITOR_SKILL_CANNOT_BE_USED_BECAUSE_IT_IS_RECHARGING);
			}
			return false;
		}

		if(isHandler() && cha.isPet() && !allowPetUse())
		{
			if(cha.getPlayer() != null)
				cha.getPlayer().sendPacket(Msg.THIS_PET_CANNOT_USE_THIS_ITEM);
			return false;
		}

		if(cha.isInAirShip() && !isHandler())
		{
			cha.sendPacket(Msg.THIS_ACTION_IS_PROHIBITED_WHILE_MOUNTED_OR_ON_AN_AIRSHIP);
			return false;
		}

		if(first && cha.getCurrentMp() < (isMagic() ? getMpConsume1() + Formulas.calcSkillMpConsume(cha, this, getMpConsume2(), false) : Formulas.calcSkillMpConsume(cha, this, getMpConsume(), false)))
		{
			if(cha.isPlayer())
				cha.sendPacket(Msg.NOT_ENOUGH_MP);
			else if(cha instanceof L2Summon)
				cha.getPlayer().sendPacket(Msg.THAT_SKILL_CANNOT_BE_USED_BECAUSE_YOUR_PET_SERVITOR_LACKS_SUFFICIENT_MP);
			return false;
		}

		if(cha.getCurrentHp() < _hpConsume + 1)
		{
			if(cha.isPlayer())
				cha.sendPacket(Msg.NOT_ENOUGH_HP);
			else if(cha instanceof L2Summon)
				cha.getPlayer().sendPacket(Msg.THAT_SKILL_CANNOT_BE_USED_BECAUSE_YOUR_PET_SERVITOR_LACKS_SUFFICIENT_HP);
			return false;
		}

		if(_mpUsage > 0 && player != null && first)
		{
			L2ItemInstance item = player.getInventory().getEquippedItemBySkill(this);
			if(item == null || item.getMana() < (_mpUsage + 1) || !item.isShadowItem())
				return false;
		}

		if(!(_isItemHandler || _altUse || isToggle()) && isMuted(cha))
			return false;

		if(_soulsConsume > cha.getConsumedSouls())
		{
			cha.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGHT_SOUL));
			return false;
		}

		if((_castType == CastType.rush_back || _castType == CastType.rush_front || _castType == CastType.rush_behind || _flyRadius > 0) && cha.isStatActive(Stats.BLOCK_MOVE))
		{
			cha.sendPacket(Msg.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
			return false;
		}

		if(cha.getIncreasedForce() < _condCharges || cha.getIncreasedForce() < _numCharges)
		{
			cha.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(_displayId));
			return false;
		}
		else if(!first && _numCharges > 0)
			cha.setIncreasedForce(cha.getIncreasedForce() - getNumCharges());


		if(cha instanceof L2Playable && first && _itemConsume[0] > 0)
			for(int i = 0; i < _itemConsume.length; i++)
			{
				L2ItemInstance requiredItems;

				if(cha.isPet())
					requiredItems = ((L2Playable) cha).getInventory().getItemByItemId(_itemConsumeId[i]);
				else
					requiredItems = player.getInventory().getItemByItemId(_itemConsumeId[i]);

				if(requiredItems == null || requiredItems.getCount() < _itemConsume[i])
				{
					player.sendPacket(Msg.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
					return false;
				}
			}

		if(first && cha != target && _castType.toString().startsWith("rush_") && !GeoEngine.canMoveToCoord(cha.getX(), cha.getY(), cha.getZ(), target.getX(), target.getY(), target.getZ(), cha.getReflection()))
		{
			cha.sendPacket(Msg.THE_TARGET_IS_LOCATED_WHERE_YOU_CANNOT_CHARGE);
			return false;
		}

		if(player != null)
		{
			//if((_isItemHandler || _altUse) && player.isInOlympiadMode() && !isForCubic() && !Config.ALT_OLY_ENABLED_SKILLS.contains(_displayId))
			if(player.isInOlympiadMode() && !_olympiadUse)
			{
				player.sendPacket(Msg.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
				return false;
			}

			if(player.inObserverMode())
			{
				cha.sendPacket(new SystemMessage(SystemMessage.OBSERVERS_CANNOT_PARTICIPATE));
				return false;
			}

			if(_isItemHandler && player.isInFlyingTransform() && !_flyingTransformUse)
			{
				cha.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(_displayId));
				return false;
			}

			if(_rideState > 0 && (_rideState & player.getRideState()) != player.getRideState())
				return false;

			// If summon siege golem, hog cannon, Swoop Cannon, check its ok to place the flag
			if(_id == 13 || _id == 299 || _id == 448)
			{
				SystemMessage sm = null;
				Siege siege = SiegeManager.getSiege(player);
				if(siege == null)
					sm = new SystemMessage(SystemMessage.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
				else if(!siege.isInProgress() || !siege.checkIsClanRegistered(player.getClanId()))
					sm = new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(_displayId);

				if(sm != null)
				{
					player.sendPacket(sm);
					return false;
				}
			}

			if(player.isFishing() && _id != 1312 && _id != 1313 && _id != 1314)
			{
				player.sendPacket(Msg.ONLY_FISHING_SKILLS_ARE_AVAILABLE);
				return false;
			}

			if(player.isCombatFlagEquipped() && _id != 3318 && _id != 847)
			{
				player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(_displayId));
				return false;
			}
		}

		SystemMessage msg = checkTarget(cha, target, forceUse, first);
		if(msg != null)
		{
			if(!isForCubic() && cha.getPlayer() != null)
				cha.getPlayer().sendPacket(msg);
			return false;
		}

		if(getPreCondition() == null)
			return true;

		Env env = new Env(cha, target, this);
		env.item = usedItem;
		env.first = first;

		if(!getPreCondition().test(env))
		{
			if(isForCubic())
				return false;

			String condMsg = getPreCondition().getMessage();
			int condMsgId = getPreCondition().getMessageId();
			if(condMsgId != 0)
			{
				SystemMessage sm = new SystemMessage(condMsgId);
				if(usedItem != null)
					sm.addItemName(usedItem.getItemId());
				else
					sm.addSkillName(getDisplayId(), getDisplayLevel());
				cha.sendPacket(sm);
			}
			else if(condMsg != null)
			{
				cha.sendMessage(condMsg);
			}
			return false;
		}

		return true;
	}

	public SystemMessage checkTarget(L2Character cha, L2Character target, boolean forceUse, boolean first)
	{
		if(debug)
			_log.info(this + " checkTarget: skill_target_type: " + _targetType + " target_type: " + target.getTargetRelation(cha, isOffensive()));

		if(cha.isNpc())
			return null;

		if(_targetType == TargetType.none || _targetType == TargetType.ground)
			return null;

		if(_targetType == TargetType.master)
			return cha.getPlayer() == target ? null : Msg.INVALID_TARGET;

		if(isOffensive())
		{
			if(cha.isInZonePeace() && (target.isPlayer() || (target.isPet() && target.getPlayer() != cha) || (target.isSummon() && target.getPlayer() != cha)))
				return Msg.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE;
			else if(target.isInZonePeace() && (target.isPlayer() || (target.isPet() && target.getPlayer() != cha) || (target.isSummon() && target.getPlayer() != cha)))
				return Msg.A_MALICIOUS_SKILL_CANNOT_BE_USED_WHEN_AN_OPPONENT_IS_IN_THE_PEACE_ZONE;
		}

		if(first && cha != target && getCastRange() > 0 && !GeoEngine.canSeeTarget(cha, target))
			return Msg.CANNOT_SEE_TARGET;

		if(!first && target != cha && getCastRange() > 0 && !cha.isInRange(target.getLoc(), getCastRange() + (getCastRange() < 200 ? 400 : 500)))
			return Msg.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED;

		TargetType type = target.getTargetRelation(cha, isOffensive());

		if(type == TargetType.invalid)
			return Msg.INVALID_TARGET;

		//if((type == TargetType.target || type == TargetType.at_war) && target instanceof L2Playable && cha.getPlayer() != null && cha.getPlayer().isInOlympiadMode() && !cha.getPlayer().isOlympiadStart() )
		//	return Msg.INVALID_TARGET;

		if(type == _targetType)
			return null;

		if(type == TargetType.summon && _targetType == TargetType.target)
			return null;

		if((type == TargetType.siege_ally) && forceUse)
		{
			if(cha.getPlayer().getSiegeState() == 3)
				return Msg.A_POWERFUL_ATTACK_IS_PROHIBITED_WHEN_ALLIED_TROOPS_ARE_THE_TARGET;

			return Msg.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SEIGE_SIDE_TEMPORARY_ALLIED_MEMBERS;
		}

		if(_targetType == TargetType.enemy)
		{
			if((type == TargetType.target || type == TargetType.at_war || type == TargetType.summon) && forceUse || type == TargetType.enemy_only || type == TargetType.treasure)
				return null;
		}
		else if(_targetType == TargetType.enemy_only && (type == TargetType.at_war && forceUse || type == TargetType.treasure))
			return null;
		else if(_targetType == TargetType.target)
		{
			if(type == TargetType.self || type == TargetType.door || (type == TargetType.enemy_only || type == TargetType.enemy || type == TargetType.at_war || type == TargetType.treasure) && forceUse)
				return null;
		}
		else if(_targetType == TargetType.door_treasure && (type == TargetType.door || type == TargetType.treasure))
			return null;
		else if(_targetType == TargetType.enemy_not && (type == TargetType.master || type == TargetType.self || type == TargetType.siege_ally || type == TargetType.summon || type == TargetType.target))
			return null;

		return Msg.INVALID_TARGET;
	}

	public boolean isOffensive()
	{
		if(_isOffensive)
			return _isOffensive;

		return _targetType == TargetType.enemy || _targetType == TargetType.enemy_only || _targetType == TargetType.door_treasure || _targetType == TargetType.npc_body || (_targetType == TargetType.self && (_affectObject == AffectObject.not_friend || _affectObject == AffectObject.undead_real_enemy || _affectObject == AffectObject.object_dead_npc_body) && _affectScope != AffectScope.single) || _isDebuff;
	}

	private List<L2Character> applyAffectLimit(List<L2Character> targets)
	{
		if(_affect_min + _affect_max < 1)
			return targets;

		int limit = _affect_min + Rnd.get(0, _affect_max);

		if(targets.size() < limit)
			return targets;

		while(targets.size() > limit)
			targets.remove(Rnd.get(targets.size()));

		return targets;
	}

	private List<L2Character> checkFriends(L2Character cha, L2Character aimingTarget, List<L2Character> targets, boolean forceUse)
	{
		List<L2Character> result = new ArrayList<>();

		switch(_affectObject)
		{
			case friend:
				for(L2Character target : targets)
					if(target == aimingTarget || target != null && !target.isDead() && (!target.isPlayer() || !((L2Player) target).isInvisible()) && aimingTarget.isFriend(target))
						result.add(target);
				break;
			case not_friend:
				TargetType aimingType = aimingTarget.getTargetRelation(cha, isOffensive());
				if(aimingType == TargetType.enemy || aimingType == TargetType.enemy_only || aimingType == TargetType.at_war || (forceUse && aimingType == TargetType.target))
				{
					if(targets.contains(aimingTarget))
					{
						result.add(aimingTarget);
						targets.remove(aimingTarget);
					}
				}
				else
					targets.remove(aimingTarget);

				for(L2Character target : targets)
				{
					TargetType targetType = target.getTargetRelation(cha, isOffensive());
					if(target != null && !target.isInZonePeace() && !target.isInvul() && (!target.isPlayer() || !((L2Player) target).isInvisible()) && target.isVisible() && !target.isDead() &&
							(targetType == TargetType.enemy || targetType == TargetType.enemy_only || targetType == TargetType.at_war) &&
							(!aimingTarget.isInZonePeace() || target instanceof L2NpcInstance) && !(target instanceof L2DoorInstance))
						result.add(target);
				}
				break;
			case undead_real_enemy:
				aimingType = aimingTarget.getTargetRelation(cha, isOffensive());
				if(aimingTarget.isUndead() && (aimingType == TargetType.enemy || aimingType == TargetType.enemy_only || aimingType == TargetType.at_war || (forceUse && aimingType == TargetType.target)))
				{
					if(targets.contains(aimingTarget))
					{
						result.add(aimingTarget);
						targets.remove(aimingTarget);
					}
				}
				else
					targets.remove(aimingTarget);

				for(L2Character target : targets)
				{
					TargetType targetType = target.getTargetRelation(cha, isOffensive());
					if(target != null && target.isUndead() && (!target.isInZonePeace() || target.isNpc()) && !target.isInvul() && target.isVisible() && !target.isDead() &&
							(targetType == TargetType.enemy || targetType == TargetType.enemy_only || targetType == TargetType.at_war))
						result.add(target);
				}
				break;
			case object_dead_npc_body:
				aimingType = aimingTarget.getTargetRelation(cha, isOffensive());
				if(aimingTarget.isNpc() && (aimingType == TargetType.npc_body))
				{
					if(targets.contains(aimingTarget))
					{
						result.add(aimingTarget);
						targets.remove(aimingTarget);
					}
				}
				else
					targets.remove(aimingTarget);

				for(L2Character target : targets)
				{
					TargetType targetType = target.getTargetRelation(cha, isOffensive());
					if(target != null && target.isNpc() && targetType == TargetType.npc_body)
						result.add(target);
				}
				break;
		}

		return result;
	}

	public List<L2Character> canSeeCheck(L2Character aimingTarget, List<L2Character> targets)
	{
		for(int i = 0; i < targets.size(); i++)
		{
			if(!GeoEngine.canSeeTarget(aimingTarget, targets.get(i)))
				targets.remove(i);
		}

		return targets;
	}

	public final boolean altUse()
	{
		return _altUse;
	}

	public final SkillType getSkillType()
	{
		return _skillType;
	}

	public final BaseStats getBaseStat()
	{
		return _baseStat == BaseStats.NONE && _isDebuff ? BaseStats.MEN : _baseStat;
	}

	public final int getActivateRate()
	{
		return _activateRate;
	}

	public final byte getMagicLevel()
	{
		return _magicLevel;
	}

	public final double getLevelMod()
	{
		return _levelMod;
	}

	public final boolean isCancelable()
	{
		return _cancelable;
	}

	public final boolean getShieldIgnore()
	{
		return _shieldignore;
	}

	public final double getCritRate()
	{
		return _critRate;
	}

	public final Element getElement()
	{
		return _element;
	}

	public final int getElementPower()
	{
		return _elementPower;
	}

	public final boolean isOverhit()
	{
		return _overhit;
	}

	public final boolean isStaticReuse()
	{
		return _staticReuse || _isItemHandler;
	}

	public final boolean isStaticHitTime()
	{
		return _staticHitTime || _isItemHandler;
	}

	/**
	 * Return the power of the skill.<BR><BR>
	 */
	public final double getPower(L2Character attacker, L2Character target)
	{
		return attacker instanceof L2Playable && target instanceof L2Playable ? _pvpPower : _power;
	}

	/**
	 * @return Returns the castRange.
	 */
	public final int getCastRange()
	{
		return _castRange;
	}

	/**
	 * @return Return the castRange for AI use
	 */
	public final int getCastRangeForAi()
	{
		return _castRange > 0 ? _castRange : _skillRadius;
	}

	/**
	 * @return Returns the hpConsume.
	 */
	public final int getHpConsume()
	{
		return _hpConsume;
	}

	/**
	 * @return Returns the id.
	 */
	public final int getId()
	{
		return _id;
	}

	public final int getIndex()
	{
		return _skillIndex;
	}

	public final int getDisplayId()
	{
		return _displayId;
	}

	public int getDisplayLevel()
	{
		return _displayLevel;
	}

	public int getIncreaseLevel()
	{
		return _increaseLevel;
	}

	public void setDisplayLevel(Short lvl)
	{
		_displayLevel = lvl;
	}

	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}

	/**
	 * @return Returns the itemConsume.
	 */
	public final int[] getItemConsume()
	{
		return _itemConsume;
	}

	/**
	 * @return Returns the itemConsumeId.
	 */
	public final int[] getItemConsumeId()
	{
		return _itemConsumeId;
	}

	public final boolean isHandler()
	{
		return _isItemHandler;
	}

	/**
	 * Является ли скилл общим
	 */
	public final boolean isCommon()
	{
		return _isCommon;
	}

	/**
	 * @return Returns the level.
	 */
	public final int getLevel()
	{
		return _level;
	}

	/**
	 * @return Returns true if skill is magic.
	 */
	public boolean isMagic()
	{
		return _isMagic;
	}

	public boolean isPhysic()
	{
		return _isPhysic;
	}

	public final boolean isSongDance()
	{
		return _isDanceSong;
	}

	public boolean isMuted(L2Character cha)
	{
		return cha.isMuted() && isMagic() || cha.isPMuted() && isPhysic();
	}

	/**
	 * @return Returns the mpConsume as _mpConsume1 + _mpConsume2.
	 */
	public final int getMpConsume()
	{
		return _mpConsume1 + _mpConsume2;
	}

	/**
	 * @return Returns the mpConsume1.
	 */
	public final int getMpConsume1()
	{
		return _mpConsume1;
	}

	/**
	 * @return Returns the mpConsume2.
	 */
	public final int getMpConsume2()
	{
		return _mpConsume2;
	}

	/**
	 * @return Returns the name.
	 */
	public final String getName()
	{
		return _name;
	}

	/**
	 * @return Returns the reuseDelay.
	 */
	public final long getReuseDelay()
	{
		return _reuseDelay;
	}

	public final int getHitTime()
	{
		return _hitTime;
	}

	public final int getCoolTime()
	{
		return _coolTime;
	}

	public final int getHitCancelTime()
	{
		return _hitCancelTime;
	}

	public final int getSkillRadius()
	{
		return _skillRadius;
	}

	public final boolean isActive()
	{
		return _operateType == SkillOpType.OP_ACTIVE;
	}

	public final boolean isPassive()
	{
		return _operateType == SkillOpType.OP_PASSIVE;
	}

	public final boolean isToggle()
	{
		return _operateType == SkillOpType.OP_TOGGLE;
	}

	public final boolean isOnAttack()
	{
		return _chanceOpType == ChanceOpType.ON_ATTACK;
	}

	public final boolean isOnCrit()
	{
		return _chanceOpType == ChanceOpType.ON_CRIT;
	}

	public final boolean isOnEvaded()
	{
		return _chanceOpType == ChanceOpType.ON_EVADED;
	}

	public final boolean isOnShield()
	{
		return _chanceOpType == ChanceOpType.ON_SHIELD;
	}

	public final boolean isOnMagicAttack()
	{
		return _chanceOpType == ChanceOpType.ON_MAGIC_ATTACK;
	}

	public final boolean isOnUnderAttack()
	{
		return _chanceOpType == ChanceOpType.ON_ATTACKED;
	}

	public final boolean isOnMagicAttacked()
	{
		return _chanceOpType == ChanceOpType.ON_MAGIC_ATTACKED;
	}

	public final boolean isOnDamageReceived()
	{
		return _chanceOpType == ChanceOpType.ON_DAMAGE_RECEIVED;
	}

	public final boolean getCanLearn(ClassId cls)
	{
		return _canLearn == null || _canLearn.contains(cls);
	}

	public final boolean getWeaponDependency(L2Character cha)
	{
		return getWeaponDependency(cha, true);
	}

	public final boolean getWeaponDependency(L2Character cha, boolean sendMsg)
	{
		if(_weaponsAllowed == 0)
			return true;

		if(cha.getActiveWeaponInstance() != null && cha.getActiveWeaponItem() != null)
			if((cha.getActiveWeaponItem().getItemType().mask() & _weaponsAllowed) != 0)
				return true;

		if(cha.getSecondaryWeaponInstance() != null && cha.getSecondaryWeaponItem() != null)
			if((cha.getSecondaryWeaponItem().getItemType().mask() & _weaponsAllowed) != 0)
				return true;

		if(sendMsg)
			cha.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(_displayId));
		return false;
	}


	public final Func[] getStatFuncs(L2Character cha)
	{
		if(_funcTemplates == null)
			return _emptyFunctionSet;
		ArrayList<Func> funcs = new ArrayList<Func>();
		for(FuncTemplate t : _funcTemplates)
		{
			Env env = new Env(cha, null, this);
			Func f = t.getFunc(env, this); // skill is owner
			if(f != null)
				funcs.add(f);
		}
		if(funcs.size() == 0)
			return _emptyFunctionSet;
		return funcs.toArray(new Func[funcs.size()]);
	}

	public final void attach(FuncTemplate f)
	{
		if(_funcTemplates == null)
			_funcTemplates = new FuncTemplate[]{f};
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	public final void attach(EffectTemplate effect)
	{
		if(_effectTemplates == null)
			_effectTemplates = new EffectTemplate[]{effect};
		else
		{
			boolean attach = true;
			if(!effect.isInstant())
				for(EffectTemplate template : _effectTemplates)
					if(!template.isInstant() && template._applyOnCaster == effect._applyOnCaster)
					{
						template.addChildTemplate(effect);
						attach = false;
						break;
					}
			if(attach)
			{
				int len = _effectTemplates.length;
				EffectTemplate[] tmp = new EffectTemplate[len + 1];
				System.arraycopy(_effectTemplates, 0, tmp, 0, len);
				tmp[len] = effect;
				_effectTemplates = tmp;
			}
		}

		if(!_useSS && Config.USE_SHOTS_EFFECT_LIST.contains(effect._name))
			_useSS = true;
	}

	public final void attach(Condition c)
	{
		preCondition = c;
	}

	@Override
	public String toString()
	{
		return _toString;
	}

	public int getEffectPoint()
	{
		return _effectPoint;
	}

	public NextAction getNextAction()
	{
		return _nextAction;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj == this || (obj instanceof L2Skill && ((L2Skill) obj).getId() == getId() && ((L2Skill) obj).getLevel() == getLevel());
	}

	public boolean isSaveable()
	{
		return _isSaveable;
	}

	public int getForceId()
	{
		return _forceId;
	}

	public int getChance()
	{
		return _chance;
	}

	public int getSoulsConsume()
	{
		return _soulsConsume;
	}

	public int getMaxSoulsConsume()
	{
		return _maxSoulsConsume;
	}

	public int getEffectNpcId()
	{
		return _effectNpcId;
	}

	public AddedSkill[] getAddedSkills()
	{
		if(_addedSkills == null)
			return new AddedSkill[0];
		return _addedSkills;
	}

	public int getNpcId()
	{
		return _npcId;
	}

	public int getNumCharges()
	{
		return _numCharges;
	}

	public int getMatak()
	{
		return _mAtk;
	}

	/**
	 * Может ли скилл тратить шоты, для хендлеров всегда false
	 */
	public boolean isSSPossible()
	{
		return _useSS && !_isItemHandler;
	}

	public FlyType getFlyType()
	{
		return _flyType;
	}

	public int getFlyRadius()
	{
		return _flyRadius;
	}

	/**
	 * Returns minimal skill reuse that will be added as cooldown after skill ad.
	 * Returned value is in seconds so * 1000 to get milliseconds
	 *
	 * @return minimal skill reuse that must be added after skill add.
	 */
	public int getMinReuseDelayOnSkillAdd()
	{
		return _minReuseDelayOnSkillAdd;
	}

	public boolean isDebuff()
	{
		return _isDebuff;
	}

	public boolean isBuff()
	{
		return !_isDebuff;
	}

	public ResistType getResistType()
	{
		return _resistType;
	}

	public int getTriggerSkillId()
	{
		return _triggerSkillId;
	}

	public int getTriggerSkillLvl()
	{
		return _triggerSkillLvl;
	}

	public boolean isCastTimeEffect()
	{
		return _isCastTimeEffect;
	}

	public boolean isForCubic()
	{
		return _isForCubic;
	}

	public boolean allowPetUse()
	{
		return _allowPetUse;
	}

	public boolean isHidden()
	{
		return _hidden;
	}
	
	public int getReuseSkillId()
	{
		return _reuseSkillId;
	}
	
	public int getChainIndex()
	{
		return _chainIndex;
	}
	
	public int getChainSkillId()
	{
		return _chainSkillId;
	}

	/**
	 * @return Returns the _targetConsumeId.
	 */
	public final int getTargetConsumeId()
	{
		return _targetConsumeId;
	}

	/**
	 * @return Returns the targetConsume.
	 */
	public final int getTargetConsume()
	{
		return _targetConsume;
	}

	public final boolean isShildHit()
	{
		return _shieldHit;
	}

	public final int getAbnormalLevel()
	{
		return _abnormalLevel;
	}

	public final AbnormalVisualEffect getAbnormalVe()
	{
		return _abnormalVe;
	}

	public final long getAbnormalTime()
	{
		return _abnormalTime;
	}

	public final void notifyEffectRemoved(L2Character effected, L2Effect effect, boolean finished)
	{
		if((!_afterEffectFinished || finished) && _afterEffectSkillId > 0)
		{
			L2Skill aes = SkillTable.getInstance().getInfo(_afterEffectSkillId, _afterEffectSkillLvl);
			if(aes != null)
			{
				if(!effected.isDead())
				{
					List<L2Character> targets = new ArrayList<>(1);
					targets.add(effected);
					aes.useSkill(effected, targets);
				}
			}
			else
				_log.warn("Warning! AfterEffect skill not found skillId: " + _afterEffectSkillId + " level: " + _afterEffectSkillLvl);
		}
	}

	public EffectTemplate getTimedEffectTemplate()
	{
		if(_effectTemplates != null)
			for(EffectTemplate templte : _effectTemplates)
				if(!templte.isInstant())
					return templte;

		return null;
	}

	public boolean isFlyingTransformUse()
	{
		return _flyingTransformUse;
	}

	public boolean isTriggered()
	{
		return _isTriggered;
	}

	public boolean isHerb()
	{
		return _isHerb;
	}

	public boolean isHeroSkill()
	{
		return _id == 395 || _id == 396 || _id == 1374 || _id == 1375 || _id == 1376;
	}

	public boolean isNobleSkill()
	{
		return (_id >= 325 && _id <= 327) || (_id >= 1323 && _id <= 1327);
	}

	public boolean isPotion()
	{
		return _isPotion;
	}

	public int getTrapLifeTime()
	{
		return _trapLifeTime;
	}

	public int getMpUsage()
	{
		return _mpUsage;
	}

	public Condition getPreCondition()
	{
		return preCondition;
	}

	public GArray<String> getAbnormalTypes()
	{
		return _abnormalTypes;
	}

	public TargetType getSkillTargetType()
	{
		return _targetType;
	}

	public boolean isForgotten()
	{
		return _isForgotten;
	}

	public String getAbnormals()
	{
		String ret = "";
		for(String abnormal : _abnormalTypes)
			ret += abnormal + ";";
		return ret;
	}

	public CastType getCastType()
	{
		return _castType;
	}

	public Location getRushLoc(L2Character cha, L2Character aimingTarget)
	{
		switch(_castType)
		{
			case rush_back:
				cha.setHeading(aimingTarget, true);
				Location destiny = Util.getPointInRadius(cha.getLoc(), getFlyRadius(), (int) Util.convertHeadingToDegree(cha.getHeading()) + 180);
				if(cha.isFlying())
				{
					ArrayList<Location> list = GeoEngine.moveListInWater(cha.getX(), cha.getY(), cha.getZ(), destiny.getX(), destiny.getY(), destiny.getZ(), cha.getReflection());
					if(list.size() > 0)
						return list.get(list.size() - 1).geo2world();
					return cha.getLoc();
				}
				destiny = GeoEngine.moveCheck(cha.getX(), cha.getY(), cha.getZ(), destiny.getX(), destiny.getY(), cha.getReflection());
				if(cha.isInRange(destiny, 45))
					return cha.getLoc();
				return destiny;
			case rush_front:
				cha.setHeading(aimingTarget, true);
			
				if(_targetType == TargetType.self)
					destiny = Util.getPointInRadius(cha.getLoc(), getFlyRadius(), (int) Util.convertHeadingToDegree(cha.getHeading()));
				else
					destiny = cha.applyOffset(aimingTarget.getLoc(), 45);

				if(cha.isFlying())
				{
					ArrayList<Location> list = GeoEngine.moveListInWater(cha.getX(), cha.getY(), cha.getZ(), destiny.getX(), destiny.getY(), destiny.getZ(), cha.getReflection());
					if(list.size() > 0)
						return list.get(list.size() - 1).geo2world();
					return cha.getLoc();
				}
				destiny = GeoEngine.moveCheck(cha.getX(), cha.getY(), cha.getZ(), destiny.getX(), destiny.getY(), cha.getReflection());
				if(cha.isInRange(destiny, 50))
					return cha.getLoc();
				return destiny;
			case rush_behind:
				cha.setHeading(aimingTarget, true);
				destiny = Util.getPointInRadius(aimingTarget.getLoc(), 30, (int) Util.convertHeadingToDegree(aimingTarget.getHeading()) + 180);
				destiny = GeoEngine.moveCheck(cha.getX(), cha.getY(), cha.getZ(), destiny.getX(), destiny.getY(), cha.getReflection());
				destiny.setH(aimingTarget.getHeading());
				if(GeoEngine.canMoveToCoord(aimingTarget.getX(), aimingTarget.getY(), aimingTarget.getZ(), destiny.getX(), destiny.getY(), destiny.getZ(), cha.getReflection()))
					return destiny;
				destiny = GeoEngine.moveCheck(cha.getX(), cha.getY(), cha.getZ(), aimingTarget.getX(), aimingTarget.getZ(), cha.getReflection());
				if(cha.isInRange(destiny, 20))
					return cha.getLoc();
				return destiny;
		}
		return null;
	}

	public boolean hasEffect(String effectName)
	{
		for(EffectTemplate et : _effectTemplates)
			if(et._attrs.getString("name", "").equals(effectName))
				return true;

		return false;
	}

	public static class AddedSkill
	{
		final private int id;
		final private int level;
		final private int minLevel;
		final private int maxLevel;

		public AddedSkill(int id, int level, int minLevel, int maxLevel)
		{
			this.id = id;
			this.level = level;
			this.minLevel = minLevel;
			this.maxLevel = maxLevel;
		}

		public L2Skill getSkill()
		{
			return SkillTable.getInstance().getInfo(id, level);
		}

		public L2Skill getSkill(L2Player player)
		{
			if(level == -1)
			{
				int lvl = player.getSkillLevel(id);
				if(lvl > 0)
					return SkillTable.getInstance().getInfo(id, lvl);
				return null;
			}
			else if(level == 0)
			{
				short lvl = 1;
				L2Skill skill;
				while((skill = SkillTable.getInstance().getInfo(id, lvl)) != null)
				{
					if(skill.getMagicLevel() == player.getLevel())
						return skill;
					else if(skill.getMagicLevel() > player.getLevel() && SkillTable.getInstance().getInfo(id, lvl - 1) != null)
						return SkillTable.getInstance().getInfo(id, lvl - 1);

					lvl++;
				}
				return null;
			}
			else if(minLevel != 0 && maxLevel != 0)
			{
				if(player.getLevel() >= minLevel && player.getLevel() <= maxLevel)
					return SkillTable.getInstance().getInfo(id, level);
				return null;
			}
			return SkillTable.getInstance().getInfo(id, level);
		}

		public int getLevel()
		{
			return level;
		}

		public int getSkillId()
		{
			return id;
		}
	}

	public int getMagicType()
	{
		return _magicType;
	}
	
	public boolean isItemSkill() {
		return _name.contains("Item Skill") || _name.contains("Talisman");
	}

	public int getBuffProtectLevel()
	{
		return _buffProtectLevel;
	}

	private boolean _isUseMacro = false;
	public boolean isUseMacro()
	{
		return _isUseMacro;
	}

	public void setIsUseMacro(boolean isUse)
	{
		_isUseMacro = isUse;
	}
}
