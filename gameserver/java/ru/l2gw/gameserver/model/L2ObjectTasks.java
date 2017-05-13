package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWar;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.util.Location;

import java.util.List;

import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

/**
 * @author rage
 * @date 19.04.2010 11:33:05
 */
public class L2ObjectTasks
{
	protected static final Log _log = LogFactory.getLog(L2ObjectTasks.class);
	protected static final Log _logMove = LogFactory.getLog("movedebug");

	/**
	 * Task of AI notification
	 */
	public static class MoveNextTask implements Runnable
	{
		protected float alldist, donedist;
		private long charStoreId;
		protected double xs, ys, zs;
		protected final Location startLoc;
		public boolean abort;
		private int lastCheckIndex;
		private boolean debug = false;

		public MoveNextTask(L2Character _character)
		{
			charStoreId = _character.getStoredId();
			startLoc = _character.getLoc().clone();
		}

		public void updateStoreId(long l)
		{
			charStoreId = l;
		}

		public MoveNextTask setDist(double dist)
		{
			//if(cha.get().isPlayer())
			//	_log.info(cha.get() + " set dist: " + String.format("%.2f", dist) + " " + cha.get().getLoc() + " --> " + cha.get().getDestination());
			alldist = (float) dist;
			donedist = 0;
			abort = false;
			debug = false;
			lastCheckIndex = 0;
			L2Character character = L2ObjectsStorage.getAsCharacter(charStoreId);

			if(character != null && character.getDestination() != null)
			{
				startLoc.set(character.getLoc());
				int dx = character.getDestination().getX() - character.getX();
				int dy = character.getDestination().getY() - character.getY();
				int dz = character.getDestination().getZ() - character.getZ();

				xs = dx / dist;
				ys = dy / dist;
				zs = dz / dist;

				if(Config.DEBUG_OLYMP_MOVE && character.isPlayer() && character.getOlympiadGameId() >= 0)
				{
					debug = true;
					_logMove.info(character + " set dist: " + String.format("%.2f", dist) + " " + character.getLoc() + " --> " + character.getDestination());
					_logMove.info(character + " " + String.format("d: %.2f xs: %.2f ys: %.2f zs: %.2f", dist, xs, ys, zs));
				}
			}
			else
				xs = ys = zs = 0;

			return this;
		}

