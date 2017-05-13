package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.instancemanager.RaidBossSpawnManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.L2ObjectTasks.SoulIncreaseTask;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.base.ItemToDrop;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.serverpackets.SocialAction;
import ru.l2gw.gameserver.serverpackets.SpawnEmitter;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class manages all Monsters.
 * <p/>
 * L2MonsterInstance :<BR><BR>
 * <li>L2MinionInstance</li>
 * <li>L2RaidBossInstance </li>
 */
public class L2MonsterInstance extends L2NpcInstance
{
	protected final static Log _log = LogFactory.getLog(L2MonsterInstance.class.getName());
	public static Log _logBoss = LogFactory.getLog("boss");
	private boolean _returnHome;
	protected boolean _cursedDrop;

	public void setOverhitAttacker(L2Character overhitAttacker)
	{
		if(overhitAttacker == null)
			overhitAttackerId = 0;
		else
			overhitAttackerId = overhitAttacker.getStoredId();
	}

	protected final class RewardInfo
	{
		protected L2Character _attacker;
		protected long _dmg = 0;

		public RewardInfo(final L2Character attacker, final long dmg)
		{
			_attacker = attacker;
			_dmg = dmg;
		}

		public void addDamage(long dmg)
		{
			if(dmg < 0)
				dmg = 0;

			_dmg += dmg;
		}

		@Override
		public int hashCode()
		{
			return _attacker.getObjectId();
		}
	}

	public final class AbsorberInfo
	{
		/**
		 * The attacker L2Character concerned by this AbsorberInfo of this L2NpcInstance
		 */
		L2Player _absorber;
		int _crystalId;
		double _absorbedHP;

		AbsorberInfo(final L2Player attacker, final int crystalId, final double absorbedHP)
		{
			_absorber = attacker;
			_crystalId = crystalId;
			_absorbedHP = absorbedHP;
		}

		/**
		 * Return the Identifier of the absorber L2Character.<BR><BR>
		 */
		@Override
		public int hashCode()
		{
			return _absorber.getObjectId();
		}
	}

	private Boolean _dead = false;
	private Object _dieLock = new Object();

	/**
	 * True if an over-hit enabled skill has successfully landed on the L2NpcInstance
	 */
	private boolean _overhit;

	/**
	 * Stores the extra (over-hit) damage done to the L2NpcInstance when the attacker uses an over-hit enabled skill
	 */
	private double _overhitDamage;

	/**
	 * Stores the attacker who used the over-hit enabled skill on the L2NpcInstance
	 */
	private long overhitAttackerId;

	/**
	 * crops
	 */
	private L2ItemInstance[] _harvestItems;
	private Object _harvestLock = new Object();
	private L2Item _seeded;
	private L2Player _seeder;

	/**
	 * Table containing all Items that a Dwarf can Sweep on this L2NpcInstance
	 */
	private L2ItemInstance[] _sweepItems;
	private ReentrantLock _sweepLock = new ReentrantLock();
	private final int _raidPoints;
	protected boolean _canBeChamion;
	private int _isChampion = 0;

