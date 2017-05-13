package ru.l2gw.gameserver.model.entity.olympiad;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.entity.Hero;
import ru.l2gw.gameserver.model.instances.L2CubicInstance;
import ru.l2gw.gameserver.model.instances.L2OlympiadManagerInstance;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.util.CountdownTimer;

import java.util.concurrent.Future;

import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.no_escape;

/**
 * ***********************************************************************************************************
 * Olympiad Game Class
 * ************************************************************************************************************
 */
public class OlympiadGame
{
	private int _arenaId;
	private OlympiadInstance _arena;
	private OlympiadTeam[] _teams;
	private boolean _team1Disconnected;
	private boolean _team2Disconnected;
	private OlympiadGameState _gameState;
	private Future<?> _scheduledTeleportTask;
	private Future<?> _scheduledPrepareTask;
	private Future<?> _scheduledFightTask;
	private Runnable _fightTask;
	private boolean _endInProgress = false;
	private int _gameType = 0;
	private String _winnerName = "";
	private OlympiadTeam _winnerTeam;

	public OlympiadGame(OlympiadTeam team1, OlympiadTeam team2, OlympiadInstance arena, int gameType)
	{
		Olympiad._olyLog.info("OG(" + arena.getArenaId() + ") started");
		_team1Disconnected = false;
		_team2Disconnected = false;

		try
		{
			_arena = arena;
			_arenaId = arena.getArenaId();
			_gameType = gameType;
			_teams = new OlympiadTeam[] { team1, team2 };
			_teams[0].setOlympiadGameId(_arenaId);
			_teams[1].setOlympiadGameId(_arenaId);
		}
		catch(Exception e)
		{
			Olympiad._olyLog.info("OG(" + arena.getArenaId() + ") aborted " + e);
			abortGame();
			return;
		}

		Olympiad._olyLog.info("OG(" + _arenaId + ") initial state");
		setGameState(OlympiadGameState.INITIAL);
		_scheduledTeleportTask = ThreadPoolManager.getInstance().scheduleGeneral(new TeleportTask(), 500);
	}

	public int getGameType()
	{
		return _gameType;
	}

	private class TeleportTask implements Runnable
	{
		private int _sec = 120;
		private int _cd = 60;

		public void run()
		{
			Olympiad._olyLog.info("OG(" + _arenaId + ") TeleportTask started");
			if(getGameState() != OlympiadGameState.INITIAL)
			{
				Olympiad._olyLog.warn("OG(" + _arenaId + ") TeleportTask teleport aborted, wrong state " + getGameState());
				return;
			}

			if(_sec > 0)
			{
				try
				{
					if(_team1Disconnected || !_teams[0].isAllOnline() || _teams[0].isInZone(no_escape))
					{
						_team1Disconnected = true;
						_teams[1].sendPacket(new SystemMessage(SystemMessage.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS));

						Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted by " + _teams[0].getDisconnectedPlayerName() + " vs " + _teams[1].getName());
						for(OlympiadUserInfo oui : _teams[0].getPlayersInfo())
						{
							int penaltyPoint = Math.min(10, oui.getPoints() / 5 + 1);
							oui.setMatchPoint(-penaltyPoint);
							oui.setPoints(oui.getPoints() - penaltyPoint);
							Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted " + oui.getName() + " penalty: " + penaltyPoint + " current: " + oui.getPoints());
						}
						abortGame();
						return;
					}
					if(_team2Disconnected || !_teams[1].isOnline() || _teams[1].isInZone(no_escape))
					{
						_team2Disconnected = true;
						_teams[1].sendPacket(new SystemMessage(SystemMessage.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS));

						Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted by " + _teams[1].getDisconnectedPlayerName() + " vs " + _teams[0].getName());
						for(OlympiadUserInfo oui : _teams[1].getPlayersInfo())
						{
							int penaltyPoint = Math.min(10, oui.getPoints() / 5 + 1);
							oui.setMatchPoint(-penaltyPoint);
							oui.setPoints(oui.getPoints() - penaltyPoint);
							Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted " + oui.getName() + " penalty: " + penaltyPoint + " current: " + oui.getPoints());
						}
						abortGame();
						return;
					}
					sendPacketToPlayers(new SystemMessage(SystemMessage.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S).addNumber(_sec));
				}
				catch(NullPointerException e)
				{
					Olympiad._olyLog.warn("OG(" + _arenaId + ") TeleportTask aborted! Can't send message to player");
					abortGame();
					return;
				}

				if(_sec == 60)
					_cd = 30;
				else if(_sec == 30)
					_cd = 15;
				else if(_sec == 15)
					_cd = 10;
				else if(_sec == 5)
					_cd = 1;

				_sec -= _cd;
				_scheduledTeleportTask = ThreadPoolManager.getInstance().scheduleGeneral(this, _cd * 1000L);
				return;
			}
			Olympiad._olyLog.info("OG(" + _arenaId + ") TeleportTask teleport players");
			teleportPlayersToArena();
		}
	}