		public void run()
		{
			L2Character follow_target, character = L2ObjectsStorage.getAsCharacter(charStoreId);
			//if(character.isPlayable() && abort)
			//	_log.info(character + " run aborted task.");
			if(character == null || !character.isMoving)
				return;

			synchronized(character._targetRecorder)
			{
				float speed = character.getMoveSpeed();

				if(speed <= 0)
				{
					character.stopMove();
					return;
				}
				long now = System.currentTimeMillis();

				if(character.isFollow)
				{
					follow_target = character.getFollowTarget();
					if(follow_target == null)
					{
						character.stopMove();
						return;
					}
					if(character.isInRangeZ(follow_target, character._offset) && GeoEngine.canSeeTarget(character, follow_target, false))
					{
						//if(character.isPlayer())
						//	_log.info(character + " arrived target/stop move notify AI, dest: " + character.getDistance(follow_target));
						character.stopMove();
						ThreadPoolManager.getInstance().executeAi(new NotifyAITask(character, CtrlEvent.EVT_ARRIVED_TARGET, null, null), character.isPlayable());
						//if(!character.isPlayer()())
						//	character.validateLocation(1);
						return;
					}
				}

				double speedMod = 1;
				if(character.isPlayer())
				{
					speedMod = character.getPlayer().clientSpeedMod;
					if(character.isSwimming())
					{
						if(speedMod < 0.5 || speedMod > 1.2)
							speedMod = 1;
					}
					else if(character.isPawn)
					{
						if(speedMod > 1.1)
							speedMod = 1.1;
						else if(speedMod < 0.95)
							speedMod = 0.95;
					}
					else
						speedMod = 1;
				}
				else if(character.isNpc())
					speedMod = 1.1;

				donedist += (now - character._startMoveTime) * character._previousSpeed * speedMod / 1000f;

				double done = donedist / alldist;

				if(done >= 1 || character.moveList.size() == 0)
				{
					if(!abort)
					{
						//if(character.isPlayer())
						//	character.sendMessage("move done");
						character.moveNext(false);
					}
					return;
				}

				int x = Integer.MIN_VALUE;
				int y = Integer.MIN_VALUE;
				int z = 0;
				try
				{
					int index = (int) (character.moveList.size() * done);
					if(index < 0 || index >= character.moveList.size())
					{
						//character.sendMessage("index: " + index + ", size: " + character.moveList.size() + ", done: " + done);
						if(debug)
							_logMove.warn(character + " index: " + index + ", size: " + character.moveList.size() + ", done: " + done);
						Thread.dumpStack();
						character.stopMove();
						return;
					}

					if(character.isPlayer() && !character.isFloating() && lastCheckIndex <= index)
					{
						lastCheckIndex = Math.min(index + 5, character.moveList.size());
						Location nsweLoc = GeoEngine.moveCheckSimple(character.moveList.subList(index, lastCheckIndex), character.getReflection());
						//_log.info(character + " NSWE check from: " + index + " to " + lastCheckIndex + " loc: " + nsweLoc);
						if(lastCheckIndex - 1 >= 0 && !nsweLoc.equals(character.moveList.get(lastCheckIndex - 1)))
						{
							//_log.info(character + " NSWE check fail move to: " + nsweLoc.geo2world());
							nsweLoc.geo2world();
							character.isMoving = false;
							ThreadPoolManager.getInstance().executeMove(new MoveToTask(character, nsweLoc));
							return;
						}
					}

					Location geoLoc = character.moveList.get(index);
					int xm = (geoLoc.getX() << 4) + L2World.MAP_MIN_X;
					int ym = (geoLoc.getY() << 4) + L2World.MAP_MIN_Y;
					x = Math.min(xm + 15, Math.max(xm + 1, startLoc.getX() + (int) Math.round(donedist * xs)));
					y = Math.min(ym + 15, Math.max(ym + 1, startLoc.getY() + (int) Math.round(donedist * ys)));
					z = geoLoc.getZ();

					if(debug)
						_logMove.info(character + " set " + index + ": " + x + "," + y + "," + z);

					if(!character.isFlying() && !character.isInBoat() && !character.isSwimming() && !character.isVehicle())
					{
						if(z - character.getZ() > 256)
						{
							if(debug)
							{
								String bug_text = character + " geo bug at: " + character.getLoc() + " => " + x + "," + y + "," + z + "\nAll path: " + character + " " + character.moveList.get(0).clone().geo2world() + " => " + character.moveList.get(character.moveList.size() - 1).clone().geo2world();
								_log.warn(bug_text);
							}
							//if(character.isPlayer() && character.getAccessLevel() >= 100)
							//	character.sendMessage(bug_text);
							character.stopMove();
							return;
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}

				// Проверяем, на всякий случай
				if(!character.isMoving || x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
					return;

				//if(character.isPlayer())
				//	_log.info(character + " new: " + loc.x +"," + loc.y + "," + loc.z + String.format(" cd: %.2f", donedist));

				if(character.isInBoat())
				{
					int dx = character.getX() - x;
					int dy = character.getY() - y;
					int dz = character.getZ() - z;
					character.setXYZ(character.getX() + dx, character.getY() + dy, character.getZ() + dz, true);
				}
				else
					character.setXYZ(x, y, z, true);

				// В процессе изменения координат, мы остановились
				if(!character.isMoving)
					return;

				character._previousSpeed = speed;
				character._startMoveTime = now;
				if(!abort)
					character._moveTask = ThreadPoolManager.getInstance().scheduleMove(character._moveTaskRunnable, character.getMoveTickInterval());
			}
		}
	}

	/**
	 * Task of AI notification
	 */
	public static class NotifyAITask implements Runnable
	{
		private final CtrlEvent _evt;
		private final long _charStoredId;
		private final Object _arg0;
		private final Object _arg1;
		private final Object _arg2;

		public NotifyAITask(L2Character cha, CtrlEvent evt, Object arg0, Object arg1)
		{
			_charStoredId = cha.getStoredId();
			_evt = evt;
			_arg0 = arg0;
			_arg1 = arg1;
			_arg2 = null;
		}

		public NotifyAITask(L2Character cha, CtrlEvent evt, Object arg0, Object arg1, Object arg2)
		{
			_charStoredId = cha.getStoredId();
			_evt = evt;
			_arg0 = arg0;
			_arg1 = arg1;
			_arg2 = arg2;
		}

		public void run()
		{
			try
			{
				L2Character cha = L2ObjectsStorage.getAsCharacter(_charStoredId);
				if(cha != null && cha.hasAI())
					cha.getAI().notifyEvent(_evt, _arg0, _arg1, _arg2);
			}
			catch(Throwable t)
			{
				_log.warn("Exception " + t);
				t.printStackTrace();
			}
		}
	}

	public static class MoveToTask implements Runnable
	{
		private long _charStoredId;
		private Location _loc;

		public MoveToTask(L2Character cha, Location loc)
		{
			_charStoredId = cha.getStoredId();
			_loc = loc;
		}

		public void run()
		{
			L2Character cha = L2ObjectsStorage.getAsCharacter(_charStoredId);
			if(cha != null)
				cha.moveToLocation(_loc, 0, false);
		}
	}

	public static class MoveInVehicleTask extends MoveNextTask
	{
		private long _playerStoredId;
		private final Location toLoc;
		private long startMoveTime;

		public MoveInVehicleTask(L2Player player)
		{
			super(player);
			_playerStoredId = player.getStoredId();
			toLoc = new Location(0, 0, 0);
		}

		public MoveInVehicleTask setTarget(Location toPos)
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player == null || toPos == null)
				return null;

			donedist = 0;
			startLoc.set(player.getLocInVehicle());
			toLoc.set(toPos);
			int dx = toPos.getX() - player.getLocInVehicle().getX();
			int dy = toPos.getY() - player.getLocInVehicle().getY();
			int dz = toPos.getZ() - player.getLocInVehicle().getZ();

			alldist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

			xs = dx / alldist;
			ys = dy / alldist;
			zs = dz / alldist;

			startMoveTime = System.currentTimeMillis();

			return this;
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);

			if(player == null || !player.isInBoat() || !player.isMoving)
				return;

			long now = System.currentTimeMillis();

			donedist += (now - startMoveTime) * player._previousSpeed / 1000f;

			double done = donedist / alldist;

			if(done >= 1)
			{
				player.setLocInVehicle(toLoc.clone());
				player.moveNext(false);
				//if(player.isInAirShip())
				//	player.broadcastPacket(new ExValidateLocationInAirShip(player));
				return;
			}

			int x = startLoc.getX() + (int) Math.round(donedist * xs);
			int y = startLoc.getY() + (int) Math.round(donedist * ys);
			int z = startLoc.getZ() + (int) Math.round(donedist * zs);

			player.setLocInVehicle(new Location(x, y, z));

			startMoveTime = System.currentTimeMillis();
			player._previousSpeed = player.getMoveSpeed();

			if(!abort)
				player._moveTask = ThreadPoolManager.getInstance().scheduleMove(this, 500);
		}
	}

