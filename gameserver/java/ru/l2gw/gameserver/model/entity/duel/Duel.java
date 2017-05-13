package ru.l2gw.gameserver.model.entity.duel;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.duel.DuelTasks.DuelTask;
import ru.l2gw.gameserver.model.entity.duel.DuelTasks.EndDuelTask;
import ru.l2gw.gameserver.model.entity.duel.DuelTasks.StartDuelTask;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.util.Location;

import java.lang.ref.WeakReference;


public class Duel
{
	protected static final Log _log = LogFactory.getLog(Duel.class.getName());

	public static final int DUELSTATE_NODUEL = 0;
	public static final int DUELSTATE_PREPARE = 1;
	public static final int DUELSTATE_DUELLING = 2;
	public static final int DUELSTATE_DEAD = 3;
	public static final int DUELSTATE_WINNER = 4;
	public static final int DUELSTATE_INTERRUPTED = 5;

	// =========================================================
	// Data Field
	protected WeakReference<L2Player> _playerA;
	protected WeakReference<L2Player> _playerB;
	protected long _duelEndTime;
	protected int _surrenderRequest = 0;

	protected FastList<PlayerCondition> _playerConditions;

	public static enum DuelResult
	{
		Continue,
		Team1Win,
		Team2Win,
		Player1Surrender,
		Player2Surrender,
		Canceled,
		Timeout
	}

	// =========================================================
	// Constructor
	public Duel(L2Player playerA, L2Player playerB)
	{
		_playerA = new WeakReference<L2Player>(playerA);
		_playerB = new WeakReference<L2Player>(playerB);

		playerA.setDuel(this);
		playerA.setDuelSide(1);
		playerB.setDuel(this);
		playerB.setDuelSide(2);

		_duelEndTime = System.currentTimeMillis() + 120000;

		_playerConditions = new FastList<PlayerCondition>();

		// Schedule duel start
		ThreadPoolManager.getInstance().scheduleGeneral(new StartDuelTask(this), this instanceof PartyDuel ? 25000 : 2000);
	}

	/**
	 * Stops all players from attacking.
	 * Used for duel timeout / interrupt.
	 */
	protected void stopFighting()
	{
		L2Player playerA = _playerA.get();
		L2Player playerB = _playerB.get();

		if(playerA != null)
		{
			playerA.abortCast();
			playerA.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			playerA.setTarget(null);
			playerA.sendPacket(Msg.ActionFail);
		}
		if(playerB != null)
		{
			playerB.abortCast();
			playerB.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			playerB.setTarget(null);
			playerB.sendPacket(Msg.ActionFail);
		}
	}

	/**
	 * Starts the duel
	 */
	public void startDuel()
	{
		// Save player Conditions
		L2Player playerA = _playerA.get();
		L2Player playerB = _playerB.get();

		if(playerA == null || playerB == null || playerA.getDuel() != this || playerB.getDuel() != this)
		{
			// clean up
			_playerConditions.clear();
			_playerConditions = null;
			restorePlayerConditions(playerA, true);
			restorePlayerConditions(playerB, true);
			return;
		}

		savePlayerConditions(playerA);
		savePlayerConditions(playerB);

		// set isInDuel() state
		playerA.setTeam(1);
		playerA.setDuelState(DUELSTATE_DUELLING);
		if(playerA.getPet() != null)
			playerA.getPet().setDuelState(DUELSTATE_DUELLING);
		playerB.setTeam(2);
		playerB.setDuelState(DUELSTATE_DUELLING);
		if(playerB.getPet() != null)
			playerB.getPet().setDuelState(DUELSTATE_DUELLING);

		// Send duel Start packets
		ExDuelReady ready = new ExDuelReady(0);
		ExDuelStart start = new ExDuelStart(0);

		broadcastToTeam1(ready);
		broadcastToTeam2(ready);
		broadcastToTeam1(start);
		broadcastToTeam2(start);

		playerA.broadcastStatusUpdate();
		playerB.broadcastStatusUpdate();
		playerA.broadcastUserInfo();
		if(playerA.getPet() != null)
			playerA.getPet().broadcastPetInfo();
		playerB.broadcastUserInfo();
		if(playerB.getPet() != null)
			playerB.getPet().broadcastPetInfo();

		// play sound
		PlaySound ps = new PlaySound(1, "B04_S01", 0, 0, new Location(0, 0, 0));
		broadcastToTeam1(ps);
		broadcastToTeam2(ps);

		// start duelling task
		ThreadPoolManager.getInstance().scheduleGeneral(new DuelTask(this), 1000);
	}

	/**
	 * Save the current player condition: hp, mp, cp, location
	 */
	public void savePlayerConditions(L2Player player)
	{
		_playerConditions.add(new PlayerCondition(player));
	}

