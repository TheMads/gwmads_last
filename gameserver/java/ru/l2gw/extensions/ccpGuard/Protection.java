package ru.l2gw.extensions.ccpGuard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.ccpGuard.managers.HwidBan;
import ru.l2gw.extensions.ccpGuard.managers.HwidManager;
import ru.l2gw.extensions.ccpGuard.managers.ProtectManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.handler.AdminCommandHandler;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.gspackets.ChangeAccessLevel;
import ru.l2gw.gameserver.loginservercon.gspackets.PlayerLogout;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.util.Util;

import java.nio.ByteBuffer;

public final class Protection
{
	private static final Log _log = LogFactory.getLog("protect");
	private static final Log _logHWID = LogFactory.getLog("hwid");
	private static String[] _positionName = {
			"Ingame bot",
			"Cheat client console CMD: <Enabled>",
			"Cheat client hooked connect (maybe l2phx)",
			"Cheat client hooked send (maybe l2phx)",
			"Cheat client hooked recv (maybe l2phx)",
			"Cheat client use L2Control", "Cheat client use L2ext",
			"Cheat client VEH detect",
			"Cheat client use L2ext hook AddPkt",
			"Unknow Soft"};

	public static boolean protect_use = false;
	private static final int GGOPCODE = 0x77033077;

