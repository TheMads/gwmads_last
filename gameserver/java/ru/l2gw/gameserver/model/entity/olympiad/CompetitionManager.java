package ru.l2gw.gameserver.model.entity.olympiad;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.tables.CharTemplateTable;

import java.util.Arrays;

/**
 * *********************************************************************************************************
 * Competition Manager Class
 * **********************************************************************************************************
 */
class CompetitionManager implements Runnable
{
	private static final GArray<OlympiadTeam[]> _emptyPlayers = new GArray<OlympiadTeam[]>(0);
	private static final GArray<Integer> _teamKeys = new GArray<Integer>();
	public void run()
	{
		Olympiad._olyLog.info("CompetitionManager: Started");

		if(Olympiad._isOlympiadEnd)
		{
			Olympiad._olyLog.info("CompetitionManager: Olympiad end");
			Olympiad._scheduledManagerTask.cancel(true);
			return;
		}
		if(!Olympiad._inCompPeriod)
			return;

		if(Olympiad.isMinRegistered())
		{
			Olympiad._olyLog.info("CompetitionManager: no min registered");
			return;
		}
		if(Olympiad._nobles.size() == 0)
		{
			Olympiad._olyLog.info("CompetitionManager: no nobleses");
			return;
		}

		GArray<OlympiadTeam[]> participants = getNonClassParticipants();
		Olympiad._olyLog.info("CompetitionManager: non class participants: " + participants.size());
		for(OlympiadTeam[] ot : participants)
		{
			OlympiadInstance arena = Olympiad.getFreeArena();
			if(arena == null)
				break;

			Olympiad._olyLog.info("CompetitionManager: create non class game " + arena.getArenaId() + " " + ot[0].getName() + " vs " + ot[1].getName());
			arena.setOlympiadGame(new OlympiadGame(ot[0], ot[1], arena, 1));
		}

		for(Integer classId : Olympiad._classBasedRegisters.keySet())
		{
			GCSArray<Integer> classed = Olympiad.getNoblesInClassRegistered(classId);
			if(classed == null || classed.size() < Config.ALT_OLY_MIN_NOBLE_CB)
				continue;

			Olympiad._olyLog.info("CompetitionManager: class " + CharTemplateTable.getClassNameById(classId) + " participants: " + classed.size());
			if(Olympiad.getFreeArenaCount() == 0)
				break;

			participants = getParticipantsFromList(classed, 2);

			for(OlympiadTeam[] ot : participants)
			{
				OlympiadInstance arena = Olympiad.getFreeArena();
				if(arena == null)
					break;

				Olympiad._olyLog.info("CompetitionManager: create class game " + arena.getArenaId() + " " + ot[0].getName() + " vs " + ot[1].getName());
				arena.setOlympiadGame(new OlympiadGame(ot[0], ot[1], arena, 2));
			}
		}

		if(Olympiad._teamBaseRegistered.size() >= Config.ALT_OLY_MIN_NOBLE_3x3)
		{
			_teamKeys.addAll(Olympiad._teamBaseRegistered.keySet());
			Olympiad._olyLog.info("CompetitionManager: start team games: " + Olympiad._teamBaseRegistered.size());
			while(_teamKeys.size() >= 2)
			{
				OlympiadInstance arena = Olympiad.getFreeArena();
				Olympiad._olyLog.info("CompetitionManager: start team games arena: " + arena);
				if(arena == null)
					break;

				OlympiadTeam ot1 = Olympiad._teamBaseRegistered.remove(_teamKeys.remove(Rnd.get(_teamKeys.size())));
				OlympiadTeam ot2 = Olympiad._teamBaseRegistered.remove(_teamKeys.remove(Rnd.get(_teamKeys.size())));

				Olympiad._olyLog.info("CompetitionManager: team game " + arena.getArenaId() + " " + ot1.getName() + " vs " + ot2.getName());
				arena.setOlympiadGame(new OlympiadGame(ot1, ot2, arena, 0));
			}
			_teamKeys.clear();
		}
	}

	private GArray<OlympiadTeam[]> getParticipantsFromList(GCSArray<Integer> list, int gameType)
	{
		int freeArenaCount = Olympiad.getFreeArenaCount();
		if(freeArenaCount < 1)
			return _emptyPlayers;

		Integer[] array;
		synchronized(list)
		{
			array = new Integer[list.size()];
			list.toArray(array);
		}
		Arrays.sort(array, NoblesComparator.getInstance());
		GArray<OlympiadTeam[]> result = new GArray<OlympiadTeam[]>(array.length / 2);
		OlympiadTeam[] team = new OlympiadTeam[2];
		int t = 0;
		for(Integer objectId : array)
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			if(player != null)
			{
				team[t] = new OlympiadTeam(gameType);
				team[t].addPlayer(player);
				team[t].setLeader(player);
				t++;
			}

			if(t == 2)
			{
				t = 0;
				result.add(team);
				list.remove((Integer) team[0].getLeaderObjectId());
				list.remove((Integer) team[1].getLeaderObjectId());
				team = new OlympiadTeam[2];
			}

			if(result.size() / 2 == freeArenaCount)
				return result;
		}
		return result;
	}

	private GArray<OlympiadTeam[]> getNonClassParticipants()
	{
		GCSArray<Integer> nonClassed = Olympiad.getNoblesInNonClassRegistered();
		if(nonClassed.size() >= Config.ALT_OLY_MIN_NOBLE_NCB)
			return getParticipantsFromList(nonClassed, 1);

		return _emptyPlayers;
	}
}
