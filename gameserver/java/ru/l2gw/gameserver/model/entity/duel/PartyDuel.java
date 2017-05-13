package ru.l2gw.gameserver.model.entity.duel;

import javolution.util.FastList;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.util.Location;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author rage
 * @date 20.05.11 14:52
 */
public class PartyDuel extends Duel
{
	private static final List<L2Player> _emptyList = new FastList<L2Player>(0);

	private final String _leader1Name;
	private final String _leader2Name;

	private GCSArray<WeakReference<L2Player>> _team1;
	private GCSArray<WeakReference<L2Player>> _team2;

	private Instance _partyInstance;

	public PartyDuel(L2Player playerA, L2Player playerB)
	{
		super(playerA, playerB);
		_leader1Name = playerA.getName();
		_leader2Name = playerB.getName();
		_duelEndTime = System.currentTimeMillis() + 300000;

		L2Party party1 = playerA.getParty();
		L2Party party2 = playerB.getParty();

		_partyInstance = InstanceManager.getInstance().createNewInstance(1, _emptyList);

		_team1 = new GCSArray<WeakReference<L2Player>>(party1.getMemberCount());
		_team2 = new GCSArray<WeakReference<L2Player>>(party2.getMemberCount());

		for(L2Player member : party1.getPartyMembers())
		{
			member.setDuel(this);
			member.setDuelState(DUELSTATE_PREPARE);
			if(member.getPet() != null)
				member.getPet().setDuelState(DUELSTATE_PREPARE);
			member.setDuelSide(1);
			member.sendPacket(Msg.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
			savePlayerConditions(member);
			_team1.add(new WeakReference<L2Player>(member));
		}

		for(L2Player member : party2.getPartyMembers())
		{
			member.setDuel(this);
			member.setDuelState(DUELSTATE_PREPARE);
			if(member.getPet() != null)
				member.getPet().setDuelState(DUELSTATE_PREPARE);
			member.setDuelSide(2);
			member.sendPacket(Msg.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
			savePlayerConditions(member);
			_team2.add(new WeakReference<L2Player>(member));
		}

		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				teleportToArena();
			}
		}, 500);
	}

	private void teleportToArena()
	{
		int pos = 0;
		for(L2Player player : getPlayersList(_team1))
		{
			player.setStablePoint(player.getLoc());
			player.teleToLocation(_partyInstance.getTemplate().getRestartPoints().get(pos), _partyInstance.getReflection());
			pos++;
		}

		pos = 9;
		for(L2Player player : getPlayersList(_team2))
		{
			player.setStablePoint(player.getLoc());
			player.teleToLocation(_partyInstance.getTemplate().getRestartPoints().get(pos), _partyInstance.getReflection());
			pos++;
		}
	}

	private void teleportBack()
	{
		for(L2Player player : getPlayersList(_team1))
		{
			restorePlayerConditions(player, false);
			player.teleToLocation(player.getStablePoint(), 0);
		}

		for(L2Player player : getPlayersList(_team2))
		{
			restorePlayerConditions(player, false);
			player.teleToLocation(player.getStablePoint(), 0);
		}

		_partyInstance.stopInstance();
	}


	private GArray<L2Player> getPlayersList(GCSArray<WeakReference<L2Player>> list)
	{
		GArray<L2Player> res = new GArray<L2Player>(list.size());
		for(WeakReference<L2Player> playerRef : list)
		{
			L2Player player = playerRef.get();
			if(player != null)
				res.add(player);
		}

		return res;
	}

	private boolean isTeamOnline(GCSArray<WeakReference<L2Player>> list)
	{
		for(WeakReference<L2Player> playerRef : list)
		{
			L2Player player = playerRef.get();
			if(player != null && !player.isDeleting() && !player.isInOfflineMode() && player.getReflection() == _partyInstance.getReflection())
				return true;
		}

		return false;
	}

	private boolean isTeamDefeated(GCSArray<WeakReference<L2Player>> list)
	{
		for(WeakReference<L2Player> playerRef : list)
		{
			L2Player player = playerRef.get();
			if(player != null && player.getDuelState() != DUELSTATE_DEAD)
				return false;
		}

		return true;
	}

	@Override
	public DuelResult checkEndDuelCondition()
	{
		// one of the players might leave during duel
		if(!isTeamOnline(_team1) || !isTeamOnline(_team2))
			return DuelResult.Canceled;

		if(System.currentTimeMillis() >= _duelEndTime)
			return DuelResult.Timeout;
		// Has a player been declared winner yet?
		else if(isTeamDefeated(_team2))
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResult.Team1Win;
		}
		else if(isTeamDefeated(_team1))
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResult.Team2Win;
		}

		return DuelResult.Continue;
	}

	@Override
	public void startDuel()
	{
		if(!isTeamOnline(_team1) || !isTeamOnline(_team2))
		{
			// clean up
			_playerConditions.clear();
			_playerConditions = null;
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					teleportBack();
				}
			}, 1000);
			return;
		}

		for(L2Player player : getPlayersList(_team1))
		{
			player.setTeam(1);
			player.setDuelState(DUELSTATE_DUELLING);
			if(player.getPet() != null)
				player.getPet().setDuelState(DUELSTATE_DUELLING);
		}

		for(L2Player player : getPlayersList(_team2))
		{
			player.setTeam(2);
			player.setDuelState(DUELSTATE_DUELLING);
			if(player.getPet() != null)
				player.getPet().setDuelState(DUELSTATE_DUELLING);
		}

		// Send duel Start packets
		ExDuelReady ready = new ExDuelReady(1);
		ExDuelStart start = new ExDuelStart(1);

		broadcastToTeam1(ready);
		broadcastToTeam2(ready);
		broadcastToTeam1(start);
		broadcastToTeam2(start);

		for(L2Player player : getPlayersList(_team1))
		{
			player.broadcastStatusUpdate();
			player.broadcastUserInfo();
			if(player.getPet() != null)
				player.getPet().broadcastPetInfo();
		}

		for(L2Player player : getPlayersList(_team2))
		{
			player.broadcastStatusUpdate();
			player.broadcastUserInfo();
			if(player.getPet() != null)
				player.getPet().broadcastPetInfo();
		}

		// play sound
		PlaySound ps = new PlaySound(1, "B04_S01", 0, 0, new Location(0, 0, 0));
		broadcastToTeam1(ps);
		broadcastToTeam2(ps);

		// start duelling task
		ThreadPoolManager.getInstance().scheduleGeneral(new DuelTasks.DuelTask(this), 1000);
	}

	@Override
	public void broadcastToTeam1(L2GameServerPacket packet)
	{
		for(L2Player player : getPlayersList(_team1))
			player.sendPacket(packet);
	}

	@Override
	public void broadcastToTeam2(L2GameServerPacket packet)
	{
		for(L2Player player : getPlayersList(_team2))
			player.sendPacket(packet);
	}

	@Override
	public void onPlayerDefeat(L2Player player)
	{
		player.setDuelState(DUELSTATE_DEAD);
		player.setTeam(0);

		for(L2Player member : getPlayersList(_team1))
			if(member.getTarget() == player)
				member.setTarget(null);

		for(L2Player member : getPlayersList(_team2))
			if(member.getTarget() == player)
				member.setTarget(null);
	}

	@Override
	protected void stopFighting()
	{
		for(L2Player player	: getPlayersList(_team1))
		{
			player.abortCast();
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			player.setTarget(null);
			player.sendPacket(Msg.ActionFail);
			player.setDuelState(DUELSTATE_PREPARE);
			if(player.getPet() != null)
				player.getPet().setDuelState(DUELSTATE_PREPARE);
		}
		for(L2Player player	: getPlayersList(_team2))
		{
			player.abortCast();
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			player.setTarget(null);
			player.sendPacket(Msg.ActionFail);
			player.setDuelState(DUELSTATE_PREPARE);
			if(player.getPet() != null)
				player.getPet().setDuelState(DUELSTATE_PREPARE);
		}
	}

	@Override
	public boolean isPartyDuel()
	{
		return true;
	}

	@Override
	public void playKneelAnimation(int side)
	{
		GArray<L2Player> looser = null;
		if(side == 1)
			looser = getPlayersList(_team1);
		else if(side == 2)
			looser = getPlayersList(_team2);

		if(looser != null)
			for(L2Player player : looser)
				player.broadcastPacket(new SocialAction(player.getObjectId(), SocialAction.SocialType.BOW));
	}

	public void endDuel(DuelResult result)
	{
		if(!isTeamOnline(_team1) || !isTeamOnline(_team2))
		{
			//clean up
			_playerConditions.clear();
			_playerConditions = null;
			teleportBack();
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

		ThreadPoolManager.getInstance().scheduleGeneral(new DuelTasks.EndDuelTask(this, result), 3000);

		// Send end duel packet
		ExDuelEnd duelEnd = new ExDuelEnd(1);
		broadcastToTeam1(duelEnd);
		broadcastToTeam2(duelEnd);
	}

	public void finishDuel(DuelResult result)
	{
		// inform players of the result
		SystemMessage sm;
		switch(result)
		{
			case Team1Win:
				// send SystemMessage
				sm = new SystemMessage(SystemMessage.S1S_PARTY_HAS_WON_THE_DUEL).addString(_leader1Name);

				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Team2Win:
				// send SystemMessage
				sm = new SystemMessage(SystemMessage.S1S_PARTY_HAS_WON_THE_DUEL).addString(_leader2Name);

				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Canceled:
			case Timeout:
				stopFighting();

				// send SystemMessage
				sm = new SystemMessage(SystemMessage.THE_DUEL_HAS_ENDED_IN_A_TIE);

				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
		}

		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				teleportBack();
			}
		}, 10000);
	}
}