	private void teleportPlayersToArena()
	{
		try
		{
			Olympiad._olyLog.info("OG(" + _arenaId + ") Teleport started " + _teams[0].getName() + " vs " + _teams[1].getName());
			boolean party = _gameType == 0 && !_teams[0].checkParty();
			for(OlympiadUserInfo oui : _teams[0].getPlayersInfo())
			{
				L2Player player = oui.getPlayer();
				if(party || player == null || !validatePlayer(player, _teams[1]))
				{
					Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted by " + oui.getName());
					for(OlympiadUserInfo oui1 : _teams[0].getPlayersInfo())
					{
						int penaltyPoint = Math.min(10, oui1.getPoints() / 5 + 1);
						oui1.setMatchPoint(-penaltyPoint);
						oui1.setPoints(oui1.getPoints() - penaltyPoint);
						Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted " + oui1.getName() + " penalty: " + penaltyPoint + " current: " + oui1.getPoints());
					}
					abortGame();
					return;
				}
			}

			party = _gameType == 0 && !_teams[1].checkParty();
			for(OlympiadUserInfo oui : _teams[1].getPlayersInfo())
			{
				L2Player player = oui.getPlayer();
				if(party || player == null || !validatePlayer(player, _teams[0]))
				{
					Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted by " + oui.getName());
					for(OlympiadUserInfo oui1 : _teams[1].getPlayersInfo())
					{
						int penaltyPoint = Math.min(10, oui1.getPoints() / 5 + 1);
						oui1.setMatchPoint(-penaltyPoint);
						oui1.setPoints(oui1.getPoints() - penaltyPoint);
						Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted " + oui1.getName() + " penalty: " + penaltyPoint + " current: " + oui1.getPoints());
					}
					abortGame();
					return;
				}
			}

			Olympiad._olyLog.info("OG(" + _arenaId + ") Teleport set olymp mode");
			_teams[0].preparePlayers(1);
			_teams[1].preparePlayers(2);
			_teams[0].restoreHpMp();
			_teams[1].restoreHpMp();
			Olympiad._olyLog.info("OG(" + _arenaId + ") Teleport teleport players");
			_teams[0].sendPacket(new ExOlympiadMode(1));
			_teams[1].sendPacket(new ExOlympiadMode(2));
			_teams[0].teleToLocation(_arena.getTemplate().getZone().getRestartPoints().get(0), _arena.getReflection());
			_teams[1].teleToLocation(_arena.getTemplate().getZone().getRestartPoints().get(1), _arena.getReflection());
		}
		catch(Exception e)
		{
			Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted! Can't teleport players " + e);
			abortGame();
			return;
		}
		Olympiad._olyLog.info("OG(" + _arenaId + ") Teleport set game state to PREPARE");
		setGameState(OlympiadGameState.PREPARE);
		announceGame();

		_scheduledPrepareTask = ThreadPoolManager.getInstance().scheduleGeneral(new PrepareTask(), 500);
	}

	private void announceGame()
	{
		for(L2OlympiadManagerInstance manager : Olympiad._olympiadManagers)
			Functions.npcSayInRange(manager, Say2C.SHOUT, _gameType == 0 ? 1300132 : _gameType == 1 ? 1300166 : 1300167, Config.SHOUT_RANGE, String.valueOf(_arenaId + 1));
	}

