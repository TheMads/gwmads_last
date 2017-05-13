package ru.l2gw.gameserver.clientpackets;

/**
 * @author rage
 * @date 17.12.10 0:15
 */
public class RequestExCubeGameChangeTeam extends L2GameClientPacket
{
	private int _arena;
	private int _team;
	
	@Override
	protected void readImpl()
	{
		// client sends -1,0,1,2 for arena parameter
		_arena = readD() + 1;
		_team = readD();
	}
	
	@Override
	public void runImpl()
	{
		// TODO
		// do not remove players after start
		/*
		if (HandysBlockCheckerManager.getInstance().arenaIsBeingUsed(_arena))
			return;
		L2Player player = getClient().getActiveChar();

		switch (_team)
		{
			case 0:
			case 1:
				// Change Player Team
				HandysBlockCheckerManager.getInstance().changePlayerToTeam(player, _arena, _team);
				break;
			case -1:
				// Remove Player (me)
			{
				int team = HandysBlockCheckerManager.getInstance().getHolder(_arena).getPlayerTeam(player);
				// client sends two times this packet if click on exit
				// client did not send this packet on restart
				if (team > -1)
					HandysBlockCheckerManager.getInstance().removePlayer(player, _arena, team);
				break;
			}
			default:
				_log.warn("Wrong Cube Game Team ID: "+_team);
				break;
		}
		*/
	}
}
