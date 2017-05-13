package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 17.12.10 0:24
 */
public class RequestExCubeGameReadyAnswer extends L2GameClientPacket
{
	int _arena;
	int _answer;
	
	@Override
	protected void readImpl()
	{
		// client sends -1,0,1,2 for arena parameter
		_arena = readD() + 1;
		// client sends 1 if clicked confirm on not clicked, 0 if clicked cancel
		_answer = readD();
	}
	
	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		
		if(player == null)
			return;

		// TODO
		switch (_answer)
		{
			case 0:
				// Cancel - Answer No
				break;
			case 1:
				// OK or Time Over
				//HandysBlockCheckerManager.getInstance().increaseArenaVotes(_arena);
				break;
			default:
				_log.warn("Unknown Cube Game Answer ID: "+_answer);
				break;
		}
	}
}