	private boolean validatePlayer(L2Player pl1, OlympiadTeam pl2)
	{
		try
		{
			if(pl1.isCursedWeaponEquipped())
			{
				pl1.sendPacket(new SystemMessage(SystemMessage.C1_IS_THE_OWNER_OF_S2_AND_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD).addCharName(pl1).addItemName(pl1.getCursedWeaponEquippedId()));
				if(pl2 != null)
					pl2.sendPacket(new SystemMessage(SystemMessage.SINCE_YOUR_OPPONENT_IS_NOW_THE_OWNER_OF_S1_THE_OLYMPIAD_HAS_BEEN_CANCELLED).addItemName(pl1.getCursedWeaponEquippedId()));
				Olympiad._olyLog.info("OG(" + _arenaId + ") Teleport aborted by " + pl1 + " is cursed");

				return false;
			}
			else if(pl1.isDead())
			{
				pl1.sendPacket(new SystemMessage(SystemMessage.C1_IS_CURRENTLY_DEAD_AND_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD).addCharName(pl1));
				if(pl2 != null)
					pl2.sendPacket(new SystemMessage(SystemMessage.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE));

				Olympiad._olyLog.info("OG(" + _arenaId + ") Teleport aborted by " + pl1 + " is dead");
				return false;
			}
			else if(pl1.inObserverMode())
			{
				if(pl2 != null)
					pl2.sendPacket(new SystemMessage(SystemMessage.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE));
				return false;
			}
			else if(!pl1.isQuestContinuationPossible(false))
			{
				pl1.sendPacket(new SystemMessage(SystemMessage.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOUR_INVENTORY_SLOT_EXCEEDS_80).addCharName(pl1));
				if(pl2 != null)
					pl2.sendPacket(new SystemMessage(SystemMessage.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE));

				Olympiad._olyLog.info("OG(" + _arenaId + ") Teleport aborted by " + pl1 + " weight limit");
				return false;
			}
		}
		catch(Exception e)
		{
			Olympiad._olyLog.info("OG(" + _arenaId + ") Teleport aborted by NPE");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private class PrepareTask implements Runnable
	{
		private boolean _start = true;
		private int _sec = 60;
		private int _cd = 10;

		public void run()
		{
			if(_start)
			{
				_start = false;
				Olympiad._olyLog.info("OG(" + _arenaId + ") PrepareTask started");
				if(getGameState() != OlympiadGameState.PREPARE)
				{
					Olympiad._olyLog.info("OG(" + _arenaId + ") PrepareTask aborted wrong state");
					abortGame();
					return;
				}

				if(_gameType != 0)
					try
					{
						_arena.notifyEvent("spawn_manager", null, null);
					}
					catch(Exception e)
					{
						Olympiad._log.info("OG(" + _arenaId + ") PrepareTask aborted cannot spawn buffers!");
						e.printStackTrace();
						abortGame();
						return;
					}
				else
				{
					_teams[0].setTeam(1);
					_teams[1].setTeam(2);
				}
			}

			if(_sec > 0)
			{
				if(_sec != 60)
				{
					SystemMessage sm = new SystemMessage(SystemMessage.THE_GAME_WILL_START_IN_S1_SECOND_S);
					sm.addNumber(_sec);
					_arena.broadcastPacket(sm);
				}

				if(!_teams[0].isOnline())
				{
					_team1Disconnected = true;
					Olympiad._olyLog.warn("OG(" + _arenaId + ") PrepareTask aborted due to crash player " + _teams[0].getDisconnectedPlayerName());
					endGame();
					return;
				}
				if(!_teams[1].isOnline())
				{
					_team2Disconnected = true;
					Olympiad._olyLog.warn("OG(" + _arenaId + ") PrepareTask aborted due to crash player " + _teams[1].getDisconnectedPlayerName());
					endGame();
					return;
				}
				if(_sec == 10)
				{
					_arena.notifyEvent("open_door", null, null);
					if(_gameType == 2)
					{
						_teams[0].restoreHpMp();
						_teams[1].restoreHpMp();
					}

					_sec = 5;
					_cd = 1;
					_scheduledPrepareTask = ThreadPoolManager.getInstance().scheduleGeneral(this, 5000);
					return;
				}
				else
					_scheduledPrepareTask = ThreadPoolManager.getInstance().scheduleGeneral(this, _cd * 1000L);

				_sec -= _cd;
				return;
			}

			deleteBuffers();
			Olympiad._olyLog.info("OG(" + _arenaId + ") PrepareTask set fight state");
			setGameState(OlympiadGameState.FIGHT);
			_arena.broadcastPacket(new SystemMessage(SystemMessage.STARTS_THE_GAME));
			_fightTask = new FightTask();
			_scheduledFightTask = ThreadPoolManager.getInstance().scheduleGeneral(_fightTask, 10);
		}
	}

	private class FightTask implements Runnable
	{
		private long _startTime = 0;
		private int _sec;
		private int _cd;
		private boolean _start = true;

		public FightTask()
		{
			_sec = (int) (Config.ALT_OLY_BATTLE / 1000);
			if(_sec > 300)
				_cd = _sec - 300;
			else if(_sec > 60)
				_cd = 60;
			else if(_sec > 10)
				_cd = 10;
			else
				_cd = 1;
		}

		public void run()
		{
			if(_start)
			{
				if(getGameState() != OlympiadGameState.FIGHT)
				{
					Olympiad._olyLog.warn("OG(" + _arenaId + ") FightTask aborted wrong state in fight task " + getGameState());
					abortGame();
					return;
				}

				_startTime = System.currentTimeMillis();
				Olympiad._olyLog.warn("OG(" + _arenaId + ") FightTask started " + _teams[0].getName() + " vs " + _teams[1].getName());
				_teams[0].setIsOlympiadStart(true);
				_teams[1].setIsOlympiadStart(true);
				broadcastPlayersState();
			}

			if(_sec > 0)
			{
				if(!_teams[0].isOnline())
				{
					_team1Disconnected = true;
					Olympiad._olyLog.warn("OG(" + _arenaId + ") PrepareTask aborted due to crash player " + _teams[0].getDisconnectedPlayerName());
					endGame();
					return;
				}
				if(!_teams[1].isOnline())
				{
					_team2Disconnected = true;
					Olympiad._olyLog.warn("OG(" + _arenaId + ") PrepareTask aborted due to crash player " + _teams[1].getDisconnectedPlayerName());
					endGame();
					return;
				}

				if(!_start)
				{
					SystemMessage sm;
					if(_sec >= 60)
					{
						sm = new SystemMessage(SystemMessage.THE_GAME_WILL_END_IN_S1_MINUTES);
						sm.addNumber(_sec / 60);
					}
					else
					{
						sm = new SystemMessage(SystemMessage.THE_GAME_WILL_END_IN_S1_SECONDS);
						sm.addNumber(_sec);
					}

					_arena.broadcastPacket(sm);
				}

				if(_sec == 60)
					_cd = 10;
				else if(_sec == 10)
					_cd = 1;

				_scheduledFightTask = ThreadPoolManager.getInstance().scheduleGeneral(this, _cd * 1000L);

				if(_start)
				{
					_start = false;
					_cd = 60;
				}

				_sec -= _cd;
				return;
			}
			endGame();
		}

		public long getStartTime()
		{
			return _startTime;
		}
	}

	public void addDamage(int objectId, int damage)
	{
		if(getGameState() != OlympiadGameState.FIGHT)
			return;

		_teams[0].addDamage(objectId, damage);
		_teams[1].addDamage(objectId, damage);
	}

	public synchronized void checkTeamDead()
	{
		for(OlympiadTeam team : _teams)
			if(team.isDead())
			{
				endGame();
				return;
			}
	}

	public synchronized void endGame()
	{
		if(_endInProgress)
		{
			Olympiad._olyLog.warn("OG(" + _arenaId + ") end alrady in progress!");
			return;
		}
		_endInProgress = true;

		Olympiad._olyLog.warn("OG(" + _arenaId + ") EndGame starting");

		deleteBuffers();
		setGameState(OlympiadGameState.END);
		long fightTime = 0;

		if(_fightTask != null)
			fightTime = (System.currentTimeMillis() - ((FightTask) _fightTask).getStartTime()) / 1000;

		if(_team1Disconnected && _team2Disconnected)
		{
			Olympiad._olyLog.info("OG(" + _arenaId + ") Olympiad Result: " + _teams[0].getName() + " vs " + _teams[1].getName() + " ... aborted/tie due to crashes!");

			if(_scheduledPrepareTask != null)
				_scheduledPrepareTask.cancel(true);

			if(_scheduledFightTask != null)
				_scheduledFightTask.cancel(true);

			return;
		}

		for(OlympiadTeam team : _teams)
			for(OlympiadUserInfo oui : team.getPlayersInfo())
			{
				oui.updateMatches(_gameType);
				L2Player player = oui.getPlayer();
				if(player != null)
				{
					player.setIsOlympiadStart(false);
					player.abortAttack();
					player.abortCast();
					if(player.getPet() != null)
						player.getPet().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

					for(L2CubicInstance cubic : player.getCubics())
						if(cubic != null)
							cubic.clearAggro();
				}
			}

		int winner = -1; // 1 - Player 1, 2 - Player 2, 0 - tie
		/**
		 * reson's
		 * 0 - Tie
		 * 1 - Player 1 disconnected, Player 2 win
		 * 2 - Player 2 disconnected, Player 1 win
		 * 3 - Player 1 die, Player 2 win
		 * 4 - Player 2 die, Player 1 win
		 * 5 - Player 1 give more damage, Player 1 win
		 * 6 - Player 2 give more damage, Player 2 win
		 */

		if(_gameType != 0)
		{
			OlympiadUserInfo oui1 = _teams[0].getPlayersInfo().get(0);
			OlympiadUserInfo oui2 = _teams[1].getPlayersInfo().get(0);
			L2Player player1 = oui1.getPlayer();
			L2Player player2 = oui2.getPlayer();
			int player1Damage = oui1.getDamage();
			int player2Damage = oui2.getDamage();

			int plOneHp = 0;
			int plTwoHp = 0;

			if(!_team1Disconnected && player1 != null)
			{
				plOneHp = (int) player1.getCurrentHp();
				if(plOneHp == 0)
					player1.setCurrentHp(1);
			}
			else
				winner = 2;

			if(!_team2Disconnected && player2 != null)
			{
				plTwoHp = (int) player2.getCurrentHp();
				if(plTwoHp == 0)
					player2.setCurrentHp(1);
			}
			else
				winner = 1;

			if(winner == -1)
			{
				Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame " + _teams[0].getName() + "(" + plOneHp + ") vs " + _teams[1].getName() + "(" + plTwoHp + ")");
				if(plOneHp > 0 && plTwoHp > 0)
				{
					Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame both live!");
					Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame " + _teams[0].getName() + "(" + plOneHp + ") dmg " + player1Damage + " " + _teams[0].getName() + "(" + plTwoHp + ") dmg " + player2Damage);
					if(player1Damage == player2Damage)
						winner = 0;
					else if(player1Damage < player2Damage)
						winner = 1;
					else
						winner = 2;
				}
				else if(plOneHp > 0)
					winner = 1;
				else
					winner = 2;
			}

			SystemMessage sm;
			if(winner == 0)
			{
				Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame with a TIE " + _teams[0].getName() + " vs " + _teams[1].getName());
				sm = new SystemMessage(SystemMessage.THE_GAME_ENDED_IN_A_TIE);
				_arena.broadcastPacket(sm);
				Hero.updateMatchHistory(_teams[0], _teams[1], (int) fightTime, 3);
				Hero.updateMatchHistory(_teams[1], _teams[0], (int) fightTime, 3);
				sendPacketToPlayers(new ExReceiveOlympiad(this));
			}
			else
			{
				int winnerPoint = Math.min(Math.min(oui1.getPoints(), oui2.getPoints()) / 5 + 1, 10);

				if(winner == 1)
				{
					_winnerName = oui1.getName();
					_winnerTeam = _teams[0];
					oui1.setMatchPoint(winnerPoint);
					oui1.setPoints(oui1.getPoints() + winnerPoint);
					oui2.setMatchPoint(-winnerPoint);
					oui2.setPoints(oui2.getPoints() - winnerPoint);
					Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame Player1 " + oui1.getName() + " win Player " + oui2.getName() + " loose");
					Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame Player1 " + oui1.getName() + " win " + oui1.getPoints() + "(" + oui1.getMatchPoints() +") points Player " + oui2.getName() + " loose " + oui2.getPoints() + "(" + oui2.getMatchPoints() + ") points");
					Olympiad._nobles.get(oui1.getObjectId()).set("wins", Olympiad._nobles.get(oui1.getObjectId()).getInteger("wins") + 1);
					Olympiad._nobles.get(oui2.getObjectId()).set("loos", Olympiad._nobles.get(oui2.getObjectId()).getInteger("loos") + 1);

					Olympiad.updateNobleData(oui1.getObjectId());
					Olympiad.updateNobleData(oui2.getObjectId());
					Hero.updateMatchHistory(_teams[0], _teams[1], (int) fightTime, 1);
					Hero.updateMatchHistory(_teams[1], _teams[0], (int) fightTime, 2);
					sendPacketToPlayers(new ExReceiveOlympiad(this));

					sm = new SystemMessage(SystemMessage.S1_HAS_WON_THE_GAME);
					sm.addString(oui1.getName());
					_arena.broadcastPacket(sm);

					sm = new SystemMessage(SystemMessage.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
					sm.addString(oui1.getName());
					sm.addNumber(winnerPoint);
					sendPacketToPlayers(sm);

					sm = new SystemMessage(SystemMessage.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
					sm.addString(oui2.getName());
					sm.addNumber(winnerPoint);
					sendPacketToPlayers(sm);

					if(_gameType == 2 && Config.ALT_OLY_REWARD_TOKENS_CB > 0)
						player1.addItem("Olympiad", Olympiad.OLYMPIAD_TOKENS_ID, Config.ALT_OLY_REWARD_TOKENS_CB, player2, true);
					else if(_gameType == 1 && Config.ALT_OLY_REWARD_TOKENS_NCB > 0)
						player1.addItem("Olympiad", Olympiad.OLYMPIAD_TOKENS_ID, Config.ALT_OLY_REWARD_TOKENS_NCB, player2, true);

					sendPacketToPlayers(new ExReceiveOlympiad(this));
				}
				else
				{
					_winnerName = oui2.getName();
					_winnerTeam = _teams[1];
					oui2.setMatchPoint(winnerPoint);
					oui2.setPoints(oui2.getPoints() + winnerPoint);
					oui1.setMatchPoint(-winnerPoint);
					oui1.setPoints(oui1.getPoints() - winnerPoint);
					Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame Player2 " + oui2.getName() + " win Player " + oui1.getName() + " loose");
					Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame Player2 " + oui2.getName() + " win " + oui2.getPoints() + "(" + oui2.getMatchPoints() +") points Player " + oui1.getName() + " loose " + oui1.getPoints() + "(" + oui1.getMatchPoints() + ") points");
					Olympiad._nobles.get(oui2.getObjectId()).set("wins", Olympiad._nobles.get(oui2.getObjectId()).getInteger("wins") + 1);
					Olympiad._nobles.get(oui1.getObjectId()).set("loos", Olympiad._nobles.get(oui1.getObjectId()).getInteger("loos") + 1);

					Olympiad.updateNobleData(oui1.getObjectId());
					Olympiad.updateNobleData(oui2.getObjectId());
					Hero.updateMatchHistory(_teams[1], _teams[0], (int) fightTime, 1);
					Hero.updateMatchHistory(_teams[0], _teams[1], (int) fightTime, 2);

					sm = new SystemMessage(SystemMessage.S1_HAS_WON_THE_GAME);
					sm.addString(oui2.getName());
					_arena.broadcastPacket(sm);

					sm = new SystemMessage(SystemMessage.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
					sm.addString(oui2.getName());
					sm.addNumber(winnerPoint);
					sendPacketToPlayers(sm);

					sm = new SystemMessage(SystemMessage.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
					sm.addString(oui1.getName());
					sm.addNumber(winnerPoint);
					sendPacketToPlayers(sm);

					if(_gameType == 2 && Config.ALT_OLY_REWARD_TOKENS_CB > 0)
						player2.addItem("Olympiad", Olympiad.OLYMPIAD_TOKENS_ID, Config.ALT_OLY_REWARD_TOKENS_CB, player1, true);
					else if(_gameType == 1 && Config.ALT_OLY_REWARD_TOKENS_NCB > 0)
						player2.addItem("Olympiad", Olympiad.OLYMPIAD_TOKENS_ID, Config.ALT_OLY_REWARD_TOKENS_NCB, player1, true);

					sendPacketToPlayers(new ExReceiveOlympiad(this));
				}
			}
		}
		else
		{
			if(_team1Disconnected)
				winner = 2;
			else if(_team2Disconnected)
				winner = 1;

			if(winner == -1)
			{
				Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame team " + _teams[0].getName() + " vs team " + _teams[1].getName());
				if(!_teams[0].isDead() && !_teams[1].isDead())
				{
					int dmg1 = 0, dmg2 = 0;
					for(OlympiadUserInfo oui : _teams[0].getPlayersInfo())
						dmg1 += oui.getDamage();
					for(OlympiadUserInfo oui : _teams[1].getPlayersInfo())
						dmg2 += oui.getDamage();

					Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame team " + _teams[0].getName() + "(" + dmg1 + ") vs team " + _teams[1].getName() + "(" + dmg2 + ") both live.");
					if(dmg1 == dmg2)
						winner = 0;
					else if(dmg1 < dmg2)
						winner = 1;
					else
						winner = 2;
				}
				else if(_teams[0].isDead())
					winner = 2;
				else
					winner = 1;
			}

			SystemMessage sm;
			if(winner == 0)
			{
				Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame with a TIE team " + _teams[0].getName() + " vs team " + _teams[1].getName());
				sm = new SystemMessage(SystemMessage.THE_GAME_ENDED_IN_A_TIE);
				_arena.broadcastPacket(sm);
				Hero.updateMatchHistory(_teams[0], _teams[1], (int) fightTime, 3);
				Hero.updateMatchHistory(_teams[1], _teams[0], (int) fightTime, 3);
				sendPacketToPlayers(new ExReceiveOlympiad(this));
			}
			else
			{
				int points = 0;
				OlympiadTeam team1, team2;

				if(winner == 1)
				{
					team1 = _teams[0];
					team2 = _teams[1];
				}
				else
				{
					team1 = _teams[1];
					team2 = _teams[0];
				}

				for(OlympiadUserInfo oui : team2.getPlayersInfo())
					points += Math.min(10, oui.getPoints() / 5 + 1);

				Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame team " + team1.getName() + " win points: " + points + " team " + team2.getName() + " loose points: " + points);
				_winnerName = team1.getName();
				_winnerTeam = team1;

				sm = new SystemMessage(SystemMessage.S1_HAS_WON_THE_GAME);
				sm.addString(team1.getName());
				_arena.broadcastPacket(sm);

				int p = points / 3;
				int m = points % 3;
				for(OlympiadUserInfo oui : team1.getPlayersInfo())
				{
					L2Player player = oui.getPlayer();
					if(oui.getObjectId() == team1.getLeaderObjectId())
					{
						oui.setMatchPoint(p + m);
						oui.setPoints(oui.getPoints() + p + m);
						sm = new SystemMessage(SystemMessage.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
						sm.addString(oui.getName());
						sm.addNumber(p + m);
						sendPacketToPlayers(sm);
						if(player != null && Config.ALT_OLY_REWARD_TOKENS_3x3 > 0)
							player.addItem("Olympiad", Olympiad.OLYMPIAD_TOKENS_ID, Config.ALT_OLY_REWARD_TOKENS_3x3, player, true);
						Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame team " + oui.getName() + " win points: " + oui.getPoints() + "(" + oui.getMatchPoints() + ")");
					}
					else
					{
						oui.setMatchPoint(p);
						oui.setPoints(oui.getPoints() + p);
						sm = new SystemMessage(SystemMessage.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
						sm.addString(oui.getName());
						sm.addNumber(p);
						sendPacketToPlayers(sm);
						if(player != null && Config.ALT_OLY_REWARD_TOKENS_3x3 > 0)
							player.addItem("Olympiad", Olympiad.OLYMPIAD_TOKENS_ID, Config.ALT_OLY_REWARD_TOKENS_3x3, player, true);
						Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame team " + oui.getName() + " win points: " + oui.getPoints() + "(" + oui.getMatchPoints() + ")");
					}
					Olympiad._nobles.get(oui.getObjectId()).set("wins", Olympiad._nobles.get(oui.getObjectId()).getInteger("wins") + 1);
					Olympiad.updateNobleData(oui.getObjectId());
				}

				for(OlympiadUserInfo oui : team2.getPlayersInfo())
				{
					p = Math.min(10, oui.getPoints() / 5 + 1);
					oui.setMatchPoint(-p);
					oui.setPoints(oui.getPoints() - p);
					sm = new SystemMessage(SystemMessage.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
					sm.addString(oui.getName());
					sm.addNumber(p);
					sendPacketToPlayers(sm);
					Olympiad._olyLog.info("OG(" + _arenaId + ") EndGame team " + oui.getName() + " loose points: " + oui.getPoints() + "(" + oui.getMatchPoints() + ")");
					Olympiad._nobles.get(oui.getObjectId()).set("loos", Olympiad._nobles.get(oui.getObjectId()).getInteger("loos") + 1);
					Olympiad.updateNobleData(oui.getObjectId());
				}

				Hero.updateMatchHistory(team1, team2, (int) fightTime, 1);
				Hero.updateMatchHistory(team2, team1, (int) fightTime, 2);
				team1.revive();
				team2.revive();
				sendPacketToPlayers(new ExReceiveOlympiad(this));
			}
		}

		_arena.broadcastPacket(new ExOlympiadMatchEnd());

		for(OlympiadTeam team : _teams)
			for(OlympiadUserInfo oui : team.getPlayersInfo())
			{
				L2Player player = oui.getPlayer();
				if(player != null)
				{
					player.stopAllEffects();
					for(QuestState qs : player.getAllActiveQuests())
						qs.getQuest().onOlympiadEnd(this, qs);
				}
			}

		new TeleportBack().startTimer();

		if(_scheduledPrepareTask != null)
			_scheduledPrepareTask.cancel(true);

		if(_scheduledFightTask != null)
			_scheduledFightTask.cancel(true);
	}

	private class TeleportBack extends CountdownTimer
	{
		public TeleportBack()
		{
			super("20;10;5;4;3;2;1;0", 0);
		}

		public void onCheckpoint(long sec) throws Throwable
		{
			sendPacketToPlayers(new SystemMessage(SystemMessage.YOU_WILL_GO_BACK_TO_THE_VILLAGE_IN_S1_SECOND_S).addNumber(sec));
		}

		public void onFinish() throws Throwable
		{
			teleportPlayersBack();
		}
	}

	public void teleportPlayersBack()
	{
		for(OlympiadTeam team : _teams)
			for(OlympiadUserInfo oui : team.getPlayersInfo())
			{
				L2Player player = oui.getPlayer();
				try
				{
					if(player == null)
						continue;

					prepareTeleportBack(player);
					if(player.getStablePoint() == null)
						player.teleToClosestTown();
					else
					{
						player.teleToLocation(player.getStablePoint(), 0);
						player.setStablePoint(null);
					}
				}
				catch(Exception e)
				{
					Olympiad._olyLog.warn("OG(" + _arenaId + ") teleportBack aborted! Can't teleport players " + e);
					e.printStackTrace();
				}
			}

		abortGame();
	}

	public static void prepareTeleportBack(L2Player player)
	{
		player.sendPacket(new ExOlympiadMode(0));

		player.setOlympiadSide(-1);
		player.setOlympiadGameId(-1);
		player.setTarget(null);
		player.setIsInOlympiadMode(false);
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());

		//Add Clan Skills
		if(player.getClanId() != 0)
		{
			for(L2Skill skill : player.getClan().getAllSkills())
				if(skill.getMinPledgeClass() <= player.getPledgeRank())
					player.addSkill(skill, false);

			if(player.getClan().getHasCastle() > 0)
				TerritoryWarManager.getTerritoryById(player.getClan().getHasCastle() + 80).giveSkills(player);
		}

		//Add Hero Skills
		if(player.isHero())
		{
			player.addSkill(SkillTable.getInstance().getInfo(395, 1));
			player.addSkill(SkillTable.getInstance().getInfo(396, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1374, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1375, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1376, 1));
		}
		player.sendPacket(new SkillList(player));
		player.setTeam(0);

		if(Config.ALT_OLY_ENABLE_HWID_CHECK)
			Olympiad.removeHWID(player.getLastHWID());
	}

	protected void handleDisconnect(L2Player player)
	{
		Olympiad._olyLog.warn("OG(" + _arenaId + ") handle disconnected: " + player);
		if(getGameState() == OlympiadGameState.FIGHT || getGameState() == OlympiadGameState.PREPARE)
		{
			if(_teams[0].contains(player.getObjectId()))
			{
				if(!_teams[0].isOnline())
				{
					_team1Disconnected = true;
					Olympiad._olyLog.warn("OG(" + _arenaId + ") player 1 " + player.getName() + " disconnected");
					endGame();
				}
				else if(getGameType() == 0 && _teams[0].isDead())
					endGame();
			}
			else if(_teams[1].contains(player.getObjectId()))
			{
				if(!_teams[1].isOnline())
				{
					_team2Disconnected = true;
					Olympiad._olyLog.warn("OG(" + _arenaId + ") player 2 " + player.getName() + " disconnected");
					endGame();
				}
				else if(getGameType() == 0 && _teams[1].isDead())
					endGame();
			}
		}
		else if(getGameState() == OlympiadGameState.INITIAL)
		{
			if(_teams[0].contains(player.getObjectId()))
			{
				_team1Disconnected = true;
				_teams[1].sendPacket(new SystemMessage(SystemMessage.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS));

				Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted by " + player.getName() + " vs " + _teams[1].getName());
				for(OlympiadUserInfo oui : _teams[0].getPlayersInfo())
				{
					int penaltyPoint = Math.min(10, oui.getPoints() / 5 + 1);
					oui.setMatchPoint(-penaltyPoint);
					oui.setPoints(oui.getPoints() - penaltyPoint);
					Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted " + oui.getName() + " penalty: " + penaltyPoint + " current: " + oui.getPoints());
				}
				Olympiad._olyLog.info("OG(" + _arenaId + ") TeleportTask aborted playerOne null or disconnected");
				abortGame();
			}
			else if(_teams[1].contains(player.getObjectId()))
			{
				_team2Disconnected = true;
				_teams[0].sendPacket(new SystemMessage(SystemMessage.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS));

				Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted by " + player.getName() + " vs " + _teams[1].getName());
				for(OlympiadUserInfo oui : _teams[1].getPlayersInfo())
				{
					int penaltyPoint = Math.min(10, oui.getPoints() / 5 + 1);
					oui.setMatchPoint(-penaltyPoint);
					oui.setPoints(oui.getPoints() - penaltyPoint);
					Olympiad._olyLog.warn("OG(" + _arenaId + ") Teleport aborted " + oui.getName() + " penalty: " + penaltyPoint + " current: " + oui.getPoints());
				}
				Olympiad._olyLog.info("OG(" + _arenaId + ") TeleportTask aborted playerTwo null or disconnected");
				abortGame();
			}
		}
	}

	public void sendPacketToPlayers(L2GameServerPacket sp)
	{
		//if(sp instanceof ExOlympiadUserInfo)
		//{
		//	if(((ExOlympiadUserInfo) sp).getSide() == 1)
		//		_teams[1].sendPacket(sp);
		//	else if(((ExOlympiadUserInfo) sp).getSide() == 2)
		//		_teams[0].sendPacket(sp);
		//}
		//else
		//{
			_teams[0].sendPacket(sp);
			_teams[1].sendPacket(sp);
		//}
	}

	public void setGameState(OlympiadGameState state)
	{
		_gameState = state;
	}

	public OlympiadGameState getGameState()
	{
		return _gameState;
	}

	protected void abortGame()
	{
		Olympiad._olyLog.warn("OG(" + _arenaId + ") Abort Game");
		if(_scheduledTeleportTask != null)
			_scheduledTeleportTask.cancel(true);

		for(OlympiadTeam team : _teams)
			for(OlympiadUserInfo oui : team.getPlayersInfo())
			{
				if(Config.ALT_OLY_ENABLE_HWID_CHECK)
					Olympiad.removeHWID(oui.getHWID());

				L2Player player = oui.getPlayer();
				if(player != null)
					player.setOlympiadGameId(-1);
			}

		deleteBuffers();
		_arena.notifyEvent("close_door", null, null);
		_arena.setOlympiadGame(null);
		if(!Olympiad.isInCompPeriod())
		{
			_arena.stopInstance();
			Olympiad._instances[_arena.getArenaId()] = null;
		}
		_fightTask = null;
	}

	protected void deleteBuffers()
	{
		_arena.notifyEvent("despawn_manager", null, null);
	}

	public int getAreanId()
	{
		return _arenaId;
	}

	public OlympiadTeam getTeam(int side)
	{
		return _teams[side];
	}

	public void broadcastPlayersState()
	{
		if(getGameState() != OlympiadGameState.FIGHT)
			return;

		for(int i = 0; i < _teams.length; i++)
			for(OlympiadUserInfo oui : _teams[i].getPlayersInfo())
			{
				L2Player player = oui.getPlayer();
				if(player != null)
				{
					player.broadcastPacket(new ExOlympiadUserInfo(player, i + 1));
					player.updateEffectIcons();
					if(player.getPet() != null)
						player.getPet().broadcastPetInfo();
				}
			}
	}

	public String getWinnerName()
	{
		return _winnerName;
	}

	public OlympiadTeam getWinnerTeam()
	{
		return _winnerTeam;
	}
}