	/**
	 * Restore player conditions
	 *
	 * @param was the duel canceled?
	 */
	public void restorePlayerConditions(L2Player player, boolean abnormalDuelEnd)
	{
		if(player == null)
			return;

		player.setDuel(null);
		player.setTeam(0);
		player.setDuelSide(0);
		player.setDuelState(DUELSTATE_NODUEL);
		player.broadcastUserInfo();
		if(player.getPet() != null)
		{
			player.getPet().setDuelState(DUELSTATE_NODUEL);
			player.getPet().broadcastPetInfo();
		}

		// if it is an abnormal DuelEnd do not restore hp, mp, cp
		if(!abnormalDuelEnd)
			for(PlayerCondition pc : _playerConditions)
				pc.restoreCondition();
	}

	/**
	 * Returns whether this is a party duel or not
	 *
	 * @return is party duel
	 */
	public boolean isPartyDuel()
	{
		return false;
	}

	/**
	 * Broadcast a packet to the challanger team
	 */
	public void broadcastToTeam1(L2GameServerPacket packet)
	{
		L2Player playerA = _playerA.get();
		if(playerA == null)
			return;

		playerA.sendPacket(packet);
	}

	/**
	 * Broadcast a packet to the challenged team
	 */
	public void broadcastToTeam2(L2GameServerPacket packet)
	{
		L2Player playerB = _playerB.get();
		if(playerB == null)
			return;

		playerB.sendPacket(packet);
	}

	/**
	 * Playback the bow animation for all loosers
	 */
	public void playKneelAnimation(int side)
	{
		L2Player looser = null;
		if(side == 1)
			looser = _playerA.get();
		else if(side == 2)
			looser = _playerB.get();

		if(looser != null)
			looser.broadcastPacket(new SocialAction(looser.getObjectId(), SocialAction.SocialType.BOW));
	}

	/**
	 * The duel has reached a state in which it can no longer continue
	 *
	 * @param duel result
	 */
	public void endDuel(DuelResult result)
	{
		L2Player playerA = _playerA.get();
		L2Player playerB = _playerB.get();

		if(playerA == null || playerB == null)
		{
			//clean up
			_playerConditions.clear();
			_playerConditions = null;
			restorePlayerConditions(playerA, true);
			restorePlayerConditions(playerB, true);
			return;
		}

		switch(result)
		{
			case Team1Win:
				playKneelAnimation(2);
				break;
			case Team2Win:
				playKneelAnimation(1);
				break;
		}

		ThreadPoolManager.getInstance().scheduleGeneral(new EndDuelTask(this, result), 3000);

		// Send end duel packet
		ExDuelEnd duelEnd;
		duelEnd = new ExDuelEnd(0);

		broadcastToTeam1(duelEnd);
		broadcastToTeam2(duelEnd);
	}

