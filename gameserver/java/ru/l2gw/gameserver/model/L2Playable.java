package ru.l2gw.gameserver.model;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2ObjectTasks.NotifyAITask;
import ru.l2gw.gameserver.model.L2Skill.TargetType;
import ru.l2gw.gameserver.model.entity.duel.Duel;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2ArtefactInstance;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.Attack;
import ru.l2gw.gameserver.serverpackets.ExAttackInAirShip;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.templates.L2CharTemplate;
import ru.l2gw.gameserver.templates.L2EtcItem;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.gameserver.templates.L2Weapon.WeaponType;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents all Playable characters in the world.
 *
 * L2PlayableInstance:
 *  * L2Player
 *  * L2Summon
 *
 */
@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public abstract class L2Playable extends L2Character
{
	/**
	 * this если Player или owner если Summon
	 */
	private L2Player owner;

	public long _checkAggroTimestamp;

	private int _restoreHpLevel = -1;
	private int _restoreMpLevel = -1;

	private int _duelState = Duel.DUELSTATE_NODUEL;
	protected boolean _massUpdating = false;

	public L2Playable(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
	}

	public abstract Inventory getInventory();

	/**
	 * Проверяет, выставлять ли PvP флаг для игрока.<BR><BR>
	 */
	@Override
	public boolean checkPvP(final L2Character target, L2Skill skill)
	{
		if(target == null || getPlayer() == null || target == this || target == getPlayer() || target == getPlayer().getPet() || getPlayer().getKarma() > 0 || (isPlayer() && ((L2Player) this).isInOlympiadMode()))
			return false;

		if(skill != null && (skill.altUse() || skill._abnormalTypes.contains("signet")))
			return false;

		// Проверка на дуэли... Мэмбэры одной дуэли не флагаются
		if(isInDuel() && target instanceof L2Playable && getDuel() == target.getDuel() && getDuelState() == Duel.DUELSTATE_DUELLING)
			return false;

		if(isInZonePeace() || target.isInZonePeace() || isInZoneBattle() || target.isInZoneBattle())
			return false;
		if(isInSiege() && target.isInSiege())
			return false;
		if(skill == null || skill.isOffensive())
		{
			if(target.getKarma() > 0)
				return false;
			else if(target instanceof L2Playable)
				return true;
		}
		else if(target.getPvpFlag() > 0 || target.getKarma() > 0 || target.isMonster())
			return true;

		return false;
	}

	/**
	 * Проверяет, можно ли атаковать цель (для физ атак)
	 */
	public boolean checkAttack(L2Character target)
	{
		if(target.isDead())
			return false;

		if(!isInRange(target, 6000))
		{
			getPlayer().sendPacket(Msg.YOUR_TARGET_IS_OUT_OF_RANGE);
			return false;
		}

		if(getPlayer().inObserverMode())
		{
			getPlayer().sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
			return false;
		}

		if(!GeoEngine.canSeeTarget(this, target))
		{
			getPlayer().sendPacket(Msg.CANNOT_SEE_TARGET);
			return false;
		}

		return true;
	}

	@Override
	public void doAttack(L2Character target)
	{
		if(Config.DEBUG)
			_log.info(this + " doAttack: --> " + target);
		
		if(isAttackingNow())
		{
			if(Config.DEBUG)
				_log.info(this + " doAttack: isAttakingNow");
			getPlayer().sendActionFailed();
			return;
		}

		if(!checkAttack(target))
		{
			if(Config.DEBUG)
				_log.info(this + " doAttack: checkTarget false");
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
			getPlayer().sendActionFailed();
			return;
		}

		fireMethodInvoked(MethodCollection.onStartAttack, new Object[] { this, target });

		// Прерывать дуэли если цель не дуэлянт
		if(isInDuel() && target.getDuel() != getDuel() && getPlayer() != null)
			getPlayer().setDuelState(Duel.DUELSTATE_INTERRUPTED);

		// Get the active weapon item corresponding to the active weapon instance (always equipped in the right hand)
		L2Weapon weaponItem = getActiveWeaponItem();

		if(weaponItem != null && (weaponItem.getItemType() == WeaponType.BOW || weaponItem.getItemType() == WeaponType.CROSSBOW))
		{
			double bowMpConsume = weaponItem.mpConsume;

			// cheap shot SA
			double chance = calcStat(Stats.MP_USE_BOW_CHANCE, 0., null, null);
			if(chance > 0 && chance > Rnd.get())
				bowMpConsume = calcStat(Stats.MP_USE_BOW, bowMpConsume, null, null);

			if(_currentMp < bowMpConsume)
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
				getPlayer().sendPacket(Msg.NOT_ENOUGH_MP);
				getPlayer().sendActionFailed();
				return;
			}

			reduceCurrentMp(bowMpConsume, null);

			L2Player player = getPlayer();
			if(player != null && !player.checkAndEquipArrows())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
				player.sendPacket(weaponItem.getItemType() == WeaponType.BOW ? Msg.NOT_ENOUGH_ARROWS : Msg.NOT_ENOUGH_BOLTS);
				player.sendActionFailed();
				return;
			}
		}

		// Verify if SoulShot are charged
		boolean wasSSCharged = getChargedSoulShot();

		// Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)
		int sAtk = Math.max(calculateAttackSpeed(), 200) - 50;

		// Get the Attack Reuse Delay of the L2Weapon
		int reuse = (int) (weaponItem != null ? weaponItem.attackReuse * 331.5 / getPAtkSpd() : 0);
		if(reuse > 0)
			reuse += sAtk;

		_attackEndTime = sAtk + System.currentTimeMillis();
		_attackReuseEndTime = reuse + System.currentTimeMillis();

		// Create a Server->Client packet Attack
		Attack attack = isInAirShip() ? new ExAttackInAirShip(this, target, wasSSCharged, weaponItem != null ? weaponItem.getCrystalType().externalOrdinal : 0) : new Attack(this, target, wasSSCharged, weaponItem != null ? weaponItem.getCrystalType().externalOrdinal : 0);

		boolean hitted;

		_isAttackAborted = false;

		setHeading(target, true);

		// Select the type of attack to start
		if(weaponItem == null)
		{
			if(isPlayer())
				hitted = doAttackHitByDual(attack, target, weaponItem, sAtk);
			else
				hitted = doAttackHitSimple(attack, target, weaponItem, 1., isSummon() || isPet(), sAtk);
		}
		else
			switch(weaponItem.getItemType())
			{
				case BOW:
				case CROSSBOW:
					hitted = doAttackHitByBow(attack, target, sAtk, reuse, weaponItem);
					break;
				case POLE:
					if(calcStat(Stats.POLE_ATTACK_ANGLE, 0, null, null) == -1)
						hitted = doAttackHitSimple(attack, target, weaponItem, 1., true, sAtk);
					else
						hitted = doAttackHitByPole(attack, weaponItem, sAtk);
					break;
				case DUAL:
				case DUALFIST:
				case DUALDAGGER:
					hitted = doAttackHitByDual(attack, target, weaponItem, sAtk);
					break;
				default:
					hitted = doAttackHitSimple(attack, target, weaponItem, 1., true, sAtk);
			}

		if(hitted)
			// if hitted by a cursed weapon, Cp is reduced to 0
			// if a cursed weapon is hitted by a Hero, Cp is reduced to 0
			if(isPlayer() && target.isPlayer() && !target.isInvul())
				if(isCursedWeaponEquipped() || isHero() && target.isCursedWeaponEquipped())
					target.setCurrentCp(0);

		if(attack.hasHits())
			broadcastPacket(attack);

		// Для кайтинга с луком
		if(reuse > 0)
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT, null, null), sAtk + 10, isPlayer());

		if(Config.ALT_PVP_AUTO_ATTACK || this instanceof L2Summon || target.isNpc() || target instanceof L2Summon && target.getPlayer() == this || target instanceof L2DoorInstance || target.getPlayer() != null && (isInZoneBattle() || isInZone(L2Zone.ZoneType.siege) || isInDuel() && getDuel() == target.getDuel() && getDuelState() == Duel.DUELSTATE_DUELLING))
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT, null, null), Math.max(reuse, sAtk) + 10, isPlayer());
		else
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}

	@Override
	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld, boolean unchargeSS)
	{
		if(target instanceof L2Playable && isInZoneBattle() != target.isInZoneBattle())
		{
			if(getPlayer() != null)
			{
				getPlayer().sendPacket(Msg.INVALID_TARGET);
				getPlayer().sendActionFailed();
			}
			return;
		}

		super.onHitTimer(target, damage, crit, miss, soulshot, shld, unchargeSS);
	}

	@Override
	public boolean isAttackable(final L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		final L2Player target = getPlayer();
		if(target == null)
			return false;

		if(attacker.isSummon() && ((L2Summon) attacker).isPosessed() && target == attacker.getPlayer())
			return true;

		if(attacker == this || attacker.getPlayer() == this)
			return false;

		//if(isDead())
		//	return false;

		if(attacker.isMonster())
			return true;

		// Автоатака на дуэлях, только враг и только если он еше не проиграл.
		if(isInDuel())
		{
			if(getDuelState() == Duel.DUELSTATE_PREPARE || getDuelState() == Duel.DUELSTATE_DEAD)
				return false;
			if(getDuelState() == Duel.DUELSTATE_DUELLING && attacker instanceof L2Playable && getDuel() == attacker.getDuel())
				return getDuelSide() != ((L2Playable) attacker).getDuelSide();
		}

		final L2Player pcAttacker = attacker.getPlayer();

		if(pcAttacker != null)
		{
			if(target.isInOlympiadMode())
			{
				if(target.getOlympiadSide() == pcAttacker.getOlympiadSide())
					return forceUse;
				return target.isOlympiadStart();
			}

			if(pcAttacker.inObserverMode())
			{
				if(sendMessage)
					pcAttacker.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
				return false;
			}

			if((isInZonePeace() || pcAttacker.isInZonePeace()) && !AdminTemplateManager.checkBoolean("peaceAttack", target))
			{
				if(sendMessage)
					attacker.sendPacket(Msg.YOU_CANNOT_ATTACK_THE_TARGET_IN_THE_PEACE_ZONE);
				return false;
			}

			if(target.getSessionVar("event_team_pvp") != null && pcAttacker.getSessionVar("event_team_pvp") != null && target.getTeam() > 0 && pcAttacker.getTeam() > 0)
			{
				return target.getTeam() != pcAttacker.getTeam();
			}

			if(target.getSiegeState() == 3 && target.getTerritoryId() == pcAttacker.getTerritoryId())
			{
				if(forceUse && sendMessage)
					attacker.sendPacket(Msg.YOU_CANNOT_FORCE_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
				return false;
			}

			boolean inParty = target.getParty() != null && target.getParty().containsMember(pcAttacker);
			boolean inClan = target.getClanId() > 0 && target.getClanId() == pcAttacker.getClanId();

			if(isInZoneBattle() && attacker.isInZoneBattle())
			{
				if(inParty || inClan)
				{
					if(forceUse)
						return true;
					else if(sendMessage)
						attacker.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				return true;
			}

			final Siege attackerSiege = pcAttacker.getClan() != null ? pcAttacker.getClan().getSiege() : null;
			final Siege targetSiege = target.getClan() != null ? target.getClan().getSiege() : null;

			boolean attackerSiegeReg = pcAttacker.getSiegeState() == 3 || attackerSiege != null && attackerSiege.getSiegeUnit().getId() == pcAttacker.getSiegeId();
			boolean targetSiegeReg = target.getSiegeState() == 3 || targetSiege != null && targetSiege.getSiegeUnit().getId() == target.getSiegeId();

			if(attackerSiegeReg && targetSiegeReg && target.getSiegeId() == pcAttacker.getSiegeId())
			{
				if(target.getSiegeState() == pcAttacker.getSiegeState() && (pcAttacker.getSiegeState() == 2 || pcAttacker.getSiegeState() == 1 && attackerSiege.isTempAllyActive()))
				{
					if(sendMessage)
						attacker.sendPacket(Msg.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SEIGE_SIDE_TEMPORARY_ALLIED_MEMBERS);
					return false;
				}
			}

			if(isInSiege() && attacker.isInSiege())
				return !((inParty || inClan) && !forceUse);

			if(inParty || inClan)
			{
				if(forceUse)
					return true;
				if(sendMessage)
					attacker.sendPacket(Msg.INVALID_TARGET);
				return false;
			}

			if(target.getKarma() > 0 || target.getPvpFlag() != 0)
				return true;
		}

		if(!forceUse && sendMessage)
			attacker.sendPacket(Msg.INVALID_TARGET);

		return forceUse;
	}

	@Override
	public int getKarma()
	{
		if(getPlayer() == null)
			return 0;
		return getPlayer().getKarma();
	}

	@Override
	public void callSkill(L2Skill skill, List<L2Character> targets, L2ItemInstance usedItem)
	{
		List<L2Character> toRemove = new ArrayList<>();

		if(!skill.altUse())
		{
			for(L2NpcInstance monster : getKnownNpc(1500, 350))
			{
				monster.getAI().notifyEvent(CtrlEvent.EVT_SEE_SPELL, skill, this);

				for(L2Character target : targets)
				{
					if(target == null)
						continue;

					if(!skill.isOffensive() && monster.getHate(target) > 0 && monster.hasAI())
					{
						ThreadPoolManager.getInstance().executeAi(new NotifyAITask(monster, CtrlEvent.EVT_ATTACKED, this, 0, skill), isPlayer());
						//target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this, 0, skill);
						break;
					}
				}
			}

			for(L2Character target : targets)
			{
				if(target == null)
					continue;

				if((target.isInvul() || !target.isVisible()) && skill.isOffensive() && !(target instanceof L2ArtefactInstance || target instanceof L2DoorInstance))
					toRemove.add(target);

				if(skill.isOffensive() && target.hasAI() && skill.getEffectPoint() > 0 && Math.abs(target.getZ() - getZ()) < 350)
					ThreadPoolManager.getInstance().executeAi(new NotifyAITask(target, CtrlEvent.EVT_ATTACKED, this, 0, skill), isPlayer());
						//target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this, 0, skill);
				// Check for PvP Flagging / Drawing Aggro
				if(checkPvP(target, skill))
					startPvPFlag(target);

				if(isInDuel() && getDuelState() != Duel.DUELSTATE_INTERRUPTED && !skill.isOffensive() && target.getDuel() == getDuel() && getPlayer() != null && getDuelSide() != getPlayer().getDuelSide())
					getPlayer().setDuelState(Duel.DUELSTATE_INTERRUPTED);
			}
		}
		for(L2Character cha : toRemove)
			targets.remove(cha);

		super.callSkill(skill, targets, usedItem);
	}

	@Override
	public void teleToLocation(int x, int y, int z)
	{
		if(isFakeDeath())
			stopEffectsByName("c_fake_death");

		super.teleToLocation(x, y, z);
	}

	/**
	 * Оповещает других игроков о поднятии вещи
	 * @param item предмет который был поднят
	 */
	public void broadcastPickUpMsg(L2ItemInstance item)
	{
		if(item == null || getPlayer() == null || getPlayer().isInvisible())
			return;

		if(item.isEquipable() && !(item.getItem() instanceof L2EtcItem) && !item.isFortFlag())
		{
			SystemMessage msg = null;
			String player_name = getPlayer().getName();
			if(item.getEnchantLevel() > 0)
			{
				int msg_id = isPlayer() ? SystemMessage.ATTENTION_S1_PICKED_UP__S2_S3 : SystemMessage.ATTENTION_S1_PET_PICKED_UP__S2_S3;
				msg = new SystemMessage(msg_id).addString(player_name).addNumber(item.getEnchantLevel()).addItemName(item.getItemId());
			}
			else
			{
				int msg_id = isPlayer() ? SystemMessage.ATTENTION_S1_PICKED_UP_S2 : SystemMessage.ATTENTION_S1_PET_PICKED_UP__S2_S3;
				msg = new SystemMessage(msg_id).addString(player_name).addItemName(item.getItemId());
			}
			getPlayer().sendPacket(msg);
			getPlayer().broadcastPacketToOthers(msg);
		}
	}

	@Override
	public L2Player getPlayer()
	{
		return owner;
	}

	public void setOwner(L2Player owner)
	{
		this.owner = owner;
	}

	@Override
	public boolean isInDuel()
	{
		return getPlayer() != null && getPlayer().isInDuel();
	}

	@Override
	public Duel getDuel()
	{
		return getPlayer() != null ? getPlayer().getDuel() : null;
	}

	public int getDuelSide()
	{
		return getPlayer() != null ? getPlayer().getDuelSide() : 0;
	}

	@Override
	public TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(isDead())
			return TargetType.pc_body;

		if(this == target)
			return TargetType.self;

		if(target instanceof L2NpcInstance)
		{
			if(target.getPlayer() != null)
				target = target.getPlayer();
			else
				return TargetType.enemy_only;
		}

		L2Player owner = getPlayer();
		if(target instanceof L2Playable && owner != null)
		{
			if(target == owner)
				return TargetType.summon;

			L2Player player = target.getPlayer();
			if(player != null)
			{
				if(owner.isInOlympiadMode() && owner.getOlympiadGameId() == player.getOlympiadGameId())
				{
					if(isPartyMember(target))
						return TargetType.target;
					if(owner.isOlympiadStart())
						return TargetType.enemy_only;
					if(owner.getOlympiadSide() != player.getOlympiadSide())
						return TargetType.invalid;
				}

				if(owner.getSiegeState() > 0 && player.getSiegeState() > 0) // Оба участники осады
				{
					if(owner.getSiegeState() == 3 && player.getSiegeState() == 3) // Участники ТВ
					{
						if(owner.getTerritoryId() == player.getTerritoryId() && offensive) // Зареганы за одну территорию и скилл плохой
							return TargetType.siege_ally;
					}
					else if(owner.getSiegeId() > 0 && owner.getSiegeId() == player.getSiegeId() && owner.getSiegeState() == player.getSiegeState())
					{
						if((owner.getSiegeState() == 2 || owner.getSiegeState() == 1 && owner.getClan().getSiege().isTempAllyActive()) && offensive)
							return TargetType.siege_ally;
					}
				}

				if(isInDuel() && target.getDuel() == getDuel())
				{
					if((getDuelSide() == player.getDuelSide() && offensive) || (player.getDuelState() == Duel.DUELSTATE_PREPARE && offensive) || player.getDuelState() == Duel.DUELSTATE_DEAD)
						return TargetType.invalid;
					if(getDuelState() == Duel.DUELSTATE_DUELLING && player.getDuelState() == Duel.DUELSTATE_DUELLING && getDuelSide() != player.getDuelSide())
						return TargetType.enemy_only;
				}

				if(owner.getSessionVar("event_team_pvp") != null && player.getSessionVar("event_team_pvp") != null && owner.getTeam() > 0 && player.getTeam() > 0)
				{
					if(getTeam() == player.getTeam() && offensive || getTeam() != player.getTeam() && !offensive)
						return TargetType.invalid;
					if(getTeam() != player.getTeam() && offensive)
						return TargetType.enemy_only;
					if(getTeam() == player.getTeam() && !offensive)
						return TargetType.target;
				}

				if(isPartyMember(target))
					return TargetType.target;

				if(isInZoneBattle() && target.isInZoneBattle())
				{
					if(isClanMember(target))
						return TargetType.at_war;
					return TargetType.enemy_only;
				}

				if(isClanMember(target) && owner.getSiegeState() != 3)
					return TargetType.target;

				if(getKarma() > 0 || getPvpFlag() > 0 || isInSiege() && target.isInSiege())
					return TargetType.enemy_only;

				if(owner.atMutualWarWith(player, owner.getClan(), player.getClan()))
					return TargetType.at_war;
			}
		}

		return TargetType.target;
	}

	@Override
	public boolean isClanMember(L2Character target)
	{
		return getPlayer() != null && (getPlayer() == target || getPlayer().isClanMember(target));
	}

	@Override
	public boolean isPartyMember(L2Character target)
	{
		return getPlayer() != null && (getPlayer() == target || getPlayer().isPartyMember(target));
	}

	@Override
	public boolean isCommandChanelMember(L2Character target)
	{
		return getPlayer() != null && (getPlayer() == target || getPlayer().isCommandChanelMember(target));
	}

	@Override
	public boolean isFriend(L2Character target)
	{
		return getPlayer() != null && getPlayer().isFriend(target);
	}

	public void setRestoreHpLevel(int restoreHpLevel)
	{
		_restoreHpLevel = restoreHpLevel;
	}

	public int getRestoreHpLevel()
	{
		return _restoreHpLevel;
	}

	public void setRestoreMpLevel(int restoreMpLevel)
	{
		_restoreMpLevel = restoreMpLevel;
	}

	public int getRestoreMpLevel()
	{
		return _restoreMpLevel;
	}

	@Override
	public void setXYZ(int x, int y, int z, boolean move)
	{
		super.setXYZ(x, y, z, move);

		if(move)
		{
			if(getPlayer() == null)
				return;

			long now = System.currentTimeMillis();

			if(now - _checkAggroTimestamp < Config.AGGRO_CHECK_INTERVAL || getPlayer().getNonAggroTime() > now)
				return;

			_checkAggroTimestamp = now;

			if(isAlikeDead() || isInvul() || !isVisible() || getCurrentRegion() == null || (getAI().getIntention() == CtrlIntention.AI_INTENTION_FOLLOW && !isPlayer()))
				return;

			for(L2WorldRegion region : getCurrentRegion().getNeighbors())
				if(region != null && region.getObjectsSize() > 0)
					for(L2NpcInstance obj : region.getNpcsList(getReflection()))
						if(obj != null && obj.hasAI())
							obj.getAI().checkAggression(this);
		}
	}

	public void setDuelState(int mode)
	{
		_duelState = mode;
	}

	public int getDuelState()
	{
		return _duelState;
	}

	@Override
	public boolean isFloating()
	{
		return isFlying() || isSwimming();
	}
	
	@Override
	public boolean isSwimming()
	{
		return isInZone(L2Zone.ZoneType.water);
	}

	@Override
	public void stopAllEffects()
	{
		_massUpdating = true;
		super.stopAllEffects();
		_massUpdating = false;

		sendChanges();
		updateEffectIcons();
	}

	@Override
	public void stopEffects()
	{
		_massUpdating = true;
		super.stopEffects();
		_massUpdating = false;

		sendChanges();
		updateEffectIcons();
	}

	@Override
	public int getTerritoryId()
	{
		return owner.getTerritoryId();
	}

	public boolean isMassUpdating()
	{
		return _massUpdating;
	}

	@SuppressWarnings("unused")
	public void setMassUpdating(final boolean massUpdating)
	{
		_massUpdating = massUpdating;
	}
}