	/**
	 * Constructor of L2MonsterInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2MonsterInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2MonsterInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template to apply to the NPC
	 */
	public L2MonsterInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		_returnHome = getAIParams() == null || getAIParams().getBool("return_home", true);
		_raidPoints = getAIParams() != null ? getAIParams().getInteger("raid_points", 0) : 0;
		_canBeChamion = Config.ALT_CHAMPION_ENABLE && !isRaid() && getLevel() >= Config.ALT_CHAMP_MIN_LVL && getLevel() <= Config.ALT_CHAMP_MAX_LVL && (getAIParams() == null || !getAIParams().getBool("no_champion", false));
		_cursedDrop = getAIParams() == null || !getAIParams().getBool("no_cursed_drop", false);
	}

	@Override
	public boolean isMovementDisabled()
	{
		// Невозможность ходить для этих мобов
		return getNpcId() == 18344 || getNpcId() == 18345 || super.isMovementDisabled();
	}

	@Override
	public boolean isLethalImmune()
	{
		if(isMinion())
			return getLeader().isRaid();

		return getNpcId() == 22215 || getNpcId() == 22216 || getNpcId() == 22217 || _isChampion > 0 || super.isLethalImmune();
	}

	/**
	 * Return True if the attacker is not another L2MonsterInstance.<BR><BR>
	 */
	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return getTemplate().can_be_attacked == 1;
	}

	@Override
	public void onSpawn()
	{
		_dead = false;
		super.onSpawn();

		if(_canBeChamion && getReflection() > 0)
			_canBeChamion = false;
		// Clear mob spoil, absorbs, seed
		setSpoiled(false);
		_sweepItems = null;
		_seeded = null;
		_seeder = null;
		_cursedDrop = _cursedDrop && getReflection() == 0;
	}

	public void spawnMinions()
	{
		if(getMinionsData() != null)
			try
			{
				minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					public void run()
					{
						try
						{
							minionList.maintainMinions();
						}
						catch(Throwable e)
						{
							_log.warn("", e);
						}
					}
				}, getMaintenanceInterval());
			}
			catch(NullPointerException e)
			{
			}
	}

	@Override
	public void callMinionsToAssist(L2Character attacker, L2Character victim, int damage)
	{
		if(minionList.hasMinions())
			for(L2NpcInstance minion : minionList.getSpawnedMinions())
			{
				if(minion == null || minion.isDead() || minion == victim)
					continue;

				if(getDistance3D(minion) > 2000 && getNpcId() != 29001)
				{
					minion.teleToLocation(getMinionPosition());
					continue;
				}

				if(minion.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
					minion.getAI().notifyEvent(CtrlEvent.EVT_PARTY_ATTACKED, attacker, victim, damage);
					//minion.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100), skill);
			}
	}

	public void setDead(boolean dead)
	{
		_dead = dead;
	}

	@Override
	public void doDie(L2Character killer)
	{
		if(minionMaintainTask != null)
			minionMaintainTask.cancel(true);

		synchronized(_dieLock)
		{
			if(_dead)
				return;
			_dead = true;
		}

		if(killer instanceof L2Playable)
		{
			// Get the L2Player that killed the L2NpcInstance
			final L2Player player = killer.getPlayer();
			if(player == null) // маловероятно но бывает
				return;
			// Notify the Quest Engine of the L2NpcInstance death if necessary
			try
			{
				Quest[] quests = getTemplate().getEventQuests(Quest.QuestEventType.ON_KILLED);
				if(quests != null)
					for(Quest q : quests)
						q.notifyKill(this, player);
			}
			catch(final Exception e)
			{
				e.printStackTrace();
			}
		}

		// Distribute Exp and SP rewards to L2Player (including Summon owner) that hit the L2NpcInstance and to their Party members
		try
		{
			calculateRewards(killer);
		}
		catch(final Exception e)
		{
			_log.warn("", e);
			e.printStackTrace();
		}

		super.doDie(killer);
		clearAggroList();

		_isChampion = Config.ALT_CHAMPION_ENABLE && _canBeChamion ? Rnd.chance(Config.ALT_CHAMPION_CHANCE) ? 1 : Rnd.chance(Config.ALT_CHAMPION2_CHANCE) ? 2 : 0 : 0;
	}

	protected void calculateRewards(L2Character lastAttacker)
	{
		if(lastAttacker == null)
			return;

		L2Player player = lastAttacker.getPlayer();
		if(player == null)
			return;

		long totalDamage = 0;
		long maxDamage = 0;
		int levelDiff = 0;
		int level = 0;

		if(getAggroListSize() == 0)
		{
			totalDamage = getMaxHp();
			maxDamage = getMaxHp();
			level = player.getLevel();
		}

		for(AggroInfo ai : _aggroList.values())
			if(ai != null)
			{
				totalDamage += ai.damage;
				L2Character cha = ai.getAttacker();

				if(isRaid())
					_logBoss.info(this + " aggroInfo: " + ai);

				if(ai.damage > maxDamage)
				{
					if(cha != null && (cha.isPlayer() || cha.getPlayer() != null))
						lastAttacker = cha.getPlayer();

					maxDamage = ai.damage;
					level = ai.level;
				}
			}

		if(Config.DEEPBLUE_DROP_RULES)
			levelDiff = level - getLevel() - (isRaid() ? 2 : 8);

		if(levelDiff < 0)
			levelDiff = 0;

		if(isRaid())
			_logBoss.info(this + " most dd: " + lastAttacker + " dmg: " + maxDamage + " lvl: " + level + " lvlDiff: " + levelDiff);

		// Manage Base, Quests and Special Events drops of the L2NpcInstance
		doItemDrop(lastAttacker.getPlayer(), levelDiff);

		// Manage Sweep drops of the L2NpcInstance
		if(isSpoiled())
			doSweepDrop(lastAttacker);

		int npcID = getTemplate().npcId;

		FastMap<L2Character, RewardInfo> rewards = new FastMap<L2Character, RewardInfo>().shared();

		if(getAggroListSize() == 0)
			rewards.put(player, new RewardInfo(player, getMaxHp()));

		for(AggroInfo info : _aggroList.values())
		{
			L2Character attacker = info.getAttacker();
			if(attacker == null)
				continue;

			L2Player owner = attacker.getPlayer();
			if(owner == null)
				continue;

			long damage = info.damage;

			if(npcID <= 0 || damage <= 1)
				continue;

			synchronized(this)
			{
				RewardInfo reward = rewards.get(owner);
				if(reward == null)
					rewards.put(owner, new RewardInfo(owner, damage));
				else
					reward.addDamage(damage);
			}
		}

		for(RewardInfo reward : rewards.values())
		{
			if(reward == null)
				continue;

			L2Character attacker = reward._attacker;

			if(attacker == null)
				continue;

			if(attacker.isDead())
				continue;

			L2Party attackerParty = null;
			if(attacker.isPlayer())
				attackerParty = ((L2Player) attacker).getParty();

			if(attackerParty == null)
			{
				long damage = reward._dmg;

				if(damage > totalDamage)
					damage = totalDamage;

				if(damage > 0)
				{
					int diff = attacker.getLevel() - getLevel();

					//kamael exp penalty
					if(attacker.getLevel() > 77 && diff > 3 && diff <= 5)
						diff += 3;

					long xp = 0;
					long sp = 0;
					long rp = 0;
					long bonusXp = 0;
					long bonusSp = 0;

					if(attacker.knowsObject(this))
					{
						long[] tmp = calculateExpAndSp(diff, damage, totalDamage, attacker.getPlayer());
						xp = tmp[0];
						sp = tmp[1];
						bonusXp = tmp[3];
						bonusSp = tmp[4];

						if(_raidPoints > 0)
						{
							rp = tmp[2];
							int diffRp = lastAttacker.getLevel() - getLevel() - 2;
							if(diffRp > 0)
							{
								double diffMod = diffRp == 1 ? 0.90 : Experience.penaltyModifier(diffRp - 1, 15) - 0.10;
								if(diffMod < 0)
									diffMod = 0;
								rp *= diffMod;
							}
						}
					}

					if(isOverhit() && player == getOverhitAttacker())
					{
						int overHitExp = calculateOverhitExp(xp);
						player.sendPacket(new SystemMessage(SystemMessage.OVER_HIT));
						xp += overHitExp;
					}

					long petXP = 0;
					long petSP = 0;
					L2Summon pet = attacker.getPet();
					if(pet != null && !pet.isDead() && attacker.isInRange(pet, Config.ALT_PARTY_DISTRIBUTION_RANGE))
					{
						float penalty = pet.getExpPenalty();
						petXP = (long) (xp * penalty);
						xp -= petXP;
						petSP = (long) (sp * penalty);
						sp -= petSP;
					}

					if(attacker.isPlayer())
						attacker.getPlayer().addExpAndSp(xp, sp, bonusXp, bonusSp, true);
					else
						attacker.addExpAndSp(xp, sp);
					if(petXP > 0)
						pet.addExpAndSp(petXP, petSP);

					if(rp > 0 && attacker.getPlayer() != null)
					{
						RaidBossSpawnManager.getInstance().addPoints(attacker.getPlayer().getObjectId(), getNpcId(), (int) rp);
						attacker.getPlayer().sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1_RAID_POINTS).addNumber((int) rp));
						_logBoss.info(attacker.getPlayer() + " got raid points: " + (int) rp + " for killing: " + this);
					}

					// Начисление душ камаэлянам
					double neededExp = attacker.calcStat(Stats.SOULS_CONSUME_EXP, 0, null, null);
					if(neededExp > 0 && xp > neededExp)
					{
						broadcastPacket(new SpawnEmitter(this, attacker.getPlayer()));
						ThreadPoolManager.getInstance().scheduleAi(new SoulIncreaseTask(attacker.getPlayer()), 1000, true);
						//attacker.setConsumedSouls(attacker.getConsumedSouls() + 1, this);
					}
				}
				rewards.remove(attacker);
			}
			else
			{
				long partyDmg = 0;
				float partyMul = 1.f;
				int partylevel = 1;

				GArray<L2Player> rewardedMembers = new GArray<L2Player>();

				for(L2Player partyMember : attackerParty.getPartyMembers())
				{
					if(partyMember == null || partyMember.isDead())
						continue;

					RewardInfo ai = rewards.get(partyMember);
					if(ai != null)
						partyDmg += ai._dmg;

					if(!partyMember.isDead())
						rewardedMembers.add(partyMember);

					rewards.remove(partyMember);
					if(partyMember.isInRange(lastAttacker, 2700) && partyMember.getLevel() > partylevel)
						partylevel = partyMember.getLevel();
				}

				if(partyDmg < totalDamage)
					partyMul = (float) partyDmg / totalDamage;
				else
					partyDmg = totalDamage;

				if(partyDmg > 0)
				{
					long xp = 0;
					long sp = 0;
					long rp = 0;

					if(attacker.knowsObject(this))
					{
						int diff = partylevel - getLevel();

						//kamael exp penalty
						if(partylevel > 77 && diff > 3 && diff <= 5)
							diff += 3;

						long[] tmp = calculateExpAndSp(diff, partyDmg, totalDamage, null);
						xp = tmp[0];
						sp = tmp[1];
						rp = tmp[2];
					}

					xp *= partyMul;
					sp *= partyMul;
					rp *= partyMul;

					// Check for an over-hit enabled strike
					// (When in party, the over-hit exp bonus is given to the whole party and splitted proportionally through the party members)
					if(isOverhit() && player == getOverhitAttacker())
					{
						int overHitExp = calculateOverhitExp(xp);
						attackerParty.broadcastToPartyMembersInRange(player, new SystemMessage(SystemMessage.OVER_HIT), 2700);
						xp += overHitExp;
					}

					if(rp > 0)
					{
						int diffRp = lastAttacker.getLevel() - getLevel() - 2;
						if(diffRp > 0)
						{
							double diffMod = diffRp == 1 ? 0.90 : Experience.penaltyModifier(diffRp - 1, 15) - 0.10;
							if(diffMod < 0)
								diffMod = 0;
							rp *= diffMod;
						}
					}

					attackerParty.distributeXpAndSp(xp, sp, rp, partyMul, rewardedMembers, lastAttacker, this);
				}
			}
		}

		// Check the drop of a cursed weapon
		if(_cursedDrop)
			CursedWeaponsManager.getInstance().dropAttackable(this, player);
	}

	private int calculateLevelDiffForDrop()
	{
		if(Config.DEEPBLUE_DROP_RULES)
		{
			int mobLevel = getLevel();
			int level = mobLevel;
			long maxDamage = 0;

			for(AggroInfo aggro : getAggroList().values())
				if(aggro.damage > maxDamage)
				{
					maxDamage = aggro.damage;
					level = aggro.level;
				}

			// According to official data (Prima), deep blue mobs are 9 or more levels below players
			if(level - mobLevel > (isRaid() ? Config.RAID_MAX_LEVEL_DIFF : 8))
				return level - mobLevel - 8;
		}

		return 0;
	}

	@Override
	public void onRandomAnimation()
	{
		// Action id для живности 1-3
		broadcastPacket(new SocialAction(getObjectId(), Rnd.get(1, 3)));
	}

	@Override
	public int getKarma()
	{
		return 0;
	}

	public L2ItemInstance[] takeHarvest()
	{
		synchronized(_harvestLock)
		{
			final L2ItemInstance[] harvest = _harvestItems;
			_harvestItems = null;
			_seeded = null;
			return harvest;
		}
	}

	public void setSeeded(final short id, L2Player player)
	{
		if(player == null)
			return;

		_seeded = ItemTable.getInstance().getTemplate(id);
		_seeder = player;
		// Количество всходов зависит от множителя HP конкретного моба
		int count = (int) getTemplate().hp_mod;
		// Для мобов с множителем HP < 1 число всходов = 1
		if(count == 0)
			count = 1;
		final List<L2ItemInstance> harvested = new FastList<L2ItemInstance>();
		final L2ItemInstance crops = ItemTable.getInstance().createItem("L2NpcInstance.setSeeded", L2Manor.getInstance().getCropType(id), count, player, this);
		// Количество всходов от xHP до (xHP + xHP/2)
		if(count > 1)
			crops.setCount(Rnd.get(count * Config.RATE_MANOR, (int) Math.round(1.5 * count * Config.RATE_MANOR)));
		else
			crops.setCount(Config.RATE_MANOR);
		harvested.add(crops);

		if(harvested.size() > 0)
			_harvestItems = harvested.toArray(new L2ItemInstance[harvested.size()]);
	}

	public boolean isSeeded(L2Player seeder)
	{
		return _seeder != null && seeder == _seeder;
	}

	public boolean isSeeded()
	{
		return _seeded != null;
	}

	/**
	 * True if a Dwarf has used Spoil on this L2NpcInstance
	 */
	private boolean _isSpoiled;

	/**
	 * Return True if this L2NpcInstance has drops that can be sweeped.<BR><BR>
	 */
	public boolean isSpoiled()
	{
		return _isSpoiled;
	}

	/**
	 * Set the spoil state of this L2NpcInstance.<BR><BR>
	 */
	public void setSpoiled(boolean isSpoiled)
	{
		_isSpoiled = isSpoiled;
	}

	public void doItemDrop(L2Player killer, int levelMod)
	{
		if(killer == null || !abilityItemDrop)
			return;

		if(isRaid() && (getAIParams() == null || !getAIParams().getBool("no_rb_range_penalty", false)))
		{
			Location loc = null;
			if(getSpawn() != null && getSpawn().getLocx() != 0 && getSpawn().getLocy() != 0)
				loc = getSpawn().getLoc();
			else if(getSpawnedLoc() != null && getSpawnedLoc().getX() != 0 && getSpawnedLoc().getY() != 0)
				loc = getSpawnedLoc();

			if(loc != null && !isInRangeZ(loc, 2000))
			{
				_log.warn("RaidBoss: " + getName() + "(" + getNpcId() + ") " + getLevel() + " too far from spawn NO drop! " + getX() + "," + getY() + "," + getZ() + " spawn " + loc);
				return;
			}
		}

		if(getTemplate().getDropData() != null)
		{
			GArray<ItemToDrop> drops = getTemplate().getDropData().rollDrop(levelMod, isRaid(), killer, false);
			for(ItemToDrop drop : drops)
			{
				// Если в моба посеяно семя, причем не альтернативное - не давать никакого дропа, кроме адены.
				if(_seeded != null && !_seeded.isAltSeed() && drop.itemId != 57 && drop.itemId != 6360 && drop.itemId != 6361 && drop.itemId != 6362)
					continue;

				L2Item item = ItemTable.getInstance().getTemplate(drop.itemId);

				if(Config.ALT_CHAMPION_ENABLE && _isChampion > 0)
				{
					if(drop.itemId == 57 || (drop.itemId >= 6360 && drop.itemId <= 6362))
						drop.count *= Config.ALT_CHAMPION_ADENA_REWARDS * (_isChampion == 2 ? Config.ALT_CHAMPION2_MUL : 1);
					else if(item.isStackable())
						drop.count *= Config.ALT_CHAMPION_REWARDS * (_isChampion == 2 ? Config.ALT_CHAMPION2_MUL : 1);
				}

				if(killer.isPremiumEnabled())
					drop.count *= Config.PREMIUM_RATE_DROP_COUNT;

				if(Config.ALT_FLOATING_RATE_ENABLE && item.isStackable() && !isRaid())
				{
					if(item.isAdena())
						drop.count *= killer.getFloatingRate().rateADENA;
					else
						drop.count *= killer.getFloatingRate().rateDROP;
				}

				if(item.isStackable() && item.getWeight() * drop.count > 15000)
				{
					long count = drop.count;
					int d = 15000 / item.getWeight();
					while(count > 0)
						if(count > d)
						{
							d = Rnd.get((int) (d * 0.90), (int) (d * 1.1));
							count -= d;
							dropItem(killer, drop.itemId, d);
						}
						else
						{
							dropItem(killer, drop.itemId, count);
							count = 0;
						}
				}
				else
					dropItem(killer, drop.itemId, drop.count);
			}
		}

		if(Config.ALT_CHAMPION_ENABLE && _isChampion > 0 && killer.getLevel() <= getLevel() && Config.ALT_CHAMPION_REWARD > 0 && Rnd.chance(Config.ALT_CHAMPION_REWARD))
			dropItem(killer, Config.ALT_CHAMPION_REWARD_ID, Rnd.get(1, (int) (Config.ALT_CHAMPION_REWARD_QTY * (_isChampion == 2 ? Config.ALT_CHAMPION2_MUL : 1))));
	}

	private void doSweepDrop(final L2Character lastAttacker)
	{
		final L2Player player = lastAttacker.getPlayer();

		if(player == null)
			return;

		final int levelDiff = calculateLevelDiffForDrop();

		final ArrayList<L2ItemInstance> spoiled = new ArrayList<L2ItemInstance>();

		if(getTemplate().getDropData() != null)
		{
			final GArray<ItemToDrop> spoils = getTemplate().getDropData().rollDrop(levelDiff, isRaid(), player, true);
			for(final ItemToDrop spoil : spoils)
			{
				if(Config.ALT_CHAMPION_ENABLE && _isChampion > 0)
					spoil.count *= Config.ALT_CHAMPION_REWARDS * (_isChampion == 2 ? Config.ALT_CHAMPION2_MUL : 1);

				if(player.isPremiumEnabled())
					spoil.count *= Config.PREMIUM_RATE_DROP_COUNT;

				if(Config.ALT_FLOATING_RATE_ENABLE)
					spoil.count *= player.getFloatingRate().rateSPOIL;

				final L2ItemInstance dropit = ItemTable.getInstance().createItem("L2NpcInstance.doSweepDrop", spoil.itemId, spoil.count, lastAttacker.isPlayer() ? (L2Player) lastAttacker : null, this);
				spoiled.add(dropit);
			}
		}

		if(spoiled.size() > 0)
			_sweepItems = spoiled.toArray(new L2ItemInstance[spoiled.size()]);
	}

	private long[] calculateExpAndSp(long diff, long damage, long totalDamage, L2Player killer)
	{
		long xp = getExpReward() * damage / totalDamage;
		long sp = getSpReward() * damage / totalDamage;

		int baseXp = (int) (getTemplate().revardExp * damage / totalDamage);
		long bonusXp = 0;
		long bonusSp = 0;
		if(killer != null)
		{
			if(killer.isPremiumEnabled())
			{
				bonusXp += xp * Config.PREMIUM_RATE_EXPSP / 100.;
				bonusSp += sp * Config.PREMIUM_RATE_EXPSP / 100.;
			}

			if(Config.ALT_FLOATING_RATE_ENABLE)
			{
				xp *= killer.getFloatingRate().rateEXPSP;
				sp *= killer.getFloatingRate().rateEXPSP;
			}

			bonusXp += (long) (calcStat(Stats.EXP_SP, getTemplate().revardExp, null, null) * damage / totalDamage * Config.VIT_RATE * killer.getVitality().getRate());
			bonusSp += (long) (calcStat(Stats.EXP_SP, getTemplate().revardSp, null, null) * damage / totalDamage * Config.VIT_RATE * killer.getVitality().getRate());

			bonusXp += xp * killer.calcStat(Stats.EXP_MODIFY, 1, null, null);
			bonusSp += sp * killer.calcStat(Stats.SP_MODIFY, 1, null, null);
		}
		xp += bonusXp;
		sp += bonusSp;

		double mod = 1;
		if(diff > 5)
		{
			mod *= Math.pow(.83, diff - 5);
			baseXp *= mod;
		}

		if(killer != null && (getAIParams() == null || !getAIParams().getBool("no_vitality", false)))
			killer.getVitality().updatePointsByExp(baseXp, getLevel(), isRaid(), isRaid());

		if(xp < 0)
			xp = 0;
		if(sp < 0)
			sp = 0;
		if(bonusXp < 0)
			bonusXp = 0;
		if(bonusSp < 0)
			bonusSp = 0;

		if(_isChampion > 0)
			mod *= Config.ALT_CHAMPION_REWARDS * (_isChampion == 2 ? Config.ALT_CHAMPION2_MUL : 1);

		xp *= mod;
		sp *= mod;
		bonusXp *= mod;
		bonusSp *= mod;

		if(killer != null)
			killer.getHuntingBonus().addPoints(Rnd.get(5));

		return new long[]{xp, sp, _raidPoints, bonusXp, bonusSp};
	}

	@Override
	public void setOverhitEnabled(final boolean status)
	{
		_overhit = status;
	}

	@Override
	public void setOverhitValues(L2Character attacker, double damage)
	{
		// Calculate the over-hit damage
		// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40
		double overhitDmg = damage - getCurrentHp();
		if(overhitDmg < 0)
		{
			// we didn't killed the mob with the over-hit strike. (it wasn't really an over-hit strike)
			// let's just clear all the over-hit related values
			setOverhitEnabled(false);
			_overhitDamage = 0;
			setOverhitAttacker(null);
			return;
		}
		setOverhitEnabled(true);
		_overhitDamage = overhitDmg;
		setOverhitAttacker(attacker);
	}

	public L2Character getOverhitAttacker()
	{
		return L2ObjectsStorage.getAsCharacter(overhitAttackerId);
	}

	public double getOverhitDamage()
	{
		return _overhitDamage;
	}

	@Override
	public boolean isOverhit()
	{
		return _overhit;
	}

	public int calculateOverhitExp(final long normalExp)
	{
		double overhitPercentage = getOverhitDamage() * 100 / getMaxHp();
		if(overhitPercentage > 25)
			overhitPercentage = 25;
		double overhitExp = overhitPercentage / 100 * normalExp;
		return (int) Math.round(overhitExp);
	}

	/**
	 * Return True if a Dwarf use Sweep on the L2NpcInstance and if item can be spoiled.<BR><BR>
	 */
	public boolean isSweepActive()
	{
		return _sweepItems != null && _sweepItems.length > 0;
	}

	/**
	 * Return table containing all L2ItemInstance that can be spoiled.<BR><BR>
	 */
	public L2ItemInstance[] takeSweep()
	{
		_sweepLock.lock();
		try
		{
			final L2ItemInstance[] sweep = _sweepItems.clone();
			_sweepItems = null;
			return sweep;
		}
		catch(Exception e)
		{
		}
		finally
		{
			_sweepLock.unlock();
		}
		return null;
	}

	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return _isChampion > 0 ? (int) (super.getMAtk(target, skill) * Config.ALT_CHAMPION_ATK) : super.getMAtk(target, skill);
	}

	@Override
	public int getMAtkSpd()
	{
		return _isChampion > 0 ? (int) (super.getMAtkSpd() * Config.ALT_CHAMPION_SPD_ATK) : super.getMAtkSpd();
	}

	@Override
	public int getPAtk(L2Character target)
	{
		return _isChampion > 0 ? (int) (super.getPAtk(target) * Config.ALT_CHAMPION_ATK) : super.getPAtk(target);
	}

	@Override
	public int getPAtkSpd()
	{
		return _isChampion > 0 ? (int) (super.getPAtkSpd() * Config.ALT_CHAMPION_SPD_ATK) : super.getPAtkSpd();
	}

	@Override
	public int getMaxHp()
	{
		return _isChampion > 0 ? (int) (super.getMaxHp() * Config.ALT_CHAMPION_HP * (_isChampion == 2 ? Config.ALT_CHAMPION2_MUL : 1)) : super.getMaxHp();
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(isDead())
			return L2Skill.TargetType.npc_body;

		if(this == target)
			return L2Skill.TargetType.self;

		if(target instanceof L2Playable || target.getPlayer() != null)
			return L2Skill.TargetType.enemy_only;

		return L2Skill.TargetType.target;
	}

	@Override
	public void setLeader(L2NpcInstance leader)
	{
		_master = leader;
		_canBeChamion = _master == null || !_master.isRaid();
	}

	@Override
	public boolean isClanMember(L2Character target)
	{
		return !(target instanceof L2Playable) && target instanceof L2NpcInstance && getFactionId().equals(((L2NpcInstance) target).getFactionId());
	}

	@Override
	public boolean isPartyMember(L2Character target)
	{
		return isClanMember(target);
	}

	@Override
	public boolean canMoveToHome()
	{
		if(isMinion())
			return getLeader().isDead();

		return _returnHome;
	}

	@Override
	public int isChampion()
	{
		return _isChampion;
	}

	@Override
	public boolean isFearImmune()
	{
		return isMinion();
	}
}