	public static void Init()
	{
		ConfigProtect.load();
		protect_use = ConfigProtect.PROTECT_ENABLE;
		if(protect_use)
		{
			_log.info("******************[ Protection System: Loading ]*******************");
			HwidBan.getInstance();
			HwidManager.getInstance();
			ProtectManager.getInstance();
			AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminHWID());
			_log.info("Unprotected IPs: " + ConfigProtect.PROTECT_UNPROTECTED_IPS.NetsCount());
			_log.info("******************[ Protection System: Finish ]********************");
		}
	}

	public static boolean checkPlayerWithHWID(GameClient client, int playerID, String playerName)
	{
		if(!protect_use || !client._prot_info.protect_used)
			return true;

		ProtectInfo pi = client._prot_info;
		pi.setPlayerName(playerName);
		pi.setPlayerId(playerID);

		if(ConfigProtect.PROTECT_ENABLE_HWID_LOCK)
		{
			if(HwidManager.checkLockedHWID(pi))
			{
				_log.info("An attempt to log in to locked character, " + pi);
				client.close(Msg.ServerClose);
				return false;
			}
		}

		if(ConfigProtect.PROTECT_WINDOWS_COUNT != 0)
		{
			final int count = ProtectManager.getInstance().getCountByHWID(pi.getHWID());
			if(count > ConfigProtect.PROTECT_WINDOWS_COUNT && count > HwidManager.getAllowedWindowsCount(pi))
			{
				_log.info("Multi windows: " + pi);
				client.close(Msg.ServerClose);
				return false;
			}
		}
		addPlayer(pi);
		return true;
	}

	public static int calcPenalty(byte[] data, ProtectInfo pi)
	{
		int sum = -1;
		if(Util.verifyChecksum(data, 0, data.length))
		{
			ByteBuffer buf = ByteBuffer.wrap(data, 0, data.length - 4);
			int lenPenalty = (data.length - 4) / 4;
			sum = 0;
			int[] dump = new int[lenPenalty - 9];
			int idx = 0;
			for(int i = 0; i < lenPenalty; i++)
			{
				int tmp = buf.getInt();
				if(i < 9)
					sum += Protection.dumpData(tmp, i, pi);
				else
					dump[idx++] = tmp;
			}
			sum += Protection.dumpData2(dump, pi);
		}
		return sum;
	}

	public static int dumpData2(int[] _ids, ProtectInfo pi)
	{
		int value = 0;

		for(int i = 0; i < _ids.length - 1; ++i)
			if(_ids[i] == 0)
				_log.info(pi + ": Cannot read DumpId. Target pos: " + i);

		if(_ids[0] != _ids[2] || _ids[1] != _ids[2])
		{
			_log.info(pi + ": L2ext or other soft was found. Debug: " + _ids[0] + " " + _ids[1] + " " + _ids[2]);
			value = ConfigProtect.PROTECT_PENALTY_BOT;
		}
		if(_ids[3] != _ids[5] || _ids[4] != _ids[5])
		{
			_log.info(pi + ": Sniffer soft was found. Debug: " + _ids[3] + " " + _ids[4] + " " + _ids[5]);
			value = ConfigProtect.PROTECT_PENALTY_BOT;
		}
		if(_ids[6] == 0xCC)
		{
			_log.info(pi + ": Sniffer soft was found. Debug: " + _ids[6]);
			value = ConfigProtect.PROTECT_PENALTY_BOT;
		}

		if(!checkVerifyFlag(pi, _ids[7]))
			value = ConfigProtect.PROTECT_PENALTY_BOT;

		return value;
	}

	public static int dumpData(int _id, int position, ProtectInfo pi)
	{
		int value = 0;
		position = position > 8 ? 9 : position;
		boolean isIdZero = false;
		if(_id == 0)
		{
			isIdZero = true;
			_log.info(pi + ": Cannot read dumpId target: " + _positionName[position] + " DEBUG INFO:" + _id);
		}
		switch(position)
		{
			case 0:
				//IG
				if(_id != 1435233386)
				{
					if(!isIdZero)
						_log.info(pi + ": " + _positionName[position] + " was found. DEBUG INFO: " + _id);
					value = ConfigProtect.PROTECT_PENALTY_IG;
				}
				break;
			case 1:
				//Console CMD
				if(_id != 16)
				{
					if(!isIdZero)
						_log.info(pi + ": " + _positionName[position] + " was found. DEBUG INFO: " + _id);
					value = ConfigProtect.PROTECT_PENALTY_CONSOLE_CMD;
				}
				break;
			case 2:
			case 3:
			case 4:
				//check debuger (0xСС) or hook (0xE9)
				int code = _id >> 24 & 0xFF;
				if(code == 0xCC)
					_log.info(pi + ": Attempts!!! Debuger was found. DEBUG INFO: " + _id);
				//L2phx (connect, send, recv)
				//if(code == 0xE9)
				//{
					//_log.info(pi + ": " + _positionName[position] + " was found. DEBUG INFO: " + _id);
					//value = ConfigProtect.PROTECT_PENALTY_L2PHX;
				//}
				break;
			case 5: //*AddNetwork;
				//if(_id != 1398167435)
				if(_id != 1435233386)
				{
					if(!isIdZero)
						_log.info(pi + ": " + _positionName[position] + " was found. DEBUG INFO: " + _id);

					value = ConfigProtect.PROTECT_PENALTY_L2CONTROL;
				}
				break;
			case 6:  //hook WndProc
				if(_id != 0x8B493CE9)
				{
					if(!isIdZero)
						_log.info(pi + ": " + _positionName[position] + " was found. DEBUG INFO: " + _id);

					value = ConfigProtect.PROTECT_PENALTY_BOT;
				}
				break;
			/*
			case 7: //VEH
				if(_id != 40405845)
				{
					if(!isIdZero)
						_log.info(pi + ": " + _positionName[position] + " was found. DEBUG INFO: " + _id);

					value = ConfigProtect.PROTECT_PENALTY_BOT;
				}
				break;
			*/
			case 8: //
				if(_id != 5622903)
				{
					if(!isIdZero)
						_log.info(pi + ": " + _positionName[position] + " was found. DEBUG INFO: " + _id);

					value = ConfigProtect.PROTECT_PENALTY_BOT;
				}
				break;
			default:
				value = 0;
				break;
		}
		return value;
	}

	public static String ExtractHWID(byte[] _data)
	{
		if(Util.verifyChecksum(_data, 0, _data.length))
		{
			StringBuilder resultHWID = new StringBuilder();
			for(int i = 0; i < _data.length - 4; i++)
				resultHWID.append(Util.fillHex(_data[i] & 0xff, 2));
			return resultHWID.toString();
		}
		else
			return "";
	}

	public static boolean checkHWIDs(ProtectInfo pi, int LastError1)
	{
		boolean resultHWID = false;
		boolean resultLastError = false;
		String HWID = pi.getHWID();
		//String HWIDSec = pi.getHWIDSec();

		if(HWID.equalsIgnoreCase("fab800b1cc9de379c8046519fa841e6"))
		{
			_logHWID.info(pi + " HWID: " + HWID + " is empty.");
			if(ConfigProtect.PROTECT_KICK_WITH_EMPTY_HWID)
				resultHWID = true;
		}
		if(LastError1 != 0)
		{
			_logHWID.info(pi + " LastError(HWID): " + LastError1 + " " + Util.LastErrorConvertion(LastError1) + " isn't empty.");
			if(ConfigProtect.PROTECT_KICK_WITH_LASTERROR_HWID)
				resultLastError = true;
		}
		return resultHWID || resultLastError;
	}

	public static boolean BF_Init(int[] P, int[] S0, int[] S1, int[] S2, int[] S3)
	{
		return false;
	}

	public static void addPlayer(ProtectInfo pi)
	{
		if(protect_use && pi != null)
		{
			ProtectManager.getInstance().addPlayer(pi);
			pi.startOnlinerTask();
		}
	}

	public static void removePlayer(ProtectInfo pi)
	{
		if(protect_use && pi != null)
		{
			ProtectManager.getInstance().removePlayer(pi.getPlayerName());
			pi.stopOnlinerTask(true);
		}
	}

	public static void doReadAuthLogin(GameClient client, ByteBuffer buf, byte[] data)
	{
		if(!protect_use || !client._prot_info.protect_used)
			return;

		int size = buf.remaining() - data.length;
		if(size < 0)
		{
			_log.info(client._prot_info + ": filed read AuthLogin! May be BOT or unprotected client! Disconnected.");
			LSConnection.getInstance().sendPacket(new PlayerLogout(client.getLoginName()));
			LSConnection.getInstance().removeAccount(client);
			client.close(Msg.ServerClose);
			return;
		}
		if(size > 0)
			buf.position(buf.position() + size);

		buf.get(data);
		ProtectInfo pi = client._prot_info;
		if(pi != null)
			pi.GGdec.decrypt(data, 0, data, 0, data.length);
	}

	public static boolean doAuthLogin(GameClient client, byte[] data, String loginName)
	{
		if(!protect_use || !client._prot_info.protect_used)
			return true;

		ProtectInfo pi = client._prot_info;
		pi.setLoginName(loginName);

		String fullHWID = Protection.ExtractHWID(data);
		if(fullHWID.equals(""))
		{
			_log.info(pi + ": AuthLogin CRC Check Fail! May be BOT or unprotected client! Disconnected.");
			LSConnection.getInstance().sendPacket(new PlayerLogout(client.getLoginName()));
			LSConnection.getInstance().removeAccount(client);
			client.close(Msg.ServerClose);
			return false;
		}

		pi.setHWID(fullHWID.substring(0, 31));
		//pi.setHWIDSec(fullHWID.substring(32 + 4 * 2, 32 + 4 * 2 + 32 - 1));

		if(ConfigProtect.PROTECT_GS_LOG_HWID)
			_logHWID.info(pi + ": login.");

		int LastError1 = ByteBuffer.wrap(data, 16, 4).getInt();
		// Lang 01 - eng, 08 - rus
		int langId = Integer.reverseBytes(ByteBuffer.wrap(data, 16 + 4 + 16, 4).getInt());

		if(langId == 0)
		{
			_log.info(pi + ": old client version.");
			LSConnection.getInstance().sendPacket(new PlayerLogout(client.getLoginName()));
			LSConnection.getInstance().removeAccount(client);
			client.close(Msg.ServerClose);
			return false;
		}

		if(Protection.checkHWIDs(pi, LastError1))
		{
			_logHWID.info(pi + ": HWID error. Disconnected.");
			LSConnection.getInstance().sendPacket(new PlayerLogout(client.getLoginName()));
			LSConnection.getInstance().removeAccount(client);
			client.close(Msg.ServerClose);
			return false;
		}

		if(HwidBan.checkFullHWIDBanned(pi))
		{
			_logHWID.info(pi + ": Rejected banned HWID. Disconnected.");
			LSConnection.getInstance().sendPacket(new PlayerLogout(client.getLoginName()));
			LSConnection.getInstance().removeAccount(client);
			client.close(Msg.ServerClose);
			return false;
		}

		int verifyFlag = ByteBuffer.wrap(data, 40, 4).getInt();

		if(!checkVerifyFlag(pi, verifyFlag))
		{
			LSConnection.getInstance().sendPacket(new PlayerLogout(client.getLoginName()));
			LSConnection.getInstance().removeAccount(client);
			client.close(Msg.ServerClose);
			return false;
		}

		if(ConfigProtect.PROTECT_ENABLE_HWID_LOCK && HwidManager.checkLockedHWID(pi))
		{
			_logHWID.info(pi + ": An attempt to log in to locked account. Disconnected.");
			LSConnection.getInstance().sendPacket(new PlayerLogout(client.getLoginName()));
			LSConnection.getInstance().removeAccount(client);
			client.close(Msg.ServerClose);
			return false;
		}
		return true;
	}

	public static boolean doReadReplyGameGuard(GameClient client, ByteBuffer buf, byte[] data)
	{
		if(!protect_use)
			return false;

		ProtectInfo pi = client._prot_info;

		pi.GGReaded = false;

		int op = buf.getInt();

		if(buf.remaining() < data.length + 2 || op != GGOPCODE)
		{
			_log.info(client._prot_info + ": Filed read ReplyGameGuardQuery! Error GG packet opcode = " + op + " size: " + buf.remaining());
			client.close(Msg.ServerClose);
			return false;
		}

		buf.getShort();
		buf.get(data);
		pi.GGdec.decrypt(data, 0, data, 0, data.length);
		pi.GGReaded = true;
		return true;
	}

	public static void doReplyGameGuard(GameClient client, byte[] data)
	{
		if(!protect_use || !ConfigProtect.PROTECT_ENABLE_GG_SYSTEM || !client._prot_info.protect_used || !client._prot_info.GGReaded)
			return;

		ProtectInfo pi = client._prot_info;

		int penalty = calcPenalty(data, pi);
		if(penalty == -1)
		{
			_log.info(pi + ": Checksumm (ReplyGameGuardQuery) is wrong. Disconnected.");
			client.close(Msg.ServerClose);
			return;
		}

		pi.addProtectPenalty(penalty);

		if(pi.getProtectPenalty() >= ConfigProtect.PROTECT_TOTAL_PENALTY)
		{
			L2Player player = client.getPlayer();
			if(player == null)
			{
				_log.info(pi + ": Cheater was found. Disconnected.");
				client.close(Msg.ServerClose);
				return;
			}

			if(ConfigProtect.PROTECT_PUNISHMENT_ILLEGAL_SOFT == 0)
			{
				_log.info(pi + ": Cheater was found. Ignored.");
				pi.setProtectPenalty(0);
			}
			else
			{
				if(ConfigProtect.PROTECT_PUNISHMENT_ILLEGAL_SOFT == 1)
					_log.info(pi + ": Cheater was found. Disconnected.");
				else if(ConfigProtect.PROTECT_PUNISHMENT_ILLEGAL_SOFT == 2)
				{
					LSConnection.getInstance().sendPacket(new ChangeAccessLevel(client.getLoginName(), -300, "Protect Autoban", -1));
					player.setAccessLevel(-300);
					_log.info(pi + ": Cheater was found. Disconnected, banned.");
				}
				else if(ConfigProtect.PROTECT_PUNISHMENT_ILLEGAL_SOFT == 3)
				{
					HwidBan.addHwidBan(client, "Cheater was found [autoban]");
					LSConnection.getInstance().sendPacket(new ChangeAccessLevel(client.getLoginName(), -300, "Protect Autoban", -1));
					player.setAccessLevel(-300);
					_log.info(pi + ": Cheater was found. Disconnected, hwid banned.");
				}
				if(!ConfigProtect.PROTECT_HTML_SHOW.equals("none"))
				{
					String filename = "data/html/" + ConfigProtect.PROTECT_HTML_SHOW;
					NpcHtmlMessage msg = new NpcHtmlMessage(5).setFile(filename);
					client.close(msg);
					return;
				}
			}
			client.close(Msg.ServerClose);
		}
	}

	public static void doDisconection(GameClient client)
	{
		removePlayer(client._prot_info);
	}

	public static boolean checkVerifyFlag(ProtectInfo pi, int flag)
	{
		int fl = Integer.reverseBytes(flag);

		if(fl == 0xFFFFFFFF)
		{
			_log.info(pi + ": Error Verify Flag.");
			return false;
		}

		if((fl & 0x10000000) != 0)
		{
			_log.info(pi + ": L2ext detect. flag: " + fl);
			return false;
		}

		if((fl & 0x01) != 0)
		{
			_log.info(pi + ": Sniffer detect. flag: " + fl);
			return false;
		}

		if((fl & 0x10) != 0)
		{
			_log.info(pi + ": Sniffer detect 2. flag: " + fl);
			return false;
		}

		if(fl == 0x50000000)
		{
			_log.info(pi + ": Error get net data client. flag: " + fl);
			return false;
		}

		return true;
	}
}