	/**
	 * Task lauching the function stopPvPFlag()
	 */
	public static class PvPFlagTask implements Runnable
	{
		private final long _playerStoredId;

		public PvPFlagTask(L2Player player)
		{
			_playerStoredId = player.getStoredId();
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player == null)
				return;

			try
			{
				if(Math.abs(System.currentTimeMillis() - player.getLastPvpAttack()) > Config.PVP_TIME)
					player.stopPvPFlag();
				else if(Math.abs(System.currentTimeMillis() - player.getLastPvpAttack()) > Config.PVP_TIME - Config.PVP_BLINK_TIME)
					player.updatePvPFlag(2);
				else
					player.updatePvPFlag(1);
			}
			catch(Exception e)
			{
				_log.warn("error in pvp flag task:", e);
			}
		}
	}

	public static class LookingForFishTask implements Runnable
	{
		private final long _playerStoredId;
		private boolean _isNoob, _isUpperGrade;
		private int _fishType, _fishGutsCheck;
		private long _endTaskTime;

		protected LookingForFishTask(L2Player player, int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
		{
			_playerStoredId = player.getStoredId();
			_fishGutsCheck = fishGutsCheck;
			_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
			_fishType = fishType;
			_isNoob = isNoob;
			_isUpperGrade = isUpperGrade;
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player == null)
				return;

			if(System.currentTimeMillis() >= _endTaskTime)
			{
				player.endFishing(false);
				return;
			}
			if(_fishType == -1)
				return;
			int check = Rnd.get(1000);
			if(_fishGutsCheck > check)
			{
				player.stopLookingForFishTask();
				player.startFishCombat(_isNoob, _isUpperGrade);
			}
		}
	}

	public static class WaterTask implements Runnable
	{
		private final long _playerStoredId;

		public WaterTask(L2Player player)
		{
			_playerStoredId = player.getStoredId();
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player == null)
				return;

			if(!player.isInZone(L2Zone.ZoneType.water))
			{
				player.setWaterTask(null);
				return;
			}

			if(!player.isDead())
			{
				double reduceHp = player.getMaxHp() / 100;

				if(reduceHp < 1)
					reduceHp = 1;

				//reduced hp, because not rest
				player.decreaseHp(reduceHp, player, true, true);
				player.sendPacket(new SystemMessage(SystemMessage.YOU_RECEIVED_S1_DAMAGE_BECAUSE_YOU_WERE_UNABLE_TO_BREATHE).addNumber((int) reduceHp));
			}
			player.setWaterTask(ThreadPoolManager.getInstance().scheduleEffect(this, 1000));
		}
	}

	public static class HitTask implements Runnable
	{
		private final long _charStoredId, _targetSoredId;
		private boolean _crit;
		private int _damage;
		private boolean _miss;
		private boolean _shld;
		private boolean _soulshot;
		private boolean _unchargeSS;

		public HitTask(L2Character character, L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld, boolean unchargeSS)
		{
			_charStoredId = character.getStoredId();
			_targetSoredId = target.getStoredId();
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
			_unchargeSS = unchargeSS;
		}

		public void run()
		{
			L2Character cha = L2ObjectsStorage.getAsCharacter(_charStoredId);
			L2Character target = L2ObjectsStorage.getAsCharacter(_targetSoredId);

			if(cha == null || target == null)
				return;

			try
			{
				cha.onHitTimer(target, _damage, _crit, _miss, _soulshot, _shld, _unchargeSS);
			}
			catch(Throwable e)
			{
				_log.warn("", e);
			}
		}
	}

	/**
	 * Task lauching the function onMagicUseTimer()
	 */
	public static class MagicUseTask implements Runnable
	{
		private final long _charStoredId;
		private final boolean _forceUse;
		private final L2ItemInstance _usedItem;

		public MagicUseTask(L2Character character, L2Character target, L2Skill skill, L2ItemInstance usedItem, boolean forceUse)
		{
			_charStoredId = character.getStoredId();
			_forceUse = forceUse;
			_usedItem = usedItem;
			character.setCastingSkill(skill);
			character.setCastingTarget(target);
		}

		public void run()
		{
			L2Character cha = L2ObjectsStorage.getAsCharacter(_charStoredId);
			if(cha == null)
				return;

			L2Skill skill = cha.getCastingSkill();
			if(skill == null)
				return;

			L2Character target = cha.getCastingTarget();
			if(target == null)
				return;

			cha.onMagicUseTimer(target, skill, _usedItem, _forceUse);
		}
	}

	public static class MagicLaunchedTask implements Runnable
	{
		public final boolean _forceUse;
		private final long _charStoredId;

		public MagicLaunchedTask(L2Character caster, boolean forceUse)
		{
			_charStoredId = caster.getStoredId();
			_forceUse = forceUse;
		}

		public void run()
		{
			L2Character cha = L2ObjectsStorage.getAsCharacter(_charStoredId);
			if(cha == null)
				return;

			L2Skill skill = cha.getCastingSkill();
			if(skill == null)
				return;

			L2Character target = cha.getCastingTarget();
			if(target == null)
				return;

			Location loc = skill.getRushLoc(cha, target);
			if(loc != null)
			{
				cha.setPrevLoc(cha.getLoc());
				cha.broadcastPacket(new FlyToLocation(cha, loc, skill.getFlyType()));
				if(skill.getCastType() == L2Skill.CastType.rush_behind)
					cha.setHeading(loc.getHeading());
				cha.setXYZ(loc.getX(), loc.getY(), loc.getZ(), false);
			}

			if(skill.getCastRange() > 0 && !GeoEngine.canSeeTarget(cha, target, cha.isFloating()))
			{
				if(cha.isNpc())
					cha.abortCast();
				else
				{
					if(cha.isPlayer())
						cha.sendPacket(Msg.CANNOT_SEE_TARGET);

					cha.breakCast(true, false);

					if(cha.isPet() || cha.isSummon())
					{
						L2Summon thisSummon = (L2Summon) cha;
						L2Player player = cha.getPlayer();

						if(player == null || thisSummon.getDistance(cha.getPlayer()) < Config.PLAYER_VISIBILITY)
							cha.getAI().setIntention(AI_INTENTION_ACTIVE);

						if(player != null)
							player.sendPacket(Msg.CANNOT_SEE_TARGET);
					}
				}
				return;
			}

			List<L2Character> targets = skill.getTargets(cha, target, _forceUse);
			cha.broadcastPacket(new MagicSkillLaunched(cha.getObjectId(), skill.getDisplayId(), skill.getDisplayLevel(), targets, skill.isBuff()));
		}
	}

	/**
	 * PremiumExpire
	 */
	public static class PremiumExpire implements Runnable
	{
		private final long _playerStoredId;

		public PremiumExpire(L2Player player)
		{
			_playerStoredId = player.getStoredId();
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player == null)
				return;
			player.getNetConnection().setPremiumExpire(0);
			if(player.getParty() != null)
				player.getParty().recalculatePartyData();
			String msg = new CustomMessage("common.PremiumEnd", player).toString();
			player.sendPacket(new ExShowScreenMessage(msg, 10000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, true));
			if(Config.PREMIUM_AUTOLOOT_ONLY)
				player.unsetVar("autoloot");
			player.sendMessage(msg);
			player.updateStats();
			// not work in GP2 :(
			//player.sendPacket(new ExBRPremiumState(player.getObjectId(), false));
		}
	}

	/**
	 * HourlyTask
	 */
	public static class HourlyTask implements Runnable
	{
		private final long _playerStoredId;

		public HourlyTask(L2Player player)
		{
			_playerStoredId = player.getStoredId();
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player == null)
				return;
			int hoursInGame = player.getHoursInGame();
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_PLAYING_FOR_AN_EXTENDED_PERIOD_OF_TIME_PLEASE_CONSIDER_TAKING_A_BREAK).addNumber(hoursInGame));
			if(player.getRecSystem() != null)
				player.sendPacket(new SystemMessage(SystemMessage.YOU_OBTAINED_S1_RECOMMENDS).addNumber(player.getRecSystem().addRecommendsLeft()));
		}
	}

	/**
	 * RecommendBonusTask
	 */
	public static class RecommendBonusTask implements Runnable
	{
		private final long _playerStoredId;

		public RecommendBonusTask(L2Player player)
		{
			_playerStoredId = player.getStoredId();
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player == null || player.getRecSystem() == null)
				return;
			player.getRecSystem().setBonusTime(0);
			player.getRecSystem().sendInfo();
		}
	}

	public static class NotifyFaction implements Runnable
	{
		private final long _npcStoredId;
		private final long _attackerStoredId;
		private final int _damage;

		public NotifyFaction(L2NpcInstance npc, L2Character attacker, int damage)
		{
			_npcStoredId = npc.getStoredId();
			_attackerStoredId = attacker.getStoredId();
			_damage = damage;
		}

		public void run()
		{
			L2NpcInstance npc = L2ObjectsStorage.getAsNpc(_npcStoredId);
			L2Character attacker = L2ObjectsStorage.getAsCharacter(_attackerStoredId);

			if(attacker == null || npc == null)
				return;

			try
			{
				for(L2NpcInstance npc1 : npc.getAroundFriends())
					if(!npc1.isIgnoreClanHelp())
						npc1.onClanAttacked(npc, attacker, _damage);
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
		}
	}

	public static class CancelAttackStance implements Runnable
	{
		private final long _charStoredId;

		public CancelAttackStance(L2Character cha)
		{
			_charStoredId = cha.getStoredId();
		}

		public void run()
		{
			L2Character cha = L2ObjectsStorage.getAsCharacter(_charStoredId);
			if(cha != null)
			{
				cha.broadcastPacket(new AutoAttackStop(cha.getObjectId()));
				try
				{
					if(cha._stanceTask != null)
						cha._stanceTask.cancel(false);
					cha._stanceTask = null;
				}
				catch(NullPointerException e)
				{
				}
			}
		}
	}

	public static class SendUserInfo implements Runnable
	{
		private final long _playerStoredId;

		public SendUserInfo(L2Player player)
		{
			_playerStoredId = player.getStoredId();
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player != null)
			{
				player.sendPacket(new UserInfo(player));
				player.sendPacket(new ExBrExtraUserInfo(player));
				if(player.getSiegeState() == 3)
					player.sendPacket(new ExDominionWarStart(player));
				player._userInfoTask = null;
			}
		}
	}

	public static class BroadcastCharInfoTask implements Runnable
	{
		private final long _playerStoredId;

		public BroadcastCharInfoTask(L2Player player)
		{
			_playerStoredId = player.getStoredId();
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player != null)
			{
				player.broadcastCharInfo();
				player._broadcastCharInfoTask = null;
			}
		}
	}

	public static class SoulIncreaseTask implements Runnable
	{
		private final long _playerStoredId;

		public SoulIncreaseTask(L2Player player)
		{
			_playerStoredId = player.getStoredId();
		}

		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(_playerStoredId);
			if(player != null)
			{
				player.increaseSouls(1);
			}
		}
	}

	public static class ResetSoulTask implements Runnable
	{
		private final long playerStoreId;

		public ResetSoulTask(L2Player player)
		{
			playerStoreId = player.getStoredId();
		}

		@Override
		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(playerStoreId);
			if(player == null)
				return;

			player.decreaseSouls(999);
		}
	}

	public static class UnblockTask implements Runnable
	{
		private final long storeId;

		public UnblockTask(L2Character cha)
		{
			storeId = cha.getStoredId();
		}

		@Override
		public void run()
		{
			L2Character cha = L2ObjectsStorage.getAsCharacter(storeId);
			if(cha == null)
				return;

			cha.unblock();
		}
	}

	public static class JailTask implements Runnable
	{
		private final long storedId;
		private final Location loc;

		public JailTask(L2Player cha, Location loc)
		{
			this.storedId = cha.getStoredId();
			this.loc = loc;
		}

		@Override
		public void run()
		{
			L2Player cha = L2ObjectsStorage.getAsPlayer(storedId);
			if(cha == null)
				return;

			cha.unsetVar("jailed");
			cha.teleToLocation(loc);
			cha.stopJail();
		}
	}

	public static class TerritoryWardPeaceTask implements Runnable
	{
		private final long storedId;

		public TerritoryWardPeaceTask(L2Player player)
		{
			this.storedId = player.getStoredId();
		}

		@Override
		public void run()
		{
			L2Player player = L2ObjectsStorage.getAsPlayer(storedId);
			if(player == null)
				return;

			L2ItemInstance ward;
			if(player.isCombatFlagEquipped() && (ward = player.getActiveWeaponInstance()) != null && ward.isTerritoryWard())
			{
				player.updateTimeInPeaceZoneWithWard();

				if(player.isInZonePeace())
				{
					if(player.getTimeInPeaceZoneWithTW() > Config.ALT_TERRITORY_WARD_PEACE_ZONE_TIMEOUT)
					{
						player.setTimeInPeaceZoneWithTW(0);
						player.destroyItem("Timeout", ward.getObjectId(), 1, null, false);
						player.setCombatFlagEquipped(false);
						TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.THE_CHARACTER_THAT_ACQUIRED_S1_WARD_HAS_BEEN_KILLED).addHideoutName(ward.getItemId() - 13479));
						TerritoryWarManager.respawnWard(player.getObjectId());
						return;
					}
				}

				ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
			}
		}
	}
}