	public void finishDuel(DuelResult result)
	{
		L2Player playerA = _playerA.get();
		L2Player playerB = _playerB.get();

		// inform players of the result
		SystemMessage sm;
		switch(result)
		{
			case Team1Win:
				restorePlayerConditions(playerA, false);
				restorePlayerConditions(playerB, false);
				// send SystemMessage
				sm = new SystemMessage(SystemMessage.S1_HAS_WON_THE_DUEL).addCharName(playerA);

				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Team2Win:
				restorePlayerConditions(playerA, false);
				restorePlayerConditions(playerB, false);
				// send SystemMessage
				sm = new SystemMessage(SystemMessage.S1_HAS_WON_THE_DUEL).addCharName(playerB);

				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Player1Surrender:
				restorePlayerConditions(playerA, false);
				restorePlayerConditions(playerB, false);
				// send SystemMessage
				sm = new SystemMessage(SystemMessage.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addCharName(playerA).addCharName(playerB);

				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Player2Surrender:
				restorePlayerConditions(playerA, false);
				restorePlayerConditions(playerB, false);
				// send SystemMessage
				sm = new SystemMessage(SystemMessage.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addCharName(playerB).addCharName(playerA);

				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Canceled:
				stopFighting();
				// dont restore hp, mp, cp
				restorePlayerConditions(playerA, true);
				restorePlayerConditions(playerB, true);
				// send SystemMessage
				sm = new SystemMessage(SystemMessage.THE_DUEL_HAS_ENDED_IN_A_TIE);

				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Timeout:
				stopFighting();
				// hp,mp,cp seem to be restored in a timeout too...
				restorePlayerConditions(playerA, false);
				restorePlayerConditions(playerB, false);
				// send SystemMessage
				sm = new SystemMessage(SystemMessage.THE_DUEL_HAS_ENDED_IN_A_TIE);

				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
		}

		//clean up
		_playerConditions.clear();
		_playerConditions = null;
	}

	/**
	 * Did a situation occur in which the duel has to be ended?
	 * Did a situation occur in which the duel has to be ended?
	 *
	 * @return DuelResultEnum duel status
	 */
	public DuelResult checkEndDuelCondition()
	{
		L2Player playerA = _playerA.get();
		L2Player playerB = _playerB.get();

		// one of the players might leave during duel
		if(playerA == null || playerB == null)
			return DuelResult.Canceled;

		// got a duel surrender request?
		if(_surrenderRequest != 0)
		{
			if(_surrenderRequest == 1)
				return DuelResult.Player1Surrender;
			else
				return DuelResult.Player2Surrender;
		}
		// duel timed out
		else if(System.currentTimeMillis() >= _duelEndTime)
			return DuelResult.Timeout;

		// Has a player been declared winner yet?
		else if(playerA.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResult.Team1Win;
		}
		else if(playerB.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResult.Team2Win;
		}
		// More end duel conditions for 1on1 duels
		else
		{
			// Duel was interrupted e.g.: player was attacked by mobs / other players
			if(playerA.getDuelState() == DUELSTATE_INTERRUPTED || playerB.getDuelState() == DUELSTATE_INTERRUPTED)
				return DuelResult.Canceled;

			// Are the players too far apart?
			if(!playerA.isInRange(playerB, 1600))
				return DuelResult.Canceled;

			if(playerA.getKarma() > 0 || playerA.getPvpFlag() != 0)
				return DuelResult.Team2Win;
			if(playerA.getKarma() > 0 || playerA.getPvpFlag() != 0)
				return DuelResult.Team1Win;
		}

		return DuelResult.Continue;
	}

	/**
	 * Register a surrender request
	 *
	 * @param surrendering player
	 */
	public void doSurrender(L2Player player)
	{
		// already recived a surrender request
		if(_surrenderRequest != 0)
			return;

		// stop the fight
		stopFighting();
		L2Player playerA = _playerA.get();
		L2Player playerB = _playerB.get();

		if(player == playerA)
		{
			_surrenderRequest = 1;
			playerA.setDuelState(DUELSTATE_DEAD);
			playerB.setDuelState(DUELSTATE_WINNER);
		}
		else if(player == playerB)
		{
			_surrenderRequest = 2;
			playerB.setDuelState(DUELSTATE_DEAD);
			playerA.setDuelState(DUELSTATE_WINNER);
		}
	}

	/**
	 * This function is called whenever a player was defeated in a duel
	 *
	 * @param dieing player
	 */
	public void onPlayerDefeat(L2Player player)
	{
		// Set player as defeated
		if(player.getDuelSide() == 1)
		{
			L2Player playerB = _playerB.get();
			if(playerB != null)
			{
				player.setDuelState(DUELSTATE_DEAD);
				playerB.setDuelState(DUELSTATE_WINNER);
			}
		}
		else if(player.getDuelSide() == 2)
		{
			L2Player playerA = _playerA.get();
			if(playerA != null)
			{
				player.setDuelState(DUELSTATE_DEAD);
				playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}

	public void onBuff(L2Player player, L2Effect debuff)
	{
		for(PlayerCondition pc : _playerConditions)
			if(pc.getPlayer() == player)
			{
				pc.registerDebuff(debuff);
				return;
			}
	}

	public static SystemMessage checkPlayer(L2Player player)
	{
		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
			return new SystemMessage(SystemMessage.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE).addCharName(player);
		if(player.isFishing())
			return new SystemMessage(SystemMessage.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING).addCharName(player);
		if(player.isAlikeDead() || player.isDead() || player.getCurrentHp() < player.getMaxHp() * 0.5 || player.getCurrentMp() < player.getMaxMp() * 0.5)
			return new SystemMessage(SystemMessage.S1_CANNOT_DUEL_BECAUSE_S1S_HP_OR_MP_IS_BELOW_50_PERCENT).addCharName(player);
		if(player.isInZone(ZoneType.peace) || player.isInZone(ZoneType.no_escape) || player.getReflection() != 0 || player.isInZone(ZoneType.water))
			return new SystemMessage(SystemMessage.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA).addCharName(player);
		if(player.isInCombat())
			return new SystemMessage(SystemMessage.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE).addCharName(player);
		if(player.isInDuel())
			return new SystemMessage(SystemMessage.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL).addCharName(player);
		if(player.getKarma() > 0 || player.getPvpFlag() != 0)
			return new SystemMessage(SystemMessage.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE).addCharName(player);
		if(Olympiad.getRegisteredGameType(player) >= 0)
			return new SystemMessage(SystemMessage.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD).addCharName(player);
		if(player.getSiegeState() > 0)
			return new SystemMessage(SystemMessage.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_A_SIEGE_WAR).addCharName(player);
		if(player.isInBoat() || player.getMountEngine().isMounted())
			return new SystemMessage(SystemMessage.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER).addCharName(player);

		return null;
	}
}
