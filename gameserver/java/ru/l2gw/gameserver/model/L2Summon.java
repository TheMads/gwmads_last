package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.scripts.Events;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.ai.L2SummonAI;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.effects.EffectTemplate;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.taskmanager.DecayTaskManager;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.util.Location;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public abstract class L2Summon extends L2Playable
{
	//private static final Log _log = LogFactory.getLog(L2Summon.class.getName());

	protected long _exp = 0;
	protected long _sp = 0;
	private int _attackRange = 36; //Melee range
	private boolean _follow = true;
	private int _maxLoad;
	private boolean _posessed = false;
	private boolean _showSpawnAnimation = true;
	private boolean _teleported = false;
	private Location _lastFollowPosition = null;

	private boolean _ssCharged = false;
	private int _spsCharged = 0;

	public static final int SIEGE_GOLEM_ID = 14737;
	public static final int SIEGE_CANNON_ID = 14768;
	public static final int SWOOP_CANNON_ID = 14839;

	private static final int SUMMON_DISAPPEAR_RANGE = 2500;
	private Constructor<?> _ai_constructor;
	protected boolean unSummonStarted = false;

	public L2Summon(final int objectId, final L2NpcTemplate template, final L2Player owner)
	{
		super(objectId, template);
		setOwner(owner);
		setXYZInvisible(owner.getX() + Rnd.get(-100, 100), owner.getY() + Rnd.get(-100, 100), owner.getZ());
		try
		{
			if(!template.ai_type.equalsIgnoreCase("npc"))
				_ai_constructor = Class.forName("ru.l2gw.gameserver.ai." + template.ai_type).getConstructors()[0];
		}
		catch(Exception e)
		{
			try
			{
				_ai_constructor = Scripts.getInstance().getClasses().get("ai." + template.ai_type).getRawClass().getConstructors()[0];
			}
			catch(Exception e1)
			{
				_log.info("No AI " + template.ai_type + " found for summon: " + template.getNpcId());
			}
		}
	}

	@Override
	public void spawnMe()
	{
		super.spawnMe();
		onSpawn();
	}

	@Override
	public void onSpawn()
	{
		if(getPlayer().getParty() != null)
			getPlayer().getParty().broadcastToPartyMembers(getPlayer(), new ExPartyPetWindowAdd(this));
	}

	@Override
	public L2SummonAI getAI()
	{
		if(_ai == null)
		{
			if(_ai_constructor != null)
				try
				{
					_ai = (L2SummonAI) _ai_constructor.newInstance(this);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			if(_ai == null)
				_ai = new L2SummonAI(this);
		}

		return (L2SummonAI) _ai;
	}

	@Override
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) _template;
	}

	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead();
	}

	// this defines the action buttons, 1 for Summon, 2 for Pets
	public abstract int getSummonType();

	@Override
	public void updateAbnormalEffect()
	{
		broadcastPetInfo();
	}

	/**
	 * @return Returns the mountable.
	 */
	public boolean isMountable()
	{
		return false;
	}

	@Override
	public void onAction(final L2Player player, boolean dontMove)
	{
		if(!dontMove && Events.onAction(player, this))
			return;
		else if(dontMove && Events.onActionShift(player, this))
			return;

		// Check if the L2Player is confused
		if(player.isConfused() || player.isBlocked())
			player.sendActionFailed();

		if(player.getTarget() != this)
		{
			// Set the target of the player
			if(player.setTarget(this))
			{
				// The color to display in the select window is White
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));

				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
		}
		else if(player == getPlayer())
		{
			if(isInRange(player, getInteractDistance(player)))
			{
				player.sendPacket(new PetStatusShow(this));
				player.sendActionFailed();
			}
			if(player.getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT && !dontMove)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
			else
				player.sendActionFailed();
		}
		else if(isAttackable(player, false, false))
		{
			// Player with lvl < 21 can't attack a cursed weapon holder
			// And a cursed weapon holder  can't attack players with lvl < 21
			if(getPlayer().isCursedWeaponEquipped() && player.getLevel() < 21 || player.isCursedWeaponEquipped() && getPlayer().getLevel() < 21)
				player.sendActionFailed();
			else
				player.getAI().Attack(this, false, dontMove);
		}
		else if(player != getPlayer())
		{
			if(!dontMove)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this, 80);

			player.sendActionFailed();
		}
		else
			player.sendActionFailed();
	}

	public long getExpForThisLevel()
	{
		if(getLevel() >= Experience.LEVEL.length)
			return 0;
		return Experience.LEVEL[getLevel()];
	}

	public long getExpForNextLevel()
	{
		if(getLevel() + 1 >= Experience.LEVEL.length)
			return 0;
		return Experience.LEVEL[getLevel() + 1];
	}

	@Override
	public int getNpcId()
	{
		return getTemplate().npcId;
	}

	public final long getExp()
	{
		return _exp;
	}

	public final void setExp(final long exp)
	{
		_exp = exp;
	}

	public int getMaxLoad()
	{
		return _maxLoad;
	}

	public void setMaxLoad(final int maxLoad)
	{
		_maxLoad = maxLoad;
	}

	public abstract int getCurrentFed();

	public abstract int getMaxMeal();

	public void followOwner()
	{
		setFollowStatus(true);
	}

	@Override
	public synchronized void doDie(L2Character killer)
	{
		super.doDie(killer);

		if(killer == null || killer == getPlayer() || killer.getObjectId() == _objectId || isInZoneBattle() || killer.isInZoneBattle())
			return;

		if(killer instanceof L2Summon)
			killer = killer.getPlayer();

		L2Player owner = getPlayer();
		if(killer == null || owner == null)
			return;

		if(killer.isPlayer())
		{
			L2Player pk = (L2Player) killer;

			if(isInSiege())
				return;

			if((!isInDuel() || getDuel() != pk.getDuel()) && getKarma() <= 0 && !(owner.getPvpFlag() > 0 || owner.atMutualWarWith(pk, owner.getClan(), pk.getClan())))
			{
				int pkCountMulti = Math.max(pk.getPkKills() / 2, 1);
				pk.increaseKarma(Config.KARMA_MIN_KARMA * pkCountMulti);
			}

			// Send a Server->Client UserInfo packet to attacker with its PvP Kills Counter
			pk.sendChanges();
		}
	}

	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
	}

	@Override
	public void onDecay()
	{
		deleteMe();
	}

	public int getControlItemObjId()
	{
		return 0;
	}

	@Override
	public void broadcastStatusUpdate()
	{
		super.broadcastStatusUpdate();
		L2Player player = getPlayer();
		if(player != null)
		{
			if(isVisible())
				player.sendPacket(new PetStatusUpdate(this));
			L2Party party = player.getParty();
			if(party != null)
				party.broadcastToPartyMembers(player, new ExPartyPetWindowUpdate(this));
		}
	}

	public void deleteMe()
	{
		stopHpMpRegeneration();
		super.deleteMe();
		getAI().stopFollow();
		detachAI();
		L2Player player = getPlayer();
		if(player != null)
		{
			if(player.getParty() != null)
				player.getParty().broadcastToPartyMembers(player, new ExPartyPetWindowDelete(this));
			player.sendPacket(new PetDelete(getObjectId(), 2));

			if(player.getTargetId() == getObjectId())
			{
				player.setTarget(null);
				if(player.getAI().getAttackTarget() == this)
					player.getAI().setAttackTarget(null);
			}

			player.sendPacket(new DeleteObject(this));
			player.setPet(null);
		}
		setReflection(0);
		stopAllEffects();
		setOwner(null);
	}

	public synchronized void unSummon()
	{
		if(unSummonStarted)
			return;

		unSummonStarted = true;

		deleteMe();
	}

	public int getAttackRange()
	{
		return _attackRange;
	}

	public void setAttackRange(int range)
	{
		if(range < 36)
			range = 36;
		_attackRange = range;
	}

	@Override
	public void setFollowStatus(boolean state)
	{
		_follow = state;
		if(_follow)
		{
			setLastFollowPosition(null);
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getPlayer(), 55);
		}
		else
		{
			setLastFollowPosition(getLoc());
			getAI().stopFollow();
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
		}
	}

	public boolean getFollowStatus()
	{
		return _follow;
	}

	public Location getLastFollowPosition()
	{
		return _lastFollowPosition;
	}

	@Override
	public void updateEffectIcons()
	{
		if(_massUpdating)
			return;

		for(L2Player player : L2World.getAroundPlayers(this))
			player.sendPacket(player == getPlayer() ? new PetInfo(this, _showSpawnAnimation ? 2 : 1) : new NpcInfo(this, player, _showSpawnAnimation));

		PartySpelled ps = new PartySpelled(this, true);

		L2Player player = getPlayer();
		if(player != null)
		{
			L2Party party = player.getParty();
			if(party != null)
				party.broadcastToPartyMembers(ps);
			else
				player.sendPacket(ps);
		}
	}

	/**
	 * @return Returns the showSpawnAnimation.
	 */
	public boolean isShowSpawnAnimation()
	{
		return _showSpawnAnimation;
	}

	/**
	 * Sets showSpawnAnimation.
	 */
	public void setShowSpawnAnimation(boolean showSpawnAnimation)
	{
		_showSpawnAnimation = showSpawnAnimation;
	}

	public int getControlItemId()
	{
		return 0;
	}

	public L2Weapon getActiveWeapon()
	{
		return null;
	}

	@Override
	public PetInventory getInventory()
	{
		return null;
	}

	@Override
	public void doPickupItem(final L2Object object)
	{}

	/**
	 * Return null.<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	/**
	 * @return the L2Party object of its L2Player owner or null.<BR><BR>
	 */
	public L2Party getParty()
	{
		return getPlayer().getParty();
	}

	public boolean isInParty()
	{
		return getPlayer().getParty() != null;
	}

	@Override
	public double getLevelMod()
	{
		return (89. + getLevel()) / 100.0;
	}

	@Override
	public boolean unChargeShots(final boolean spirit)
	{
		if(getPlayer() == null)
			return false;

		if(spirit && _spsCharged != 0)
		{
			_spsCharged = 0;
			getPlayer().AutoShot();
			return true;
		}

		if(_ssCharged)
		{
			_ssCharged = false;
			getPlayer().AutoShot();
			return true;
		}

		getPlayer().AutoShot();
		return false;
	}

	@Override
	public boolean getChargedSoulShot()
	{
		return _ssCharged;
	}

	@Override
	public int getChargedSpiritShot()
	{
		return _spsCharged;
	}

	public void chargeSoulShot()
	{
		_ssCharged = true;
	}

	public void chargeSpiritShot(final int state)
	{
		_spsCharged = state;
	}

	public int getSoulshotConsumeCount()
	{
		return getTemplate().soulshotCount;
	}

	public int getSpiritshotConsumeCount()
	{
		return getTemplate().spiritshotCount;
	}

	public boolean isPosessed()
	{
		return _posessed;
	}

	public void setPossessed(final boolean possessed)
	{
		_posessed = possessed;
	}

	public boolean isInRange()
	{
		return getPlayer() != null && getDistance(getPlayer()) < SUMMON_DISAPPEAR_RANGE;
	}

	public void teleportToOwner()
	{
		if(getPlayer() == null)
			return;
		setIsTeleporting(true);
		stopMove();
		teleToLocation(getPlayer().getLoc(), getPlayer().getReflection());
		//setXYZ(getPlayer().getX(), getPlayer().getY(), getPlayer().getZ(), false);
		setFollowStatus(true);
		updateEffectIcons();
		setIsTeleporting(false);
	}

	public void broadcastPetInfo()
	{
		// После PetInfo нужно обязательно обновлять иконки бафов (они затираются).
		// Поэтому броадкаст для удобства совмещен с updateEffectIcons()
		updateEffectIcons();
	}

	@Override
	public void startPvPFlag(L2Character target)
	{
		if(getPlayer() == null)
			return;
		getPlayer().startPvPFlag(target);
	}

	@Override
	public int getPvpFlag()
	{
		if(getPlayer() == null)
			return 0;
		return getPlayer().getPvpFlag();
	}

	@Override
	public int getTeam()
	{
		if(getPlayer() == null)
			return 0;
		return getPlayer().getTeam();
	}


	@Override
	public int getBuffLimit()
	{
		L2Player player = getPlayer();
		if(player != null)
			return (int) player.calcStat(Stats.BUFF_LIMIT, Config.ALT_BUFF_LIMIT, null, null);

		return (int) calcStat(Stats.BUFF_LIMIT, Config.ALT_BUFF_LIMIT, null, null);
	}

	/**
	 * Делает броадкаст для пета
	 */
	@Override
	public void broadcastUserInfo()
	{
		broadcastPetInfo();
	}

	public void setLastFollowPosition(Location loc)
	{
		_lastFollowPosition = loc;
	}

	public abstract float getExpPenalty();

	public final int getSp()
	{
		return (int)_sp;
	}

	public void setSp(final int sp)
	{
		_sp = sp;
	}

	public boolean isHungry()
	{
		return false;
	}

	public synchronized void startFeed()
	{
	//Do Nothing
	}

	public abstract void sendPetInfo();

	public boolean isSiegeWeapon()
	{
		return getNpcId() == SIEGE_GOLEM_ID || getNpcId() == SIEGE_CANNON_ID || getNpcId() == SWOOP_CANNON_ID;
	}

	public int getWeaponItemId()
	{
		return 0;
	}

	public int getArmorItemId()
	{
		return 0;
	}

	public boolean isTeleported()
	{
		return _teleported;
	}

	public void setTeleported(boolean teleported)
	{
		_teleported = teleported;
	}

	public void storeSummonEffects()
	{
		if(getPlayer() == null)
			return;
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("REPLACE INTO character_summons_effects VALUES(?, ?, ?, ?, ?, ?)");
			int order = 0;
			for(L2Effect effect : getAllEffects())
				if(effect != null && effect.isInUse() && effect.getSkill().isSaveable() && !effect.getSkill().isToggle() && effect.getTimeLeft() > 0)
				{
					stmt.setInt(1, getPlayer().getObjectId());
					stmt.setInt(2, getNpcId());
					stmt.setInt(3, effect.getSkillId());
					stmt.setInt(4, effect.getSkillLevel());
					stmt.setLong(5, effect.getTimeLeft());
					stmt.setInt(6, order);
					stmt.execute();
					order++;
				}
		}
		catch(final Exception e)
		{
			_log.warn("Error could not store Skills:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
		}
	}

	public void restoreSummonEffects()
	{
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			if(getPlayer() != null && !getPlayer().isInOlympiadMode())
			{
				stmt = con.prepareStatement("SELECT * FROM character_summons_effects WHERE char_obj_id = ? and npc_id = ? ORDER BY `order`");
				stmt.setInt(1, getPlayer().getObjectId());
				stmt.setInt(2, getNpcId());
				rset = stmt.executeQuery();
				while(rset.next())
				{
					int skillId = rset.getInt("skill_id");
					int skillLvl = rset.getInt("skill_level");
					long duration = rset.getLong("duration");

					L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
					if(duration > 0 && skill != null)
					{
						EffectTemplate et = skill.getTimedEffectTemplate();
						if(et == null)
							continue;
						Env env = new Env(this, this, skill);
						L2Effect effect = et.getEffect(env);
						effect.setAbnormalTime(duration);
						addEffect(effect);
					}
				}
				stmt.close();
			}
			stmt = con.prepareStatement("DELETE FROM character_summons_effects WHERE char_obj_id = ? and npc_id = ?");
			stmt.setInt(1, getPlayer().getObjectId());
			stmt.setInt(2, getNpcId());
			stmt.execute();
		}
		catch(final Exception e)
		{
			_log.warn("Error could not store Skills:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt, rset);
		}
	}
}
